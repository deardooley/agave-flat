<?php

session_start();
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') 
{
	header("Access-Control-Allow-Origin: *");
	header("Content-type: application/json");
	echo '{}';
	exit;
}
define('BASEPATH', '');

# In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
} 
require_once "include/config.php";
require_once "include/remote_logging.php";
require_once "include/response.php";
require_once "include/database.php";
require_once "include/auth.php";


if ($_SERVER['REQUEST_METHOD'] == "GET") // validate a token
{
	post_log($username, $config['iplant.service.log.activitykey']['verify']);
	
	format_success_response();
}
else if ($_SERVER['REQUEST_METHOD'] == "POST") // create a token
{
	$token = array();
	
	if ( isset($_POST['username']) and $_POST['username'] != '') 
	{
		if ($config['debug']) error_log("passing in username ".$_POST['username']);
		
		// verify they are one of the super-users who can request tokens for other people
		if (in_array($username, $config['iplant.service.trusted.users']))
		{	
			if ($config['debug']) error_log("auth user $username is trusted");
			// verify it's a valid user they're requesting a token for
			$username_check_function = "{$config['iplant.auth.source']}_is_valid_username";
			//error_log($username_check_function);
			if ($username_check_function($_POST['username']) )
			{
				if ($config['debug']) error_log("requested user ".$_POST['username']. " is valid");
				$token['username'] = $_POST['username'];
			}
			
		}
		else
		{
			format_error_response("Permission denied. You do not have sufficient permissions to create proxy tokens for other API users.", ERROR_403);
		}
			
	}
	else
	{
		$token['username'] = $username;
	}
	
	if (empty($_POST['internalUsername']) || is_valid_interal_user($_POST['internalUsername'], $token['username'])) 
	{
		if ($config['debug']) error_log("requested internal user " . $_POST['internalUsername'] . " is valid");
		$token['internal_username'] = $_POST['internalUsername'];
	}
	else
	{
		format_error_response("No internal user found matching username '{$_POST['internalUsername']}'" , ERROR_403);
	}
	
	// create a new token array and insert into the db
	$token['creator'] = $username;
	$token['token'] = get_nonce($token['username']);
	$token['created_at'] = date('Y-m-d H:i:s');
	$token['renewed_at'] = date('Y-m-d H:i:s');
	
	if (!empty($_POST['lifetime'])) {
		if (is_numeric($_POST['lifetime']) and $_POST['lifetime'] > 0) 
			$token['expires_at'] =  date('Y-m-d H:i:s', strtotime('+'.$_POST['lifetime'].' seconds'));
		else 
			format_error_response('Invalid value for token lifetime.', ERROR_400);
	} else {
		// 2 hours by default
		$token['expires_at'] = date('Y-m-d H:i:s', strtotime('+2 hours'));
	}
	
	if (!empty($_POST['maxUses'])) {
		if (is_numeric($_POST['maxUses']) and $_POST['maxUses'] > 0) 
			$token['remaining_uses'] = $_POST['maxUses'];
		else 
			format_error_response('Invalid value for max uses.', ERROR_400);
	} else {
		// unlimited
		$token['remaining_uses'] = -1;
	}
	
	$token['id'] = save_token($token);
	
	if ($token['id'])
	{
		post_log($username, $config['iplant.service.log.activitykey']['create']);
		$response = array('token'    => $token['token'],
					   'username' => $token['username'],
					   'internalUsername' => $token['internal_username'],
					   'creator'  => $token['creator'],
					   'remainingUses' => ($token['remaining_uses'] == -1 ? 'unlimited' : $token['remaining_uses']),
					   'created'  => date('c', strtotime($token['created_at'])),
					   'expires'  => date('c', strtotime($token['expires_at'])),
					   'renewed'  => date('c', strtotime($token['renewed_at'])),
					   '_links' => get_hal_links_for_token($token)
					);
		
		/*$response['lifetime'] = strtotime($token['expires_at']) - strtotime($token['created_at']);
		$response['agent'] = $_SERVER['HTTP_USER_AGENT'];
		$response['ipaddress'] = $_SERVER['REMOTE_ADDR'];
		
		error_log(json_encode($response));
		unset($response['lifetime']);
		unset($response['agent']);
		unset($response['ipaddress']);*/
		
		format_success_response( $response );
	}
	else
	{
		format_error_response('Failed to create a token', ERROR_500);
	}		  
}

else {
	format_response('Unknown request method', ERROR_400);
}
