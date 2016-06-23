<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

class DatabaseUtil
{
	/**
	 * @var string $pluginurl The url to this plugin
	 */
	var $database_name = '';

	var $db = null;

	function DatabaseUtil($database_name) { $this->__construct($database_name); }

	function __construct($database_name)
	{
		$this->database_name = $database_name;
	}

	function connect()
	{
		global $config;

		/* Basic setup commands to get a connection on every invocation */
		$this->db = new mysqli($config['iplant.database'][$this->database_name]['host'],
									$config['iplant.database'][$this->database_name]['username'],
									$config['iplant.database'][$this->database_name]['password'],
									$config['iplant.database'][$this->database_name]['name']);

		if ($this->db->connect_errno) {
			format_response('error', 'Failed to connect to db: ' . $this->db->connect_error, '');
		}
	}

	function close()
	{
		$this->db->close();
	}

	function get_requests_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['monthly_requests'];

		if ($config['debug']) error_log ($sql);

		if ( $result = $this->db->query($sql))
		{
				$usage = array();

				while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
					if ($config['debug']) error_log (print_r($row,1));
					$usage[$row['request_year']][$row['request_month']] = (int)$row['total_usage'];
				}

				$this->close();

				return array("type" => "requests", "units"=>"api requests", "usage"=>$usage);
		}
		else
		{
			format_error_response("Failed to retrieve request usage: ".$this->db->error);
		}
	}

	function get_ip_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['ip'];

		if ($config['debug']) error_log ($sql);


		if ($result = $this->db->query($sql))
		{
			$usage = array();

			while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
				if ($config['debug']) error_log (print_r($row,1));
				$usage[$row['request_year']][$row['request_month']][$row['UserIP']]['requests'] = (int)$row['total_usage'];
			}

			$this->close();
		}
		else
		{
			$this->close();
			format_error_response("Failed to retrieve ip usage: ".$this->db->error);
		}

		// read geo cache from file
		$geocode_cache = file_get_contents("geocode.dat");

		if (!empty($geocode_cache)) {
			if ($config['debug']) {
				error_log("Found geo cache");
			} else {
				error_log("No geo cache found");
			}

			$geo = unserialize($geocode_cache);
		}

		// geocode the ip. use cache when possible, otherwise lookup manually
		foreach ($usage as $year => $months)
		{
			foreach ($months as $month => $user_ips)
			{
				foreach ($user_ips as $ip => $info)
				{
					if (empty($geo[$ip]))
					{
						if ($config['debug']) error_log("No geo cache entry found for $ip. Looking it up.");
						// continue;

						$response = file_get_contents("http://freegeoip.net/json/".$ip);

						if (empty($response))
						{
							if ($config['debug']) error_log("No response from geolocation lookup at http://freegeoip.net/json/".$ip);
							continue;
						}
						else
						{
							$json = json_decode($response, true);

							if ($config['debug']) error_log(print_r($json, 1));

							$usage[$year][$month][$ip] = array(
								"requests" => (int)$row['total_usage'],
								"lat" => $json['latitude'],
								"lng" => $json['longitude'],
								"country" => $json['country_name'],
								"city" => $json['city'],
								"region" => $json['region_code']
							);

							// save the new ip resolution to the cache
							$geo[$ip] = $json;
						}
					}
					else
					{
						if ($config['debug']) error_log("Using geo cache entry found for $ip");

						$usage[$year][$month][$ip] = array(
							"requests" => (int)$usage[$year][$month][$ip]['requests'],
							"lat" => $geo[$ip]['latitude'],
							"lng" => $geo[$ip]['longitude'],
							"country" => $geo[$ip]['country_name'],
							"city" => $geo[$ip]['city'],
							"region" => $geo[$ip]['region_code']
						);
					}
				}
			}
		}

		file_put_contents('geocode.dat', serialize($geo));

		return array("type" => "ip", "units"=>"ip address", "usage"=>$usage);
	}

	function get_users_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['monthly_users'];

		if ($config['debug']) error_log ($sql);

		if ( $result = $this->db->query($sql))
		{
			$usage = array();

			while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
				if ($config['debug']) error_log (print_r($row,1));
				$usage[$row['request_year']][$row['request_month']] = (int)$row['total_usage'];
			}

			$this->close();

			return array("type" => "users", "units"=>"unique users", "usage"=>$usage);
		}
		else
		{
			$this->close();
			format_error_response("Failed to retrieve user usage: ".$this->db->error);
		}
	}

	function get_jobs_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['monthly_jobs'];

		if ($config['debug']) error_log ($sql);

		if ( $result = $this->db->query($sql))
		{
			$usage = array();

			while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
				if ($config['debug']) error_log (print_r($row,1));
				$usage[$row['request_year']][$row['request_month']] = (int)$row['total_usage'];
			}

			$this->close();

			return array("type" => "jobs", "units"=>"job submissions", "usage"=>$usage);
		}
		else
		{
			$this->close();
			format_error_response("Failed to retrieve job usage: ".$this->db->error);
		}
	}

	function get_data_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['monthly_data'];

		if ($config['debug']) error_log ($sql);

		if ( $result = $this->db->query($sql))
		{
			$usage = array();

			while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
				if ($config['debug']) error_log (print_r($row,1));
				$usage[$row['request_year']][$row['request_month']] = (int)$row['total_usage'];
			}

			$this->close();

			return array("type" => "data", "units"=>"bytes transferred", "usage"=>$usage);
		}
		else
		{
			$this->close();
			format_error_response("Failed to retrieve data usage: ".$this->db->error);
		}
	}

	function get_hours_by_month()
	{
		global $config;

		$this->connect();

		$sql = $config['iplant.database'][$this->database_name]['queries']['monthly_hours'];

		if ($config['debug']) error_log ($sql);

		if ( $result = $this->db->query($sql))
		{
			$usage = array();

			while ($row = $result->fetch_array(MYSQLI_ASSOC)) {
				if ($config['debug']) error_log (print_r($row,1));
				$usage[$row['request_year']][$row['request_month']] = (int)$row['total_usage'];
			}

			$this->close();

			return array("type" => "hours", "units"=>"cpu core hours", "usage"=>$usage);
		}
		else
		{
			$this->close();
			format_error_response("Failed to retrieve compute hours: ".$this->db->error);
		}
	}
}

?>
