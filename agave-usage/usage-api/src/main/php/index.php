<?php

/********************************************************
 *
 *		Agave Usage Service - currently readonly
 *
 ********************************************************/

define('BASEPATH', '');

require_once "include/config.php";
# In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
}
require_once "include/response.php";
require_once "include/database.php";
require_once "include/remote_logging.php";


/* Optional auth if we need it */
//
//require_once "include/auth.php";

if ( $_SERVER['REQUEST_METHOD'] != ('GET') ) // validate a token
{
	format_error_response( $_SERVER['REQUEST_METHOD'] . ' is not supported', ERROR_501);
}
else
{
	if ( !is_valid_ip($_SERVER['REMOTE_ADDR']) ) {
		format_error_response('Request from invalid ip address '.$_SERVER['REMOTE_ADDR'], ERROR_403);
	}
	else if (!is_valid_type($_GET['type']))
	{
		format_error_response('Unsupported usage type '.$_GET['type'], ERROR_501);
	}
	else
	{
		post_log('guest', $config['iplant.service.tenants.activitykey'][strtolower($_GET['type'])]);

		format_response('success', '', get_usage_by_type($_GET['type']));
	}
}

function get_usage_by_type($type = 'requests', $timeframe = 'month')
{
	$method = 'get_'.$type.'_by_'.$timeframe;
	$foundation_cache_file = "foundation_".$type."_cache.dat";
	$agave_cache_file = "agave_".$type."_cache.dat";
	$foundation_usage = array();
	$agave_usage = array();

	// check for foundation cache
	if (file_exists($foundation_cache_file) && filectime($foundation_cache_file) > strtotime("1 day ago"))
	{
		$foundation_usage = file_get_contents($foundation_cache_file);
		$foundation_usage = unserialize($foundation_usage);
	}

	// if not there, build one
	if (empty($foundation_usage))
	{
		$foundation = new DatabaseUtil('foundation');

		$foundation_usage = $foundation->$method();

		file_put_contents($foundation_cache_file, serialize($foundation_usage));
	}

	// check for agave cache
	if (file_exists($agave_cache_file) && filectime($agave_cache_file) > strtotime("1 day ago"))
	{
		$agave_usage = file_get_contents($agave_cache_file);
		$agave_usage = unserialize($agave_usage);
	}

	// if not there, build one
	if (empty($agave_usage))
	{
		$agave = new DatabaseUtil('agave');

		$agave_usage = $agave->$method();

		file_put_contents($agave_cache_file, serialize($agave_usage));
	}

	// build summary response
	$total_usage = array('units'=>$foundation_usage['units']);

	for ($year=2011; $year<=date('Y'); $year++)
	{
		for ($month=1; $month<13; $month++)
		{
			if ($type == "ip")
			{
				$total_usage['usage'][$year][$month] = null;

				if (isset($foundation_usage['usage'][$year][$month]))
				{
					foreach($foundation_usage['usage'][$year][$month] as $ip => $ip_info)
					{
						$total_usage['usage'][$year][$month][$ip] = $foundation_usage['usage'][$year][$month][$ip];
					}
				}

				if (isset($agave_usage['usage'][$year][$month]))
				{
					foreach($agave_usage['usage'][$year][$month] as $ip => $ip_info)
					{
						if (empty($total_usage['usage'][$year][$month][$ip])) {
							$total_usage['usage'][$year][$month][$ip] = $agave_usage['usage'][$year][$month][$ip];
						} else {
							$total_usage['usage'][$year][$month][$ip]['requests'] += (int)$agave_usage['usage'][$year][$month][$ip]['requests'];
						}
					}
				}
			}
			else
			{
				$total_usage['usage'][$year][$month] =
					(int)((empty($foundation_usage['usage'][$year][$month]) ? 0 : $foundation_usage['usage'][$year][$month]) +
						(empty($agave_usage['usage'][$year][$month]) ? 0 : $agave_usage['usage'][$year][$month]));
			}
		}
	}

	return $total_usage;

	//  return array("summary" => $total_usage, "foundation" => $foundation_usage, "agave" => $agave_usage);
}

function is_valid_type($type = '')
{
	$supported_types = array('jobs', 'users', 'requests', 'hours', 'ip', 'data');

	return in_array($type, $supported_types);
}

function is_valid_ip($ip_address)
{
	global $config;

	if ( empty($ip_address) ) return FALSE;

	foreach($config['iplant.service.trusted.ip'] as $trusted_ip) {
		if (ereg($trusted_ip, $ip_address)) return TRUE;
	}

	return FALSE;
}
