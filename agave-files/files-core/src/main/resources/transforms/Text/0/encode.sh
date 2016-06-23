#!/bin/bash

source ~/.bashrc


# iRODS environment is initialized in the .bashrc file
# iinit is already initialized and available 

# These parameters are passed in by the service
sourcePath=$1
destPath=$2
callbackUrl=$3


curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null

exit 0;
