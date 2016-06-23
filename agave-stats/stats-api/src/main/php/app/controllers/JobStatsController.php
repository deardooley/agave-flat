<?php
use Agave\Bundle\ApiBundle\Auth\JWTClient;

class JobStatsController extends AbstractAgaveController
{

    /*
    |--------------------------------------------------------------------------
    | Job Stats Controller
    |--------------------------------------------------------------------------
    |
    | Gives a general overview of job stats
    |
    */

    protected function getPrimaryStats()
    {
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

    protected function getHypermediaLinks()
    {
        return array(
            'self' => array('href' => URL::action('JobStatsController@index')),
            'parent' => array('href' => URL::action('SummaryStatsController@index')),
        );
    }

    /**
     * Returns an object with aggregate totals for various user stats.
     */
    private function getTotals()
    {
        return array(
            "jobs" => (int)$this->getTotalJobCount(),
            "cores" => (int)$this->getTotalCoreCount(),
            "nodes" => (int)$this->getTotalNodeCount(),
            "systems" => (int)$this->getUniqueValueCountOfJobTableColumn('total.systems', 'execution_system'),
            "runTime" => (int)$this->getTotalRunTimeInSeconds(),
            "users" => (int)$this->getUniqueJobOwnerCount()
        );
    }

    private function getTotalCoreCount()
    {
        return (int)$this->getSumOfJobTableColumn('total.cores', 'processor_count');
    }

    private function getTotalNodeCount()
    {
        return (int)$this->getSumOfJobTableColumn('total.nodes', 'node_count');
    }

    /**
     * Returns total number of jobs
     * @return mixed
     */
    private function getTotalJobCount()
    {
        $cacheKey = $this->getCachePrefix() . 'total.jobs';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId) {

                $query = DB::table('jobs')
                    ->select(DB::raw("count(id) as totalJobs"));

                if ($username !== 'guest') {
                    $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                return (int)$query->first()->totalJobs;
            }
        );
    }

    /**
     * Returns total number of jobs
     * @return mixed
     */
    private function getTotalCompletedJobCount()
    {
        $cacheKey = $this->getCachePrefix() . 'total.jobs';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId) {

                $query = DB::table('jobs')
                    ->select(DB::raw("count(id) as totalJobs"))
                    ->whereNotNull("start_time")
                    ->whereNotNull("end_time");

                if ($username !== 'guest') {
                    $query->where('owner', '=', $username)->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                return (int)$query->first()->totalJobs;
            }
        );
    }

    /**
     * Sums all the values in a column of the database
     * @param $cacheSuffix
     * @param $column The column to sum
     * @return mixed
     */
    private function getSumOfJobTableColumn($cacheSuffix, $column)
    {
        $cacheKey = $this->getCachePrefix() . $cacheSuffix;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

//    if ($this->force) {
        Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
//    }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId, $column) {

                $query = DB::table('jobs')
                    ->select(DB::raw("sum((" . $column . ")) as totalValue"));

                if ($username !== 'guest') {
                    $query->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                return (int)$query->first()->totalValue;
            }
        );
    }

    /**
     * Counts the number of unique values in a column of the job table
     * @param $cacheSuffix
     * @param $column name of the column to count unique values
     * @return mixed
     */
    private function getUniqueValueCountOfJobTableColumn($cacheSuffix, $column)
    {
        $cacheKey = $this->getCachePrefix() . $cacheSuffix;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId, $column) {

                $query = DB::table('jobs')
                    ->select(DB::raw("count(distinct " . $column . ", tenant_id) as uniqueValueCount"));

                if ($username !== 'guest') {
                    $query->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                return (int)$query->first()->uniqueValueCount;
            }
        );
    }

    /**
     * Calculates total job runtime in seconds.
     * @return mixed
     */
    private function getTotalRunTimeInSeconds()
    {
        $cacheKey = $this->getCachePrefix() . 'total.runTime';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($timeframe, $username, $tenantId) {

                // $query = DB::select("SELECT SUM((UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time))) as runTime
                //                      FROM jobs
                //                      WHERE `status` in ('FINISHED', 'FAILED', 'ARCHIVING_FAILED', 'STOPPED', 'KILLED')
                //                         AND (end_time IS NOT NULL OR last_updated IS NOT NULL)
                //                         AND start_time IS NOT NULL
                //                         AND (UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time)) < 172800");
                $query = DB::table("jobs")
                    ->select(DB::raw("SUM((UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time))) as totalRunTime"))
                    ->whereIn('status', array('FINISHED', 'FAILED', 'ARCHIVING_FAILED', 'STOPPED', 'KILLED'))
                    ->whereRaw("(UNIX_TIMESTAMP(IFNULL(end_time, last_updated)) - UNIX_TIMESTAMP(start_time)) < 172800")
                    ->whereNotNull("start_time")
                    ->whereNotNull("end_time");
//            ->whereRaw("IFNULL(end_time, last_updated) is not null");


                if ($username !== 'guest') {
                    $query->where('owner', '=', $username)
                        ->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }
                // $result = $query->get();
                // dd(DB::getQueryLog());
                // die();
                return $query->first()->totalRunTime;
            }
        );
    }

    private function getUniqueJobOwnerCount()
    {
        return (int)$this->getUniqueValueCountOfJobTableColumn('total.owners', 'owner');
    }

    /**
     * Returns arrays of leaders in the operations categories.
     */
    private function getLeadersSeries()
    {
        return [
            array(
                "category" => "countries",
                "values" => $this->getCountryLeaders()
            ),
            array(
                "category" => "schedulers",
                "values" => $this->getJobExecutionSystemSeriesLeaders('leaders.schedulers', 'sys.scheduler_type', 10, 'DESC')
            ),
            array(
                "category" => "executionType",
                "values" => $this->getJobExecutionSystemSeriesLeaders('leaders.executionTypes', 'sys.execution_type', 10, 'DESC')
            ),
            array(
                "category" => "systems",
                "values" => $this->getJobExecutionSystemSeriesLeaders('leaders.systems', 'sys.name as systemName, sys.system_id', 10, 'DESC')
            ),
            array(
                "category" => "minQueueWaitTime",
                "values" => $this->getJobQueueWaitTimeSeriesLeaders('leaders.minWaitTime', 10, 'ASC')
            ),
            array(
                "category" => "maxQueueWaitTime",
                "values" => $this->getJobQueueWaitTimeSeriesLeaders('leaders.maxWaitTime', 10, 'DESC')
            )
        ];
    }

    /**
     * Returns to to job submission countries.
     */
    private function getCountryLeaders()
    {
        $cacheKey = $this->getCachePrefix() . 'leaders.countries';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'], function () {
            $countries = array();
            $locations = $this->getLocations();

            foreach ($locations as $location) {
                if (empty($countries[$location['country']])) {
                    $countries[$location['country']] = $location['requests'];
                } else {
                    $countries[$location['country']] += $location['requests'];
                }
            }

            arsort($countries);

            $countries = array_slice($countries, 0, 10);

            foreach ($countries as $name => $count) {
                $countries[$name] = array('name' => $name, 'count' => $count);
            }

            return array_values($countries);
        });
    }

    /**
     * Returns to to job scheduler used.
     */
    private function getJobSystemRunTimeLeaders($limit = false)
    {
        $cacheKey = $this->getCachePrefix() . 'runTime';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
            function () use ($username, $timeframe, $tenantId, $limit) {
                $query = DB::table('jobs')
                    ->select(DB::raw("execution_system, count(id) as total_system_jobs, ROUND(AVG(TIME_TO_SEC(requested_time)) / 60,2) as 'avg_request_min', ROUND(AVG((UNIX_TIMESTAMP(end_time) - UNIX_TIMESTAMP(start_time)) / 60),2) as 'avg_actual_min', (SUM(TIME_TO_SEC(requested_time) - (UNIX_TIMESTAMP(end_time) - UNIX_TIMESTAMP(start_time))) / 60) / count(id) as 'avg_excess_min"))
                    ->whereNotNull('start_time')
                    ->whereNotNull('end_time');

                if ($username !== 'guest') {
                    $query->where('tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }

                $query->groupBy(DB::raw("execution_system, tenant_id"));

                $query->orderBy(DB::raw("avg_excess_min"), 'asc');

                if ($limit) {
                    $query->take($limit);
                }

                return $query->get();
            });
    }

    /**
     * Returns to to job scheduler used.
     */
    private function getJobQueueWaitTimeSeriesLeaders($cacheSuffix, $limit = false, $order = 'DESC')
    {
        $cacheKey = $this->getCachePrefix() . $cacheSuffix;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
            function () use ($username, $timeframe, $tenantId, $limit, $order) {
//          $result = DB::select(
//              "select j.execution_system,
//                      MIN(jqt.queue_end_time - jqt.queue_start_time) as min_wait_time,
//                      MAX(jqt.queue_end_time - jqt.queue_start_time) as max_wait_time,
//                      AVG(jqt.queue_end_time - jqt.queue_start_time) as avg_wait_time
                $result = DB::select(
                   "select j.execution_system as 'name',
                        AVG(jqt.queue_end_time - jqt.queue_start_time) as 'count'
                    from jobs j left join
                        (
                            select je.job_id as job_id,
                                 UNIX_TIMESTAMP(max(CASE WHEN je.status = 'QUEUED' THEN je.created ELSE NULL END)) as queue_start_time,
                                 UNIX_TIMESTAMP(max(CASE WHEN je.status = 'RUNNING' THEN je.created ELSE NULL END)) as queue_end_time
                            from jobevents as je
                            where je.status in ('RUNNING', 'QUEUED')
                            group by je.job_id, je.tenant_id
                        ) as jqt on j.id = jqt.job_id
                    where j.start_time is not null
                        and jqt.queue_end_time >= jqt.queue_start_time
                        and j.tenant_id = " . ($username !== 'guest' ? "'$tenantId'" : 'j.tenant_id') . "
                        and j.created >= '" . date('Y-m-d 00:00:00', (empty($timeframe) ? strtotime('-10 years') : $timeframe)) . "'
                    group by j.execution_system, j.tenant_id
                    order by `count` " . $order . "
                    limit " . ($limit ? $limit : 1000000));
                foreach ($result as $row) {
                    $row->count = (int)$row->count;
                };

                return $result;
            }
        );
    }

    /**
     * Returns to to job scheduler used.
     */
    private function getJobSchedulersLeaders()
    {
        $cacheKey = $this->getCachePrefix() . 'leaders.systems';
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
            function () use ($username, $timeframe, $tenantId) {
                $query = DB::table('jobs')
                    ->join('systems', 'jobs.execution_system', '=', 'systems.system_id')
                    ->join('executionsystems', 'systems.id', '=', 'executionsystems.id')
                    ->select(DB::raw("executionsystems.scheduler_type as name, count(jobs.id) as submissions"))
                    ->where('jobs.tenant_id', '=', 'systems.tenant_id');

                if ($username !== 'guest') {
                    $query->where('jobs.tenant_id', '=', $tenantId);
                }

                if (!empty($timeframe)) {
                    $query->where('jobs.created', '>', date('yyyy-mm-dd 00:00:00', $timeframe));
                }


                return (int)$query->first()->totalOwners;
            });
    }

    /**
     * Returns to to job scheduler used.
     */
    private function getJobExecutionSystemSeriesLeaders($cacheSuffix, $field, $limit = false, $order = false)
    {
        $cacheKey = $this->getCachePrefix() . $cacheSuffix;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($cacheKey, $_ENV['cacheLifetime'],
            function () use ($username, $timeframe, $tenantId, $field, $limit, $order) {
                $results = DB::select(
                   "select " . $field . " as 'name', count(j.id) as 'count'
                    from jobs j
                        left join (
                            select s.*, ex.scheduler_type, ex.execution_type
                            from systems s left join executionsystems ex on s.id = ex.id
                        ) sys on j.execution_system = sys.system_id
                    where sys.tenant_id = j.tenant_id
                        and j.tenant_id = " . ($username !== 'guest' ? "'$tenantId'" : 'j.tenant_id') . "
                        and j.created >= '" . date('Y-m-d 00:00:00', (empty($timeframe) ? strtotime('-10 years') : $timeframe)) . "'
                    group by `name`
                    order by `count` DESC
                    limit " . ($limit ? $limit : 1000000));

                return $results;
            });
    }

    private function getAverages()
    {

        return array(
            "runTime" => $this->getAverageJobRunTime(),
            "cores" => $this->getAverageCoreCount(),
            "nodes" => $this->getAverageNodeCount(),
            "jobsPerUser" => $this->getAverageJobsPerUser()
        );
    }

    private function getAverageJobRunTime()
    {
        $avgRunTime = $this->getTotalRunTimeInSeconds() / $this->getTotalCompletedJobCount();
        $averageRunTime = ceil($avgRunTime);

        $hr = floor($averageRunTime / (60 * 60));
        if ($hr < 10) $hr = "0$hr";
        $min = floor(($averageRunTime % (60 * 60)) / (60));
        if ($min < 10) $min = "0$min";
        $sec = floor((($averageRunTime % (60 * 60)) % (60)));
        if ($sec < 10) $sec = "0$sec";

//    return "$hr:$min:$sec";
        return $averageRunTime;
    }

    private function getAverageCoreCount()
    {
        return (float)number_format((float)$this->getTotalCoreCount() / (float)$this->getTotalJobCount(), 1);
    }

    private function getAverageNodeCount()
    {
        return (float)number_format((float)$this->getTotalNodeCount() / (float)$this->getTotalJobCount(), 1);
    }

    private function getAverageJobsPerUser()
    {
        return (float)number_format((float)$this->getTotalJobCount() / (float)($this->getUniqueJobOwnerCount() * $this->getAverageNodeCount()), 1);
    }

    private function getTraffic()
    {
        return array(
            array(
                'name' => 'hourly',
                // 'total' => $hourly_total,
                'values' => $this->getHourlySubmissions(),
            ),
            array(
                'name' => 'daily',
                'values' => $this->getDailySubmissions(),
            ),
        );
    }

    private function getHourlySubmissions()
    {
        // $query = "select HOUR(r.CreatedAt - INTERVAL 18 HOUR) as 'name', count(HOUR(r.CreatedAt - INTERVAL 18 HOUR)) as 'count' from `Usage` r ";
        // $query = $this->applyQueryConditions($query, 'usage');
        // $query .= " group by HOUR(r.CreatedAt - INTERVAL 18 HOUR) ";
        return $this->getSubmissionTraffic('traffic.hourly', "DATE_FORMAT((CreatedAt - INTERVAL 18 HOUR), '%H')");
    }

    private function getDailySubmissions()
    {
        // $query = "select DATE_FORMAT(r.CreatedAt, '%Y-%m-%d') as 'name', count(DATE_FORMAT(r.CreatedAt, '%Y-%m-%d')) as 'count' from `Usage` r ";
        // $query = $this->applyQueryConditions($query, 'usage');
        // $query .= " group by DATE_FORMAT(r.CreatedAt, '%Y-%m-%d') ";
        return $this->getSubmissionTraffic('traffic.daily', "DATE_FORMAT(CreatedAt, '%Y-%m-%d')");
    }

    private function getSubmissionTraffic($cacheSuffix, $field, $limit = false, $order = false)
    {
        $cacheKey = $this->getCachePrefix() . $cacheSuffix;
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        if ($this->force) {
            Cache::tags($category, $tenantId, $username, $timeframe)->forget($this->getCachePrefix() . $cacheKey);
        }

        return Cache::tags($category, $tenantId, $username, $timeframe)->remember($this->getCachePrefix() . $cacheKey, $_ENV['cacheLifetime'],
            function () use ($field, $limit, $order, $timeframe, $username, $tenantId) {

                $query = DB::table('Usage')->select(DB::raw("$field as 'name', count($field) as 'count'"))->where('ActivityKey', '=', 'JobsSubmit');

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
    private function getLocations()
    {
        $cacheKey = $this->getCachePrefix() . "locations.ip";
        $category = $this->getControllerShortName();
        $tenantId = JWTClient::getCurrentTenant();
        $username = JWTClient::getCurrentEndUser();
        $timeframe = $this->timeframe;

        $query = "select r.UserIP as 'ip', count(r.UID) as 'cnt' from `Usage` r ";
        $query .= "where r.ActivityKey in ('JobsSubmit', 'JobsResubmit') ";
        $query = $this->applyQueryConditions($query, 'usage');
        $query .= " group by r.UserIP order by count(r.UID)";

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
}
