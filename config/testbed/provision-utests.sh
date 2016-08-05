#!/usr/bin/env bash
# usage run-utests.sh "module" ie. profiles and the containers to run ie. "mysql beanstalk"
# assumes the mysql host is defined in the /etc/hosts file
# working dir agave project root
# usage :  <agave_proj_root>/config/testbed/provision-utests.sh <module:the agave module> "<containers:list of compose services>" <suite:testng suite> <image:image for test runner>
# usage :  <agave_proj_root>/config/testbed/provision-utests.sh monitors "mysql beanstalkd mongodb ftp" testng-remote.xml agaveapi/mvn-runner
#
# variables
testrun_log=testrun.log
echo "" > $testrun_log
module=$1
core_module=agave-$module/$module-core
suite=$3
containers=$2



echo "1 module     : $module"
echo "2 containers : $containers"
echo "3 suite      : $suite"
# options are currently agaveapi/mvn-runner or slave-mvn-runner:1.0
# the first is for use with Mac workstation using docker toolbox setup
# the second is for use with Jenkins distributed build
echo " image for utest runner      : $IMAGE"
echo " users home directory        : $USER_HOME"

function error_exit
{
      logging "end this failed session ..."
      logging  ""
      exit 1
}

function test_exit
{
    logging "end this unit test run ..."
    docker-compose -p $module -f config/testbed/unit-test.yml down
    exit 0
}

function logging
{
    echo "`date` $1"  >> $testrun_log
}

# begin log for unit test session
  logging "begin session `date` ..."

# make sure the module end flag file is not there
rm -f "$module".end

# start the compose file
  logging "  start the compose file with datastores "

  # set MODULE environment variable for utest compose container
  export MODULE=$module
  export SUITE=$suite
  export COMPOSE_HTTP_TIMEOUT=90

  echo " in provistion-utest IMAGE     : $IMAGE  "
  echo " in provision-utest  USER_HOME : $USER_HOME "
  echo " in provision-utest  MODULE    : $MODULE "
  echo " in provision-utest  SUITE     : $SUITE  "

  # start up all supporting containers here sans build container
  docker-compose -f config/testbed/third-party.yml -p $module up -d $containers
  # let's see'um
  echo " show us the containers running ..."
  docker ps
  sleep 20

  # Once the supporting containers have started
  # start the utest container to run the unit tests
  echo " startup the test runner container "
  docker-compose -f config/testbed/third-party.yml -p $module up -d utest

  # poll for module end flag file
  turns=0
  while [ ! -f "$module".end ]; do
      echo "   **** polling for $module.end $turns ****"
      if [ $turns -lt 10 ]
      then
         (( turns++ ))
         sleep 10
      else
         echo " haven't found end file time to abort"
         break
      fi
  done
  echo "  unit tests end file found... or timed out moving on ... "
  echo ""

  # test results to aggregate location
  cp -R $core_module/target/surefire-reports testreports/$module-$suite
  cp_result=$?
  if [ $cp_result == 0 ]; then
    echo "  Successfully copied test results "
  else
    echo "  Failed to copy test results"
  fi

  # stop the docker containers
  echo " stop third-party.yml services from provision-utests ...."
  docker-compose -p $module -f config/testbed/third-party.yml stop
  sleep 10

  echo " ***** log from test container test run ****** "
  echo " log results $module_utest_1 "
  echo "";echo "";echo ""
  echo "***************   container log  begin  ***************"
  docker  logs "$module"_utest_1
  echo "";echo "";echo ""
  echo "***************   container log  end    ***************"
  sleep 10

  echo " remove third-party.yml services from provision-utests ...."
  docker-compose -p $module -f config/testbed/third-party.yml rm -f
  sleep 10

  rm -f "$module".end

trap test_exit ERR
