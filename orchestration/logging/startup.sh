#!/bin/bash

# copy the template for population
cp config/harvester.conf.SAMPLE config/harvester.conf

# update the hostname for publishing node info
sed -i 's/HOSTNAME/docker.example.com/' config/harvester.conf

# look up all active containers and replace the container id with the
# image id to match the stream from the docker daemon harvester
for i in `docker ps | grep -v '^CONTAINER' | awk '{print $NF}'`;
do
  container_info=$(docker inspect $i)
  container_id=$(echo "${container_info}" | jq -r '.[0].ID')
  container_image=$(cat "${container_info}" | jq -r '.[0].Config.Image')

  sed -i 's#'$i'#'$container_image'#' config/harvester.conf

done

# cat for sanity checking
#cat config/harvester.conf

# run the compose file to bring the logio harvesters and server up
docker up -d
