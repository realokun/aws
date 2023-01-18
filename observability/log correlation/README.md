# Introduction

This document explains the technique used for the implementation of the log correlation pattern in a distributed Product Catalog application.

As can be seen on the architecture [diagram](https://github.com/realokun/aws/blob/master/diagrams/ProductCatalogArch.png), a single request to the Product Catalog application gets processed by the sequence of services on AWS cloud:

1. Amazon Route53
2. Amazon CloudFront
3. Application Load Balancer
4. EC2 (Java presentaion application)
5. Amazon API Gateway REST API
6. AWS Lambda (Python business logc)

By correlating the log entries produced by these services, we are able to track the request propagation roundtrip. It is especially important for the steps 4, 5, and 6 that are the most likely sources of the application errors.

# Solution

The CloudFront distribution uses a JavaScript [function](generate_header_x-correlation-id.js), to inject a cutom header `x-correlation-id`the with generated UUID value.

