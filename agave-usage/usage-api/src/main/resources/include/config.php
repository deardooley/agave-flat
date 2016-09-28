<?php  //if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/****************************************************************************************
 ****************************************************************************************
 *
 *						Agave Usage Service Configuration File
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

$config['iplant.service.tenants.servicekey'] = 'USAGE02';
$config['iplant.service.tenants.activitykey']['jobs'] = 'JobsMonthly';
$config['iplant.service.tenants.activitykey']['users'] = 'UsersMonthly';
$config['iplant.service.tenants.activitykey']['data'] = 'DataMonthly';
$config['iplant.service.tenants.activitykey']['requests'] = 'RequestsMonthly';
$config['iplant.service.tenants.activitykey']['ip'] = 'IPMonthly';
$config['iplant.service.tenants.activitykey']['hours'] = 'HoursMonthly';

/****************************************************************************************
 *						Database Connection Properties
 ****************************************************************************************/

$config['iplant.database.host'] = envVar('MYSQL_HOST', '${foundation.db.host}');
$config['iplant.database.username'] = envVar('MYSQL_USERNAME','${foundation.db.username}');
$config['iplant.database.password'] = envVar('MYSQL_PASSWORD','${foundation.db.password}');
$config['iplant.database.name'] = envVar('MYSQL_DATABASE','${foundation.db.database}');
$config['iplant.database.usage.table.name'] = envVar('IPLANT_DB_USAGE_TABLE','${foundation.db.usage.table}');

$config['iplant.database']['foundation']['host'] = envVar('FOUNDATION_MYSQL_HOST', '${foundation.v1.db.host}');
$config['iplant.database']['foundation']['username'] = envVar('FOUNDATION_MYSQL_USERNAME','${foundation.v1.db.username}');
$config['iplant.database']['foundation']['password'] = envVar('FOUNDATION_MYSQL_PASSWORD','${foundation.v1.db.password}');
$config['iplant.database']['foundation']['name'] = envVar('FOUNDATION_MYSQL_DATABASE','${foundation.v1.db.database}');
$config['iplant.database']['foundation']['queries']['monthly_users'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(distinct Username) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_requests'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(UID) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_jobs'] = "select YEAR(created) as request_year, Month(created) as request_month, count(id) as total_usage from `Jobs` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_hours'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(( processorCount * (greatest( UNIX_TIMESTAMP(startTime), UNIX_TIMESTAMP(endTime) ) - least( UNIX_TIMESTAMP(startTime), UNIX_TIMESTAMP(endTime) )) / 3600 )) as total_usage from Jobs where startTime is not null group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['ip'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, UserIP, count(UID) as total_usage from `Usage` where UserIP not like '%:%' and UserIP not like '127.0.0.1' and UserIP <> 'None' group by request_year, request_month, UserIP";
$config['iplant.database']['foundation']['queries']['monthly_data'] = "select YEAR(created) as request_year, Month(created) as request_month, (20000000 * count(id))  as total_usage from `Jobs` group by request_year, request_month";

$config['iplant.database']['agave']['host'] = envVar('MYSQL_HOST', '${foundation.db.host}');
$config['iplant.database']['agave']['username'] = envVar('MYSQL_USERNAME','${foundation.db.username}');
$config['iplant.database']['agave']['password'] = envVar('MYSQL_PASSWORD','${foundation.db.password}');
$config['iplant.database']['agave']['name'] = envVar('MYSQL_DATABASE','${foundation.db.database}');
$config['iplant.database']['agave']['queries']['monthly_users'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(distinct Username) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_requests'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(UID) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_jobs'] = "select YEAR(created) as request_year, Month(created) as request_month, count(id) as total_usage from `jobs` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_hours'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(( node_count * processor_count * (greatest( UNIX_TIMESTAMP(start_time), UNIX_TIMESTAMP(end_time) ) - least( UNIX_TIMESTAMP(start_time), UNIX_TIMESTAMP(end_time) )) / 3600 )) as compute_hours from jobs where start_time is not null and end_time is not null and DATE_ADD(start_time, INTERVAL 49 HOUR) > end_time group by request_year, request_month";
$config['iplant.database']['agave']['queries']['ip'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, UserIP, count(UID) as total_usage from `Usage` where UserIP not like '%:%' and UserIP not like '127.0.0.1' and UserIP <> 'None' group by request_year, request_month, UserIP";
$config['iplant.database']['agave']['queries']['monthly_data'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(attempts * bytes_transferred) as total_usage from `transfertasks` group by request_year, request_month";


/****************************************************************************************
 *						iPlant Foundation API Service Endpoints
 ****************************************************************************************/

$config['iplant.foundation.services']['log'] = addTrailingSlash(envVar('IPLANT_LOG_SERVICE', '${foundation.service.log}'));

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
