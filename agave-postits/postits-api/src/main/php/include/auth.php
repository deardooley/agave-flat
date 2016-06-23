<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

function authenticate($username, $password)
{
	global $config;
	
	if ($config['debug']) error_log($username . ' / ' . $password);
	
	if ( empty($username) or empty($password) )
	{
		return format_error_response('Authentication failed', ERROR_401);
	}
	else
	{
		$auth_response = authenticated_get($config['iplant.foundation.services']['auth'], $username, $password);
		
		if (empty($auth_response) || $auth_response->status == 'error')
		{
			return format_error_response('Authentication failed', ERROR_401);
		}
		else
		{
			return true;
		}
	}
}

/**
 * Get an auth token for the user from the auth service
 */
function get_auth_token($username, $password, $max_uses=1, $lifetime=7200, $internal_username=FALSE) 
{
	global $config;
	
	if ( empty($username) or empty($password) )
	{
		format_error_response("Invalid username/password combination.", ERROR_400);
	}
	else
	{
		$fields = array();
		$fields['lifetime'] = $lifetime;
		if ($internal_username) $fields['internalUsername'] = $internal_username;
		if ($max_uses != -1) $fields['maxUses'] = $max_uses;
		
		if ($_SERVER['REQUEST_METHOD'] == 'GET') {
			$auth_response = authenticated_get($config['iplant.foundation.services']['auth'], $username, $password);
		} else {
			$auth_response = authenticated_post($config['iplant.foundation.services']['auth'], $username, $password, $fields);
		}
		
		if (empty($auth_response) or $auth_response->status == 'error')
		{	
			format_error_response("Invalid username/password combination.", ERROR_401);
		}
		else if (empty($auth_response->result))
		{
			format_error_response("Error retrieving short term token.", ERROR_500);
		}
		else
		{
			return $auth_response->result->token;
		}
	}
}

/**
 * Perform an http get using basic auth on the endpoint
 */
function authenticated_get($url, $uname, $pass) 
{
	$ch = curl_init();                                                                         
	
	$request = curl_init();
	curl_setopt( $request, CURLOPT_URL, $url);
	curl_setopt( $request, CURLOPT_RETURNTRANSFER, 1);  // RETURN CONTENTS OF CALL
	curl_setopt( $request, CURLOPT_HEADER, 0 );  // DO NOT RETURN HTTP HEADERS
	curl_setopt( $request, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
	curl_setopt( $request, CURLOPT_USERPWD, $uname.":".$pass);
	curl_setopt( $request, CURLOPT_SSL_VERIFYPEER, FALSE);
	curl_setopt( $request, CURLOPT_SSL_VERIFYHOST, FALSE);
	curl_setopt( $request, CURLOPT_FOLLOWLOCATION, 1);
	curl_setopt( $request, CURLOPT_FORBID_REUSE, 1);
	$response = curl_exec( $request );
	error_log($response);
	curl_close($request);
	
	return json_decode($response, false);
}

/**
 * Perform an http post using basic auth on the endpoint
 */
function authenticated_post($url, $uname, $pass, $form_vars=array()) 
{
	
	$post_data = serialize_form_data($form_vars);
	error_log("invoking post on $url with $post_data");
	
	$curl = curl_init(); 
	curl_setopt($curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC ) ; 
	curl_setopt($curl, CURLOPT_USERPWD, $uname.":".$pass); 
	curl_setopt($curl, CURLOPT_SSLVERSION,3); 
	curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, FALSE); 
	curl_setopt($curl, CURLOPT_SSL_VERIFYHOST, 1);
	curl_setopt($curl, CURLOPT_HEADER, false); 
	curl_setopt($curl, CURLOPT_POST, true); 
	curl_setopt($curl, CURLOPT_POSTFIELDS, $post_data ); 
	curl_setopt($curl, CURLOPT_RETURNTRANSFER, true); 
	curl_setopt($curl, CURLOPT_URL, $url); 
	
	$response = curl_exec($curl);  
	error_log($response);
	curl_close($curl);
	
	return json_decode($response, false);
}
