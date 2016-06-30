#!/bin/bash

# assumes a previous maven run to install all jars into local maven repo
# run from the root of agave project
#  mvn -s config/maven/settings-SAMPLE.xml -f pom.xml  -P agave,plain  clean install
# usage: config/testbed/onerun-simulate-jenkins.sh <workdir> <module> <container list> <utest runner image to use> <users home dir that is running tests>
#  config/testbed/onerun-simulate-jenkins.sh `pwd` notifications "mysql beanstalkd mongodb" agaveapi/mvn-runner /home/apim

WORKSPACE=$1
cd $WORKSPACE
export IMAGE=$4
export USER_HOME=$5

module=$2
containers=$3
echo $containers
echo "image env var $IMAGE "
echo "users home dir $USER_HOME "

# create the aggregate testreports folder
if [ ! -d "testreports" ]
then
     mkdir testreports
else
     echo " clean out testreports "
     rm -rf testreports/"$module"*/*
fi

echo "*******************************************************************"
echo "* $module  "
echo "*******************************************************************"
config/testbed/provision-utests.sh $module "$containers" testng.xml
sleep 10
echo ""

echo " ***** cleaning up after test run ****** "
docker ps
echo " stopping unit test containers if any  "
docker stop $(docker ps -f label=agaveapi.test -q)
sleep 10

echo " removing stopped unit test containers "
docker rm -f $(docker ps -f label=agaveapi.test -aq)
sleep 10

echo " clean up docker space "
docker run -v /var/run/docker.sock:/var/run/docker.sock -v /var/lib/docker:/var/lib/docker --rm martin/docker-cleanup-volumes
echo "  disk space :  "
        df -h
echo ""

echo "******* ending unit test run  $(date)  *******"
