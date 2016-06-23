<?php

   // XSEDE oAuth PHP Library (v1.0)
   
   // Author / Contributor: Ryan Gentner <rgentner@ccr.buffalo.edu>
   // Center for Computational Research.
   // University at Buffalo -- State University of New York.
   
   // Last Updated: December 16, 2011
   
   // Code derived from consulting the following project on Google Code:
   // http://code.google.com/p/oauth/
   
   // ...as well as documentation located here:
   
   // XSEDE Science Gateway OAuth Protocol Specification
   // https://docs.google.com/document/pub?id=1BJBG92yJULLNAVixBDqyq48ohpMZ9ToGHP9qKWSSGcE

   /*
   
   Please follow these 3 steps so your gateway can begin working with oAuth:
   
   (1) Configuration -- Edit the following constants in this file:
       
         * OAUTH_MODE                       [line 72] (set to either DEV or PROD)
         * OAUTH_CALLBACK_LOCATION          [line 77]
         * OAUTH_PRIVATE_KEY                [line 90]
         * OAUTH_CERT_REQUEST               [line 106]
         * OAUTH_CERT_LIFETIME              [line 117]
   
         * OAUTH_GATEWAY_IDENTIFIER         (this is defined in 2 locations: 1 for PROD [line 140], 1 for DEV [line 149])
         
         
   (2) Create a file which will serve as the entry point into the oAuth logic. Your browser will point to the location
       of this php script.  Its contents are as follows:
       
         ------------------------------------------------------------------------

         require_once '/path/to/oauth.php';
   
         OAuth::initialize_login_ui();

         ------------------------------------------------------------------------
       
   (3) Upon successful authentication against oAuth, your browser will be redirected to the URL defined in the
       OAUTH_CALLBACK_LOCATION constant you defined below.  At the very top of that file should be this code:
       
         ------------------------------------------------------------------------
         
         require_once '/path/to/oauth.php';
 
         $token =    (isset($_REQUEST['oauth_token']))    ? $_REQUEST['oauth_token']    : '';
         $verifier = (isset($_REQUEST['oauth_verifier'])) ? $_REQUEST['oauth_verifier'] : '';
 
         $cert_data = OAuth::acquire_certificate($verifier, $token);
 
         ... remaining code here ...

         ------------------------------------------------------------------------

      $cert_data now contains the username and certificate data, which you are now free to work with any way
      you like.
   
   */
   
   // ==================================================================================
   
   // [OAUTH_MODE: UPDATE AS NECESSARY; POSSIBLE VALUES: DEV, PROD]
   // - If OAUTH_MODE is set to DEV, the OAUTH_ENDPOINT_BASE and OAUTH_GATEWAY_IDENTIFIER pertaining to the development version of oAuth will be used
   // - If OAUTH_MODE is set to PROD, the OAUTH_ENDPOINT_BASE and OAUTH_GATEWAY_IDENTIFIER pertaining to the production version of oAuth will be used
    
   define('OAUTH_MODE', 'DEV');
   
   // [OAUTH_CALLBACK_LOCATION: UPDATE AS NECESSARY]
   // The URL which will be accessed upon successful authentication against oAuth
   
   define('OAUTH_CALLBACK_LOCATION',  'https://path/to/callback.php');
      
   // ==================================================================================

   /*
   
   Generate an RSA private key:
   > openssl genrsa -out private.key 2048
   
   Assign the path of the private key to OAUTH_PRIVATE_KEY (below)
   
   */

   define('OAUTH_PRIVATE_KEY',   '/path/to/private.key');
   
   /*
      
   Generate a certificate request
   > openssl req -new -key private.key -out cert_request.csr
   
   Make sure to remove the lines from this newly generated file:
   -----BEGIN CERTIFICATE REQUEST-----
   and
   -----END CERTIFICATE REQUEST-----

   Assign the path of the generated certificate request to OAUTH_CERT_REQUEST (below)

   */
   
   define('OAUTH_CERT_REQUEST',     '/path/to/cert_request.csr');
   
   /*
   
   OAUTH_CERT_LIFETIME:
   
   An optional parameter specifying a requested lifetime (in seconds) for the certificate to be 
   issued in the final 'getcert' call.
   
   */
   
   define('OAUTH_CERT_LIFETIME',    '950400');
   
   /*
   
   Generate / Derive RSA public key from RSA private key:
   > openssl rsa -in private.key -pubout
   
   The public key produced as the result of calling the last command is to be supplied at one of the following
   sites:
   
   - https://portal.xsede.org/oauth/register (production)
   - https://go.teragrid.org/oauth/register  (development)
   
   Upon submission of that form, you will be issued a gateway identifier.  Assign the
   gateway identifier to the OAUTH_GATEWAY_IDENTIFIER constant of the corresponding section (PROD / DEV) below.
   
   */
   
   if (OAUTH_MODE == 'PROD') {
   
      // Production settings
      
      define('OAUTH_ENDPOINT_BASE',       'https://portal.xsede.org');
      define('OAUTH_GATEWAY_IDENTIFIER',  'specify_a_gateway_identifier_here');   
   
   }
   
   if (OAUTH_MODE == 'DEV') {
   
      // Development settings
      
      define('OAUTH_ENDPOINT_BASE',       'https://go.teragrid.org');
      define('OAUTH_GATEWAY_IDENTIFIER',  'specify_a_gateway_identifier_here');   
   
   }   
   
   // At this point, no more code modifications are necessary
   
   define('OAUTH_ENDPOINT_TEMP_CRED_REQUEST',          OAUTH_ENDPOINT_BASE.'/oauth/initiate');
   define('OAUTH_ENDPOINT_AUTHORIZE',                  OAUTH_ENDPOINT_BASE.'/oauth/authorize');
   define('OAUTH_ENDPOINT_TOKEN_REQUEST',              OAUTH_ENDPOINT_BASE.'/oauth/token');
   define('OAUTH_ENDPOINT_CERT_REQUEST',               OAUTH_ENDPOINT_BASE.'/oauth/getcert');
   define('OAUTH_ENDPOINT_CLIENT_REGISTRATION',        OAUTH_ENDPOINT_BASE.'/oauth/register');   
         
   // ==================================================================================
      
      
   class OAuth {

      // Parameters common to each phase
      
      private static $_common_params = array(
                                          'oauth_signature_method' => 'RSA-SHA1',
                                          'oauth_version' => '1.0',
                                          'oauth_consumer_key' => OAUTH_GATEWAY_IDENTIFIER
                                       );

      // ==================================================================================

      /*
         @function initialize_login_ui
         Wrapper for handling first phase of oAuth negotiations   
      */  
                  
      public static function initialize_login_ui() {
      
         $credential_data = self::temporary_credential_request();
    
         self::redirect_to_login_ui($credential_data);
   
      }//initialize_login_ui

      // ==================================================================================

      /*
         @function acquire_certificate
         Wrapper for handling second phase of oAuth negotiations   
         
         @param String $verifier [The verification code obtained from the callback]
         @param String $token [The OAuth temporary credentials identifier]
         
         @returns String [The username of the successfully authenticated user along with his/her PEM encoded certificate] 
      */  
                  
      public static function acquire_certificate($verifier, $token) {
      
         $token_data = self::token_request($verifier, $token);
   
         $token = self::extract_urn_value($token_data, 'oauth_token');
   
         return self::get_certificate($token);
   
      }//acquire_certificate
      
      // ==================================================================================      
      
      /*
         @function temporary_credential_request
         Initial call to access (and negotiate with) the oAuth service 
         
         @returns String [The OAuth temporary credentials identifier]     
      */  
               
      public static function temporary_credential_request() {
         
         $defaults = array_merge(self::$_common_params, array(
         
                           // oauth_signature will be generated below
                           'oauth_timestamp' => time(),
                           'oauth_nonce' => self::_generate_nonce(),
                           'oauth_callback' => OAUTH_CALLBACK_LOCATION,
                           'certlifetime' => OAUTH_CERT_LIFETIME,
                           'certreq' => file_get_contents(OAUTH_CERT_REQUEST)
   
                        ));
         
   
         // Generate the base string, followed by the signature -----------------
   
         $base_string = self::_generateBaseString(OAUTH_ENDPOINT_TEMP_CRED_REQUEST, $defaults);
               
         $defaults['oauth_signature'] = self::_generateSignature($base_string);      
         
         
         $link = self::_createLink(OAUTH_ENDPOINT_TEMP_CRED_REQUEST, $defaults);
         
         return self::_processResponse($link);
          
      }//temporary_credential_request
   
      // ==================================================================================

      /*
         @function redirect_to_login_ui
         Directs the user to the link referencing the login UI
                  
         @param String $credential_data [The query string appended to the oAuth authorization endpoint]         
      */   
            
      public static function redirect_to_login_ui($credential_data) {
      
         $newLink = OAUTH_ENDPOINT_AUTHORIZE.'?'.$credential_data;
         
         header("Location: $newLink");
      
      }//redirect_to_login_ui
      
      // ==================================================================================

      /*
         @function token_request
         
         @param String $verifier [The verification code obtained from the callback]
         @param String $token [The OAuth temporary credentials identifier]
         
         @returns String [The OAuth access token identifier]              
      */   
               
      public static function token_request($verifier, $token) {
               
         $defaults = array_merge(self::$_common_params, array(
         
                           'oauth_token' => $token,
                           'oauth_verifier' => $verifier,
                           // oauth_signature will be generated below
                           'oauth_timestamp' => time(),
                           'oauth_nonce' => self::_generate_nonce()

                        ));
         
         
         // Generate the base string, followed by the signature -----------------
   
         $base_string = self::_generateBaseString(OAUTH_ENDPOINT_TOKEN_REQUEST, $defaults);
                     
         $defaults['oauth_signature'] = self::_generateSignature($base_string);      
         
         
         $link = self::_createLink(OAUTH_ENDPOINT_TOKEN_REQUEST, $defaults);
   
         return self::_processResponse($link);
          
      }//token_request
   
      // ==================================================================================

      /*
         @function get_certificate
         
         @param String $token [The OAuth access token identifier obtained in the Token Request]
         
         @returns String [The username of the successfully authenticated user along with his/her PEM encoded certificate]              
      */   
               
      public static function get_certificate($token) {
         
         $defaults = array_merge(self::$_common_params, array(
         
                           'oauth_token' => $token,
                           // oauth_signature will be generated below
                           'oauth_timestamp' => time(),
                           'oauth_nonce' => self::_generate_nonce()

                        ));
         
   
         // Generate the base string, followed by the signature -----------------
   
         $base_string = self::_generateBaseString(OAUTH_ENDPOINT_CERT_REQUEST, $defaults);
               
         $defaults['oauth_signature'] = self::_generateSignature($base_string);      
         
         
         $link = self::_createLink(OAUTH_ENDPOINT_CERT_REQUEST, $defaults);
   
         return self::_processResponse($link);
          
      }//get_certificate

      // ==================================================================================   

      /*
         @function extract_urn_value
         
         @param String $urn [The uniform resource name (haystack)]
         @param String $param [The parameter whose value is of interest (needle)]
         
         @returns String [The value associated with the parameter in the URN]
                         [An empty string is returned if the parameter cannot be located in the URN]
                         
      */         

      public static function extract_urn_value($urn, $param) {
      
         $elems = explode('&', $urn);
      
         foreach ($elems as $pair) {
         
            list($k, $v) = explode('=', $pair);
      
            if ($k == $param) {
         
               return urldecode($v);
            
            }
         
         }//foreach
      
         return '';
         
      }//extract_urn_value
               
      // ==================================================================================

      /*
         @function _generateBaseString
         
         @param String $endpoint [A base URL]
         @param Array $params    [Associative array whose key-value pairs will be formatted as a URL query string]
                  
         @returns String [A concatenation of url encoded entities (endpoint, params) which serves as the basis
                         for subsequent signature generation]
      */
         
      private static function _generateBaseString($endpoint, &$params) {
   
         $parts = array(
         
                     'GET',
                     $endpoint,
                     self::_build_http_query($params)
            
                  );
         
         $parts = self::_urlencode_rfc3986($parts);
         
         return implode('&', $parts);
      
      }//_generateBaseString
      
      // ==================================================================================

      /*
         @function _processResponse
         
         @param String $link [The URL referencing the data of interest]
                  
         @returns String [The data referenced at that URL]
                         * If HTTP Status 500 is returned in the response, the contents of that response
                           are simply dumped to the screen and the script which caused invocation of this call
                           is terminated.
                   
      */
            
      private static function _processResponse($link) {
   
         $ch = curl_init();
         
         curl_setopt_array($ch, array(
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_URL => $link
         ));
         
         $response = curl_exec($ch);
               
         curl_close($ch);
         
         $error_code_matches = preg_match('/HTTP Status 500/', $response);
         
         if ($error_code_matches == 0) {
         
            return $response;
            
         }
         else {
         
            // There was an error... present it and exit
            
            print $response;
            exit;
            
         }
   
      }//_processResponse
   
      // ==================================================================================
      
      /*
         @function _createLink
         
         @param String $endpoint [A base URL]
         @param Array $params    [Associative array whose key-value pairs will serve as the query string for the URL]
                  
         @returns String [The constructed url, e.g. http://domain/endpoint?query_string]
      */
      
      private static function _createLink($endpoint, $params = array()) {
      
         $a = array();
         
         foreach ($params as $k => $v) {
            $a[] = "$k=".urlencode($v);
         }
            
         $paramString = implode('&', $a);
         
         $absLink = $endpoint.'?'.$paramString;
         
         return $absLink;
      
      }//_createLink
   
      // ==================================================================================

      /*
         @function _generateSignature
         
         @param String $base_string [The data to be encoded]
         
         @returns String [A base64 encoded signature]
      */
               
      private static function _generateSignature($base_string) {
   
         $privatekeyid = openssl_get_privatekey('file://'.OAUTH_PRIVATE_KEY);
       
         openssl_sign($base_string, $signature, $privatekeyid);
        
         // Release the key resource
         openssl_free_key($privatekeyid);
         
         return base64_encode($signature);
       
      }//_generateSignature
   
      // ==================================================================================

      /*
         @function _generate_nonce
         Adapted from http://code.google.com/p/oauth/
      */
         
      private static function _generate_nonce() {
      
         $mt = microtime();
         $rand = mt_rand();
   
         return md5($mt . $rand); // md5s look nicer than numbers
     
      }//_generate_nonce
        
      // ==================================================================================

      /*
         @function _build_http_query
         Adapted from http://code.google.com/p/oauth/
      */
         
      private static function _build_http_query($params) {
      
         if (!$params) return '';
   
         // Urlencode both keys and values
         
         $keys = self::_urlencode_rfc3986(array_keys($params));
         $values = self::_urlencode_rfc3986(array_values($params));
         $params = array_combine($keys, $values);
   
         // Parameters are sorted by name, using lexicographical byte value ordering.
         // Ref: Spec: 9.1.1 (1)
         
         uksort($params, 'strcmp');
   
         $pairs = array();
       
         foreach ($params as $parameter => $value) {
         
            if (is_array($value)) {
            
               // If two or more parameters share the same name, they are sorted by their value
               // Ref: Spec: 9.1.1 (1)
               // June 12th, 2010 - changed to sort because of issue 164 by hidetaka
               
               sort($value, SORT_STRING);
               
               foreach ($value as $duplicate_value) {
                  $pairs[] = $parameter . '=' . $duplicate_value;
               }
           
            }
            else {
               $pairs[] = $parameter . '=' . $value;
            }
         
         }//foreach
       
         // For each parameter, the name is separated from the corresponding value by an '=' character (ASCII code 61)
         // Each name-value pair is separated by an '&' character (ASCII code 38)
         
         return implode('&', $pairs);
       
      }//_build_http_query
      
      // ==================================================================================  

      /*
         @function _urlencode_rfc3986
         Adapted from http://code.google.com/p/oauth/
      */
      
      private static function _urlencode_rfc3986($input) {
      
         if (is_array($input)) {
            return array_map(array('OAuth', '_urlencode_rfc3986'), $input);
         }
         else if (is_scalar($input)) {
            return str_replace('+', ' ', str_replace('%7E', '~', rawurlencode($input)));
         }
         else {
            return '';
         }
     
      }//_urlencode_rfc3986

   }//OAuth
   
?>
