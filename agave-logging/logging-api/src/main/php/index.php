<?php

/********************************************************
 *
 *		Tracking Service for iPlant Services
 *
 ********************************************************/

define('BASEPATH', '');

date_default_timezone_set('America/Chicago');

require_once "include/config.php";
# In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
} 
require_once "include/response.php";
require_once "include/database.php";


/* Optional auth if we need it */
//
//require_once "include/auth.php";


if ( !empty($_POST) ) // validate a token
{
	if ( !is_valid_ip($_SERVER['REMOTE_ADDR']) ) {
		format_error_response('Request from invalid ip address '.$_SERVER['REMOTE_ADDR'], ERROR_403);
	}
	
	if (empty($_POST['username'])) {
		format_error_response('username must be specified', ERROR_400);		
	}
	
	if (empty($_POST['servicekey'])) {
		format_error_response('servicekey must be specified', ERROR_400);		
	}
	else if (!is_valid_service_key($_POST['servicekey']))
	{
		format_error_response('Invalid servicekey', ERROR_400);
	}
	
	if (empty($_POST['activitykey'])) 
	{
		format_error_response('activitykey must be specified', ERROR_400);		
	} 
	else if (!is_valid_activity_key($_POST['servicekey'], $_POST['activitykey']))
	{
		format_error_response('Invalid activitykey for servicekey', ERROR_400);
	}
	
	if (empty($_POST['clientId'])) 
	{
		// unauthenticated call. ex. postit redeption
		$_POST['clientId'] = '';
	}
	
	if (empty($_POST['tenantId'])) 
	{
		format_error_response('tenantId must be specified', ERROR_400);		
	}
	
	insert_request($_POST);
	
	format_success_response();
}
else
{
	format_error_response('Invalid method', ERROR_400);	
}

function is_valid_ip($ip_address) 
{
	global $config;
	
	if ( empty($ip_address) ) return FALSE;
	
	foreach($config['iplant.service.trusted.ip'] as $trusted_ip) {
		if (ereg($trusted_ip, $ip_address)) return TRUE;
	}
	
	return FALSE;
}