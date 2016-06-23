<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class TenantsStatsController extends AbstractAgaveController
{
  /*
  |--------------------------------------------------------------------------
  | Tenant Stats Controller
  |--------------------------------------------------------------------------
  |
  | Gives a general overview of user stats
  |
  */

  protected function getPrimaryStats() {
    return array(
      "timeframe" => $this->getTimeframeString(),
      "totals" => $this->getTotals(),
      "leaders" => $this->getLeaderSeries(),
      "traffic" => $this->getTenantTrafficSeries(),
      // "locations" => $this->getLocations(),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('TenantsStatsController@index')),
      'parent' => array('href' => URL::action('SummaryStatsController@index')),
    );
  }

  /**
   * Returns an object with aggregate totals for various user stats.
   */
  private function getTotals() {
    $total = $this->getTotalTenantCount();
    $new = $this->getNewTenantCount();
    return array(
      "total" => $total,
      "new" => $new,
      "active" => ($total - $new)
    );
  }

  private function getTotalTenantCount() {
    $total = 0;
    $monthlyTenants = $this->getTotalTenantsByMonth();
    foreach ($monthlyTenants as $count) {
      $total += $count;
    }
    return $total;
  }

  /**
   * Adds up new user registrations over the request timeframe.
   */
  private function getNewTenantCount() {
    $monthlyNewTenants = $this->getNewTenantsByMonth();
    $total = 0;
    foreach($monthlyNewTenants as $count) {
      $total += $count;
    }
    return $total;
  }

  /**
  * Returns an array of leader series objects containing the top 10
  * entries in each leader category.
  */
  private function getLeaderSeries() {
    return array(
      array(
        'name' => 'activity',
        'values' => $this->getActivityLeaders()
      )
    );
  }

  /**
   * Weighted locations from which tenants are accessing agave.
   */
  private function getLocations() {
    $cacheKey = $this->getCachePrefix() . "locations.ip";
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    $query = "select r.UserIP as 'ip', count(r.TenantId) as 'cnt' from `Usage` r ";
    $query = $this->applyQueryConditions($query, 'usage');
    $query .= " group by r.TenantId order by count(r.TenantId)";

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
   * Retrieves the top 10 tenants by activity over the request timeframe
   */
  private function getActivityLeaders() {
    $cacheKey = $this->getCachePrefix() . 'leaders.activity';
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($timeframe, $username, $tenantId) {

        $topTenants = array();

        $userTraffic = $this->getTenants('tenants.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')");

        foreach($userTraffic as $row) {
//          if ($row->status !== 'OFFLINE' ) {
            if (!empty($topTenants[$row->TenantId])) {
              $topTenants[$row->TenantId] += $row->requests;
            } else {
              $topTenants[$row->TenantId] = $row->requests;
            }
//          }
        }

        arsort($topTenants);

        $topTenants = array_slice($topTenants, 0, 10);

        foreach($topTenants as $name=>$count) {
          $topTenants[$name] = array( 'name' => $name, 'count' => $count );
        }

        return array_values($topTenants);
      }
    );
  }

  /**
   * Returns an array of leader objects representing top tenants by various categories.
   */
  private function getTenantTrafficSeries() {
    return array(
      array(
        'name' => 'dailyTotal',
        'values' => $this->getTotalTenantsByDay(),
      ),
      array(
        'name' => 'monthlyTotal',
        'values' => $this->getTotalTenantsByMonth(),
      ),
      array(
        'name' => 'dailyNew',
        'values' => $this->getNewTenantsByDay(),
      ),
      array(
        'name' => 'monthlyNew',
        'values' => $this->getNewTenantsByMonth(),
      ),
    );
  }

  /**
   * Number of tenants by month during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getTotalTenantsByMonth($limit=false, $order=false) {
    $tenants = $this->getTenants('tenants.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')", $limit, $order);
    $monthlyTenants = array();
    foreach ($tenants as $row) {
      if (empty($monthlyTenants[$row->monthOfUse])) {
        $monthlyTenants[$row->monthOfUse] = 1;
      } else {
        $monthlyTenants[$row->monthOfUse]++;
      }
    }
    return $monthlyTenants;
  }

  /**
   * Number of tenants by day during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getTotalTenantsByDay($limit=false, $order=false) {
    $tenants = $this->getTenants('tenants.all.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')", $limit, $order);
    $dailyTenants = array();
    foreach ($tenants as $row) {
      if (empty($dailyTenants[$row->monthOfUse])) {
        $dailyTenants[$row->monthOfUse] = 1;
      } else {
        $dailyTenants[$row->monthOfUse]++;
      }
    }
    return $dailyTenants;
  }

  /**
   * Retrieves the number of requests per tenant by month over the request timeframe
   *
   * @param string $cacheKey the unique key used to store this query result in the cache
   * @param string $field the date format string by which to group the results
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getTenants($cacheKey, $field, $limit=false, $order=false) {
    $cacheKey = $this->getCachePrefix() . $cacheKey;
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }

    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($field, $limit, $order, $timeframe, $username, $tenantId) {

        $query = DB::table('Usage')
            ->select(DB::raw("$field as monthOfUse, `TenantId`, count(`UID`) as requests"));

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy(DB::raw("$field, TenantId"));

        if ($order) {
          $query->orderBy(DB::raw("count(UID)"), 'desc');
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
   * Number of first-time tenants by month during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewTenantsByMonth($limit=false, $order=false) {
    $tenants = $this->getNewTenants('tenants.new.monthly', "DATE_FORMAT(MIN(created), '%Y-%m')", $limit, $order);
    $monthlyTenants = array();
    foreach ($tenants as $row) {
      $monthlyTenants[$row->firstUse] = $row->newTenants;
    }
    return $monthlyTenants;
  }

  /**
   * Number of first-time tenants by day during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewTenantsByDay($limit=false, $order=false) {
    $tenants = $this->getNewTenants('tenants.new.daily', "DATE_FORMAT(MIN(created), '%Y-%m-%d')", $limit, $order);
    $dailyTenants = array();
    foreach ($tenants as $row) {
      $dailyTenants[$row->firstUse] = $row->newTenants;
    }
    return $dailyTenants;
  }

  /**
   * Retrieves the number of first time tenants by month over the request timeframe.
   *
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewTenants($cacheKey, $field, $limit=false, $order=false) {
    $cacheKey = $this->getCachePrefix() . $cacheKey;
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    if ($this->force) {
      Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix().$cacheKey);
    }



    return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix().$cacheKey, $_ENV['cacheLifetime'],
      function() use ($field, $limit, $order, $timeframe, $username, $tenantId) {
        $sql = sprintf(
            'select s.firstUse, count(s.tenant_id) as newTenants
            from (
                select %s as firstUse, uu.tenant_id as tenant_id
                from `tenants` uu
                where %s
                group by uu.tenant_id
              ) s
            group by s.firstUse %s %s ',
            $field,
            (empty($timeframe) ? '1=1' : "where MIN(uu.created) > '". date('yyyy-mm-dd 00:00:00', $timeframe) ."' "),
//            ($username !== 'guest' ? "and uu.tenant_id = '".$tenantId."'" : ''),
            ($order ? "order by s.firstUse $order " : ''),
            ($limit ? "limit $limit" : ''));
        return DB::select($sql);
      }
    );
  }
}
