<?php
/**
 * Created by PhpStorm.
 * User: Jordan
 * Date: 04/07/14
 * Time: 3:54 PM
 */

return array(
    /*
      |--------------------------------------------------------------------------
      | Absolute path to location where parsed swagger annotations will be stored
      |--------------------------------------------------------------------------
    */
    'doc-dir' => app_path() . '/storage/docs',

    /*
      |--------------------------------------------------------------------------
      | Relative path to access parsed swagger annotations.
      |--------------------------------------------------------------------------
    */
    'doc-route' => 'docs',

    /*
      |--------------------------------------------------------------------------
      | Absolute path to directory containing the swagger annotations are stored.
      |--------------------------------------------------------------------------
    */
    "app-dir" => "app",

    /*
      |--------------------------------------------------------------------------
      | Absolute path to directories that you would like to exclude from swagger generation
      |--------------------------------------------------------------------------
    */
    "excludes" => array(
        app_path()."/storage",
        app_path()."/tests",
        app_path()."/views",
        app_path()."/config"
    ),

    /*
      |--------------------------------------------------------------------------
      | Turn this off to remove swagger generation on production
      |--------------------------------------------------------------------------
    */
    "generateAlways" => true,

    "api-key" => "auth_token",

    /*
      |--------------------------------------------------------------------------
      | Edit to set the api's version number
      |--------------------------------------------------------------------------
    */
    "default-api-version" => $_ENV['version'],

    /*
      |--------------------------------------------------------------------------
      | Edit to set the swagger version number
      |--------------------------------------------------------------------------
    */
    "default-swagger-version" => "2.0",

    /*
      |--------------------------------------------------------------------------
      | Edit to set the api's base path
      |--------------------------------------------------------------------------
    */
    "default-base-path" => "stats/v2",

    /*
      |--------------------------------------------------------------------------
      | Edit to trust the proxy's ip address - needed for AWS Load Balancer
      |--------------------------------------------------------------------------
    */
    "behind-reverse-proxy" => true,
    /*
      |--------------------------------------------------------------------------
      | Uncomment to add response headers when swagger is generated
      |--------------------------------------------------------------------------
    */
    /*"viewHeaders" => array(
        'Content-Type' => 'text/plain'
    ),*/

    /*
      |--------------------------------------------------------------------------
      | Uncomment to add request headers when swagger performs requests
      |--------------------------------------------------------------------------
    */
    /*"requestHeaders" => array(
        'TestMe' => 'testValue'
    ),*/
);
