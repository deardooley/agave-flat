#!/bin/sh

# If no config file was included in the environment, use the default
if [[ -z "$FLYWAY_CONFIG" ]]; then
  # use the flyway default location in case user just wants to drop
  # in the properties file and run the migration
  if [[ -e "/source/agave-migrations/flyway.properties" ]]; then
      FLYWAY_CONFIG=/source/agave-migrations/flyway.properties
  else
      FLYWAY_CONFIG=flyway.properties
  fi
fi

# Create the config file if it does not already exist
if [[ ! -e "$FLYWAY_CONFIG" ]]; then
  touch $FLYWAY_CONFIG

  # resolve the jdbc connection info from the environment if present
  if [[ -z $MYSQL_URL ]]; then
    # if a host is provided, we can build the url out of that, otherwise
    # we will fall back on the maven settings
    if [[ -n "$MYSQL_HOST" ]]; then
      echo "flyway.url=jdbc:mysql://$MYSQL_HOST/?zeroDateTimeBehavior=convertToNull&amp;sessionVariables=FOREIGN_KEY_CHECKS=0&amp;relaxAutoCommit=true&amp;tinyInt1isBit=false" > $FLYWAY_CONFIG
    fi
  else
      echo "flyway.url=$MYSQL_URL" >> $FLYWAY_CONFIG
  fi

  if [[ -n "$MYSQL_USER" ]]; then
    echo "flyway.user=$MYSQL_USER" >> $FLYWAY_CONFIG
  fi

  if [[ -n "$MYSQL_PASSWORD" ]]; then
    echo "flyway.password=$MYSQL_PASSWORD" >> $FLYWAY_CONFIG
  fi

  if [[ -n "$MYSQL_DATABASE" ]]; then
    echo "flyway.schemas=$MYSQL_DATABASE" >> $FLYWAY_CONFIG
  fi

  if [[ -n "$BASELINE_VERSION" ]]; then
    echo "flyway.baselineVersion=$BASELINE_VERSION" >> $FLYWAY_CONFIG
  fi

fi

# clear out submodules in parent folder to keep build tight
sed -i 's#<module.*##g' /source/pom.xml
sed -i 's#</modules.*##g' /source/pom.xml

# finally run the migration command
eval "mvn -s ../config/maven/settings-SAMPLE.xml -Dflyway.configFile=${FLYWAY_CONFIG} -P agave,sandbox -Dskip.migrations=false $@"
