FROM agaveapi/php-api-base:alpine

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

ENV DOCUMENT_ROOT /var/www/html/public

COPY html /var/www/html

RUN cd /var/www/html && \
    composer install && \
    chown -R apache:apache .
