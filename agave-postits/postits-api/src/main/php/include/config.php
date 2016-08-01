<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/****************************************************************************************
 ****************************************************************************************
 *
 *						Agave Postits Service Configuration File
 *
 ****************************************************************************************
 ****************************************************************************************/

date_default_timezone_set('America/Chicago');

$config['debug'] = envVar('DEBUG', true);
$config['service.version'] = envVar('IPLANT_SERVICE_VERSION','${foundation.service.version}');
$config['service.default.page.size'] = intval(envVar('IPLANT_DEFAULT_PAGE_SIZE', '100'));

/****************************************************************************************
 *						Trusted Users
 ****************************************************************************************/

$config['iplant.service.trusted.users'][] = 'ipcservices';
$config['iplant.service.trusted.users'][] = 'dooley';
$config['iplant.service.trusted.users'][] = 'vaughn';
$config['iplant.service.trusted.users'][] = 'lenards';
$config['iplant.service.trusted.users'][] = 'denni';
$config['iplant.service.trusted.users'][] = 'wregglej';
$config['iplant.service.trusted.users'][] = 'healyk';
$config['iplant.service.trusted.users'][] = 'psarando';
$config['iplant.service.trusted.users'][] = 'hariolf';
$config['iplant.service.trusted.users'][] = 'sriram';

$config['iplant.service.trusted.domains'][] = 'agave.iplantc.org';
$config['iplant.service.trusted.domains'][] = 'agave.iplantcollaborative.org';
$config['iplant.service.trusted.domains'][] = 'agaveapi.org';
$config['iplant.service.trusted.domains'][] = 'agaveapi.io';
$config['iplant.service.trusted.domains'][] = 'iplant-dev.tacc.utexas.edu';
$config['iplant.service.trusted.domains'][] = 'iplant-qa.tacc.utexas.edu';
$config['iplant.service.trusted.domains'][] = 'iplantc.org';
$config['iplant.service.trusted.domains'][] = 'iplantcollaborative.org';

/****************************************************************************************
 *						Logging keys
 ****************************************************************************************/
 
$config['iplant.service.log.servicekey'] = 'POSTITS02';
$config['iplant.service.log.activitykey']['create'] = 'PostItsAdd';
$config['iplant.service.log.activitykey']['redeem'] = 'PostItRedeem';
$config['iplant.service.log.activitykey']['revoke'] = 'PostItsDelete';
$config['iplant.service.log.activitykey']['list'] = 'PostItList';

/****************************************************************************************
 *						Database Connection Properties
 ****************************************************************************************/
 
$config['iplant.database.host'] = envVar('MYSQL_HOST', '${foundation.db.host}');
$config['iplant.database.username'] = envVar('MYSQL_USERNAME','${foundation.db.username}');
$config['iplant.database.password'] = envVar('MYSQL_PASSWORD','${foundation.db.password}');
$config['iplant.database.name'] = envVar('MYSQL_DATABASE','${foundation.db.database}');
$config['iplant.database.postits.table.name'] = envVar('IPLANT_DB_POSTITS_TABLE','${foundation.db.postits.table}');

/****************************************************************************************
 *						Agave API Service Endpoints
 ****************************************************************************************/
$config['iplant.foundation.services']['proxy'] = addTrailingSlash(envVar('IPLANT_PROXY_SERVICE', '${foundation.service.proxy}'));
$config['iplant.foundation.services']['auth'] = addTrailingSlash(envVar('IPLANT_AUTH_SERVICE', '${foundation.service.auth}'));
$config['iplant.foundation.services']['io'] = addTrailingSlash(envVar('IPLANT_IO_SERVICE', '${foundation.service.files}'));
$config['iplant.foundation.services']['apps'] = addTrailingSlash(envVar('IPLANT_APPS_SERVICE', '${foundation.service.apps}'));
$config['iplant.foundation.services']['postit'] = addTrailingSlash(envVar('IPLANT_POSTIT_SERVICE', '${foundation.service.postits}'));
$config['iplant.foundation.services']['profile'] = addTrailingSlash(envVar('IPLANT_PROFILE_SERVICE', '${foundation.service.profiles}'));
$config['iplant.foundation.services']['log'] = addTrailingSlash(envVar('IPLANT_LOG_SERVICE', '${foundation.service.log}'));
 
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