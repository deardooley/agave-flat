#!/bin/bash

source ~/.bashrc


# iRODS environment is initialized in the .bashrc file
# iinit is already initialized and available 

# These parameters are passed in by the service
sourcePath=$1
destPath=$2
#callbackUrl=$3

# Notify the service that the transform is starting

#curl -o /dev/null -O "$callbackUrl/TRANSFORMING" > /dev/null

# Do some work. In this case, tar up the source directory/file to a destination iRODS path
#ibun -cDtar $destPath $sourcePath

# if the file already has a tar extension, just move it, otherwise tar it
shopt -s nocasematch
if [ ${sourcePath: -4} == ".tar" ]; then
    mv $sourcePath $destPath
else
    tar -cf $destPath $sourcePath
fi



if [ "$?"  -ne 0 ]; then
#	curl -o /dev/null -O "$callbackUrl/TRANSFORMING_FAILED" > /dev/null
	exit 1;
fi

# Notify the service that the transform is completed
#curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null

exit 0;