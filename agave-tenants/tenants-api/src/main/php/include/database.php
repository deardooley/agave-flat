<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/* Basic setup commands to get a connection on every invocation */
$db = mysql_connect($config['iplant.database']['agave']['host'],
										$config['iplant.database']['agave']['username'],
										$config['iplant.database']['agave']['password']);

if (!$db)
{
    format_response('error', 'Could not connect: ' . mysql_error($db), '');
}
mysql_select_db($config['iplant.database']['agave']['name'], $db);

function get_tenants()
{
	global $db, $config, $DEBUG;;

	$sql = "select id, name, tenant_id, base_url, contact_email, contact_name, uuid from `".$config['iplant.database']['agave']['tenants']['name'] . "` where status = 'LIVE' order by tenant_id asc";

	if ($DEBUG) error_log ($sql);

	$result = mysql_query($sql) or format_error_response("Failed to retrieve tenants: ".mysql_error($db));

	$tenants = array();

	while ($row = mysql_fetch_array($result, MYSQL_ASSOC))
	{
		$tenant = array(
			'id' => $row['uuid'],
			'name' => $row['name'],
			'baseUrl' => $row['base_url'],
			'code' => $row['tenant_id'],
			'contact' => array(
				array(
					'name' => $row['contact_name'],
					'email' => $row['contact_email'],
					'url' => '',
					'type' => 'admin',
					'primary' => true
				)
			),
			'_links' => array(
				'self' => array(
					'href' => $config['iplant.foundation.services']['tenants'].$row['uuid'] 
				)
			)
		);
		$tenants[] = $tenant;
	}

	return $tenants;
}

function get_tenant_by_id($uuid = '')
{
	global $db, $config, $DEBUG;;

	$sql = "select id, name, tenant_id, base_url, contact_email, contact_name, uuid from `".$config['iplant.database']['agave']['tenants']['name'] .
		"` where uuid = '" . addslashes($uuid) . "' and status = 'LIVE' order by tenant_id asc limit 1";

	if ($DEBUG) error_log ($sql);

	$result = mysql_query($sql) or format_error_response("Failed to retrieve tenant: ".mysql_error($db));

	$tenants = array();

	while ($row = mysql_fetch_array($result, MYSQL_ASSOC))
	{
		$tenant = array(
			'id' => $row['uuid'],
			'name' => $row['name'],
			'baseUrl' => $row['base_url'],
			'code' => $row['tenant_id'],
			'contact' => array(
				array(
					'name' => $row['contact_name'],
					'email' => $row['contact_email'],
					'url' => '',
					'type' => 'admin',
					'primary' => true
				)
			),
			'_links' => array(
				'self' => array(
					'href' => $config['iplant.foundation.services']['tenants'].$row['uuid'] 
				)
			)
		);
		$tenants[] = $tenant;
	}

	return $tenants;
}
