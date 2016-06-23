FROM agaveapi/java:7-maven

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY source /source

WORKDIR /source/agave-migrations

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD ["flyway:info"]