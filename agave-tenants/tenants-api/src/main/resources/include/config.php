<?php  //if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/****************************************************************************************
 ****************************************************************************************
 *
 *						Agave Tenants Service Configuration File
 *
 ****************************************************************************************
 ****************************************************************************************/

date_default_timezone_set('America/Chicago');
header("Access-Control-Allow-Origin: *");
header('Content-type: application/json');

$config['debug'] = false;
$config['service.version'] = envVar('IPLANT_SERVICE_VERSION','${foundation.service.version}');

/****************************************************************************************
 *						Trusted Users
 ****************************************************************************************/

$config['iplant.service.trusted.users'][] = 'ipcservices';
$config['iplant.service.trusted.users'][] = 'dooley';
$config['iplant.service.trusted.users'][] = 'vaughn';
$config['iplant.service.trusted.users'][] = 'sterry1';
$config['iplant.service.trusted.users'][] = 'jstubbs';

$config['iplant.service.trusted.ip'][] = '.*..*..*..*';

/****************************************************************************************
 *						Logging keys
 ****************************************************************************************/

$config['iplant.service.tenants.servicekey'] = 'TENANTS02';
$config['iplant.service.tenants.activitykey']['list'] = 'TenantsList';


/****************************************************************************************
 *						Database Connection Properties
 ****************************************************************************************/

$config['iplant.database']['agave']['host'] = envVar('MYSQL_HOST', '${foundation.db.host}');
$config['iplant.database']['agave']['username'] = envVar('MYSQL_USERNAME','${foundation.db.username}');
$config['iplant.database']['agave']['password'] = envVar('MYSQL_PASSWORD','${foundation.db.password}');
$config['iplant.database']['agave']['name'] = envVar('MYSQL_DATABASE','${foundation.db.database}');
$config['iplant.database']['agave']['tenants']['name'] = envVar('IPLANT_DB_TENANTS_TABLE','${foundation.db.tenants.table}');

/****************************************************************************************
 *						iPlant Foundation API Service Endpoints
 ****************************************************************************************/

$config['iplant.foundation.services']['tenants'] = addTrailingSlash(envVar('IPLANT_TENANTS_SERVICE','${foundation.service.tenants}'));
$config['iplant.foundation.services']['log'] = addTrailingSlash(envVar('IPLANT_LOG_SERVICE','${foundation.service.log}'));

/****************************************************************************************
 *						Error Response Codes
 ****************************************************************************************/

define( 'ERROR_200', 'HTTP/1.0 200 OK');
define( 'ERROR_400', 'HTTP/1.0 400 Bad Request');
define( 'ERROR_401', 'HTTP/1.0 401 Unauthorized');
define( 'ERROR_403', 'HTTP/1.0 403 Forbidden');
define( 'ERROR_404', 'HTTP/1.0 404 Not Found');
define( 'ERROR_500', 'HTTP/1.0 500 Internal Server Error');
define( 'ERROR_501', 'HTTP/1.0 501 Not Implemented');

function envVar($varName, $default='') {
	if (empty($varName)) {
		return $default;
	} else {
		$envVarName = strtoupper($varName);
		$envVarName = str_replace('.', '_', $varName);
		$val = getenv($envVarName);
		return (empty($val) ? $default : $val);
	}
}

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
