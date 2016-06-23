#!/bin/bash
echo "docker image setup script"
# we need to be in the *-core project directory for this script to run
echo " working directory is : `pwd`"

# move all the files for running tests into the staging directory target/docker/agave
mkdir -p target/docker/agave

# tar up the project to add to container
tar -czvf ../agave.tar.gz .

# move the agave tar to target for image build
mv ../agave.tar.gz target/docker

# need to copy src/test/docker files into target/agave
cp  src/test/docker/*  target/docker

# cd to docker directory
cd target/docker

echo " building the image for the unit test container"
# create image for unit tests for server with all the correct files in place
docker build  --rm --no-cache=true -t agaveapi/utest-apps .

