import boto3
import base64
import gzip
import json
import os
import re
# the following dependencies are provided with the Lambda Layer:
import pyshorteners

sns = boto3.resource('sns')

"""
This function is a destination for the CloudWatch event log subscription filter.
It receives an event in case if the filter has selected an error message from the
application log. It sends an SMS notification with the shortened link to the
Cloud Watch Insights query to analyze the issue.
"""
def lambda_handler(event, context):
    
    print("Event received: " + json.dumps(event)) 
    
    # fetch the Base64 encoded and compressed data from the event
    cw_data = event['awslogs']['data']
    # decode the data
    compressed_data = base64.b64decode(cw_data)
    # decompress and extract the data
    data = gzip.decompress(compressed_data)
    
    data_dict = json.loads(data)
    print("Data extracted: " + json.dumps(data_dict))
    
    # loop through the logEvents array
    for event in data_dict['logEvents']:
        event_message = event['message']
        print("Event message extracted: {}...".format(event_message[0:100]))
    
        # look for a UUID in the log entry. For simplicity, we assume that the first found UUID
        # represents a log correlation id. The more reliable approach would be to use structured
        # log message format
        match = re.search('(\w{8}(-\w{4}){3}-\w{12}?)', event_message)
        
        # select the suitable query for Amazon CloudWatch Log Insights
        long_url = os.environ['CW_LOG_INSIGHTS_GEN_QUERY']
        msg = "An error has occurred in the ProductCatalogUI application. "
        if (match is not None):
            # for correlated log entry
            log_correlation_id = match.group()
            print("Found log correlation id: " + log_correlation_id)
            msg += "Log correlation id: {}. ".format(log_correlation_id)
            long_url = os.environ['CW_LOG_INSIGHTS_CORRELATED_QUERY'].format(log_correlation_id)
    
        # shorten the link, see https://www.askpython.com/python/examples/url-shortener
        print("The long link: " + long_url)
        type_tiny = pyshorteners.Shortener()
        short_url = type_tiny.tinyurl.short(long_url)
        print("The shortened link: " + short_url)

        msg += 'Please check the application logs: {}'.format(short_url)
        print(msg)

        # publish the application error notification to an SNS topic
        topic = sns.Topic(os.environ['SNS_TOPIC'])
        response = topic.publish(
            Message=msg,
            MessageAttributes={
                "error_notification": {
                    "DataType": "String",
                    "StringValue": "true"
                }
            }    
        )