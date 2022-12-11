#!/bin/bash
#
# This script sends local image file to Amazon Rekognition service for the label detection
#
# To run this script:
# chmod u+x detect-labels.sh
# ./detect-labels.sh
#
aws rekognition detect-labels --image-bytes fileb://mypicture.jpg --max-labels 3 --output json --query Labels[*].[Name,Confidence]