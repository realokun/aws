#!/bin/bash
#
# This script creates ZIP file that can be uploaded to AWS Lambda service as a Python layer.
# This layer contains pyshorteners, a Python lib to help you short and expand urls using the most
# famous URL Shorteners availables.
#
# To run this script:
# chmod u+x make_layer.sh
# ./make_layer.sh
#
# Create new directory. It has to be named python.
mkdir python
# Install the packages you need, e.g.
pip3 install pyshorteners -t python/
# Create ZIP archive containing python directory
zip -r py_layer.zip python/