#!/bin/bash

# cd $HOME/agave
#
# git pull
#
# git submodule update
#
# mvn -Dskip.integration.tests=true -Dmaven.test.skip=true -Dforce.check.update=false -Dforce.check.version=false -s config/maven/settings.xml clean install
# mvn -Dforce.check.version=false clean install

DIR=$( cd "$( dirname "$0" )" && pwd )
# deploy swagger docs to apach web root
mkdir /var/www/html/v2
chmod -R 755 /var/www/html/v2

rm -rf /var/www/html/v2/docs
ln -s $DIR/agave-apidocs/apidocs-api/target/docker/html /var/www/html/v2/docs
chmod -R 755 /var/www/html/v2/docs

# deploy php apps to apache web root
for i in auth postits logging tenants usage; do
	rm -rf /var/www/html/v2/$i
	ln -s $DIR/agave-$i/$i-api/target/docker/html /var/www/html/v2/$i
	chmod -R 755 /var/www/html/v2/$i
done

# deploy java webapps to tomcat
for i in apps jobs files metadata monitors notifications profiles systems transforms; do
	rm -rf $CATALINA_HOME/webapps/$i* $CATALINA_HOME/webapps/work/Catalina/localhost/$i
	cp $DIR/agave-$i/$i-api/target/*.war $CATALINA_HOME/webapps/;
done

# bounce tomcat
$CATALINA_HOME/bin/kill.sh
# $CATALINA_HOME/bin/startup.sh

#service tomcat restart
export JAVA_OPTS="-Djsse.enableCBCProtection=false $JAVA_OPTS"
tail -f $CATALINA_HOME/logs/catalina.out
