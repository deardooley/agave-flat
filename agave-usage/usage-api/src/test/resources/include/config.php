<?php  //if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/****************************************************************************************
 ****************************************************************************************
 *
 *						iPlant Usage Service Configuration File
 *
 ****************************************************************************************
 ****************************************************************************************/

date_default_timezone_set('America/Chicago');
header("Access-Control-Allow-Origin: *");
header('Content-type: application/json');

$config['debug'] = false;
$config['service.version'] = '${foundation.service.version}';

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

$config['iplant.database.host'] = '${foundation.db.host}';
$config['iplant.database.username'] = '${foundation.db.username}';
$config['iplant.database.password'] = '${foundation.db.password}';
$config['iplant.database.name'] = '${foundation.db.database}';
$config['iplant.database.auth.table.name'] = '${foundation.db.postits.table}';

$config['iplant.database']['foundation']['host'] = '${foundation.v1.db.test.host}';
$config['iplant.database']['foundation']['username'] = '${foundation.v1.db.test.username}';
$config['iplant.database']['foundation']['password'] = '${foundation.v1.db.test.password}';
$config['iplant.database']['foundation']['name'] = '${foundation.v1.db.test.database}';
$config['iplant.database']['foundation']['queries']['monthly_users'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(distinct Username) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_requests'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(UID) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_jobs'] = "select YEAR(created) as request_year, Month(created) as request_month, count(id) as total_usage from `Jobs` group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['monthly_hours'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(( processorCount * (greatest( UNIX_TIMESTAMP(startTime), UNIX_TIMESTAMP(endTime) ) - least( UNIX_TIMESTAMP(startTime), UNIX_TIMESTAMP(endTime) )) / 3600 )) as total_usage from Jobs where startTime is not null group by request_year, request_month";
$config['iplant.database']['foundation']['queries']['ip'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, UserIP, count(UID) as total_usage from `Usage` where UserIP not like '%:%' and UserIP not like '127.0.0.1' and UserIP <> 'None' group by request_year, request_month, UserIP";
$config['iplant.database']['foundation']['queries']['monthly_data'] = "select YEAR(created) as request_year, Month(created) as request_month, (20000000 * count(id))  as total_usage from `Jobs` group by request_year, request_month";

$config['iplant.database']['agave']['host'] = '${foundation.db.test.host}';
$config['iplant.database']['agave']['username'] = '${foundation.db.test.username}';
$config['iplant.database']['agave']['password'] = '${foundation.db.test.password}';
$config['iplant.database']['agave']['name'] = '${foundation.db.test.database}';
$config['iplant.database']['agave']['queries']['monthly_users'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(distinct Username) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_requests'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, count(UID) as total_usage from `Usage` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_jobs'] = "select YEAR(created) as request_year, Month(created) as request_month, count(id) as total_usage from `jobs` group by request_year, request_month";
$config['iplant.database']['agave']['queries']['monthly_hours'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(( node_count * processor_count * (greatest( UNIX_TIMESTAMP(start_time), UNIX_TIMESTAMP(end_time) ) - least( UNIX_TIMESTAMP(start_time), UNIX_TIMESTAMP(end_time) )) / 3600 )) as compute_hours from jobs where start_time is not null and end_time is not null and DATE_ADD(start_time, INTERVAL 49 HOUR) > end_time group by request_year, request_month";
$config['iplant.database']['agave']['queries']['ip'] = "select YEAR(CreatedAt) as request_year, Month(CreatedAt) as request_month, UserIP, count(UID) as total_usage from `Usage` where UserIP not like '%:%' and UserIP not like '127.0.0.1' and UserIP <> 'None' group by request_year, request_month, UserIP";
$config['iplant.database']['agave']['queries']['monthly_data'] = "select YEAR(created) as request_year, Month(created) as request_month, sum(attempts * bytes_transferred) as total_usage from `transfertasks` group by request_year, request_month";


/****************************************************************************************
 *						iPlant Foundation API Service Endpoints
 ****************************************************************************************/

$config['iplant.foundation.services']['log'] = addTrailingSlash('${foundation.service.log}');

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
