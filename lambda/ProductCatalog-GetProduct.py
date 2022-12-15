import boto3
import os
import json
import decimal
from botocore.client import Config
from codeguru_profiler_agent import with_lambda_profiler
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

dynamodb = boto3.resource('dynamodb')
cloudwatch = boto3.client('cloudwatch')

"""
Set the addressing_style to 'path' when using s3v4 signature version, 
see https://github.com/boto/boto3/issues/1644#issuecomment-451611263 for details
"""
s3_client = boto3.client('s3', config=Config(s3={'addressing_style': 'path'}, signature_version='s3v4'))

# This function uses resource API and performs proper error handling
@with_lambda_profiler()
def lambda_handler(event, context):
    
    # Include this id into each log record
    logCorrelationId = context.aws_request_id
    if 'context' in event:
        logCorrelationId = event['context']['logCorrelationId']
    
    logger.info("({}) Event received: {}".format(logCorrelationId, event))

    try:    
        # Validate input
        if 'productId' not in event:
            raise ValueError("Validation error: Missing required parameter [productId]")
        if 'environment' not in event:
            raise ValueError("Validation error: Missing required parameter [environment]")        
        
        # Use the environment-specific table name
        tableName = os.environ[event['environment']]
        table = dynamodb.Table(tableName)
    
        logger.info("({}) Querying table {} for productId={}".format(logCorrelationId, table.table_name, event['productId']))
    
        # Convert input data to numeric
        productId = int(event['productId'])
        
        if productId == -1:
            # Test case, return a first available item
            result = table.scan(
                Limit=1
            )

            item = result['Items'][0]
            
        else:
            # Regular case, retrieve an item by key
            result = table.get_item(
                Key={
                    'Id': productId
                }
            )
        
            # Item was not found
            if 'Item' not in result:
                raise ValueError("A product with Id={} does not exist.".format(productId))   
        
            item = result['Item']

        # If the image is provided, generate the pre-signed url
        if 'Image' in item:
            presigned_url = s3_client.generate_presigned_url(
                ClientMethod='get_object',
                Params= {
                    'Bucket': item['Bucket'],
                    'Key': item['Image']
                }, 
                ExpiresIn=os.environ['EXPIRATION_SEC']
            )       
            
            item['Image'] = presigned_url
   
        item['logCorrelationId'] = logCorrelationId
        
        # Publish custom metrics to CloudWatch
        publishMetrics(productId, event['environment'])

    except Exception as ex:
        logger.error("({}) {}".format(logCorrelationId, ex))
        raise ValueError("({}) [BadRequest] Unable to complete request. Cause: {}".format(logCorrelationId, ex))    
        
    logger.info("({}) Returning result: {}".format(logCorrelationId,item))

    return item
    
def publishMetrics(productId, environment):
    
    cloudwatch.put_metric_data(
        MetricData = [
            {
                'MetricName': 'Product Catalog',
                'Dimensions': [
                    {
                        'Name': 'ENVIRONMENT',
                        'Value': environment
                    }
                ],
                'Unit': 'None',
                'Value': productId
            },
        ],
        Namespace = 'Product Catalog'
    )
    
    return None

# A decimal type encoder for json.dumps()
class DecimalEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, decimal.Decimal):
            if "." not in str(o): 
                return int(o)
            else:    
                return float(o)
        return super(DecimalEncoder, self).default(o)    