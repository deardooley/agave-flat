<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class SummaryStatsController extends AbstractAgaveController {

  /*
  |--------------------------------------------------------------------------
  | Summary Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of stats from each major area
  |
  */

  protected function getPrimaryStats() {
    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => array(
        "totalJobs" => (int)$this->getJobs(),
        "totalData" => (int)$this->getData(),
        "totalUsers" => (int)$this->getUsers(),
        "totalRequests" => (int)$this->getRequests(),
        "totalApps" => (int)$this->getApps(),
        "totalClients" => (int)$this->getClients(),
        "totalTenants" => count($this->getTenants()),
        "status" => $this->getStatus()
        ),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('SummaryStatsController@index')),
      'parent' => array('href' => URL::action('StatsController@index')),
      'data' => array('href' => URL::action('DataStatsController@index')),
      'jobs' => array('href' => URL::action('JobStatsController@index')),
      'users' => array('href' => URL::action('UserStatsController@index')),
      'apps' => array('href' => URL::action('AppStatsController@index')),
      'clients' => array('href' => URL::action('ClientStatsController@index')),
      'code' => array('href' => URL::action('CodeStatsController@index')),
      'operations' => array('href' => URL::action('OperationsStatsController@index')),
      'tenants' => array('href' => URL::action('TenantsStatsController@index')),
      'traffic' => array('href' => URL::action('TrafficStatsController@index')),
    );
  }

  private function getClients() {
    $total = 0;
    $apim = new APIMClient($this->force);
    $tenants = $this->getTenants();
    foreach ($tenants as $tenant) {
      $total += count($apim->getAllApplicationsForTenant($tenant->tenant_id));
    }

    return $total;
  }

  private function getData() {
    $cacheKey = $this->getCachePrefix() .'total.bytes';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)
        ->remember($cacheKey, $_ENV['cacheLifetime'],
        function() use ($timeframe, $username, $tenantId)
        {
          $query = DB::table('transfertasks')
              ->select(DB::raw("sum(bytes_transferred) as total"));

          if (!empty($timeframe)) {
            $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
          }

          if ($username !== 'guest') {
            $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
          }

          return (int)$query->first()->total;
        }
    );
  }

  private function getTenants()
  {
    $cacheKey = $this->getCachePrefix() . "tenants.list.all";

    return Cache::remember($cacheKey, $_ENV['cacheLifetime'],
        function () {
          return DB::table('tenants')
              ->select(array('tenant_id', 'name'))
              ->where('status', '<>', 'DISABLED')
              ->where('tenant_id', '<>', 'irmacs')
              ->get();
        }
    );
  }

  private function getJobs() {
    $cacheKey = $this->getCachePrefix() .'total.jobs';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('jobs')
            ->select(DB::raw("count(id) as totalJobs"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalJobs;
      }
    );
  }

  private function getUsers() {
    $cacheKey = $this->getCachePrefix() . 'total.users';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('Usage')
            ->select(DB::raw("count(distinct Username) as totalUsers"));

        if ($username !== 'guest') {
          $query->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalUsers;
      }
    );
  }

  private function getRequests() {
    $cacheKey = $this->getCachePrefix() . 'total.requests';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        if ($username == 'guest' && empty($timeframe)) {
          $query = DB::table('Usage')
              ->select(DB::raw("count(1) as totalRequests"));
        }
        else {
          $query = DB::table('Usage')
              ->select(DB::raw("count(UID) as totalRequests"));

          if ($username !== 'guest') {
            $query->where('TenantId', '=', $tenantId);
          }

          if (!empty($timeframe)) {
            $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
          }
        }

        return (int)$query->first()->totalRequests;
      }
    );
  }

  private function getApps() {
    $cacheKey = $this->getCachePrefix() .'total.apps';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('softwares')
            ->select(DB::raw("count(id) as totalApps"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        return $query->first()->totalApps;
      }
    );
  }

  private function getStatus() {
    $cacheKey = $this->getCachePrefix() . '.totals.status';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    $statusioClient = new StatusioClient();

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function() use ($statusioClient) {
      $isOperational = $statusioClient->isOperational($_ENV['stauspageId']);
      return $isOperational ? 'UP' : 'DOWN';
    });
  }
}
