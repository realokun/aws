# Introduction

This document explains the technique used for the implementation of the log correlation pattern in a distributed Product Catalog application.

As can be seen on the architecture [diagram](https://github.com/realokun/aws/blob/master/diagrams/ProductCatalogArch.png), a single request to the Product Catalog application gets processed by the sequence of services on AWS cloud:

1. Amazon Route53
2. Amazon CloudFront
3. Application Load Balancer
4. EC2 (Java presentation application)
5. Amazon API Gateway REST API
6. AWS Lambda (Python business logic)

By correlating the log entries produced by these services, we are able to track the request propagation roundtrip. It is especially important for the steps 4, 5, and 6 that are the most likely sources of the application errors.

# Solution

The CloudFront distribution uses a JavaScript [function](generate_header_x-correlation-id.js), to inject a custom header `x-correlation-id` with generated UUID value.

The EC2-based Java application uses the value of the `x-correlation-id` header to prefix every log entry it produces as can be seen in the class [CorrelatingLogger.java](https://github.com/realokun/aws/blob/master/application/ProductCatalogUI/src/main/java/com/aws/vokunev/prodcatalog/util/CorrelatingLogger.java).



