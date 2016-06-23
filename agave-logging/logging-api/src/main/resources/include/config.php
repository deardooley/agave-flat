<?php  //if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/****************************************************************************************
 ****************************************************************************************
 *
 *						Agave Logging Service Configuration File
 *
 ****************************************************************************************
 ****************************************************************************************/

$DEBUG = true;

date_default_timezone_set('America/Chicago');
header("Access-Control-Allow-Origin: *");
header('Content-type: application/json');

$config['service.version'] = envVar('IPLANT_SERVICE_VERSION','${foundation.service.version}');
$config['iplant.service.trusted.ip'][] = '.*..*..*..*';

/****************************************************************************************
 *						Database Connection Properties
 ****************************************************************************************/

$config['iplant.database.host'] = envVar('MYSQL_HOST', '${foundation.db.host}');
$config['iplant.database.username'] = envVar('MYSQL_USERNAME','${foundation.db.username}');
$config['iplant.database.password'] = envVar('MYSQL_PASSWORD','${foundation.db.password}');
$config['iplant.database.name'] = envVar('MYSQL_DATABASE','${foundation.db.database}');
$config['iplant.database.usage.table.name'] = envVar('IPLANT_DB_USAGE_TABLE','${foundation.db.usage.table}'); //Usage
$config['iplant.database.usageservices.table.name'] = envVar('IPLANT_DB_USAGESERVICES_TABLE','${foundation.db.usageservices.table}'); //UsageServices
$config['iplant.database.usageactivities.table.name'] = envVar('IPLANT_DB_USAGEACTIVITIES_TABLE','${foundation.db.usageactivities.table}'); //UsageActivities

/****************************************************************************************
 *						Error Response Codes
 ****************************************************************************************/
 
define( 'ERROR_200', 'HTTP/1.0 200 OK');
define( 'ERROR_400', 'HTTP/1.0 400 Bad Request');
define( 'ERROR_401', 'HTTP/1.0 401 Unauthorized');
define( 'ERROR_403', 'HTTP/1.0 403 Forbidden');
define( 'ERROR_405', 'HTTP/1.0 405 Method Not Allowed');
define( 'ERROR_404', 'HTTP/1.0 404 Not Found');
define( 'ERROR_500', 'HTTP/1.0 500 Internal Server Error');
define( 'ERROR_501', 'HTTP/1.0 501 Not Implemented');

//if (!function_exists('getEnv')) {
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
//}

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