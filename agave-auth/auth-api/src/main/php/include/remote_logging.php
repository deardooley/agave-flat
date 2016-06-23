<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

//error_reporting(E_ALL);
//ini_set("show_errors", "1");

require_once("config.php");
//post_log('dooley', 'AuthCreate');

/* Print a success response */
function post_log($username=false, $activity_key=false)
{
	global $config;
	
	if (!$username or !$activity_key) return;
	
	error_log("User $username performed $activity_key with ".$_SERVER['HTTP_USER_AGENT']." from ".$_SERVER['REMOTE_ADDR']);
	
	$url = $config['iplant.service.log.host'] . $config['iplant.service.log.servicekey'] . '/' . $username . '/' . $activity_key;
	$post_data = "servicekey=".$config['iplant.service.log.servicekey'];
	$post_data .= "&activitykey=$activity_key";
	$post_data .= "&username=$username";
	$post_data .= "&activitycontext=";
	$post_data .= "&userip=".$_SERVER['REMOTE_ADDR'];
	
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


?>