<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');
//ini_set('show_errors', 1);
/****************************************************************************************
 ****************************************************************************************
 *
 *						iPlant Auth Service Configuration File
 *
 ****************************************************************************************
 ****************************************************************************************/

date_default_timezone_set('GMT');
header("Access-Control-Allow-Origin: *");
header('Content-type: application/json');
	
$config['debug'] = true;
$config['service.version'] = '${foundation.service.version}';
/****************************************************************************************
 *						LDAP Connection Properties
 ****************************************************************************************/

# valid values are ldap and irods
$config['iplant.auth.source'] = 'ldap';
$config['iplant.auth.realm'] = '${foundation.ldap.realm}';

$config['iplant.ldap.host'] = '${foundation.ldap.url}';
$config['iplant.ldap.port'] = ${foundation.ldap.port};
$config['iplant.ldap.base.dn'] = '${foundation.ldap.dn}';

/****************************************************************************************
 *						IRODS Connection Properties
 ****************************************************************************************/

$config['iplant.irods.host'] = '${irods.host}';
$config['iplant.irods.port'] = ${irods.port};
$config['iplant.irods.zone'] ='${irods.zone}';
$config['iplant.irods.resource'] ='${irods.resource}';

/****************************************************************************************
 *						Trusted Users
 ****************************************************************************************/

$config['iplant.service.trusted.users'][] = 'ipcservices';
$config['iplant.service.trusted.users'][] = 'dooley';
$config['iplant.service.trusted.users'][] = 'vaughn';
$config['iplant.service.trusted.users'][] = 'jmccurdy';
$config['iplant.service.trusted.users'][] = 'sterry1';


/****************************************************************************************
 *						Database Connection Properties
 ****************************************************************************************/
 
$config['iplant.database.host'] = '${foundation.db.host}';
$config['iplant.database.username'] = '${foundation.db.username}';
$config['iplant.database.password'] = '${foundation.db.password}';
$config['iplant.database.name'] = '${foundation.db.database}';
$config['iplant.database.auth.table.name'] = '${foundation.db.auth.table}';
$config['iplant.database.internaluser.table.name'] = '${foundation.db.internaluser.table}';


/****************************************************************************************
 *						Logging Parameters
 ****************************************************************************************/

$config['iplant.service.log.host'] = addTrailingSlash('${foundation.service.log}');
$config['iplant.service.log.servicekey'] = 'AUTH02';
$config['iplant.service.log.activitykey']['create'] = 'AuthCreate';
$config['iplant.service.log.activitykey']['renew'] = 'AuthRenew';
$config['iplant.service.log.activitykey']['revoke'] = 'AuthRevoke';
$config['iplant.service.log.activitykey']['list'] = 'AuthList';
$config['iplant.service.log.activitykey']['verify'] = 'AuthVerify';


/****************************************************************************************
 *						iPlant Foundation API Service Endpoints
 ****************************************************************************************/
 
$config['iplant.foundation.services']['auth'] = addTrailingSlash('${foundation.service.auth}');
$config['iplant.foundation.services']['profile'] = addTrailingSlash('${foundation.service.profiles}');

/****************************************************************************************
 *						Error Response Codes
 ****************************************************************************************/
 
define( 'ERROR_200', 'HTTP/1.0 200 OK');
define( 'ERROR_400', 'HTTP/1.0 400 Bad Request');
define( 'ERROR_401', 'HTTP/1.0 401 Unauthorized');
define( 'ERROR_403', 'HTTP/1.0 403 Forbidden');
define( 'ERROR_404', 'HTTP/1.0 404 Not Found');
define( 'ERROR_500', 'HTTP/1.0 500 Internal Server Error');


function addTrailingSlash($value) {
	if (!endsWith($value, '/')) {
		$value .= '/';
	}
	return $value;
}
function endsWith($haystack,$needle,$case=true) {
    if($case){return (strcmp(substr($haystack, strlen($haystack) - strlen($needle)),$needle)===0);}
    return (strcasecmp(substr($haystack, strlen($haystack) - strlen($needle)),$needle)===0);
}
?>