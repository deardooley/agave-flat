#!/bin/bash

source ~/.bashrc

# iRODS environment is initialized in the .bashrc file
# iinit is already initialized and available 

# These parameters are passed in by the service
system=$1
sourcePath=$2
destPath=$3
#callbackUrl=$4

# Notify the service that the transform is starting

# Notice that we ALWAYS check for the existence of callbackURL
# but we don't check that it was valid. 
#if [ -n $callbackUrl ]; then
#	curl -o /dev/null -O "$callbackUrl/TRANSFORMING" > /dev/null
#fi


# unpack the archive in iRods system
#ibun -x $sourcePath $destPath
mkdir $destPath
tar -xf $sourcePath -C $destPath

if [ "$?"  -ne 0 ]; then
	exit 1;
fi

# Notify the service that the transform is completed
if [ -n $callbackUrl ]; then
	curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null
fi

exit 0;