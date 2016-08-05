#!/bin/sh

# variables
core_module=agave-$MODULE/$MODULE-core
suite=target/test-classes/$SUITE
user=`whoami`
echo ""
echo " *****  suite:       $suite    *****"
echo ""
echo " *****  MODULE:      $MODULE   *****"
echo ""
echo " *****  core_module: agave-$MODULE/$MODULE-core   *****"
echo ""
echo " *****  SUITE:       $SUITE    *****"
echo ""
echo " *****  USER:        $user     *****"
echo ""
echo "current directory : `pwd`"
echo "$(ls -la ~ | grep .m2)"
echo ""


# run the flyway plugin to prepare an empty database
  mvn -Dskip.migrations=false -s config/maven/settings-SAMPLE.xml -f agave-migrations/pom.xml flyway:migrate
  sleep 10

  mvn -s config/maven/settings-SAMPLE.xml -f $core_module/pom.xml  -Dsuite.testng=$suite -P agave,utest  test
  sleep 10

  touch "$MODULE".end