## Introduction

This document explains the implementation of the log correlation pattern in a distributed Product Catalog application.

As can be seen on the architecture [diagram](https://github.com/realokun/aws/blob/master/diagrams/ProductCatalogArch.png), a single request to the Product Catalog application gets processed by the sequence of services on AWS cloud:

1. Amazon Route53
2. Amazon CloudFront
3. Application Load Balancer
4. EC2 (Java presentation application)
5. Amazon API Gateway REST API
6. AWS Lambda (Python business logic)

By correlating the log entries produced by these services, we are able to track the request propagation roundtrip. It is especially important for the steps 4, 5, and 6 that are the most likely sources of the application errors.

## Solution

1. The CloudFront distribution uses a JavaScript [function](generate_header_x-correlation-id.js), to inject a custom HTTP header `x-correlation-id` with UUID value. This value will be used as a Log Correlation ID by the downstream services.
2. The custom header `x-correlation-id` passes through the Application Load Balancer without any changes. 
3. The EC2-based Java application extracts the value of the received `x-correlation-id` header to prefix every log entry it produces with [CorrelatingLogger.java](https://github.com/realokun/aws/blob/master/application/ProductCatalogUI/src/main/java/com/aws/vokunev/prodcatalog/util/CorrelatingLogger.java).
4. The application logs get shipped to the CloudWatch log group `/Application/ProductCatalog/appplication` by the Cloud Watch agent installed on the EC2 instance. This [manual](https://github.com/realokun/aws/tree/master/observability/shipping%20EC2%20logs) explains all the required configuration steps.
5. The CloudWatch Log group `/Application/ProductCatalog/appplication` has a Subscription Filter attached with the filter pattern `"Exception"`. The Lambda [function](log_subscription_filter.py) used as the filter destination, extracts the Log Correlation ID from the error message and manufactures a link to the CloudWatch Log Insights with the query across multiple log groups. This link is then sent to an SNS topic to be delivered to an operations engineer. The query uses the value of the Log Correlation ID to select all the matching log messages, produced by different parts of the application, e.g.
```
fields @log, @message | filter @message like "5303d3aa-37ee-45b5-a4c4-d318cb7655c2" | sort by @timestamp asc
```
5. When making REST API request for the application data with [ApiAccessor.java](https://github.com/realokun/aws/blob/master/application/ProductCatalogUI/src/main/java/com/aws/vokunev/prodcatalog/dao/ApiAccessor.java), the application copies the value of the Log Correlation ID into the standard AWS HTTP header `x-amzn-requestid`.
6. API Gateway uses the value of the received `x-amzn-requestid` header to prefix the execution log entries, as documented in [CloudWatch log formats for API Gateway](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-logging.html).
7. API Gateway uses the Integration Request [mapping template](api_gateway_transformation_template.json) to pass the value of the Log Correlation ID as part of the event payload to downstream Lambda function.
8. The [Lambda function](lambda_get_product.py) uses the received value of the Log Correlation ID to prefix every log entry it produces.