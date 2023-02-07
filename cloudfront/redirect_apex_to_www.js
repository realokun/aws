/*
* This Amazon CloudFront function performs 301 Moved Permanently redirect of GET requests made to the
* apex (root) domain to www subdomain. It preserves the original path and the query string.
*
* For simplicity sake, the other types of requests are not handled here. E.g. for POST methods, 
* a 308 Permanent Redirect must be used instead, as the method change is explicitly prohibited with this status.
*/
function handler(event) {
    
    // Refer to Amazon CloudFront Functions event structure: 
    // https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/functions-event-structure.html
    var request = event.request;
    var uri = request.uri;
    var querystring = request.querystring;

	// When GET request to the apex (root) domain is received
	if(request.method === 'GET' && request.headers.host.value === 'cloud101.link') {
		// Create a redirect response to the www subdomain instead  
		var newurl = 'https://www.cloud101.link'
		// Add uri path if present in the original request
		if (uri) {
		    newurl += uri
		}
		// Add query parameters if present in the original request
		if (Object.keys(querystring).length > 0) {
		    newurl += '?'
		    for (var param in querystring) {
		        newurl = newurl.concat(param).concat('=').concat(querystring[param].value).concat('&')
		    }
		    // Knock off the trailing '&'
		    newurl = newurl.slice(0, -1)
		}
		// Return the redirect response
        return {
        	statusCode: 301,
            statusDescription: 'Moved Permanently',
            headers:
              { "location": { "value": newurl } }
        }
	}
	// Return the original request
    return request;
}