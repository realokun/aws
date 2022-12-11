#!/bin/bash
#
# This script creates Amazon S3 pre-signed URL for the object soma_smoothie.jpg stored in product-catalog-images bucket. The URL expires in 15 seconds.
#
# To run this script:
# chmod u+x presign.sh
# ./presign.sh
#
aws s3 presign s3://product-catalog-images/soma_smoothie.jpg --expires-in 15