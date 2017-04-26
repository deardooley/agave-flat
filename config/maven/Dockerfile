#############################################################
#  Build image for java code
#  tag: agaveapi/java:8-maven instead
#
#  The image can be built by issuing this command from the 
#  directory containing this file:
#
#   docker build -t agaveapi/java:8-maven .
#
#############################################################

FROM jeanblanchard/java:8

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