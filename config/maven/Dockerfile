#############################################################
#  Build image for java code
#  tag: agaveapi/java:7-maven instead
#
#############################################################

FROM jeanblanchard/java:7

MAINTAINER Rion Dooley <dooley@tacc.utexas.edu>

RUN apk --update add zip git curl && \
	cd /usr/share && \
    wget ftp://mirror.reverse.net/pub/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip && \
    unzip apache-maven-3.3.9-bin.zip && \
    ln -s /usr/share/apache-maven-3.3.9/bin/mvn /usr/bin/mvn && \
    rm apache-maven-3.3.9-bin.zip && \
    mkdir /sources

WORKDIR /sources

VOLUME /sources

CMD mvn clean package