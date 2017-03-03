#!/bin/sh

# If no config file was included in the environment, use the default
if [[ -z "$FLYWAY_CONFIG" ]]; then
  # use the flyway default location in case user just wants to drop
  # in the properties file and run the migration
  if [[ -e "$FLYWAY_HOME/conf/flyway.conf" ]]; then
      FLYWAY_CONFIG=$FLYWAY_HOME/conf/flyway.conf
  elif [[ -e "$HOME/flyway.conf" ]]; then
      FLYWAY_CONFIG=$HOME/flyway.conf
  elif [[ -e "flyway.properties" ]]; then
      FLYWAY_CONFIG=$(pwd)/flyway.conf
  fi
elif [[ ! -e "$FLYWAY_CONFIG" ]]; then 
	echo "No config file present at $FLYWAY_CONFIG. Unable to run migrations!" 1>&2 
fi

#
# Filter the config file properties with the values from
# the runtime environment.
#
 
# resolve the jdbc connection info from component variables if not specified directly
if [[ -z $MYSQL_URL ]]; then
  # if a host is provided, we can build the url out of that, otherwise
  # we will fall back on the maven settings
  MYSQL_HOST=${MYSQL_HOST:-mysql}
  MYSQL_PORT=${MYSQL_PORT:-3306}

  MYSQL_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/'?zeroDateTimeBehavior=convertToNull&amp;sessionVariables=FOREIGN_KEY_CHECKS=0&amp;relaxAutoCommit=true&amp;rtinyInt1isBit=false'
fi

sed -i 's#flyway.url=.*##g' $FLYWAY_CONFIG
echo "flyway.url=$MYSQL_URL" >> $FLYWAY_CONFIG

if [[ -n "$MYSQL_USERNAME" ]]; then
  sed -i 's#flyway.user=.*#flyway.user='$MYSQL_USERNAME'#' $FLYWAY_CONFIG
fi

if [[ -n "$MYSQL_PASSWORD" ]]; then
  sed -i 's#flyway.password=.*#flyway.password='$MYSQL_PASSWORD'#' $FLYWAY_CONFIG
fi

if [[ -n "$MYSQL_DATABASE" ]]; then
  sed -i 's#flyway.schemas=.*#flyway.schemas='$MYSQL_DATABASE'#' $FLYWAY_CONFIG
fi

if [[ -n "$BASELINE_VERSION" ]]; then
  sed -i 's#flyway.baselineVersion=.*#flyway.baselineVersion='$BASELINE_VERSION'#' $FLYWAY_CONFIG
fi

#
# Call the command passed into the Docker run command
#
eval "$@"