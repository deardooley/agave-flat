<?php

class PingdomClient
{
  private $base_url = 'https://api.pingdom.com/api/2.0';
  private $_username;
  private $_password;
  private $_token;
  private $force = false;
  /**
   * Builds a Pingdom Client
   *
   * @param string $username
   * @param string $password
   * @param string $token
   */
  public function __construct($username, $password, $token, $force=false)
  {
      $this->_username = $username;
      $this->_password = $password;
      $this->_token = $token;
      $this->force = $force;
  }

  /**
   * Retrieves all active checks for the account.
   */
  public function getChecks() {
    $json = $this->_getResponse('/checks', 'checks.all');
    // Log::debug("pingdom checks response\n".$json);
    $response = json_decode($json, 0);
    if ($response) {
      return $response->checks;
    }
  }

  /**
   * Fetches detailed information about the check with the given id
   * @param string $checkId unique idenifier of the check
   */
  public function getCheck($checkId) {
    $json = $this->_getResponse('/checks/'.$checkId, 'check.'.$checkId);
    $response = json_decode($json, 0);
    if ($response) {
      return $response->check;
    }
  }

  /**
   * Returns uptime percentage of check with given ID starting at the given timestamp
   * @param string $checkId unique idenifier of the check
   * @param int $timestamp unix timestamp of the beginning timeframe to return uptime
   */
  public function getUptime($checkId, $timestamp=0) {
    // if (!$timestamp) {
    //   $timestamp = 0;
    // }
    //Log::debug('Checking /summary.average/'.$checkId. '?includeuptime=true&from='.$timestamp);
    $json = $this->_getResponse('/summary.average/'.$checkId. '?includeuptime=true&from='.$timestamp, 'uptime.'.$checkId, true, 86400);
    $response = json_decode($json, 0);
    if ($response) {
      //Log::debug(print_r($response,1));
      if ($response->summary->status) {
        return (float)number_format(100.0 - (100.0 * (float)$response->summary->status->totaldown / (float)($response->summary->status->totalup + $response->summary->status->totaldown)), 2);
      }
    }

    return null;
  }

  /**
   * Returns downtime in milliseconds of check with given ID starting at the given timestamp
   * @param string $checkId unique idenifier of the check
   * @param int $timestamp unix timestamp of the beginning timeframe to return downtime
   */
  public function getDowntime($checkId, $timestamp=0) {
    $json = $this->_getResponse('/summary.average/'.$checkId. '?includeuptime=true&from='.$timestamp, 'uptime.'.$checkId, true, 86400);
    $response = json_decode($json, 0);
    if ($response) {
      if ($response->summary->status) {
        return $response->summary->status->totaldown;
      }
    }

    return null;
  }

  /**
   * Retrieve the current overall status of the API
   */
  public function getStatus() {
    $json = $this->_getResponse('/checks', 'checks.all');
    $response = json_decode($json, 0);
    if ($response) {
      foreach ($response->checks as $check) {
        if ($check->status === 'down') {
          return 'down';
        } else if ($check->status !== 'up') {
          return 'unknown';
        }
      }
      return 'up';
    }
    return 'unknown';
  }

  /**
   * Performs a generic authenticated GET request on the api with the given path.
   *
   * @param string $path path portion of the query.
   * @param [type] $cacheKey [description]
   */
  private function _getResponse($path, $cacheKey, $ignoreForce=false, $lifetime=false) {

    if ($this->force && !$ignoreForce) {
      Cache::forget('pingdom.'.$cacheKey);
    }

    if (!$lifetime) {
      $lifetime = $_ENV['cacheLifetime'];
    }

    $url = $this->base_url . $path;
    $username = $this->_username;
    $password = $this->_password;
    $token = $this->_token;

    //Log::debug("Calling $username:$password -H 'App-Key: $token' $url");

    return Cache::remember('pingdom.'.$cacheKey, $lifetime,
      function() use ($url, $username, $password, $token) {
        $ch = curl_init();
        Log::debug("Calling $username:$password -H 'App-Key: $token' $url");
        $request = curl_init();
        curl_setopt( $request, CURLOPT_URL, $url);
        curl_setopt( $request, CURLOPT_USERPWD, $username . ":" . $password);
        curl_setopt( $request, CURLOPT_HTTPHEADER, array('App-Key: '. $token));
        curl_setopt( $request, CURLOPT_RETURNTRANSFER, 1);  // RETURN CONTENTS OF CALL
        curl_setopt( $request, CURLOPT_HEADER, 0 );  // DO NOT RETURN HTTP HEADERS
        curl_setopt( $request, CURLOPT_SSL_VERIFYPEER, FALSE);
        curl_setopt( $request, CURLOPT_SSL_VERIFYHOST, FALSE);
        curl_setopt( $request, CURLOPT_FOLLOWLOCATION, 1);
        curl_setopt( $request, CURLOPT_FORBID_REUSE, 1);
        $response = curl_exec( $request );
        curl_close($request);
        // error_log(print_r($response, 1));
        // $json = json_decode($response, false);
        //error_log(print_r($json, 1));

        return $response;
      }
    );
  }
}
