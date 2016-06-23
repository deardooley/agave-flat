<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class AppStatsController extends AbstractAgaveController {

  /*
  |--------------------------------------------------------------------------
  | App Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of app stats
  |
  */

  protected function getPrimaryStats() {
    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => $this->getTotals(),
      "averages" => $this->getAverages(),
      "leaders" => $this->getLeadersSeries(),
      "locations" => $this->getLocations(),
      "traffic" => $this->getTraffic(),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('AppStatsController@index')),
      'parent' => array('href' => URL::action('SummaryStatsController@index')),
    );
  }

  /**
   * Returns an object with aggregate totals for various user stats.
   */
  private function getTotals() {
    return array(
      "apps" => (int)$this->getTotalAppCount(),
      "created" => (int)$this->getTotalCreatedAppCount(),
      "deleted" => (int)$this->getTotalDeletedAppCount(),
      "used" => (int)$this->getTotalUsedAppCount(),
      "public" => (int)$this->getTotalPublicAppCount(),
      "private" => (int)$this->getTotalPrivateAppCount(),
      "shared" => (int)$this->getTotalSharedAppCount()
    );
  }

  private function getTotalAppCount() {
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

  private function getTotalCreatedAppCount() {
    $cacheKey = $this->getCachePrefix() .'total.created';
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

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return $query->first()->totalApps;
      }
    );
  }

  private function getTotalPublicAppCount() {
    $cacheKey = $this->getCachePrefix() .'total.public';
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
            ->select(DB::raw("count(id) as totalApps"))->where('publicly_available','=',1);

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalApps;
      }
    );
  }

  private function getTotalPrivateAppCount() {
    return $this->getTotalAppCount() - $this->getTotalPublicAppCount();
  }

  private function getTotalSharedAppCount() {
    $cacheKey = $this->getCachePrefix() .'total.shared';
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
            ->join('software_permissions', 'softwares.id', '=', 'software_permissions.software_id')
            ->select(DB::raw("count(softwares.id) as sharedApps"))
            ->whereNotNull('software_permissions.software_id');

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->sharedApps;
      }
    );
  }

  private function getTotalDeletedAppCount() {
    $cacheKey = $this->getCachePrefix() .'total.deleted';
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
            ->select(DB::raw("count(softwares.id) as deletedApps"))
            ->where('available', '=', 0);

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return $query->first()->deletedApps;
      }
    );
  }

  private function getTotalUsedAppCount() {
    $cacheKey = $this->getCachePrefix() .'total.used';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('jobs')->select(DB::raw("count(distinct software_name) as totalUsedApps"));


        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalUsedApps;
      }
    );
  }

  private function getTotalAppUsers() {
    $cacheKey = $this->getCachePrefix() .'total.users';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('jobs')->select(DB::raw("count(distinct owner) as totalAppUsers"));


        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalAppUsers;
      }
    );
  }
  /**
   * Returns arrays of leaders in the operations categories.
   */
  private function getLeadersSeries() {
    return [
      array(
        "category" => "tags",
        "values" => $this->getTagLeaders()
      ),
      array(
        "category" => "uses",
        "values" => $this->getUsesLeaders()
      )
    ];
  }

  /**
   * Returns to to job submission countries.
   */
  private function getTagLeaders() {
    $cacheKey = $this->getCachePrefix() . 'leaders.tags';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
      function() use ($username, $tenantId, $timeframe) {

        $query = DB::table('softwares')->select('tags');


        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $tags = array();
        foreach($query->get() as $row) {
          foreach (explode(',', $row->tags) as $tag) {
            if ($tag) {
              if (isset($tags[$tag])) {
                $tags[$tag]++;
              } else {
                $tags[$tag] = 1;
              }
            }
          }
        }

        arsort($tags);

        foreach($tags as $name=>$count) {
          $tags[$name] = array('name'=>$name, 'count'=>$count);
        }


        $tags = array_slice($tags, 0, 10);

        return array_values($tags);
    });
  }

  private function getUsesLeaders() {
    $cacheKey = $this->getCachePrefix() . 'leaders.uses';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
      function() use ($username, $tenantId, $timeframe) {

        $query = DB::table('jobs')->select(DB::raw("software_name, count(id) as uses"));


        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy('software_name')
              ->orderBy(DB::raw("count(id)"), 'desc')
              ->take(10);

        $uses = array();

        foreach($query->get() as $row) {
          $uses[] = array('name'=>$row->software_name, 'count'=>$row->uses);
        }

        return array_values($uses);
    });
  }

  private function getAverages() {

    return array(
      "usedPerUser" => $this->getAverageAppsUsedPerUser(), // average file size
      "registeredPerUser" => $this->getAverageAppRegistrationsPerUser(), // average file size
      "sharedPerUser" => $this->getAverageSharedAppsPerUser(), // average file size
    );
  }

  private function getAverageAppsUsedPerUser() {
    return (float)number_format((float)$this->getTotalAppCount() / (float)$this->getTotalAppUsers(), 1);
  }

  private function getAverageAppRegistrationsPerUser() {
    return (float)number_format((float)$this->getTotalCreatedAppCount() / (float)$this->getTotalUsers(), 1);
  }

  private function getAverageSharedAppsPerUser() {
    return (float)number_format((float)$this->getTotalSharedAppCount() / (float)$this->getTotalUsers(), 1);
  }

  private function getAverageCoreCount() {
    return (float)number_format((float)$this->getTotalCoreCount() / (float)$this->getTotalJobCount(), 1);
  }

  private function getAverageJobsPerUser() {
    return (float)number_format((float)$this->getTotalJobCount() / (float)$this->getUniqueJobOwnerCount(), 1);
  }

  private function getTraffic() {
    return array(
      array(
        'name' => 'hourly',
        'values' => $this->getHourlyRegistrations(),
      ),
      array(
        'name' => 'dailyTraffic',
        'values' => $this->getDailyRegistrations(),
      ),
    );

    return [
        $this->getHourlyRegistrations(),
        $this->getDailyRegistrations()
    ];
  }

  private function getHourlyRegistrations() {
    return $this->getRegistrationTraffic('registrations.hourly', "DATE_FORMAT((created - INTERVAL 18 HOUR), '%H')");
  }

  private function getDailyRegistrations() {
    return $this->getRegistrationTraffic('registrations.daily', "DATE_FORMAT(created, '%Y-%m-%d')");
  }

  private function getRegistrationTraffic($cacheSuffix, $field, $limit=false, $order=false) {
    $cacheKey = $this->getCachePrefix() . $cacheSuffix;
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($field, $limit, $order, $timeframe, $username, $tenantId) {

        $query = DB::table('softwares')->select(DB::raw("$field as 'name', count($field) as 'count'"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)
                ->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy(DB::raw($field));

        if ($order) {
          $query->orderBy(DB::raw("count($field)"), 'desc');
        }

        if ($limit) {
          $query->take($limit);
        }
        $result = $query->get();

        return $result;
      }
    );
  }

  /**
   * Weighted locations from which users are accessing agave.
   */
  private function getLocations() {
    $cacheKey = $this->getCachePrefix() . "locations.ip";
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    $query = "select r.UserIP as 'ip', count(r.UID) as 'cnt' from `Usage` r ";
    $query .= "where r.ActivityKey in ('AppsAdd', 'AppsPublish') ";
    $query = $this->applyQueryConditions($query, 'usage');
    $query .= " group by r.UserIP order by count(r.UID)";

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function() use ($query) {

      $reverseIpUrl = $_ENV['freegeoipUrl'].'/json/';
      $resolvedIps = array();

      foreach (DB::select($query) as $usageRecord) {

        // resolve geocoordinates for ip address
        $ipGeoInfo = Cache::tags('traffic', 'ips')->rememberForever($usageRecord->ip, function() use($usageRecord, $reverseIpUrl) {
          Log::debug("Geolocating ip address at $reverseIpUrl" . $usageRecord->ip . "...");
          $response = file_get_contents($reverseIpUrl.$usageRecord->ip);

          if (empty($response)) {
            Log::debug("No response from geolocation lookup at $reverseIpUrl".$usageRecord->ip);
            return array(
              "lat" => null,
              "lng" => null,
              "country" => null,
              "city" => null,
              "region" => null
            );
          } else {
            $json = json_decode($response, true);

            Log::debug("Successfully resolved geolocation for ".$usageRecord->ip.": ".$response);

            return array(
              "lat" => $json['latitude'],
              "lng" => $json['longitude'],
              "country" => $json['country_name'],
              "city" => $json['city'],
              "region" => $json['region_code']
            );
          }
        });

        $resolvedIps[$usageRecord->ip] = $ipGeoInfo;
        $resolvedIps[$usageRecord->ip]['requests'] = $usageRecord->cnt;
      }

      return $resolvedIps;

    });
  }

  private function getTotalUsers() {
    $cacheKey = $this->getCachePrefix() . 'traffic.users';
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

}
