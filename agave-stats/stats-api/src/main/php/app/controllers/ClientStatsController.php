<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class ClientStatsController extends AbstractAgaveController
{
    /*
    |--------------------------------------------------------------------------
    | Tenant Stats Controller
    |--------------------------------------------------------------------------
    |
    | Gives a general overview of user stats
    |
    */

    protected function getPrimaryStats()
    {
        return array(
            "timeframe" => $this->getTimeframeString(),
            "totals" => $this->getTotals(),
            "leaders" => $this->getLeaderSeries(),
            "traffic" => $this->getTenantTrafficSeries(),
            // "locations" => $this->getLocations(),
            "_links" => $this->getHypermediaLinks()
        );
    }

    protected function getHypermediaLinks()
    {
        return array(
            'self' => array('href' => URL::action('ClientStatsController@index')),
            'parent' => array('href' => URL::action('SummaryStatsController@index')),
        );
    }

    /**
     * Returns an object with aggregate totals for various user stats.
     */
    private function getTotals()
    {
        return array(
            "total" => $this->getTotalClientCount(),
            "new" => $this->getNewClientCount(),
            "active" => $this->getActiveClientCount()
        );
    }

    private function getTenants()
    {
        $cacheKey = $this->getCachePrefix() . "tenants.list.all";
        $category = $this->getControllerShortName();

        return Cache::remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () {
                return DB::table('tenants')
                    ->select(array('tenant_id', 'name'))
                    ->where('status', '<>', 'DISABLED')
                    ->where('tenant_id', '<>', 'irmacs')
                    ->get();
            }
        );
    }

    private function getTenantById($tenantId)
    {
        foreach ($this->getTenants() as $tenant) {
            if ($tenant->tenant_id) {
                return $tenant;
            }
        }
        return null;
    }

    private function getTotalClientCount()
    {
        $total = 0;

        foreach ($this->getTenants() as $tenant) {
            $total += count($this->apim->getAllApplicationsForTenant($tenant->tenant_id));
        }

        return $total;
    }

    private function getClientCountPerTenant()
    {
        $tenants = array();

        foreach ($this->getTenants() as $tenant) {
            $tenants[] = array(
                "name" => $tenant->name,
                "tenantId" => $tenant->tenant_id,
                "count" => count($this->apim->getAllApplicationsForTenant($tenant->tenant_id)));
        }

        return $tenants;
    }

    /**
     * Adds up new user registrations over the request timeframe.
     */
    private function getNewClientCount()
    {
        $monthlyNewClients = $this->getNewClientsByMonth(false, 'DESC');
//        $total = 0;
        foreach ($monthlyNewClients as $count) {
//            $total += $count;
            return $count;
        }

//        return $total;
    }

    /**
     * Adds up new active clients over time.
     */
    private function getActiveClientCount()
    {
        $activeClients = $this->getClients('clients.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')", false, false);

        return count($activeClients);
    }

    /**
     * Returns an array of leader series objects containing the top 10
     * entries in each leader category.
     */
    private function getLeaderSeries()
    {
        return array(
            array(
                'name' => 'tenants',
                'values' => $this->getClientCountPerTenant()
            ),
            array(
                'name' => 'activity',
                'values' => $this->getActivityLeaders()
            ),
            array(
                'name' => 'users',
                'values' => $this->getEndUserLeaders()
            ),
        );
    }

    /**
     * Weighted locations from which clients are accessing agave.
     */
    private function getLocations()
    {
        $cacheKey = $this->getCachePrefix() . "locations.ip";
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        $query = "select r.UserIP as 'ip', count(r.TenantId) as 'cnt' from `Usage` r ";
        $query = $this->applyQueryConditions($query, 'usage');
        $query .= " group by r.TenantId order by count(r.TenantId)";

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function () use ($query) {

            $reverseIpUrl = $_ENV['freegeoipUrl'] . '/json/';
            $resolvedIps = array();

            foreach (DB::select($query) as $usageRecord) {

                // resolve geocoordinates for ip address
                $ipGeoInfo = Cache::tags('traffic', 'ips')->rememberForever($usageRecord->ip, function () use ($usageRecord, $reverseIpUrl) {
                    Log::debug("Geolocating ip address at $reverseIpUrl" . $usageRecord->ip . "...");
                    $response = file_get_contents($reverseIpUrl . $usageRecord->ip);

                    if (empty($response)) {
                        Log::debug("No response from geolocation lookup at $reverseIpUrl" . $usageRecord->ip);
                        return array(
                            "lat" => null,
                            "lng" => null,
                            "country" => null,
                            "city" => null,
                            "region" => null
                        );
                    } else {
                        $json = json_decode($response, true);

                        Log::debug("Successfully resolved geolocation for " . $usageRecord->ip . ": " . $response);

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
     * Retrieves the top 10 clients by activity over the request timeframe
     */
    private function getActivityLeaders()
    {
        $cacheKey = $this->getCachePrefix() . 'leaders.activity';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId) {

                $topClients = array();

                $userTraffic = $this->getClients('clients.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')");

                foreach ($userTraffic as $row) {
                    if (!empty($topClients[$row->TenantId . '-' . $row->ClientApplication])) {
                        $topClients[$row->TenantId . '-' . $row->ClientApplication] += $row->requests;
                    } else {
                        $topClients[$row->TenantId . '-' . $row->ClientApplication] = $row->requests;
                    }
                }

                arsort($topClients);

                $topClients = array_slice($topClients, 0, 10);

                foreach ($topClients as $name => $count) {
                    list($tenantId, $clientId) = explode("-", $name);
                    $clientDetails = $this->apim->getUserApplicationDetails($tenantId, $clientId);
                    $topClients[$name] = array(
                        'name' => empty($clientDetails['NAME']) ? $clientId : $clientDetails['NAME'],
                        'clientId' => $clientId,
//                        'owner' => $clientDetails['USER_ID'],
                        'tenant' => $tenantId,
                        'count' => $count
                    );
                }

                return array_values($topClients);
            }
        );
    }

    /**
     * Retrieves the top 10 clients by unique end user total over the request timeframe
     */
    private function getEndUserLeaders()
    {
        $cacheKey = $this->getCachePrefix() . 'leaders.endUsers';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId) {

                $topClients = array();

                $userTraffic = $this->getClientsEndUserCount(10, 'DESC');

                foreach ($userTraffic as $row) {
                    if (!empty($topClients[$row->TenantId . '-' . $row->ClientApplication])) {
                        $topClients[$row->TenantId . '-' . $row->ClientApplication] += $row->requests;
                    } else {
                        $topClients[$row->TenantId . '-' . $row->ClientApplication] = $row->requests;
                    }
                }

                arsort($topClients);

                $topClients = array_slice($topClients, 0, 10);

                foreach ($topClients as $name => $count) {
                    list($tenantId, $clientId) = explode("-", $name);
                    $clientDetails = $this->apim->getUserApplicationDetails($tenantId, $clientId);
                    $topClients[$name] = array(
                        'name' => empty($clientDetails['NAME']) ? $clientId : $clientDetails['NAME'],
                        'clientId' => $clientId,
//                        'owner' => $clientDetails['USER_ID'],
                        'tenant' => $tenantId,
                        'count' => $count
                    );
                }

                return array_values($topClients);
            }
        );
    }

    /**
     * Returns an array of leader objects representing top clients by various categories.
     */
    private function getTenantTrafficSeries()
    {
        return array(
//            array(
//                'name' => 'dailyTotal',
//                'values' => $this->getTotalClientsByDay(),
//            ),
            array(
                'name' => 'monthlyTotal',
                'values' => $this->getTotalClientsByMonth(),
            ),
//            array(
//                'name' => 'dailyNew',
//                'values' => $this->getNewClientsByDay(),
//            ),
            array(
                'name' => 'monthlyNew',
                'values' => $this->getNewClientsByMonth(),
            ),
        );
    }

    /**
     * Number of clients by month during the request timeframe.
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getTotalClientsByMonth($limit = false, $order = false)
    {
        $clients = $this->getClients('clients.all.monthly', "DATE_FORMAT(CreatedAt, '%Y-%m')", $limit, $order);
        $monthlyClients = array();
        foreach ($clients as $row) {
            if (empty($monthlyClients[$row->monthOfUse])) {
                $monthlyClients[$row->monthOfUse] = 1;
            } else {
                $monthlyClients[$row->monthOfUse]++;
            }
        }
        return $monthlyClients;
    }

    /**
     * Number of clients by day during the request timeframe.
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getTotalClientsByDay($limit = false, $order = false)
    {
        $clients = $this->getClients('clients.all.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')", $limit, $order);
        $dailyClients = array();
        foreach ($clients as $row) {
            if (empty($dailyClients[$row->monthOfUse])) {
                $dailyClients[$row->monthOfUse] = 1;
            } else {
                $dailyClients[$row->monthOfUse]++;
            }
        }
        return $dailyClients;
    }

    /**
     * Retrieves the number of requests per client by month over the request timeframe
     *
     * @param string $cacheKey the unique key used to store this query result in the cache
     * @param string $field the date format string by which to group the results
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getClients($cacheKey, $field, $limit = false, $order = false)
    {
        $cacheKey = $this->getCachePrefix() . $cacheKey;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($field, $limit, $order, $timeframe, $username, $tenantId) {

                $query = DB::table('Usage')
                    ->select(DB::raw("$field as monthOfUse, `ClientApplication`, `TenantId`, count(`UID`) as requests"));

                if (!empty($timeframe)) {
                    $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                $query->groupBy(DB::raw("$field, TenantId, ClientApplication"));

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
     * Retrieves the end user count per client by month over the request timeframe
     *
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getClientsEndUserCount($limit = false, $order = false)
    {
        $cacheKey = $this->getCachePrefix() . 'leaders.endUsers';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($limit, $order, $timeframe, $username, $tenantId) {

                $query = DB::table('Usage')
                    ->select(DB::raw("`ClientApplication`, `TenantId`, count(distinct `Username`) as 'userCount'"));

                if (!empty($timeframe)) {
                    $query->where('CreatedAt', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                $query->groupBy(DB::raw("TenantId, ClientApplication"));

                if ($order) {
                    $query->orderBy(DB::raw("`userCount`"), 'desc');
                }

                if ($limit) {
                    $query->take($limit);
                }

                $results = $query->get();

                $topClients = array();
                foreach ($results as $row) {
                    $clientDetails = $this->apim->getUserApplicationDetails($row->TenantId, $row->ClientApplication);
                    $topClients[] = array(
                        'name' => empty($clientDetails['NAME']) ? $row->ClientApplication : $clientDetails['NAME'],
                        'clientId' => $row->ClientApplication,
//                        'owner' => $clientDetails['USER_ID'],
                        'tenant' => $row->TenantId,
                        'count' => $row->userCount
                    );
                }

                return $topClients;
            }
        );
    }

    /**
     * Number of first-time clients by month during the request timeframe.
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getNewClientsByMonth($limit = false, $order = false)
    {
        $clients = $this->getNewClients('clients.new.monthly', "DATE_FORMAT(MIN(CreatedAt), '%Y-%m')", $limit, $order);
        $monthlyClients = array();
        foreach ($clients as $row) {
            $monthlyClients[$row->firstUse] = $row->newClients;
        }
        return $monthlyClients;
    }

    /**
     * Number of first-time clients by day during the request timeframe.
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getNewClientsByDay($limit = false, $order = false)
    {
        $clients = $this->getNewClients('clients.new.daily', "DATE_FORMAT(MIN(CreatedAt), '%Y-%m-%d')", $limit, $order);
        $dailyClients = array();
        foreach ($clients as $row) {
            $dailyClients[$row->firstUse] = $row->newClients;
        }
        return $dailyClients;
    }

    /**
     * Retrieves the number of first time clients by month over the request timeframe.
     *
     * @param int $limit max results, default all
     * @param string $order sort ASC or DESC by month.
     */
    private function getNewClients($cacheKey, $field, $limit = false, $order = false)
    {
        $cacheKey = $this->getCachePrefix() . $cacheKey;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($field, $limit, $order, $timeframe, $username, $tenantId) {
                $sql = sprintf(
                    'select s.firstUse, count(s.ClientApplication) as newClients
            from (
                select %s as firstUse, uu.ClientApplication, uu.TenantID as TenantID
                from `Usage` uu
                where %s %s
                group by uu.ClientApplication, uu.TenantID
              ) s
            group by s.firstUse %s %s ',
                    $field,
                    (empty($timeframe) ? '1=1' : "where MIN(uu.CreatedAt) > '" . date('yyyy-mm-dd 00:00:00', $timeframe) . "' "),
                    ($username !== 'guest' ? "and uu.TenantId = '" . $tenantId . "'" : ''),
                    ($order ? "order by s.firstUse $order " : ''),
                    ($limit ? "limit $limit" : ''));
                return DB::select($sql);

            }
        );
    }
}
