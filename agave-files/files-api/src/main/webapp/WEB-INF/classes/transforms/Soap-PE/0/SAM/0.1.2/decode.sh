#!/bin/bash

source ~/.bashrc

# iRODS environment is initialized in the .bashrc file
# iinit is already initialized and available 

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

# $tmpLocal is the local copy of the file from sourcePath
tmpLocal=/tmp/`date +%s | bc`
tmpResult=/tmp/$RANDOM

# -N 4 specifies the number of iRODS threads
# This is hard-coded now and we have no idea if this is
# the most efficient value. We could set it in .iplantrc
# or we could change it dynamically based on the file
# size of $sourcePath
iget -N 4 -V $sourcePath $tmpLocal 
if [ "$?" -ne "0" ]; then
	if [ -n $callbackUrl ]; then
		curl -o /dev/null -O "$callbackUrl/TRANSFORMING_FAILED" > /dev/null
	fi
	exit 1;
fi

# SOAP pair-end alignment to SAM
./soap2sam.pl -p $tmpLocal > $tmpResult

# Now, stage $tempLocal back out to $destPath
iput -N 4 -f -V $tmpLocal $destPath 
if [ "$?" -ne "0" ]; then
	if [ -n $callbackUrl ]; then
		curl -o /dev/null -O "$callbackUrl/TRANSFORMING_FAILED" > /dev/null
	fi
	exit 1;
fi

rm $tmpLocal
rm $tmpResult

# Notify the service that the transform is completed
if [ -n $callbackUrl ]; then
	curl -o /dev/null -O "$callbackUrl/TRANSFORMING_COMPLETED" > /dev/null
fi

exit 0;