FROM agaveapi/php-api-base:alpine

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

COPY html /var/www/html/docs

RUN  mkdir -p /app/config && \
	 rm /var/www/html/index.php

# This file will get sourced by the docker-entrypoint.sh script when the container
# starts up, thereby updating the swagger doc path
COPY changehostname.sh /app/config/environment.sh 

