<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

$db = mysql_connect($config['iplant.database.host'], $config['iplant.database.username'], $config['iplant.database.password']);

if (!$db) 
{
    format_response('error', 'Could not connect: ' . mysql_error($db), '');
}

mysql_select_db($config['iplant.database.name'], $db);

init_table();

/* Looks up the base url for the tenant's api based on the current tenant id */
function get_tenant_info($tenant_id='')
{
	global $db, $config, $DEBUG;
	
	$sql = "select base_url from tenants where tenant_id = '$tenant_id'";
	
	if ($config['debug']) error_log($sql);
	
	$result = mysql_query($sql, $db) or format_error_response("Failed to retrieve tenant: ".mysql_error($db));
	
	return mysql_fetch_array($result, MYSQL_ASSOC);
}

/* save a token */
function save_postit($postit)
{
	global $db, $config, $DEBUG, $tenant_id;
	
	$sql = "insert into ".$config['iplant.database.postits.table.name']." (postit_key, token, creator, ip_address, created_at, expires_at, remaining_uses, target_url, target_method, internal_username, tenant_id) values ('".$postit['postit_key']."', '".$postit['token']."', '".$postit['creator']."', '".$_SERVER['REMOTE_ADDR']."', '".$postit['created_at']."', '".$postit['expires_at']."', ".$postit['remaining_uses'].", '".$postit['target_url']."', '".$postit['target_method']."', '" . $postit['internal_username'] . "', '$tenant_id')";
	
	if ($config['debug']) error_log($sql);
	
	mysql_query($sql) or format_error_response("Failed to save token: ".mysql_error($db));
	
	return mysql_insert_id($db);
}

/* redeem a token */
function redeem_postit($postit_key)
{
	global $db, $config, $DEBUG;
	
	$sql = "update ".$config['iplant.database.postits.table.name']." set remaining_uses = remaining_uses - 1 where remaining_uses > 0 and postit_key = '$postit_key'";
	
	if ($config['debug']) error_log($sql);
	
	mysql_query($sql, $db) or format_error_response("Failed to redeem token: ".mysql_error($db));
	
	return TRUE;
}

/* revoke a postit */
function revoke_postit($postit_id)
{
	global $db, $config, $DEBUG, $tenant_id;
	
	$sql = "update ".$config['iplant.database.postits.table.name']." set expires_at = '".date('Y-m-d H:i:s')."' where id = '$postit_id' and tenant_id = '$tenant_id'";
	
	if ($config['debug']) error_log($sql);
	
	mysql_query($sql, $db) or format_error_response("Failed to revoke postit: ".mysql_error($db));
	
	return TRUE;
}

/* get a postit by its db id*/
function get_postit_by_id($postit_id)
{
	global $db, $config, $DEBUG, $tenant_id;
	
	$sql = "select * from ".$config['iplant.database.postits.table.name']." where id = $postit_id and tenant_id = '$tenant_id'";
	
	if ($config['debug']) error_log($sql);
	
	$result = mysql_query($sql, $db) or format_error_response("Failed to retrieve token: ".mysql_error($db));
	
	return mysql_fetch_array($result, $db);
}

/* return all active user postits */
function get_active_postits($username, $limit=100, $offset=0)
{
	global $db, $config, $DEBUG, $tenant_id;
	
	$sql = "select * from ".$config['iplant.database.postits.table.name'] . 
		   " where creator = '".$username."' " . 
		   " 	   AND expires_at > CURRENT_TIMESTAMP " . 
		   "	   AND remaining_uses <> 0 " . 
		   "	   AND tenant_id = '".$tenant_id."' " .
		   " LIMIT ".intval($limit)." OFFSET ".intval($offset)."";
	
	if ($config['debug']) error_log($sql);
	
	$result = mysql_query($sql, $db) or format_error_response("Failed to retrieve token: ".mysql_error($db));
	
	return $result;
}

/* get a postit by the url key*/
function get_postit_by_key($postit_key)
{
	global $db, $config;
	
	$sql = "select * from ".$config['iplant.database.postits.table.name']." where postit_key = '$postit_key'";
	
	if ($config['debug']) error_log($sql);
	
	$result = mysql_query($sql, $db) or format_error_response("Failed to retrieve token: ".mysql_error($db));
	
	return mysql_fetch_array($result, MYSQL_ASSOC);
}

/* generates a unique 32 character nonce to be used as the postit key/url token*/
function get_unique_nonce($seed)
{
	$found = true;
	while ( $found )
	{
		$nonce = md5($seed.strtotime('now').rand());
		$found = get_postit_by_key($nonce);
	}
	
	return $nonce;
}

/* Create the token table if it does not already exist */
function init_table()
{
	global $db, $config;
	
	$sql = "show tables like '".$config['iplant.database.postits.table.name']."'";
	
	$result = mysql_query($sql, $db);
	
	if ( mysql_num_rows($result) == 0)
	{
		// create the table
		mysql_query("CREATE TABLE `".$config['iplant.database.postits.table.name']."` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_url` varchar(32768) NOT NULL,
  `target_method` varchar(6) NOT NULL DEFAULT 'GET',
  `postit_key` varchar(64) NOT NULL,
  `creator` varchar(32) NOT NULL,
  `token` varchar(64) NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  `remaining_uses` int(7) NOT NULL DEFAULT '-1',
  `internal_username` varchar(32) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;") or format_error_response("Failed to create table ".$config['iplant.database.postits.table.name']." : ".mysql_error($db));
		
		if ($config['debug']) error_log($sql);
	}
}

?>