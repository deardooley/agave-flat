#
# build image named agave/utest-apps
# $> docker build --force-rm=true --no-cache=true -t agaveapi/utest-apps .
#
FROM agaveapi/unit-test
MAINTAINER Steve Terry <sterry1@tacc.utexas.edu>

# add runtests.sh script
ADD runtests /runtests
RUN chmod 755 /runtests

ADD agave.tar.gz /agave

CMD ["/runtests"]

