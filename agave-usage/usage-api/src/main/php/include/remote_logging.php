<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require_once("config.php");

/* Print a success response */
function post_log($username=false, $activity_key=false, $tenant_id='iplantc.org', $client_id='00')
{
	global $config;
	
	if (!$username or !$activity_key) return;
	
	error_log("User $username performed $activity_key");
	
	$url = $config['iplant.foundation.services']['log'] . $config['iplant.service.tenants.servicekey'];
	$post_data = "servicekey=".$config['iplant.service.tenants.servicekey'];
	$post_data .= "&activitykey=$activity_key";
	$post_data .= "&username=$username";
	$post_data .= "&activitycontext=";
	$post_data .= "&userip=".$_SERVER['REMOTE_ADDR'];
	$post_data .= "&clientId=$client_id";
	$post_data .= "&tenantId=$tenant_id";
	
	//open connection
	$ch = curl_init();
	
	//set the url, number of POST vars, POST data
	curl_setopt( $ch, CURLOPT_URL ,$url);
	curl_setopt( $ch, CURLOPT_POST, 5);
	curl_setopt( $ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt( $ch, CURLOPT_POSTFIELDS, $post_data);
	
	//execute post
	$result = curl_exec($ch);
	
	//close connection
	curl_close($ch);
}