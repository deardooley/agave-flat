<?php

class APIMClient
{
  private $force = false;

  /**
   * Builds a Pingdom Client
   *
   * @param string $username
   * @param string $password
   * @param string $token
   */
  public function __construct($force=false)
  {
      $this->force = $force;
  }

  /**
   * Retrieves all client applications for a given tenant
   *
   * @param string $tenantId The id of the tenant. ex. "agave.prod"
   * @return array array of client application detail records
   */
  public function getAllApplicationsForTenant($tenantId) {
    $query = "select c.APPLICATION_ID as ClientApplication, c.NAME, c.DESCRIPTION, c.CALLBACK_URL, s.USER_ID, s.EMAIL_ADDRESS from AM_APPLICATION c left join AM_SUBSCRIBER s on c.SUBSCRIBER_ID = s.SUBSCRIBER_ID";

    if ($tenantConfig = $this->getTenantConfig($tenantId)) {
      return $this->_doQuery($tenantConfig, $query, "all." . $tenantId);
    } else {
      return array();
    }
  }

  /**
   * Retrieves all client applications for a given tenant
   *
   * @param string $tenantId The id of the tenant. ex. "agave.prod"
   * @param string $username a valid user in the given tenant
   * @return array array of client application detail records
   */
  public function getAllUserApplicationsForTenant($tenantId, $username) {
    $query = "select c.APPLICATION_ID as ClientApplication, c.NAME, c.DESCRIPTION, c.CALLBACK_URL, s.USER_ID, s.EMAIL_ADDRESS from AM_APPLICATION c left join AM_SUBSCRIBER s on c.SUBSCRIBER_ID = s.SUBSCRIBER_ID where s.USER_ID = '".mysqli_real_escape_string($username)."'";

    if ($tenantConfig = $this->getTenantConfig($tenantId)) {
      return $this->_doQuery($tenantConfig, $query, "all." . $tenantId);
    } else {
      return array();
    }
  }

  /**
   * Retrieves details about a specific client application within a tenant
   *
   * @param string $tenantId The id of the tenant. ex. "agave.prod"
   * @param string $clientId a valid client id within the given tenant
   * @return array array containing client application detail record
   */
  public function getUserApplicationDetails($tenantId, $clientId) {

    $clientApplications = $this->getAllApplicationsForTenant($tenantId);

    foreach($clientApplications as $clientApp) {
      if ($clientApp['ClientApplication'] === $clientId) {
        return $clientApp;
      }
    }
    return null;
  }

  /**
   * Fetches the remote database connection info for a given tenant id
   * @return array of connectivity parameters to query the apim database
   */
  public function getTenantConfig($tenantId) {
    return array(
        "host" => empty($_ENV['apim.db.'.$tenantId.'.host']) ? $_ENV['apim.db.host'] : $_ENV['apim.db.'.$tenantId.'.host'],
        "username" => empty($_ENV['apim.db.'.$tenantId.'.username']) ? $_ENV['apim.db.username'] : $_ENV['apim.db.'.$tenantId.'.username'],
        "password" => empty($_ENV['apim.db.'.$tenantId.'.password']) ? $_ENV['apim.db.password'] : $_ENV['apim.db.'.$tenantId.'.password'],
        "database" => empty($_ENV['apim.db.'.$tenantId.'.database']) ? ('apimgtdb_'.str_replace('.','-',$tenantId)) : $_ENV['apim.db.'.$tenantId.'.database'],
        "port" => empty($_ENV['apim.db.'.$tenantId.'.port']) ? intval($_ENV['apim.db.port']) : intval($_ENV['apim.db.'.$tenantId.'.port'])
    );
  }

  /**
   * Performs a generic query against an APIM database using the given tenant configs
   * and returns the raw results of the query.
   *
   * @param mixed $tenantConfig The database connectivity info for a given tenant's APIM
   * @param string $query the database query to perform to fetch the set of client applications
   * @param string $cacheKey the id used to cache these results
   * @param int $lifetime the duration for which the cache should be valid. $_ENV['cacheLifetime'] by default.
   * @return mixed result of database query
   */
  private function _doQuery($tenantConfig, $query, $cacheKey, $lifetime=false) {

    if ($this->force) {
      Cache::forget($cacheKey);
    }

    if (!$lifetime) {
      $lifetime = $_ENV['cacheLifetime'];
    }

    return Cache::remember($cacheKey, $lifetime,
      function() use ($query, $tenantConfig) {

        $rows = array();
        try {
          $con = mysqli_connect($tenantConfig['host'],
              $tenantConfig['username'],
              $tenantConfig['password'],
              $tenantConfig['database'],
              $tenantConfig['port']);

          $result = mysqli_query( $con , $query);

          $rows = mysqli_fetch_all($result, MYSQLI_ASSOC);

          mysqli_close($con);

        } catch (Exception $e) {
          if (mysqli_connect_errno()) {
            Log::error("Failed to connect to tenant db: " . mysqli_connect_error());
          }
          return array();
        }

        return $rows;
      }
    );
  }
}
