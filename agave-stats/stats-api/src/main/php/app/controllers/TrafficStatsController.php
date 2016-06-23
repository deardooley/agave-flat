<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;
use Agave\Bundle\ApiBundle\Entity\Usage as Usage;

class TrafficStatsController extends AbstractAgaveController {

  /*
  |--------------------------------------------------------------------------
  | Traffic Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of traffic stats
  |
  */

  protected function getPrimaryStats() {
    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => $this->getTotals(),
      "leaders" => $this->getLeaderSeries(),
      "traffic" => $this->getTrafficSeries(),
      "locations" => $this->getLocations(),
//      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
			'self' => array('href' => URL::action('TrafficStatsController@index')),
      'parent' => array('href' => URL::action('StatsController@index')),
    );
  }

	/**
	 * Returns an object with aggregate totals for various traffic stats.
	 */
  private function getTotals() {
    return array(
      "requests" => (int)$this->getTotalRequestCount(),
    );
  }

  /**
	 * Returns an array of traffic series objects containing the top 10
	 * entries in each traffic category.
	 */
  private function getTrafficSeries() {
    return array(
      array(
        'name' => 'hourly',
        // 'total' => $hourly_total,
        'values' => $this->getHourlyTraffic(),
      ),
      array(
        'name' => 'dailyTraffic',
        'values' => $this->getDailyTraffic(),
      ),
    );
  }

  /**
  * Returns an array of leader series objects containing the top 10
  * entries in each leader category.
  */
  private function getLeaderSeries() {
    return array(
      array(
        'name' => 'countries',
        'values' => $this->getCountryLeaders(),
      ),
      array(
        'name' => 'clients',
        'values' => $this->getClientLeaders(10),
      ),
      array(
        'name' => 'tenants',
        'values' => $this->getTenantLeaders(),
      ),
    );
  }

  private function getHourlyTraffic() {
    return $this->getSeries('traffic.hourly', "DATE_FORMAT((CreatedAt - INTERVAL 18 HOUR), '%H')");
  }

  private function getDailyTraffic() {
    return $this->getSeries('traffic.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')");
  }

  private function getTenantLeaders() {
    $tenantLeaders = $this->getSeries('leaders.tenants', 'TenantId', 10, true);
    $tenants = Cache::get('tenantIds');
    $leaders = array();
    foreach($tenantLeaders as $index => $leader) {
      if (!in_array($leader->name, $tenants)) {
        unset($tenantLeaders[$index]);
      } else {
        $leader->name = DB::table('tenants')->select('name')->where('tenant_id', '=', $leader->name)->first()->name;
      }
    }
    return array_values($tenantLeaders);
  }

  private function getCountryLeaders() {
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

  /**
  * Retrieves the requests per IP during the given timeframe.
  */
  private function getLocations() {
    $cacheKey = $this->getCachePrefix() . "locations.ip";
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    $query = "select r.UserIP as 'ip', count(r.UserIP) as 'cnt' from `Usage` r ";
    $query = $this->applyQueryConditions($query, 'usage');
    $query .= " group by r.UserIP order by count(r.UserIP)";

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

  /**
   * Returns traffic leaders by client identity.
   *
   * @param bool|false $limit
   * @param string $order
   * @return mixed
     */
  private function getClientLeaders($limit=false, $order='DESC') {
    $cacheKey = $this->getCachePrefix() . 'leaders.clients';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
        function() use ($limit, $order, $timeframe, $username, $tenantId) {

          $query = DB::table('Usage')->select(DB::raw("TenantId, ClientApplication as 'name', count(UID) as 'totalClients'"));

          if ($username !== 'guest') {
            $query->where('Username', '=', $username)
                ->where('TenantId', '=', $tenantId);
          }

          if (!empty($timeframe)) {
            $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
          }

          $query->groupBy('TenantId', 'ClientApplication');

          if ($order) {
            $query->orderBy(DB::raw("totalClients"), 'desc');
          }

          if ($limit) {
            $query->take($limit);
          }
          $result = $query->get();

          $leaders = array();
          foreach($result as $leader) {
            $clientDetails = $this->apim->getUserApplicationDetails($leader->TenantId, $leader->name);
            $leaders[] = array(
                'name' => empty($clientDetails['NAME']) ? $leader->name : $clientDetails['NAME'],
                'clientId' => $leader->name,
//              'owner' => $clientDetails['USER_ID'],
                'tenant' => $leader->TenantId,
                'count' => $leader->totalClients
            );
          }

          return $leaders;
        }
    );
  }

  private function getSeries($cacheSuffix, $field, $limit=false, $order=false) {
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

        $query = DB::table('Usage')->select(DB::raw("$field as 'name', count($field) as 'count'"));

        if ($username !== 'guest') {
          $query->where('Username', '=', $username)
                ->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy(DB::raw($field));

        if ($order) {
          $query->orderBy(DB::raw("'count'"), 'desc');
        }

        if ($limit) {
          $query->take($limit);
        }
        $result = $query->get();

        return $result;
      }
    );
  }

  private function getTotalRequestCount() {
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

	private function runQuery($query, $statName, $maxResults=false) {
  	$key = $this->getCachePrefix() . $statName;
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      $result = $this->em->createQuery($query)->getSingleScalarResult();
      Cache::tags($category, $tenantId, $username, $timeframe)->put($key, $result, $_ENV['cacheLifetime']);
      return $result;
    } else {
      return Cache::tags($category, $tenantId, $username, $timeframe)->remember($key, $_ENV['cacheLifetime'], function() use ($query) {
        return $this->em->createQuery($query)->getSingleScalarResult();
      });
    }
  }

}
