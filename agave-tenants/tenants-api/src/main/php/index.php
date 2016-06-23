<?php

/********************************************************
 *
 *		Agave Tenants Service - currently readonly
 *
 ********************************************************/

define('BASEPATH', '');

require_once "include/config.php";
# In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
}
require_once "include/response.php";
require_once "include/database.php";
require_once "include/remote_logging.php";


/* Optional auth if we need it */
//
//require_once "include/auth.php";

post_log('guest', $config['iplant.service.tenants.activitykey']['list']);


if ( $_SERVER['REQUEST_METHOD'] != ('GET') ) // validate a token
{
	format_error_response( $_SERVER['REQUEST_METHOD'] . ' is not supported', ERROR_501);
}
else
{
	if ( !is_valid_ip($_SERVER['REMOTE_ADDR']) ) {
		format_error_response('Request from invalid ip address '.$_SERVER['REMOTE_ADDR'], ERROR_403);
	}
	else if ( empty($_GET['tenant_id']) )
	{
		$tenants = get_tenants();

		format_response('success', '', $tenants);
	}
	else
	{
		$tenant = get_tenant_by_id($_GET['tenant_id']);

		if (empty($tenant))
		{
			format_error_response('No tenant found with id ' . $_GET['tenant_id'] . ' ' .$_SERVER['REMOTE_ADDR'], ERROR_404);
		}
		else
		{
			format_response('success', '', $tenant);
		}
	}
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
