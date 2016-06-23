<?php

class BitbucketClient
{
  private $base_url = 'https://api.bitbucket.org/1.0/repositories/taccaci/agave';
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
   * Retrieves all "bug" issues
   */
  public function getOpenIssues() {
    $path = $this->base_url."/issues?";
    $query = "kind=bug&sort=created_on&limit=50";
    $query .= "&status=open&status=new";

    $json = $this->_getResponse($path . $query, 'issues');

    if ($json) {
      return $json->issues;
    }
  }

  /**
   * Fetches detailed information about the current enhancement issues
   */
  public function getFeatureRequests() {
    $path = $this->base_url."/issues?";
    $query = "kind=enhancement&sort=created_on&limit=50";
    $query .= "&status=open&status=new";

    $json = $this->_getResponse($path . $query, 'enhancements');

    if ($json) {
      return $json->issues;
    }
  }

  /**
   * Returns total commits
   */
  public function getCommitCount() {
    $totalCommits = 0;
    $page = 1;
    $hasNext = true;
    while($hasNext && $page < 50) {
      $json = $this->_getResponse("https://api.bitbucket.org/2.0/repositories/taccaci/agave/commits?page=$page", "commits-page-{$page}");
      Log::debug(print_r($json,1));
      if ($json && !empty($json->next)) {
        $totalCommits += count($json->values);
      } else {
        $hasNext = false;
      }
      $page++;
    }

    return $totalCommits;
  }

  /**
   * Performs a generic unauthenticated GET request on the api with the given path.
   *
   * @param string $path path portion of the query.
   * @param [type] $cacheKey [description]
   */
  private function _getResponse($url, $cacheSuffix, $lifetime=false) {

    $cacheKey = 'bitbucket.'.$cacheSuffix;

    if ($this->force) {
      Cache::forget($cacheKey);
    }

    if (!$lifetime) {
      $lifetime = $_ENV['cacheLifetime'];
    }

    return Cache::remember($cacheKey, $lifetime,
      function() use ($url) {

        $ch = curl_init();
        Log::debug("Calling bitbucket: $url");
        $request = curl_init();
        curl_setopt( $request, CURLOPT_URL, $url);
        curl_setopt( $request, CURLOPT_RETURNTRANSFER, 1);  // RETURN CONTENTS OF CALL
        curl_setopt( $request, CURLOPT_HEADER, 0 );  // DO NOT RETURN HTTP HEADERS
        curl_setopt( $request, CURLOPT_SSL_VERIFYPEER, FALSE);
        curl_setopt( $request, CURLOPT_SSL_VERIFYHOST, FALSE);
        curl_setopt( $request, CURLOPT_FOLLOWLOCATION, 1);
        curl_setopt( $request, CURLOPT_FORBID_REUSE, 1);
        $response = curl_exec( $request );
        curl_close($request);

        return json_decode($response);
      }
    );
  }
}
