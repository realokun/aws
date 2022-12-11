#!/bin/bash
#
# This script uses Amazon Translate service to translate text from English to Spanish
#
# To run this script:
# chmod u+x translate-text.sh
# ./translate-text.sh
#aws translate translate-text --text "Hello and welcome to Amazon Cloud" --output json --source-language-code en --target-language-code es 