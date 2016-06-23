<?php

use Illuminate\Console\Command;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Input\InputArgument;

class StatsCache extends Command {

	/**
	 * The console command name.
	 *
	 * @var string
	 */
	protected $name = 'stats:cache';

	/**
	 * The console command description.
	 *
	 * @var string
	 */
	protected $description = 'Rebuilds one or more Agave Stats API caches.';

	protected $statApiResources = array(
		'apps' => 'AppStatsController',
		'clients' => 'ClientStatsController',
		'code' => 'CodeStatsController',
		'data' => 'DataStatsController',
		'jobs' => 'JobStatsController',
		'operations' => 'OperationsStatsController',
		'summary' => 'SummaryStatsController',
		'traffic' => 'TrafficStatsController',
		'users' => 'UserStatsController',
		'tenants' => 'TenantsStatsController',
	);
	/**
	 * Create a new command instance.
	 *
	 * @return void
	 */
	public function __construct()
	{
		parent::__construct();
	}

	/**
	 * Execute the console command.
	 *
	 * @return mixed
	 */
	public function fire()
	{
		$resourcesToRebuild = $this->option('resource');
		$force = $this->option('force') ? 'true' : 'false';

		if (empty($resourcesToRebuild)) {
			$resourcesToRebuild = array_keys($this->statApiResources);
		}
		$this->info(date(DATE_RFC2822) ." Running stats cache command");

		foreach($resourcesToRebuild as $resourceName) {
			$this->updateCacheForResource($resourceName, $force);
		}
	}

	private function updateCacheForResource($resourceName, $force) {
		$this->info("Rebuilding {$resourceName} cache...");
		$start_at = strtotime('now');

		$url = "http://localhost/stats/v2/{$resourceName}?force={$force}";

		$this->info("Calling {$url}...");

		$ch = curl_init();
		curl_setopt( $ch, CURLOPT_URL , $url);
		curl_setopt( $ch, CURLOPT_RETURNTRANSFER, TRUE);
		curl_setopt( $ch, CURLOPT_FOLLOWLOCATION, TRUE);
		curl_setopt( $ch, CURLOPT_FORBID_REUSE, TRUE);
		curl_setopt( $ch, CURLOPT_SSLVERSION,3);
		curl_setopt( $ch, CURLOPT_SSL_VERIFYPEER, FALSE);
		curl_setopt( $ch, CURLOPT_HEADER, 0);
		curl_exec($ch);

		if(curl_error($ch)) {
			$this->error(curl_error($ch));
		}

		curl_close($ch);
		
		$end_at = strtotime('now');
		$time = $end_at - $start_at;
		$this->info("...finished rebuilding {$resourceName} cache in {$time} seconds");
	}

	// private function getResourceController($resourceName) {
	// 	return new $this->statApiResources[$resourceName]();
	// }

	/**
	 * Get the console command arguments.
	 *
	 * @return array
	 */
	protected function getArguments()
	{
		return array(
			// array('example', InputArgument::REQUIRED, 'An example argument.'),
		);
	}

	/**
	 * Get the console command options.
	 *
	 * @return array
	 */
	protected function getOptions()
	{
		return array(
			array('resource', 'r', InputOption::VALUE_OPTIONAL | InputOption::VALUE_IS_ARRAY, 'Which resource cache to rebuild.', null),
			array('force', 'f', InputOption::VALUE_NONE, 'Should the cache be forcefully recreated.', null),
		);
	}

}
