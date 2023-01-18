/*
* This Amazon CloudFront function injects x-correlation-id header with UUID value to the request,
* unless the request already contains it. The value of this header is used as a log correlation ID by
* the downstream services. 
*
* This function needs to be associated with the Viewer Request in the respective CloudFront distribution.
*
* Since the CloudFront built-in Crypto module doesn't provide randomUUID() method, we generate UUID
* based on the less efficient Math.random() as per: https://hackernoon.com/using-javascript-to-create-and-generate-uuids
*
* The original idea was to add x-amzn-requestid header as it is supported by
* various services, like API Gateway. However it is included in the list of restricted headers, as per:
* https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions-restrictions.html
*
* According to CloudFront policy, the header names should be in lowercase.
*/
function handler(event) {
    
    var HEADER_NAME = 'x-correlation-id';
    var request = event.request;

    if (!request.headers[HEADER_NAME]) {
        // Add the x-amzn-requestid header to the incoming request
        // if the client didn't provide one
        request.headers[HEADER_NAME] = {value: uuid()};
    }    

    return request;
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}