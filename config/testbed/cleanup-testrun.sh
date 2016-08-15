#!/usr/bin/env bash


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