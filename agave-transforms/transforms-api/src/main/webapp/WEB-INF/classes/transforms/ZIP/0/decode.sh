#!/bin/bash

# These parameters are passed in by the service
sourcePath=$1
destPath=$2
callbackUrl=$3

# Notify the service that the transform is starting

# Notice that we ALWAYS check for the existence of callbackURL
# but we don't check that it was valid.
if [ -n $callbackUrl ]; then
	curl -o /dev/null -O "$callbackUrl/TRANSFORMING" > /dev/null
fi

mkdir $destPath
unzip $sourcePath -d $destPath

# Notify the service that the transform is completed
if [ -n $callbackUrl ]; then
	curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null
fi

exit 0;
