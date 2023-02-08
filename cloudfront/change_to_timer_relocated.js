/*
* This function simply replaces the target page in the request.
* To see it in action, navigate to: https://d1f28333hybq4l.cloudfront.net/timer.html
*/
function handler(event) {
    var request = event.request;
    request.uri = request.uri.replace("timer.html", "timer_relocated.html");
    return request;
}