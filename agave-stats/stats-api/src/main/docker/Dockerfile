FROM agaveapi/php-api-base:alpine

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

ENV DOCUMENT_ROOT /var/www/html/public

COPY html /var/www/html

RUN cd /var/www/html && \
    sed -i 's#opcache.enable_cli=1#opcache.enable_cli=0#g' /etc/php/conf.d/opcache-recommended.ini && \
    composer install && \
    chown -R apache:apache .
