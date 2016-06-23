<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/* Basic setup commands to get a connection on every invocation */
$db = mysql_connect($config['iplant.database.host'], $config['iplant.database.username'], $config['iplant.database.password']);

if (!$db) 
{
    format_response('error', 'Could not connect: ' . mysql_error($db), '');
}

mysql_select_db($config['iplant.database.name'], $db);

init_table();


/* insert the request in the db */
function insert_request($args)
{
	global $db, $config, $DEBUG;
	
	foreach($args as $key=>$value) {
		$args[$key] = clean_query($value);
	}
	
	$sql = "insert into `".$config['iplant.database.usage.table.name']."` (Username, ServiceKey, ActivityKey, ActivityContext, CreatedAt, CallingIP, UserIP, ClientApplication, TenantId) values ('".$args['username']."', '".$args['servicekey']."', '".$args['activitykey']."', '".$args['activitycontext']."', '".date('Y-m-d h:j:s')."', '".$_SERVER['REMOTE_ADDR']."', '".$args['userip']."', '".$args['clientId']."', '".$args['tenantId']."')";
	
	if ($DEBUG) error_log ($sql);
	
	mysql_query($sql) or format_error_response("Failed to save request: ".mysql_error($db));
	
	return mysql_insert_id($db);
}

function is_valid_service_key($service_key)
{
	global $db, $config, $DEBUG;
	
	$service_key = clean_query($service_key);
	
	$sql = "select * from `".$config['iplant.database.usageservices.table.name']."` where ServiceKey = '$service_key'";
	
	//if ($DEBUG) error_log ($sql);
	
	$result = mysql_query($sql) or format_error_response("Failed to retrieve activities: ".mysql_error($db));
	
	return ( mysql_num_rows($result) == 1 );
}

function is_valid_activity_key($service_key, $activity_key)
{
	global $db, $config, $DEBUG;
	
	$service_key = clean_query($service_key);
	
	$sql = "select * from `".$config['iplant.database.usageactivities.table.name']."` where ActivityKey = '$activity_key' and ServiceKey = '$service_key'";
	
	//if ($DEBUG) error_log ($sql);
	
	$result = mysql_query($sql) or format_error_response("Failed to retrieve activity key: ".mysql_error($db));
	
	return ( mysql_num_rows($result) == 1 );
}

function get_activities($service_key) 
{
	global $db, $config, $DEBUG;;
	
	$service_key = clean_query($service_key);
	
	$sql = "select * from `".$config['iplant.database.usageactivities.table.name']."` where ServiceKey = '".$service_key."' order by ServiceKey ASC";
	
	//if ($DEBUG) error_log ($sql);
	
	$result = mysql_query($sql) or format_error_response("Failed to retrieve activities: ".mysql_error($db));
	
	$activities = array();
	
	while ($row = mysql_fetch_array($result, $db))
	{
		$activities[] = $row;
	}
	
	return $activities;
}

function get_services() 
{
	global $db, $config, $DEBUG;;
	
	$sql = "select * from `".$config['iplant.database.usageservices.table.name']."` order by ServiceKey ASC";
	
	//if ($DEBUG) error_log ($sql);
	
	$result = mysql_query($sql) or format_error_response("Failed to retrieve activities: ".mysql_error($db));
	
	$services = array();
	
	while ($row = mysql_fetch_array($result, $db))
	{
		$services[] = $row;
	}
	
	return $services;
}

/* checks for php version and magic quotes and prevents sql injection */
function clean_query($value)
{
	if(get_magic_quotes_gpc()) { // prevents duplicate backslashes
		$value = stripslashes($value);
	}
	
	if (phpversion() >= '4.3.0') {
		$value = mysql_real_escape_string($value);
	} else {
		$value = mysql_escape_string($value);
	}
	
	return $value;
}

/* Create the token table if it does not already exist */
function init_table()
{
	global $db, $config;
	
	$sql = "show tables like '".$config['iplant.database.usage.table.name']."'";
	
	$result = mysql_query($sql, $db);
	
	if ( mysql_num_rows($result) == 0)
	{
		// create the table
		mysql_query("CREATE TABLE `".$config['iplant.database.usage.table.name']."` (
  `UID` int(11) unsigned NOT NULL auto_increment,
  `Username` varchar(64) NOT NULL default '',
  `ServiceKey` varchar(30) NOT NULL default '',
  `ActivityKey` varchar(32) NOT NULL default '',
  `ActivityContext` varchar(64) default NULL,
  `CreatedAt` datetime NOT NULL,
  `CallingIP` varchar(15) default NULL,
  `UserIP` varchar(15) default NULL,
  `ClientApplication` varchar(64) NOT NULL DEFAULT '',
  `TenantId` varchar(64) NOT NULL DEFAULT '',
  `UserAgent` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`UID`),
  KEY `ServiceKey` (`ServiceKey`),
  KEY `ActivityKey` (`ActivityKey`),
  KEY `Username` (`Username`),
  KEY `CreatedAt` (`CreatedAt`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;") or format_error_response("Failed to create table ".$config['iplant.database.auth.table.name']." : ".mysql_error($db));

		// create the table
		mysql_query("CREATE TABLE `".$config['iplant.database.usageservices.table.name']."` (
  `ID` int(200) NOT NULL default '',
  `ServiceKey` varchar(30) NOT NULL default '',
  `Description` text,
  PRIMARY KEY  (`ServiceKey`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;") or format_error_response("Failed to create table ".$config['iplant.database.usageservices.table.name']." : ".mysql_error($db));

		// create the table
		mysql_query("CREATE TABLE `".$config['iplant.database.usageactivities.table.name']."` (
  `id` int(200) NOT NULL default '',
  `ActivityKey` varchar(32) NOT NULL default '',
  `Description` text,
  `ServiceKey` varchar(30) NOT NULL default '',
  PRIMARY KEY  (`ActivityKey`,`ServiceKey`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;") or format_error_response("Failed to create table ".$config['iplant.database.usageactivities.table.name']." : ".mysql_error($db));
	
	}
}

?>