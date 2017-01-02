<?php

/**
 * Created by PhpStorm.
 * User: dooley
 * Date: 1/1/17
 * Time: 4:52 PM
 */
class ImpersonationClient
{
    private $tenant = '';

    public function __construct($tenantId='') {
        $this->tenant = get_tenant($tenantId);
        error_log(print_r($this->tenant,1));
    }

    /**
     * Returns impersonation access token for the given user.
     * @param $username
     * @return bool|string token if it could be obtained, false otherwise.
     */
    public function getTokenForUser($username) {
        global $config;

        $client_cache = $this->getCache($username);
        error_log("Client cache is currently returning " . print_r($client_cache));
        // if the user auth cache exists, use that.
        // existing tokens will be auto-refreshed by default
        if (!empty($client_cache) && !$config['debug']) {
            error_log("Returning cache value " . print_r($client_cache));
            return $client_cache['access_token'];
        }
        // otherwise pull a fresh one
        else {
            error_log("Getting service user auth cache ");
            $service_user_config = $this->getServiceUserConfig();
            error_log("Config reutrned was: " . print_r($service_user_config,1));

            // build oauth token request body with "token_user" extension for the "admin_password" grant type
            $postData = sprintf('token_username=%s&username=%s&password=%s&grant_type=admin_password&scope=%s',
                urlencode($username),
                urlencode($service_user_config['username']),
                urlencode($service_user_config['password']),
                'PRODUCTION');

            // add service user agent
            $headers = array(
                "User-Agent: " . "Agave PostIts API/" . $config['service.version'] . ";" . $this->tenant['tenant_id'],
                "Authorization: Basic " . md5($service_user_config['client_key'].':'.$service_user_config['client_secret']));

            // make the actual request and cache the results
            error_log("Calling token service at ". addTrailingSlash($this->tenant['base_url']) . 'token');
            $client_cache = $this->_post(addTrailingSlash($this->tenant['base_url']) . 'token', $postData, $headers);

            error_log("response from token call was: " . print_r($client_cache,1));
            if (!empty($client_cache)) {
                $client_cache['expires_at'] = strtotime('+' . $client_cache['expires_in'] . ' seconds');
                $this->setCache($username, $client_cache);
                error_log("Saving cache for user {$username} until {$client_cache['expires_at']}");
                return $client_cache['access_token'];
            }
            else {
                error_log("Empty response from the service. no token available. request will fail.");
            }
        }

        return false;
    }

    /**
     * Refreshes an existing auth token with the given refresh token stored in teh
     * same cache document.
     *
     * @param $username
     */
    public function refreshTokenForUser($username, $authCache)
    {
        global $config;
        $serviceUserConfig = $this->getServiceUserConfig();

        // build standard oauth refresh token request body
        $postData = sprintf('grant_type=%s&refresh_token=%s&scope=%s',
            'refresh_token',
            urlencode($authCache['refresh_token']),
            'PRODUCTION');

        // add service user agent
        $headers = array(
            "User-Agent: " . "Agave PostIts API/" . $config['service.version'] . ";" . $this->tenant['tenant_id'],
            "Authorization: Basic " . md5($serviceUserConfig['client_key'].':'.$serviceUserConfig['client_secret']));

        // make the actual request and cache the results
        $refresh_response = $this->_post($this->tenant['base_url'] . '/token', $postData, $headers);

        if (!empty($response)) {
            $refresh_response['expires_at'] = strtotime('+' . $refresh_response['expires_in'] . ' seconds');
            $json = array_merge($authCache, $refresh_response);
            $this->setCache($username, $json);

            return $json;
        }
        else {
            return false;
        }
    }


    /**
     * Reads service user token from service config. If a tenant-specific config
     * exists, it uses that, otherwise, defaults to a "_common" config.
     *
     * @return array|
     */
    public function getServiceUserConfig() {
        $service_user_config = array(
            'username' => envVar("AGAVE_POSTIT_SERVICE_{$this->tenant['tenant_id']}_USER", envVar("AGAVE_POSTIT_SERVICE_USER")),
            'password' => envVar("AGAVE_POSTIT_SERVICE_{$this->tenant['tenant_id']}_PASSWORD", envVar("AGAVE_POSTIT_SERVICE_PASSWORD")),
            'client_key' => envVar("AGAVE_POSTIT_SERVICE_{$this->tenant['tenant_id']}_CLIENT_KEY", envVar("AGAVE_POSTIT_SERVICE_CLIENT_KEY")),
            'client_secret' => envVar("AGAVE_POSTIT_SERVICE_{$this->tenant['tenant_id']}_CLIENT_SECRET", envVar("AGAVE_POSTIT_SERVICE_CLIENT_SECRET")),
        );

        return $service_user_config;
    }

    /**
     * Performs generic HTTP POST on the given url with the
     * provided postData and headers. Content-Type must be supplied
     * in the calling method in the headers, if explicitly needed.
     * @param $url
     * @param $postData
     * @param $headers
     * @return
     */
    private function _post($url, $postData, $headers) {
        global $config;

        //open connection
        $ch = curl_init();
        error_log("Calling token service at " . $url);
        error_log("Post data: {$postData}");
        error_log("headers: ". print_r($headers,1));

        //set the url, number of POST vars, POST data
        curl_setopt( $ch, CURLOPT_URL ,$url);
        curl_setopt( $ch, CURLOPT_POST, 1);
        curl_setopt( $ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt( $ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt( $ch, CURLOPT_HEADER, $headers);

        //execute post
        $result = curl_exec($ch);

        if (curl_errno($ch)) error_log("Request to token service at " . $url . " failed with following exception. " . curl_error($ch));
        error_log(curl_error($ch));
        //close connection
        curl_close($ch);

        if ($config['debug']) error_log($result);

        if (empty($result)) {
            $json = json_decode($result);
            if (!empty($json)) {
                return $json;
            }
        }

        return array();
    }

    /**
     * Fetches the auth response for the user from a local cache
     *
     * @param $username
     * @return array|bool valid array representing the cached server response or false
     */
    private function getCache($username)
    {
        $cacheDir = $this->getServiceUserCacheDir($username);
        $cacheFile = $cacheDir . '/' . $username . '.json';

        // our default response is false
        $json = false;

        // cache the token for the given amount of time.
        // we should be using redis here. move to slim already
        if (file_exists($cacheFile)) {
            error_log("Cache file found at {$cacheFile}");
            // read the cache from disk
            $userCache = file_get_contents($cacheFile);
            if (!empty($userCache)) {
                $json = json_encode($userCache);
                error_log(print_r($json, 1));

                // see if the cache expired
                if (strtotime('now') >= $json['expires_at']) {
                    error_log("Cache is expired " . strtotime('now') . '>=' . $json['expires_at']);
                    // if so, refresh
                    $refresh_response = $this->refreshTokenForUser($username, $json);

                    // did the refresh fail? if so, delete cache file to force a
                    // fresh pull next time.
                    if ($refresh_response === false) {
                        error_log("deleting old cache");
                        unlink($cacheFile);
                        $json = false;
                    } else {
                        error_log("Refreshed cache");
                        error_log(print_r($refresh_response, 1));
                        $json = $refresh_response;
                    }
                } // it's valid, return the cache
                else {
                    return $json;
                }
            }
        } else {
            // cache doesn't exist
        }

        return $json;
    }

    /**
     * Persists the
     * @param $username
     * @param $refresh_response
     */
    private function setCache($username, $clientCache)
    {
        global $config;
        
        $cacheFilePath = $this->getServiceUserCacheDir() . '/' . $username . '.json';

        file_put_contents($cacheFilePath, $clientCache);
    }

    /**
     * Fetch the directory the user's auth cache file would be stored.
     * @return string
     */
    private function getServiceUserCacheDir() {
        global $config;
        
        $cacheDir = $config['service.user.cache.dir'] . '/' . $this->tenant['tenant_id'];

        // craete the cache dir if it doesn't exist
        if (!file_exists($cacheDir)) {
            mkdir($cacheDir, null, true);
        }
        
        return $cacheDir;
    }

}