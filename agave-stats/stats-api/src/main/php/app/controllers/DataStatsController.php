<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class DataStatsController extends AbstractAgaveController {

  /*
  |--------------------------------------------------------------------------
  | Data Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of data stats
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
      "transfers" => (int)$this->getTotalRootTransfers(),
      "files" => (int)$this->getTotalFilesTransfered(),
      "uploads" => (int)$this->getTotalUploadsCount(),
      "downloads" => (int)$this->getTotalDownloadsCount(),
      "retries" => (int)$this->getTotalRetriesCount(),
      "bytes" => (int)$this->getTotalBytesCopiedCount(),
      "systems" => (int)$this->getTotalUniqueSystemsCount(),
      "countries" => (int)$this->getTotalUniqueCountriesCount(),
      "users" => (int)$this->getTotalUsers()
    );
  }

  private function getTotalFilesTransfered() {
    $cacheKey = $this->getCachePrefix() .'total.transfers.all';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("count(id) as totalTransfers"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        return $query->first()->totalTransfers;
      }
    );
  }

  private function getTotalUploadsCount() {
    $cacheKey = $this->getCachePrefix() .'total.uploads';
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
            ->select(DB::raw("count(UID) as totalUploads"))->where('ActivityKey', '=', 'IOUpload');

        if ($username !== 'guest') {
          $query->where('Username', '=', $username)->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return $query->first()->totalUploads;
      }
    );
  }

  private function getTotalDownloadsCount() {
    $cacheKey = $this->getCachePrefix() .'total.downloads';
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
            ->select(DB::raw("count(UID) as totalDownloads"))->whereIn('ActivityKey', array('IODownload', 'IOPublicDownload'));

        if ($username !== 'guest') {
          $query->where('Username', '=', $username)->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return $query->first()->totalDownloads;
      }
    );
  }

  private function getTotalRetriesCount() {
    $cacheKey = $this->getCachePrefix() .'total.retries';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("sum(attempts) as total"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        return (int)$query->first()->total;
      }
    );
  }

  private function getTotalUniqueSystemsCount() {
    $cacheKey = $this->getCachePrefix() .'total.systems';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('logical_files')
            ->select(DB::raw("count(distinct system_id) as total"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        return $query->first()->total;
      }
    );
  }

  private function getTotalUniqueCountriesCount() {
    $cacheKey = $this->getCachePrefix() . 'totals.countries';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function() {
      $countries = array();
      $locations = $this->getLocations();

      foreach($locations as $location) {
        $countries[$location['country']] = 1;
      }

      return count($countries);
    });
  }

  private function getTotalBytesCopiedCount() {
    $summaryCachePrefix = 'summary.'.JWTClient::getCurrentTenant();
    $cacheKey = $summaryCachePrefix.'total.bytes';
    $category = $summaryCachePrefix;
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($summaryCachePrefix.$cacheKey);
    }

    return (int)Cache::tags($category, $tenantId, $username, $timeframe)->remember($summaryCachePrefix.$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("sum(bytes_transferred) as total"));

        if ($username !== 'guest') {
          $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
        }

        return (int)$query->first()->total;
      }
    );
  }


  /**
   * Returns arrays of leaders in the operations categories.
   */
  private function getLeadersSeries() {
    return [
      array(
        "category" => "countries",
        "values"  => $this->getLeadersCountries()
        ),
      array(
        "category" => "totalBytes",
        "values"  => $this->getLeadersUserBytes()
      ),
      array(
        "category" => "totalTransfers",
        "values"  => $this->getLeadersUserTransfers()
      ),
      array(
        "category" => "singleTransferSize",
        "values"  => $this->getLeadersUserMaxSize()
      ),
      array(
        "category" => "protocolVsTransfers",
        "values"  => [
          array(
            "name" => "SFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "FTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "GridFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "HTTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "S3",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Azure",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "IRODS",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Swift",
            "count" => rand(10000000, 10000000000000)
          )]),
      array(
        "category" => "protocolVsSize",
        "values"  => [
          array(
            "name" => "SFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "FTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "GridFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "HTTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "S3",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Azure",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "IRODS",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Swift",
            "count" => rand(10000000, 10000000000000)
          )]),
      array(
        "category" => "protocolVsRetries",
        "values"  => [
          array(
            "name" => "SFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "FTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "GridFTP",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "HTTP(S)",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "S3",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Azure",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "IRODS",
            "count" => rand(10000000, 10000000000000)
          ),
          array(
            "name" => "Openstack",
            "count" => rand(10000000, 10000000000000)
          )])
    ];
  }

  private function getLeadersCountries() {
    $cacheKey = $this->getCachePrefix() . 'leaders.countries';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function() {
      $countries = array();
      $locations = $this->getLocations();

      foreach($locations as $location) {
        if (empty($countries[$location['country']])) {
          $countries[$location['country']] = $location['requests'];
        } else {
          $countries[$location['country']] += $location['requests'];
        }
      }

      arsort($countries);

      $countries = array_slice($countries, 0, 10);

      foreach($countries as $name=>$count) {
        $countries[$name] = array( 'name' => $name, 'count' => $count );
      }

      return array_values($countries);
    });
  }

  private function getLeadersUserTransfers() {
    $cacheKey = $this->getCachePrefix() . 'leaders.user.transfers';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("owner, count(id) as totalTransfers"));

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy('owner')
              ->orderBy(DB::raw("count(id)"), 'desc')
              ->limit(10);

        $leaders = array();
        foreach($query->get() as $leader) {
          $leaders[] = array('name'=>$leader->owner, 'count'=>$leader->totalTransfers);
        }

        return $leaders;
      }
    );
  }

  private function getLeadersUserBytes() {
    $cacheKey = $this->getCachePrefix() . 'leaders.user.size';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("owner, sum(bytes_transferred) as totalBytes"));

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy('owner')
              ->orderBy(DB::raw("count(id)"), 'desc')
              ->limit(10);

        $leaders = array();
        foreach($query->get() as $leader) {
          $leaders[] = array('name'=>$leader->owner, 'count'=>$leader->totalBytes);
        }

        return $leaders;
      }
    );
  }

  private function getLeadersUserMaxSize() {
    $cacheKey = $this->getCachePrefix() . 'leaders.users.max';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("owner, max(bytes_transferred) as totalBytes"))
            ->whereNull('root_task')->whereNull('parent_task');

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy('owner')
              ->orderBy(DB::raw("count(id)"), 'desc')
              ->limit(10);

        $leaders = array();
        foreach($query->get() as $leader) {
          $leaders[] = array('name'=>$leader->owner, 'count'=>$leader->totalBytes);
        }

        return $leaders;
      }
    );
  }

  private function getLeadersProtocolTransfers() {
    $cacheKey = $this->getCachePrefix() . 'leaders.protocol.transfers';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("s.protocol, max(bytes_transferred) as totalBytes"))
            ->whereNull('root_task')->whereNull('parent_task');

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy('owner')
              ->orderBy(DB::raw("count(id)"), 'desc')
              ->limit(10);

        $leaders = array();
        foreach($query->get() as $leader) {
          $leaders[] = array('name'=>$leader->owner, 'count'=>$leader->totalBytes);
        }

        return $leaders;
      }
    );
  }



  private function getAverages() {

    return array(
      "fileSize" => (int)$this->getAverageFileSize(), // average file size
      "transfersPerUser" => (int)$this->getAverageTransfersPerUser(), // average transfers per user
      "filesPerUser" => (int)$this->getAverageFilesPerUser(), // average files per user
      "bytesPerTransfer" => (int)$this->getAverageBytesPerTransfer(), // average transfer size
      "filesPerTransfer" => (int)$this->getAverageFilesPerTransfer(), // average files per transfer
      "transferTime" => (int)$this->getAverageTransferTime()
    );
  }

  private function getAverageFileSize() {
    return (int)($this->getTotalBytesCopiedCount() / $this->getTotalFilesTransfered());
  }

  private function getAverageBytesPerTransfer() {
    return (int)($this->getTotalBytesCopiedCount() / $this->getTotalRootTransfers());
  }

  private function getAverageFilesPerTransfer() {
    return (float)number_format((float)$this->getTotalFilesTransfered() / (float)$this->getTotalRootTransfers(), 1);
  }

  private function getAverageTransferTime() {
    return $this->getAverageTransferTimeInSeconds();
    // $avgTime = $this->getAverageTransferTimeInSeconds();
    //
    // $hr = floor($avgTime / (60*60));
    // if ($hr < 10) $hr = "0$hr";
    // $min = floor(($avgTime % (60*60)) / (60));
    // if ($min < 10) $min = "0$min";
    // $sec = floor((($avgTime % (60*60)) % (60)));
    // if ($sec < 10) $sec = "0$sec";
    //
    // return "$hr:$min:$sec";
  }

  private function getTraffic() {
    return array(
//      array(
//        'name' => 'hourly',
//        // 'total' => $hourly_total,
//        $this->getHourlyTraffic(),
//      ),
      array(
        'name' => 'dailyTraffic',
        'values' => $this->getDailyTraffic(),
      ),
    );
  }

  private function getHourlyTraffic() {
    return $this->getTransferTraffic('traffic.hourly', "DATE_FORMAT((CreatedAt - INTERVAL 18 HOUR), '%H')");
  }

  private function getDailyTraffic() {
    return $this->getTransferTraffic('traffic.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')");
  }

  private function getTransferTraffic($cacheSuffix, $field, $limit=false, $order=false) {
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

        $query = DB::table('Usage')->select(DB::raw("$field as 'name', count($field) as 'count'"))->where('ActivityKey', 'like', 'IO%');

        if ($username !== 'guest') {
          $query->where('Username', '=', $username)
                ->where('TenantId', '=', $tenantId);
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
    $query .= "where r.ActivityKey like 'IO%' ";
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

        $query = DB::table('transfertasks')
            ->select(DB::raw("count(distinct owner) as totalUsers"));

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalUsers;
      }
    );
  }

  private function getTotalRootTransfers() {
    $cacheKey = $this->getCachePrefix() . 'total.transfers.root';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $query = DB::table('transfertasks')
            ->select(DB::raw("count(id) as totalUsers"))->whereNull('root_task')->whereNull('parent_task');

        if ($username !== 'guest') {
          $query->where('tenant_id', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        return (int)$query->first()->totalUsers;
      }
    );
  }

  private function getAverageFilesPerUser() {
    $cacheKey = $this->getCachePrefix() . 'average.transfers.root';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $sql = sprintf("select avg(t.user_transfers) as averageFiles
                        from (
                            select count(id) as user_transfers
                            from transfertasks
                            where %s and %s
                            group by owner) t",
                        ($username == 'guest') ? '1=1' : "owner = '$username'",
                        (empty($timeframe) ? '1=1' : "created > '". date('yyyy-mm-dd 00:00:00', $timeframe) . "'"));

        return (int)DB::select($sql);
      }
    );
  }

  private function getAverageTransfersPerUser() {
    $cacheKey = $this->getCachePrefix() . 'average.transfers.all';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $sql = sprintf("select avg(t.user_transfers) as averageTransfers
                        from (
                            select count(id) as user_transfers
                            from transfertasks
                            where %s and %s
                            group by owner) t",
                        ($username == 'guest') ? '1=1' : "owner = '$username'",
                        (empty($timeframe) ? '1=1' : "created > '". date('yyyy-mm-dd 00:00:00', $timeframe) . "'"));

        return (int)DB::select($sql);

      }
    );
  }

  private function getAverageTransferTimeInSeconds() {
    $cacheKey = $this->getCachePrefix() . 'average.transfers.time';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {
        $query = DB::table("transfertasks")
            ->select(DB::raw("SUM((UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time))) as totalTransferTime"))
            ->whereIn('status', array('COMPLETED', 'FAILED'))
            ->whereRaw("(UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time)) < 172800")
            ->whereNotNull("start_time")
            ->whereRaw("IFNULL(end_time, last_updated) is not null");


        if ($username !== 'guest') {
          $query->where('owner', '=', $username)
                ->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }
        // $result = $query->get();
        // dd(DB::getQueryLog());
        // die();
        return (int)$query->first()->totalTransferTime;
        }
    );
  }
}
