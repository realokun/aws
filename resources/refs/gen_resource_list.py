import json
import boto3
from boto3.dynamodb.conditions import Key
import pathlib
import shortuuid

dynamodb = boto3.resource('dynamodb')

# This function creates unique references for each Amazon S3 object that
# matches the criteria specified in the event, e.g:
# {
#   "title": "Victor's supplemental course materials",
#   "bucket": "my-favorite-things",
#   "prefix": "resources/courses/"
# }

# The value of the 'title' attribute is simply carried over to the response, e.g:
# {
#   "title": "Victor's supplemental course materials",
#   "content": [
#     {
#       "name": "Advanced Developing on AWS",
#       "ref": "3McmkZgbonz5"
#     },
#     {
#       "name": "Developing on AWS",
#       "ref": "3L7VQ8TxHVje"
#     }
#   ]
# }
#
def lambda_handler(event, context):

    bucket_name = event['bucket']
    prefix = event['prefix']
    
    print("Processing request for objects in bucket='{}', prefix='{}'".format(bucket_name, prefix))

    response = {}
    response['title']=event['title']
    
    # Provide references for every object that matches the location criteria
    content=[]
    
    s3 = boto3.client('s3')     
    objects = s3.list_objects_v2(
        Bucket=bucket_name, 
        Prefix=prefix 
    ) ['Contents']
    
    for obj in objects:
        key = obj['Key']
        # Exclude the prefix itsefl
        if key != prefix:
            item={}
            item['name'] = pathlib.Path(key).stem
            item['ref'] = get_reference(bucket_name, key)
            print("Reference has been processed: {}".format(item))
            content.append(item)

    response['content']=content

    return response
    
# This function returns a unique reference associated with the given object    
def get_reference(bucket_name, object_key):
    
    # Try to find the object in the DB
    table = dynamodb.Table("References")
    resource = "{}#{}".format(bucket_name, object_key)

    ref = ''
    try:    
        response = table.query(IndexName="res-index",KeyConditionExpression=Key('res').eq(resource))
        response_count = response['Count']
        if(response_count == 0): 
            # The resource is not in the DB yet, let's add it 
            print("Processing resource {}".format(resource))
            ref = shortuuid.ShortUUID().random(length=12)
            table.put_item(
                Item={
                    'ref': ref,
                    'res': resource,
                    'hits': 0,
                }
            )
        elif(response_count == 1):
            ref = response['Items'][0]['ref']
        else:
            raise Exception("Found multiple entries for object='{}'".format(resource))
    
    except Exception as ex:
        print(ex)
        raise ValueError("[BadRequest] Unable to complete request. Cause: {}".format(ex))        

    return ref 