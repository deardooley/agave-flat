#!/bin/bash
echo "docker image setup script"
# we need to be in the *-core project directory for this script to run
echo " working directory is : `pwd`"

# cd to docker directory
cd target/docker

# stop the container if running
docker stop utest-apps

# remove utest-apps container if exists
docker rm utest-apps

# create and run container that runs the unit tests by runtests script.
docker run --name utest-apps -v `pwd`/test-output/:/tmp/test-output/ -d agaveapi/utest-apps /runtests

# log the container output
docker logs --follow=true utest-apps

echo "wait on the tests to complete"
# give the tests time to run
docker wait utest-apps

RSLT=$?
echo "the container exited with value : $RSLT"

echo "copy the test-results folder into docker host file system"
# copy test results to docker host
docker cp utest-apps:/tmp/test-output/ target/testoutput

exit $RSLT


# export skiptests=" -s config/maven/settings.xml -Dskip.docker.build=true -DskipTests=true -Dmaven.test.skip=true -Dforce.check.update=false -Dforce.check.version=false"
# export runtests=" -s config/maven/settings.xml -Dskip.docker.build=true -DskipTests=true -Dmaven.test.skip=false -Dforce.check.update=false -Dforce.check.version=false"

# mvn $skiptests clean -pl :apps-core
# mvn $runtests test-compile -pl :apps-core
# mvn $skiptests package -pl :apps-core
# mvn $skiptests pre-integration-test -pl :apps-core

# run these with -s ../../config  @ apps-core level
# mvn $skiptests clean
# mvn $runtests test-compile
# mvn $skiptests package
# mvn $skiptests pre-integration-test

