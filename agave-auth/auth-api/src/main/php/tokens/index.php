<?php
session_start();
define('BASEPATH', '');

if (!function_exists('json_encode')) {
	require_once '../include/jsonwrapper_inner.php';
} 
require_once "../include/config.php";
require_once "../include/remote_logging.php";
require_once "../include/response.php";
require_once "../include/database.php";
require_once "../include/auth.php";


if ($_SERVER['REQUEST_METHOD'] == "GET") // validate a token
{
	post_log($username, $config['iplant.service.log.activitykey']['list']);
	
	if ( !empty($_REQUEST['token']) )
	{
		post_log($username, $config['iplant.service.log.activitykey']['renew']);
		
		$token = get_token_by_username_and_password($username, $_REQUEST['token']);
		
		if ($token)
		{
			$tokens = array( 'token'    => $token['token'],
							   'username' => $token['username'],
							   'creator'  => $token['creator'],
							   'internalUsername' => @$token['internal_username'],
							   'remainingUses' => ($token['remaining_uses'] == -1 ? 'unlimited' : $token['remaining_uses']),
							   'created'  => date('c', strtotime($token['created_at'])),
							   'expires'  => date('c', strtotime($token['expires_at'])),
							   'renewed'  => date('c', strtotime($token['renewed_at'])),
							   '_links' => get_hal_links_for_token($token)
							);
		}
		else
		{
			format_error_response('Invalid or expired token', ERROR_400);	
		}
	}
	else
	{
		$results = get_active_user_tokens($username);
		$tokens = array();
	
		foreach ($results as $token)
		{
			$tokens[] = array( 'token'    => $token['token'],
							   'username' => $token['username'],
							   'creator'  => $token['creator'],
							   'internalUsername' => @$token['internal_username'],
							   'remainingUses' => ($token['remaining_uses'] == -1 ? 'unlimited' : $token['remaining_uses']),
							   'created'  => date('c', strtotime($token['created_at'])),
							   'expires'  => date('c', strtotime($token['expires_at'])),
							   'renewed'  => date('c', strtotime($token['renewed_at'])),
							   '_links' => get_hal_links_for_token($token)
							);
		}
	}	
	format_success_response($tokens);
}
else if ($_SERVER['REQUEST_METHOD'] == "PUT") // renew a token
{
	if ( !empty($_REQUEST['token']) )
	{
		post_log($username, $config['iplant.service.log.activitykey']['renew']);
		
		$token = get_token_by_username_and_password($username, $_REQUEST['token']);
		
		if ($token)
		{
			renew_token($token['id']);
			
			$token = get_token_by_username_and_password($username, $_REQUEST['token']);
			error_log(print_r($token,1));
			$tokens = array( 'token'    => $token['token'],
							   'username' => $token['username'],
							   'creator'  => $token['creator'],
							   'internalUsername' => @$token['internal_username'],
							   'remainingUses' => ($token['remaining_uses'] == -1 ? 'unlimited' : $token['remaining_uses']),
							   'created'  => date('c', strtotime($token['created_at'])),
							   'expires'  => date('c', strtotime($token['expires_at'])),
							   'renewed'  => date('c', strtotime($token['renewed_at'])),
							   '_links' => get_hal_links_for_token($token)
							);
							   
			format_success_response($tokens);
		}
		else
		{
			format_error_response('Invalid or expired token', ERROR_400);	
		}
	}
	else
	{
		format_response('No token specified', ERROR_400);
	}
}
else if ($_SERVER['REQUEST_METHOD'] == "DELETE") // renew a token
{
	if ( !empty($_REQUEST['token']) )
	{
		post_log($username, $config['iplant.service.log.activitykey']['renew']);
		
		$token = get_token_by_username_and_password($username, $_REQUEST['token']);
		
		if ($token)
		{
			revoke_token($token['id']);
			post_log($username, $config['iplant.service.log.activitykey']['revoke']);
			format_success_response();
		}
		else
		{
			format_error_response('Invalid or expired token', ERROR_400);	
		}
	}
	else
	{
		format_response('No token specified', ERROR_400);
	}
}
else {
	format_response('Unknown request method', ERROR_400);
}

