<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class UserStatsController extends AbstractAgaveController
{
  /*
  |--------------------------------------------------------------------------
  | User Stats Controller
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
      "traffic" => $this->getUserTrafficSeries(),
      // "locations" => $this->getLocations(),
      "_links" => $this->getHypermediaLinks()
    );
  }

  protected function getHypermediaLinks() {
    return array(
      'self' => array('href' => URL::action('UserStatsController@index')),
      'parent' => array('href' => URL::action('SummaryStatsController@index')),
    );
  }

  /**
   * Returns an object with aggregate totals for various user stats.
   */
  private function getTotals() {
    $total = $this->getTotalUserCount();
    $new = $this->getNewUserCount();
    return array(
      "total" => $total,
      "new" => $new,
      "active" => ($total - $new)
    );
  }

  private function getTotalUserCount() {
    $total = 0;
    $monthlyUsers = $this->getTotalUsersByMonth();
    foreach ($monthlyUsers as $count) {
      $total += $count;
    }
    return $total;
  }

  /**
   * Adds up new user registrations over the request timeframe.
   */
  private function getNewUserCount() {
    $monthlyNewUsers = $this->getNewUsersByMonth();
    $total = 0;
    foreach($monthlyNewUsers as $count) {
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
   * Weighted locations from which users are accessing agave.
   */
  private function getLocations() {
    $cacheKey = $this->getCachePrefix() . "locations.ip";
    $category = $this->getControllerShortName();
    $tenantId = JWTClient::getCurrentTenant();
    $username = JWTClient::getCurrentEndUser();
    $timeframe = $this->timeframe;

    $query = "select r.UserIP as 'ip', count(r.Username) as 'cnt' from `Usage` r ";
    $query = $this->applyQueryConditions($query, 'usage');
    $query .= " group by r.UserIP order by count(r.Username)";

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
   * Retrieves the top 10 users by activity over the request timeframe
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

        $topUsers = array();

        $userTraffic = $this->getUsers('users.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')");

        foreach($userTraffic as $row) {
          if (!in_array($row->Username, array('sterry1', 'systest', 'testuser', 'ipctest', 'jstubbs', 'dooley', 'ldapbind'))  ) {
            if (!empty($topUsers[$row->Username])) {
              $topUsers[$row->Username] += $row->requests;
            } else {
              $topUsers[$row->Username] = $row->requests;
            }
          }
        }

        arsort($topUsers);

        $topUsers = array_slice($topUsers, 0, 10);

        foreach($topUsers as $name=>$count) {
          $topUsers[$name] = array( 'name' => $name, 'count' => $count );
        }

        return array_values($topUsers);
      }
    );
  }

  /**
   * Returns an array of leader objects representing top users by various categories.
   */
  private function getUserTrafficSeries() {
    return array(
      array(
        'name' => 'dailyTotal',
        'values' => $this->getTotalUsersByDay(),
      ),
      array(
        'name' => 'monthlyTotal',
        'values' => $this->getTotalUsersByMonth(),
      ),
      array(
        'name' => 'dailyNew',
        'values' => $this->getNewUsersByDay(),
      ),
      array(
        'name' => 'monthlyNew',
        'values' => $this->getNewUsersByMonth(),
      ),
    );
  }

  /**
   * Number of users by month during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getTotalUsersByMonth($limit=false, $order=false) {
    $users = $this->getUsers('users.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')", $limit, $order);
    $monthlyUsers = array();
    foreach ($users as $row) {
      if (empty($monthlyUsers[$row->monthOfUse])) {
        $monthlyUsers[$row->monthOfUse] = 1;
      } else {
        $monthlyUsers[$row->monthOfUse]++;
      }
    }
    return $monthlyUsers;
  }

  /**
   * Number of users by day during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getTotalUsersByDay($limit=false, $order=false) {
    $users = $this->getUsers('users.all.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')", $limit, $order);
    $dailyUsers = array();
    foreach ($users as $row) {
      if (empty($dailyUsers[$row->monthOfUse])) {
        $dailyUsers[$row->monthOfUse] = 1;
      } else {
        $dailyUsers[$row->monthOfUse]++;
      }
    }
    return $dailyUsers;
  }

  /**
   * Retrieves the number of requests per user by month over the request timeframe
   *
   * @param string $cacheKey the unique key used to store this query result in the cache
   * @param string $field the date format string by which to group the results
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getUsers($cacheKey, $field, $limit=false, $order=false) {
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

        $query = DB::table('Usage')
            ->select(DB::raw("$field as monthOfUse, `Username`, count(`UID`) as requests"));

        if ($username !== 'guest') {
          $query->where('TenantId', '=', $tenantId);
        }

        if (!empty($timeframe)) {
          $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
        }

        $query->groupBy(DB::raw("$field, Username"));

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
   * Number of first-time users by month during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewUsersByMonth($limit=false, $order=false) {
    $users = $this->getNewUsers('users.new.monthly', "DATE_FORMAT(MIN(CreatedAt), '%Y-%m')", $limit, $order);
    $monthlyUsers = array();
    foreach ($users as $row) {
      $monthlyUsers[$row->firstUse] = $row->newUsers;
    }
    return $monthlyUsers;
  }

  /**
   * Number of first-time users by day during the request timeframe.
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewUsersByDay($limit=false, $order=false) {
    $users = $this->getNewUsers('users.new.daily', "DATE_FORMAT(MIN(CreatedAt), '%Y-%m-%d')", $limit, $order);
    $dailyUsers = array();
    foreach ($users as $row) {
      $dailyUsers[$row->firstUse] = $row->newUsers;
    }
    return $dailyUsers;
  }

  /**
   * Retrieves the number of first time users by month over the request timeframe.
   *
   * @param int $limit max results, default all
   * @param string $order sort ASC or DESC by month.
   */
  private function getNewUsers($cacheKey, $field, $limit=false, $order=false) {
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
            'select s.firstUse, count(s.uname) as newUsers
            from (
                select %s as firstUse, uu.Username as uname
                from `Usage` uu
                where %s %s
                group by uu.Username
              ) s
            group by s.firstUse %s %s ',
            $field,
            (empty($timeframe) ? '1=1' : "where MIN(uu.CreatedAt) > '". date('yyyy-mm-dd 00:00:00', $timeframe) ."' "),
            ($username !== 'guest' ? "and uu.TenantId = '".$tenantId."'" : ''),
            ($order ? "order by s.firstUse $order " : ''),
            ($limit ? "limit $limit" : ''));
        return DB::select($sql);

      }
    );
  }
}
