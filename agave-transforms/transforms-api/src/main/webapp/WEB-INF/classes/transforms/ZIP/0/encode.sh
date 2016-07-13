#!/bin/bash

# These parameters are passed in by the service
sourcePath=$1
destPath=$2
callbackUrl=$3

shopt -s nocasematch
if [ ${sourcePath: -4} == ".zip" ]; then
    mv $sourcePath $destPath
else
    zip $destPath $sourcePath
fi

if [ "$?"  -ne 0 ]; then
	curl -o /dev/null -O "$callbackUrl/TRANSFORMING_FAILED" > /dev/null
	exit 1;
fi  

# Notify the service that the transform is completed
curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null

exit 0;