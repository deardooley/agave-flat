FROM agaveapi/java:7-maven

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

ENV FLYWAY_VERSION 4.1.1
ENV FLYWAY_HOME /source/flyway-$FLYWAY_VERSION

WORKDIR /source

RUN apk --update add bash && \
	wget https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/$FLYWAY_VERSION/flyway-commandline-$FLYWAY_VERSION.tar.gz && \ 

	# Now install the already built jar files into the container
	# local maven repository
	ls -al && \
	tar xzf ./flyway-commandline-$FLYWAY_VERSION.tar.gz && \ 

	ls -al $FLYWAY_HOME && \
	rm flyway-commandline-$FLYWAY_VERSION.tar.gz && \
	rm -rf $FLYWAY_HOME/drivers 

# Now move in the filtered data and migrations
# from the build.
COPY conf/flyway.conf 			$FLYWAY_HOME/conf/flyway.conf
COPY lib/** 					$FLYWAY_HOME/lib/
COPY sql 						$FLYWAY_HOME/sql
COPY drivers 					$FLYWAY_HOME/drivers
COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

WORKDIR $FLYWAY_HOME

ENTRYPOINT ["/docker-entrypoint.sh"]

CMD ["$FLYWAY_HOME/flyway", "info"]
