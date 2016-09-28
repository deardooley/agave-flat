<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

$db = mysql_connect($config['iplant.database.host'], $config['iplant.database.username'], $config['iplant.database.password']);

if (!$db)
{
    format_response('error', 'Could not connect: ' . mysql_error($db), '');
}

mysql_select_db($config['iplant.database.name'], $db);

// force timezone for this connection to line up with PHP timezone
// this will carry for all database interactions on this connection.
mysql_query("SET time_zone = 'CDT'");

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

function search_active_postits($username, $query=array(), $limit=100, $offset=0) {
	global $db, $config, $DEBUG, $tenant_id;

	if (empty($query)) {
		return get_active_postits($username, $limit, $offset);
	}
	else {
		$search_terms = array(
			'created' => array('field' => 'created', 'type' => 'date'),
			'expires' => array('field' => 'expires', 'type' => 'date'),
			'internalusername' => array('field' => 'internal_username', 'type' => 'string'),
			'url' => array('field' => 'url', 'type' => 'string'),
			'method' => array('field' => 'method', 'type' => 'string'),
			'noauth' => array('field' => 'noauth', 'type' => 'string'),
			'remaininguses' => array('field' => 'remaining_uses', 'type' => 'int'),
			'postit' => array('field' => 'postit_key', 'type' => 'string'),
			'creator' => array('field' => 'creator', 'type' => 'string'));

		$operators = array(
			'in' => "%s in ('%s')",
			'nin' => "%s not in ('%s')",
			'eq' => "%s = '%s'",
			'neq' => "%s <> '%s'",
			'gt' => "%s > '%s'",
			'gte' => "%s >= '%s'",
			'lt' => "%s < '%s'",
			'lte' => "%s <= '%s'",
			'like' => "%s like '%s'",
			'nlike' => "%s not like '%s'",

			'on' => "DATE_FORMAT(`%s`,'%Y-%m-%d') = '%s'",
			'before' => "DATE_FORMAT(`%s`,'%Y-%m-%d %H:%i') < '%s'",
			'after' => "DATE_FORMAT(`%s`,'%Y-%m-%d %H:%i') > '%s'",
			'between' => "DATE_FORMAT(`%s`,'%Y-%m-%d %H:%i') >= %s and DATE_FORMAT(`%s`,'%Y-%m-%d %H:%i') <= %s");

		$sql = "select * from " . $config['iplant.database.postits.table.name'] .
			" where creator = '" . $username . "' " .
			" 	   AND expires_at > '".date('Y-m-d H:i:s')."' " .
			"	   AND remaining_uses <> 0 " .
			"	   AND tenant_id = '" . $tenant_id . "' ";

		foreach ($query as $qterm) {
			if (in_array($qterm['operator'], array_keys($operators))) {
				$sterm = $search_terms[$qterm['term']];
				if ($qterm['operator'] == 'in' || $qterm['operator'] == 'nin' || $qterm['operator'] == 'between') {
					$qvals = explode(',', $qterm['value']);
					$qval = [];
					foreach($qvals as $qv) {
						$qval[] = format_serch_term_value($sterm['type'], $qv);
						if ($sterm['type'] == 'date') {
							$qval[] = date('Y-m-d H:i:s', strtotime($qv));
						}
						else if ($sterm['type'] == 'int') {
							$qval[] = intval($qv);
						}
						else {
							$qval[] = $qv;
						}
					}
					$qval = implode("','", $qval);
				}
				else {
					$qval = format_serch_term_value($sterm['type'], $qterm['value']);
				}


				if ($qterm['operator'] == 'between') {
					list($from, $to) = explode(',', $qval);

					$sql .= sprintf($operators[$qterm['operator']], $sterm['field'], mysql_escape_string($from), $sterm['field'], mysql_escape_string($to));
				}
				else {
					$sql .= sprintf($operators[$qterm['operator']], $sterm['field'], mysql_escape_string($qval));
				}
			}
		}

		$sql .= " LIMIT " . intval($limit) . " OFFSET " . intval($offset) . "";

		if ($config['debug']) error_log($sql);

		$result = mysql_query($sql, $db) or format_error_response("Failed to retrieve token: " . mysql_error($db));

		return $result;
	}
}

/**
 * Formats the query value into a valid prmiary type
 * @param $stype
 * @param $qv
 * @return bool|int|string
 */
function format_serch_term_value($stype, $qv) {
	if ($stype == 'date') {
		return date('Y-m-d H:i:s', strtotime($qv));
	}
	else if ($stype == 'int') {
		return intval($qv);
	}
	else {
		return $qv;
	}
}

/* return all active user postits */
function get_active_postits($username, $limit=100, $offset=0)
{
	global $db, $config, $DEBUG, $tenant_id;

	$sql = "select * from ".$config['iplant.database.postits.table.name'] .
		   " where creator = '".$username."' " .
		   " 	   AND expires_at > '".date('Y-m-d H:i:s')."' " .
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