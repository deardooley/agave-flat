<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

$mysqli = new mysqli($config['iplant.database.host'], $config['iplant.database.username'], $config['iplant.database.password'], $config['iplant.database.name']);

if ($mysqli->connect_errno) 
{
    format_response('error', "Failed to connect to MySQL: " . $mysqli->connect_error, '');
}

init_table();

/* save a token */
function save_token($token)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "insert into ".$config['iplant.database.auth.table.name']." (username, token, creator, ip_address, created_at, expires_at, renewed_at, remaining_uses, internal_username) values ('".$token['username']."', '".$token['token']."', '".$token['creator']."', '".$_SERVER['REMOTE_ADDR']."', '".$token['created_at']."', '".$token['expires_at']."', NULL, {$token['remaining_uses']}, '".$token['internal_username']."')";
	
	if ($config['debug']) error_log($sql);
	
	$mysqli->query($sql) or format_error_response("Failed to save token: ".$mysqli->error);
	
	return $mysqli->insert_id;
}

/* save a token */
function renew_token($token_id)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "update ".$config['iplant.database.auth.table.name']." set expires_at = '".date('Y-m-d H:i:s', strtotime('+2 hours'))."', renewed_at = CURRENT_TIMESTAMP where id = ".$token_id;
	
	if ($config['debug']) error_log($sql);
	
	$mysqli->query($sql) or format_error_response("Failed to renew token: ".$mysqli->error);
	
	return TRUE;
}

/* redeem a token */
function redeem_token($username, $token)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "update ".$config['iplant.database.auth.table.name']." set remaining_uses = remaining_uses - 1 where username = '$username' and remaining_uses > 0 and token = '$token'";
	
	if ($config['debug']) error_log($sql);
	
	$mysqli->query($sql) or format_error_response("Failed to renew token: ".$mysqli->error);
	
	return TRUE;
}

/* revoke a token */
function revoke_token($token_id)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "update ".$config['iplant.database.auth.table.name']." set expires_at = '".date('Y-m-d H:i:s')."' where id = ".$token_id;
	
	if ($config['debug']) error_log($sql);
	
	$mysqli->query($sql) or format_error_response("Failed to renew token: ".$mysqli->error);
	
	return TRUE;
}

/* get a token */
function get_token_by_id($token_id)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "select * from ".$config['iplant.database.auth.table.name']." where id = $token_id";
	
	if ($config['debug']) error_log($sql);
	
	$result = $mysqli->query($sql) or format_error_response("Failed to retrieve token: ".$mysqli->error);
	
	return $result->fetch_array();
}

/* get a token */
function get_token_by_username_and_password($username, $token)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "select * from ".$config['iplant.database.auth.table.name']." where username = '$username' and token = '$token' and (remaining_uses = -1 or remaining_uses > 0) and expires_at > '".date('Y-m-d H:i:s')."'";
	
	if ($config['debug']) error_log($sql);
	
	$result = $mysqli->query($sql) or format_error_response("Failed to retrieve token: ".$mysqli->error);
	
	return $result->fetch_array();
}

function get_active_user_tokens($username)
{
	global $mysqli, $config, $DEBUG;
	
	$sql = "select * from ".$config['iplant.database.auth.table.name']." where username = '$username' and (remaining_uses = -1 or remaining_uses > 0) and expires_at > '".date('Y-m-d H:i:s')."'";
	
	if ($config['debug']) error_log($sql);
	
	$result = $mysqli->query($sql) or format_error_response("Failed to retrieve token: ".$mysqli->error);
	
	$tokens = array();
	
	while ($row = $result->fetch_array())
	{
		$tokens[] = $row;
	}
	
	return $tokens;
}

/* validate a u/p pair */
function is_valid_token_for_user($username, $token)
{
	global $mysqli, $config;
	
	$sql = "select * from ".$config['iplant.database.auth.table.name']." where username = '$username' and token = '$token' and (remaining_uses = -1 or remaining_uses > 0) and expires_at > '".date('Y-m-d H:i:s')."'";

	if ($config['debug']) error_log($sql);
	
	$result = $mysqli->query($sql) or format_error_response("Failed to retrieve token: ".$mysqli->error);
	
	return $result->num_rows == 1;
}

/* generates a 32 character nonce to be used as the token */
function get_nonce($username)
{
	return md5($username.strtotime('now').rand());
}

/* Create the token table if it does not already exist */
function init_table()
{
	global $mysqli, $config;
	
	$sql = "show tables like '".$config['iplant.database.auth.table.name']."'";
	
	$result = $mysqli->query($sql);
	
	if ( $result->num_rows == 0)
	{
		// create the table
		$mysqli->query("CREATE TABLE `".$config['iplant.database.auth.table.name']."` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `creator` varchar(32) NOT NULL,
  `expires_at` datetime NOT NULL,
  `internal_username` varchar(32) NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `renewed_at` datetime NOT NULL,
  `remaining_uses` int(11) NOT NULL,
  `token` varchar(64) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;") or format_error_response("Failed to create table ".$config['iplant.database.auth.table.name']." : ".$mysqli->error);
		
		if ($config['debug']) error_log($sql);
	}
}

function is_valid_interal_user($internal_username, $api_username)
{
	global $mysqli, $config;
	
	$internal_username =  mysql_real_escape_string($internal_username);
	$api_username =  mysql_real_escape_string($api_username);
	
	$sql = "select * from ".$config['iplant.database.internaluser.table.name']." where `created_by` = '{$api_username}' and `username` = '{$internal_username}' and `currently_active` = 1";
	
	if ($config['debug']) error_log($sql);
	
	$result = $mysqli->query($sql) or format_error_response("Failed to retrieve token: ".$mysqli->error);
	
	return $result->num_rows == 1;
}

?>