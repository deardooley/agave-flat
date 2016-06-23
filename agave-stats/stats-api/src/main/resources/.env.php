<?php

function env_or_default($varname, $default='') {
    if (empty($varname)) {
        return $default;
    } else {
        $envvarname = strtoupper($varname);
        $envvarname = str_replace('.', '_', $envvarname);
        $val = getenv($envvarname);
        return (empty($val) ? $default : $val);
    }
}


return array(

    // what is the version of the api?
    'version' => env_or_default('IPLANT_SERVICE_VERSION', '${foundation.service.version}'),

    // status.io api key
    'stauspageId' => env_or_default('STATUSIO_ID', '${foundation.service.stats.statusio.id}'),

    // Should JWT be verified against local public keys
    'verifyJWT' => false,

    // how long should the cache live
    'cacheLifetime' => intval(env_or_default('STATS_CACHE_LIFETIME', '${foundation.service.stats.cache.lifetime}')),

    // where do we go to do reverse ip lookups? Should be a linked container
    'freegeoipUrl' => env_or_default('FREEGEOIP_URL', '${foundation.service.stats.freegeoip.url}'),

    // What is the connectivity info for the pingdom api?
    'pingdomUsername' => env_or_default('PINGDOM_USERNAME', '${foundation.service.stats.pingdom.username}'),
    'pingdomPassword' => env_or_default('PINGDOM_PASSWORD', '${foundation.service.stats.pingdom.password}'),
    'pingdomToken' => env_or_default('PINGDOM_TOKEN', '${foundation.service.stats.pingdom.token}'),

    // agave api mysql connection info - should point at mirror db
    'mysql.agave.host' => env_or_default('MYSQL_HOST', '${foundation.db.host}'),
    'mysql.agave.database' => env_or_default('MYSQL_DATABASE', '${foundation.db.database}'),
    'mysql.agave.username' => env_or_default('MYSQL_USERNAME', '${foundation.db.username}'),
    'mysql.agave.password' => env_or_default('MYSQL_PASSWORD', '${foundation.db.password}'),
    'mysql.agave.port' => env_or_default('MYSQL_PORT', '${foundation.db.port}'),

    // agave api mysql connection info - should point at mirror db
    'mysql.foundation.host' => env_or_default('MYSQL_HOST', '${foundation.db.host}:${foundation.db.port}'),
    'mysql.foundation.database' => env_or_default('MYSQL_DATABASE', 'iplant-api'),
    'mysql.foundation.username' => env_or_default('MYSQL_USERNAME', '${foundation.db.username}'),
    'mysql.foundation.password' => env_or_default('MYSQL_PASSWORD', '${foundation.db.password}'),
    'mysql.foundation.port' => env_or_default('MYSQL_PORT', '${foundation.db.port}'),

    'redis.host' => env_or_default('REDIS_HOST', '${foundation.service.stats.redis.host}'),
    'redis.port' => env_or_default('REDIS_PORT', '${foundation.service.stats.redis.port}'),

    'apim.db.host' => env_or_default('APIM_MYSQL_HOST', '${foundation.db.host}'),
    'apim.db.username' => env_or_default('APIM_MYSQL_USERNAME', '${foundation.db.username}'),
    'apim.db.password' => env_or_default('APIM_MYSQL_PASSWORD', '${foundation.db.password}'),
    'apim.db.port' => env_or_default('APIM_MYSQL_PORT', '${foundation.db.port}'),

    'apim.db.iplantc.org.host' => env_or_default('IPLANTC_ORG_MYSQL_HOST', '${foundation.db.host}'),
    'apim.db.iplantc.org.database' => env_or_default('IPLANTC_ORG_MYSQL_DATABASE', '${foundation.db.database}'),
    'apim.db.iplantc.org.username' => env_or_default('IPLANTC_ORG_MYSQL_USERNAME', '${foundation.db.username}'),
    'apim.db.iplantc.org.password' => env_or_default('IPLANTC_ORG_MYSQL_PASSWORD', '${foundation.db.password}'),
    'apim.db.iplantc.org.port' => env_or_default('IPLANTC_ORG_MYSQL_PORT', '${foundation.db.port}'),

    'apim.db.araport.org.host' => env_or_default('ARAPORT_ORG_MYSQL_HOST', '${foundation.db.host}'),
    'apim.db.araport.org.database' => env_or_default('ARAPORT_ORG_MYSQL_DATABASE', '${foundation.db.database}'),
    'apim.db.araport.org.username' => env_or_default('ARAPORT_ORG_MYSQL_USERNAME', '${foundation.db.username}'),
    'apim.db.araport.org.password' => env_or_default('ARAPORT_ORG_MYSQL_PASSWORD', '${foundation.db.password}'),
    'apim.db.araport.org.port' => env_or_default('ARAPORT_ORG_MYSQL_PORT', '${foundation.db.port}'),

);
