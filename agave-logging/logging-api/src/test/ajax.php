<?php

define('BASEPATH', '');
 
 # In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
} 
require_once "include/response.php";
require_once "include/config.php";
require_once "include/database.php";

if ( !empty($_GET['action']) ) // validate a token
{
	$action = $_GET['action'];
	if ( $action == 'service' ) 
	{
		$results = get_services();
		format_success_response($results);
	} 
	else if ( $action == 'activity' )
	{
		if (!empty($_GET['key']))
		{
			$service_key = $_GET['key'];
			$results = get_activities($_GET['key']);
			format_success_response($results);
		}
		else
		{
			format_error_response('key must be specified', ERROR_400);
		}
	} 
	else 
	{
		format_error_response('Invalid action', ERROR_400);
	}
}
else
{
	format_error_response('action must be specified', ERROR_400);
}