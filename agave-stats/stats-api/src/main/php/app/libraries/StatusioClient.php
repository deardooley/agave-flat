<?php

class StatusioClient {

  private $base_url = 'https://api.status.io/1.0';

  function __construct() {}

  public function getStatusSummary($statuspage_id) {
    $components = Cache::get('statusio-containers');
    if (empty($components)) {

      $expiresAt = strtotime('+10 minutes');

      $ch = curl_init();
      
      $request = curl_init();
      curl_setopt( $request, CURLOPT_URL, $this->base_url . '/status/' . $statuspage_id);
      curl_setopt( $request, CURLOPT_RETURNTRANSFER, 1);  // RETURN CONTENTS OF CALL
      curl_setopt( $request, CURLOPT_HEADER, 0 );  // DO NOT RETURN HTTP HEADERS
      curl_setopt( $request, CURLOPT_SSL_VERIFYPEER, FALSE);
      curl_setopt( $request, CURLOPT_SSL_VERIFYHOST, FALSE);
      curl_setopt( $request, CURLOPT_FOLLOWLOCATION, 1);
      curl_setopt( $request, CURLOPT_FORBID_REUSE, 1);
      $response = curl_exec( $request );
      curl_close($request);
      // error_log(print_r($response, 1));
      $json = json_decode($response, false);
      //error_log(print_r($json, 1));

      if ($this->isValidResponse($json)) {
        $components = $json->result->status;
        Cache::put('statusio-containers', json_encode($components), $expiresAt);
        //Cache::get('statusio-containers');
      } else {
        $components = array();
      }
    } else {
      $components = json_decode($components, false);
    }
    return $components;
  }

  public function isValidResponse($response) {
    return (!empty($response->result) && !empty($response->result->status));
  }

  public function isOperational($statuspage_id) {

    $components = $this->getStatusSummary($statuspage_id);

    // error_log(print_r($components,1));
    if (!empty($components)) {
      foreach ($components as $component) {
        //error_log(print_r($component,1));
        if (strtolower($component->containers[0]->status) != 'operational') return false;
      }
    }
    return true;
  }

}
