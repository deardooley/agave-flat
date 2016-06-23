#!/bin/bash -x

export myclasspath=target/profiles/WEB-INF

 EXPR="java -jar src/test/scripts/jetty-runner-8.1.9.v20130131.jar --jar target/profiles/WEB-INF/lib/c3p0-0.9.1.2.jar --port 8182 --path /profiles target/profiles.war &"
eval $EXPR
export PID=$!
echo $PID > src/test/scripts/pid.current
more src/test/scripts/pid.current
cp src/test/scripts/pid.current ../../pid.current
exit 0
