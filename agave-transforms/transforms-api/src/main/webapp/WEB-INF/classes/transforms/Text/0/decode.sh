#!/bin/bash

# These parameters are passed in by the service
sourcePath=$1
destPath=$2
callbackUrl=$3

icp -V $sourcePath $destPath

# Notify the service that the transform is completed
 curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null

exit 0;
