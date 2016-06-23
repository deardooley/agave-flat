#!/bin/bash

# assumes a previous maven run to install all jars into local maven repo
# run from the root of agave project
#  mvn -s config/maven/settings-SAMPLE.xml -f pom.xml  -P agave,plain  clean install
# usage: config/testbed/simulate-jenkins.sh <current project directory> <utest runner image to use> <user home dir running tests>
#  config/testbed/simulate-jenkins.sh `pwd` agaveapi/mvn-runner /root


WORKSPACE=$1
cd $WORKSPACE
export IMAGE=$2
export USER_HOME=$3

echo "image env var $IMAGE "
echo "users home dir $USER_HOME "

# docker-machine ssh slt-b2d df -h

# create the aggregate testreports folder
if [ ! -d "testreports" ]
then
     mkdir testreports
else
     # clean out testreports
     rm -rf testreports/*
fi


echo "*******************************************************************"
echo "* Notifications"
echo "*******************************************************************"
config/testbed/provision-utests.sh notifications  "mysql beanstalkd mongodb" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Profiles"
echo "*******************************************************************"
config/testbed/provision-utests.sh profiles  "mysql" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Metadata"
echo "*******************************************************************"
config/testbed/provision-utests.sh metadata  "mysql mongodb beanstalkd" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Systems "
echo "*******************************************************************"
config/testbed/provision-utests.sh systems  "mysql mongodb beanstalkd myproxy sftp irods irodspam irods4 http ftp" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Monitors"
echo "*******************************************************************"
config/testbed/provision-utests.sh monitors "mysql mongodb beanstalkd myproxy sftp irods irodspam irods4 http ftp" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Files"
echo "*******************************************************************"
config/testbed/provision-utests.sh files "mysql mongodb beanstalkd myproxy sftp irods irodspam irods4 http ftp" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Transforms"
echo "*******************************************************************"
config/testbed/provision-utests.sh transforms "mysql mongodb beanstalkd" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Apps"
echo "*******************************************************************"
config/testbed/provision-utests.sh apps "mysql mongodb" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Jobs"
echo "*******************************************************************"
config/testbed/provision-utests.sh jobs "mysql mongodb" testng.xml
sleep 10
echo ""

echo "*******************************************************************"
echo "* Realtime"
echo "*******************************************************************"
config/testbed/provision-utests.sh realtime "mysql mongodb" testng.xml
sleep 10
echo ""

sleep 10
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
# docker-machine ssh slt-b2d df -h
echo ""
echo "******* ending unit test run  $(date)  *******"
