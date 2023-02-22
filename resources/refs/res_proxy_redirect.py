import boto3
from botocore.exceptions import ClientError
import json
import os

# This function is used as a proxy integration with API Gateway.
# It uses the path parameter 'ref' to locate the referenced S3 resource in the DB.
# If generates the presigned S3 URL for the referenced S3 resource and returns HTTP 302
# redirect to the generated URL.
def lambda_handler(event, context):
    
    print(json.dumps(event))
    
    # Validate the request
    if 'pathParameters' not in event or 'ref' not in event['pathParameters']:
        raise Exception("Expected path parameter 'ref' was not found in the request.")
    
    ref = event['pathParameters']['ref']
    print('Resolving reference {}'.format(ref))
    
    # Try to locate the reference in the DB
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table("References")
    result = table.get_item(
        Key={
            'ref': ref
        }
    )

    # Unable to locate the reference
    if 'Item' not in result:
        raise ValueError("Reference {} does not exist.".format(ref))   
        
    print(result['Item'])
    
    # Extract the S3 properties from the resource
    resource = result['Item']['res']
    parts = resource.split('#')
    bucket = parts[0] 
    object = parts[1] 
    
    # Update the value of the hit counter
    table.update_item(
        Key={'ref': ref},
        UpdateExpression='SET #attr = #attr + :inc',    
        ConditionExpression="attribute_exists(#attr)",      
        ExpressionAttributeNames={
            "#attr": "hits"
        },        
        ExpressionAttributeValues={
            ':inc': 1
        },
        ReturnValues="NONE"
    )

    # Generate the S3 pre-signed url
    s3 = boto3.client('s3')
    print("Generating presigned URL for bucket={} and object={}".format(bucket, object))
    presigned_url = s3.generate_presigned_url(
        ClientMethod='get_object',
        Params= {
            'Bucket': bucket,
            'Key': object
        }, 
        ExpiresIn=os.environ['EXPIRATION_SEC']
    )   
    print(presigned_url)

    return {
        'statusCode': 302,
        "isBase64Encoded": 'false',
        'headers': {"Location": presigned_url}     
    }