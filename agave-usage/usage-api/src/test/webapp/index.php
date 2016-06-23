<?php 
/**
 * Main page for the postit service. It is essentially a url shortener service for iplant that creates
 * disposable pre-authenticated urls for iPlant endpoints.
 */
date_default_timezone_set('America/Chicago');

session_start();

//get_url_domain($_GET['url']);
 
define('BASEPATH', '');

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: PUT, GET, POST, DELETE, OPTIONS");
header("Allow: PUT, GET, POST, DELETE, OPTIONS");

# In PHP 5.2 or higher we don't need to bring this in
if (!function_exists('json_encode')) {
	require_once 'include/jsonwrapper_inner.php';
} 

require_once "include/config.php";
require_once "include/remote_logging.php";
require_once "include/response.php";
require_once "include/database.php";

// look for a postit key in the url path
//$postit_key = get_url_key();
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') 
{
	$headers = apache_request_headers();
	header("Access-Control-Allow-Headers: " . $headers['Access-Control-Request-Headers']);
	exit(0);
}
else if (isset($_GET['postit_key'])) // redeeming a postit
{
	$postit = get_postit_by_key($_GET['postit_key']);
	
	if (is_valid_postit($postit))
	{	
		// if they are performing a delete, then expire the toekn immediately
		if ($_SERVER['REQUEST_METHOD'] == 'DELETE') 
		{
			require_once "include/jwt_auth.php";
			
			post_log('guest', $config['iplant.service.log.activitykey']['revoke'], $tenant_id, $client_id);
			
			// user must be the original owner in order to revoke the postit
			if ($postit['creator'] == $username || in_array($username, $config['iplant.service.trusted.users']))
			{
				// go ahead and expire the postit
				revoke_postit($postit['id']);
				format_success_response();
			}
			else
			{
				// can't revoke another person's postit key
				format_error_response('User does not have permission to revoke this postit.', ERROR_403);
			}
		} 
		else 
		{
			post_log('guest', $config['iplant.service.log.activitykey']['redeem'], $postit['tenant_id'], '');
			
			redeem_postit($_GET['postit_key']);
		
			forward_postit_request($postit['target_url'], $postit['target_method'], $postit['creator'], $postit['token'], $postit['tenant_id']);	
		}
	}
	else 
	{
		if ($_SERVER['REQUEST_METHOD'] == 'DELETE') 
		{
			require_once "include/jwt_auth.php";
			post_log('guest', $config['iplant.service.log.activitykey']['revoke'], $tenant_id, $client_id);
		} else {
			// this is a random bad request. we can track it in apache, but not in our logs due to no tenant info
		}
		
		format_error_response('No matching postit found.', ERROR_404);
	}
} 
else 
{
	if ($_SERVER['REQUEST_METHOD'] == 'GET') 
	{
		require_once "include/jwt_auth.php";
		
		post_log($username, $config['iplant.service.log.activitykey']['list'], $tenant_id, $client_id);
		
		$postits = array();
		$db_results = get_active_postits($username);
		
		while ($postit = mysql_fetch_array($db_results)) 
		{
			$postits[] = array(
				'created' => date('c', strtotime($postit['created_at'])),
				'expires' => date('c', strtotime($postit['expires_at'])),
				'internalUsername' => $postit['internal_username'],
				'url' => $postit['target_url'],
				'method' => $postit['target_method'],
				//'token' => $postit['token'],
				'remainingUses' => empty($postit['remaining_uses'])? 0 : intval($postit['remaining_uses']),
				'postit' => $postit['postit_key'],
				'creator' => $postit['creator'],
			    '_links' => get_hal_links_for_token($postit)
			);
		}
		
		format_success_response($postits);
	}
	else if ($_SERVER['REQUEST_METHOD'] == 'POST')
	{
		require_once "include/jwt_auth.php";
		
		post_log($username, $config['iplant.service.log.activitykey']['create'], $tenant_id, $client_id);
		
		if ($_REQUEST['url']) 
		{
			$postit = array();
			$postit['created_at'] = date('Y-m-d H:i:s');
		
			// support proxy requests for trusted users just like the auth service
			if (!empty($_REQUEST['username']))
			{
				if ($username == $_REQUEST['username'] || in_array($username, $config['iplant.service.trusted.users']))
				{
					// set the creator field rather than reset the username field since we still
					// need to call the auth service for a token.
					$postit['creator'] = $_REQUEST['username'];
				}
				else
				{	
					format_error_response('User does not have permission to create a postit for this user.', ERROR_403);
				}
			}
			else
			{
				$postit['creator'] = $username;	
			}
		
			// gotta have a url
			$url = $_REQUEST['url'];
			$parsed_url = parse_url($url);
			
			if ( empty($parsed_url) )
			{
				format_error_response('Bad url', ERROR_400);
			} 
			else
			{
				// the url must be within their tenant
				$tenant = get_tenant_info($tenant_id);
				if (empty($tenant)) 
				{
					format_error_response("Unable to resolve url. Unknown tenant id.", ERROR_400);
				} 
				else if ( strpos($url, $tenant['base_url']) !== 0 )
				{	
					// verify the url is an iplant url. If not, reject it. We're not a spam proxy server
					format_error_response('Invalid URL. Only URLs from the ' . $tenant_id .' tenant may be posted.', ERROR_400);
				}
				else 
				{
					// if the url is explicitly listed in the trusted domain list
					$postit['target_url'] = $url;
				}
			}
		
			// GET, PUT, POST, DELETE are supported
			$method = $_REQUEST['method'];
		
			if (!is_valid_http_method($method))
			{
				format_error_response('Invalid http method. Supported methods are GET, PUT, POST, and DELETE.', ERROR_501);
			} 
			else
			{
				$postit['target_method'] = $method;	
			}
		
			// support time limit restrictions. default is 30 days
			$lifetime = !isset($_REQUEST['lifetime']) ? 2592000 : $_REQUEST['lifetime'] ;
		
			if ( is_numeric($lifetime) and (int)$lifetime > 0 ) 
				$postit['expires_at'] =  date('Y-m-d H:i:s', strtotime('+'.$lifetime.' seconds'));
			else 
				format_error_response('Invalid value for token lifetime.', ERROR_400);
		
		
			// support multiple use restrictions
			$max_uses = !isset($_REQUEST['maxUses']) ? 1 : $_REQUEST['maxUses'];
		
			if (is_numeric($max_uses)) 
			{
				if ((int)$max_uses > 0 || (int)$max_uses == -1) 
					$postit['remaining_uses'] = $max_uses;
			}
			else 
			{
				format_error_response('Invalid value for max uses.', ERROR_400);
			}
		
			if ( !empty($_REQUEST['internalUsername']) ) {
				$postit['internal_username'] = $_REQUEST['internalUsername'];
			} else {
				$postit['internal_username'] = '';
			}
		
			// support passwordless urls
			if (empty($_REQUEST['noauth']) || $_REQUEST['noauth'] === 0 || $_REQUEST['noauth'] === '0') {
				$postit['token'] = 1;
// 				$postit['token'] = get_auth_token($username, password, $max_uses, $lifetime, $postit['internal_username']);				
			}
		
			$postit['postit_key'] = get_unique_nonce($url.$method.$username);
		
			$postit_id = save_postit($postit);
		
			$postit['internalUsername'] = empty($postit['internal_username']) ? null : $postit['internal_username'];
			$postit['authenticated'] = empty($postit['token']) ? FALSE : TRUE;
			$postit['created'] = date('c', strtotime($postit['created_at']));
			$postit['expires'] = date('c', strtotime($postit['expires_at']));
			$postit['remainingUses'] = ($postit['remaining_uses'] == -1 ? 'unlimited' : intval($postit['remaining_uses']));
			$postit['postit'] = $postit['postit_key'];
			$postit['url'] = $postit['target_url'];
			$postit['method'] = $postit['target_method'];
			$postit['_links'] = get_hal_links_for_token($postit);
			
			unset($postit['target_url']);
			unset($postit['target_method']);
			unset($postit['internal_username']);
			unset($postit['token']);
			unset($postit['postit_key']);
			unset($postit['remaining_uses']);
			unset($postit['created_at']);
			unset($postit['expires_at']);
		
			format_success_response($postit);
		} 
		else 
		{
			format_error_response('Bad request. No url specified.', ERROR_400);
		}
	}
	else
	{
		require_once "include/jwt_auth.php";
		
		post_log($username, $config['iplant.service.log.activitykey']['create'], $tenant_id, $client_id);
		
		format_error_response('Unsupported method.', ERROR_405);
	}
}

/**
 * We only support core HTTP methods postits. No HEADER, etc will be accepted
 */
function is_valid_http_method($method)
{
	$http_methods = array('GET', 'PUT', 'POST', 'DELETE');
	
	return in_array(strtoupper($method), $http_methods);
}	

function proxy_response($postit)
{
	if ($postit['method'] == 'GET' || $postit['method'] == 'DELETE')
	{
		print call_postit_url($postit['url'], $postit['method'], $postit['username'], $postit['token']);
	} 
	else 
	{
		//format_error_response('Action not yet implemented.', ERROR_501);
		
		//TODO: file uploads need to be handled differently
		$post_data = '';
		foreach($_REQUEST as $key=>$value) 
		{ 
			$post_data .= $key.'='.$value.'&amp;'; 
		}
		rtrim($post_data,'&amp;');
		
		print call_postit_url($postit['url'], $postit['method'], $postit['username'], $postit['token'], $post_data);
	}
}

/** 
 * Finds the domain of the url. eg mobile.example.com => example.com 
 */
function get_url_domain($url)
{
	// get host name from URL
	preg_match("/^((http|https):\/\/)?([^\/]+)/i", $url, $matches);
	$host = $matches[3];
	
	// get last two segments of host name
	preg_match("/[^\.\/]+\.[^\.\/]+$/", $host, $matches);
	return $matches[0];
}

/** 
 * Pull the posit key out of the url
 */
function get_url_key() 
{
	global $config;
	
	$url = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
	$url = rtrim($url, '/');
	if ($url == rtrim($config['iplant.foundation.services']['postit'], '/')) 
	{
		return FALSE;
	} 
	else 
	{
		$tokens = explode('/', $url);
		return $tokens[sizeof($tokens)-2];
	}
}

/** 
 * Forward the stored url request using the stored credentials. No validation is done here, it is strictly a proxy
 * service at this point. The results of the call are forwarded back to the caller. If the user stored a post or put
 * any files/forms that are posted to the postit url will be forwarded as well.
 */
function forward_postit_request($url, $method="GET", $username='', $need_auth=false, $tenant_id) 
{
	//error_log("$url, $method, $username, $password, $post_data");
	global $config;
	
	$tenant = get_tenant_info($tenant_id);
	if (empty($tenant)) {
		format_error_response("Unable to resolve PostIt. Unknown tenant id.", ERROR_400);
	} 
	else 
	{ 
		$tenant_base_url = $tenant['base_url'];
		$tenant_base_url = parse_url($tenant_base_url, PHP_URL_HOST);
		$config['iplant.service.trusted.domains'] = array($tenant_base_url);
	}
	
	$redirect_host = parse_url($url, PHP_URL_HOST);
	if (in_array($redirect_host, $config['iplant.service.trusted.domains']))
	{
		$redirect_path = substr($url, strpos($url, $redirect_host) + strlen($redirect_host));
		$url = 'https://iplant-qa.tacc.utexas.edu/v2' . str_replace('/v2', '', $redirect_path);
	} 
	else if ($redirect_host != 'iplant-qa.tacc.texas.edu')
	{
		// don't send a jwt to outside domains. jwt should only go to the backend api services
		$need_auth = false;
	}
	
	if ($config['debug']) error_log($url);
	
	$headers = apache_request_headers();
	$ch = curl_init();
	curl_setopt( $ch, CURLOPT_URL , $url);
	curl_setopt( $ch, CURLOPT_RETURNTRANSFER, FALSE);
	curl_setopt( $ch, CURLOPT_FOLLOWLOCATION, TRUE);
	curl_setopt( $ch, CURLOPT_FORBID_REUSE, TRUE);
	$referring_url = resolve_tenant_url($config['iplant.foundation.services']['postit'], $tenant_id) . $postit['postit_key'];
	curl_setopt( $ch, CURLOPT_REFERER, $referring_url);
	curl_setopt( $ch, CURLOPT_SSLVERSION,3); 
	curl_setopt( $ch, CURLOPT_SSL_VERIFYPEER, FALSE); 
	curl_setopt( $ch, CURLOPT_SSL_VERIFYHOST, 1);
	curl_setopt( $ch, CURLOPT_HEADER, 0); 
	if ($config['debug']) error_log("auth [$need_auth] -> $username");
	
	
	//error_log("set common ops");
	if ($method == 'POST') 
	{
		//error_log("post");
		curl_setopt( $ch, CURLOPT_POST, TRUE);
		
		
		// forward the file in a multipart form upload if necessary
		if (!empty($_FILES['fileToUpload'])) 
		{
			// check that they didn't try to push too much data through the service.
			if (empty($_POST) && $_SERVER['CONTENT_LENGTH'] > 0) 
			{
				format_error_response('The server was unable to handle that much POST data ('.$_SERVER['CONTENT_LENGTH'].' bytes) due to its current configuration', ERROR_500);
			} 
			else 
			{ 
				//curl_setopt( $ch, CURLOPT_UPLOAD, 1);
				//curl_setopt( $ch, CURLOPT_INFILESIZE, filesize($_FILES['fileToUpload']['tmp_name']));
				error_log("post -> $url -> file ->" . $_FILES['fileToUpload']['tmp_name']);
				
				$fileinfo = pathinfo($_FILES['fileToUpload']['tmp_name']);
				
				$_POST["fileToUpload"] = "@".$fileinfo['basename'];
				// change directory to the temp path so the filename won't 
				chdir($fileinfo['dirname']);
			}
		}
		$post_data = serialize_form_data($_POST);
		if ($config['debug']) error_log("post -> $url -> $post_data");
		curl_setopt( $ch, CURLOPT_POSTFIELDS, $_POST);		
	} 
	else if ($method == 'PUT') 
	{
		$post_data = serialize_form_data($_POST);
		if ($config['debug']) error_log("put -> $url -> $post_data");
		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PUT");
		curl_setopt($ch, CURLOPT_POSTFIELDS, $post_data);	
	}
	else if ($method == 'DELETE') 
	{
		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "DELETE");
	}
	else
	{
		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "GET");
		
		// try to guess the mimetype
		$url_path = parse_url($url, PHP_URL_PATH);
		$path_parts = pathinfo($url_path);
		if (!empty($path_parts['extension'])) {
			$mimetype = mime_content_type($path_parts['basename']);
			header("Content-Type: $mimetype");
		}
	}
	
	// authenticate if necessary
	if ($username && $need_auth) 
	{
		// create a jwt header for use calling the api internally
		$jwt_prefix="eyJ0eXAiOiJKV1QiLCJhbGciOiJTSEEyNTZ3aXRoUlNBIiwieDV0IjoiTm1KbU9HVXhNelpsWWpNMlpEUmhOVFpsWVRBMVl6ZGhaVFJpT1dFME5XSTJNMkptT1RjMVpBPT0ifQ==";
		$jwt_suffix = "FA6GZjrB6mOdpEkdIQL/p2Hcqdo2QRkg/ugBbal8wQt6DCBb1gC6wPDoAenLIOc+yDorHPAgRJeLyt2DutNrKRFv6czq1wz7008DrdLOtbT4EKI96+mXJNQuxrpuU9lDZmD4af/HJYZ7HXg3Hc05+qDJ+JdYHfxENMi54fXWrxs=";
		$jwt_claims = array(
			"iss" => "wso2.org/products/am",
			"exp" =>  strtotime('+1 week'),
			"http://wso2.org/claims/subscriber" => $username,
			"http://wso2.org/claims/applicationid" => "5",
			"http://wso2.org/claims/applicationname" => "DefaultApplication",
			"http://wso2.org/claims/applicationtier" => "Unlimited",
			"http://wso2.org/claims/apicontext" => "*",
			"http://wso2.org/claims/version" => "v2",
			"http://wso2.org/claims/tier" => "Unlimited",
			"http://wso2.org/claims/keytype" => "PRODUCTION",
			"http://wso2.org/claims/usertype" => "APPLICATION_USER",
			"http://wso2.org/claims/enduser" => $username ,
			"http://wso2.org/claims/enduserTenantId" => "-9999",
			"http://wso2.org/claims/emailaddress" => $username . "@" .$tenant_id,
			"http://wso2.org/claims/fullname" => "Dev User",
			"http://wso2.org/claims/givenname" => "Dev",
			"http://wso2.org/claims/lastname" => "User",
			"http://wso2.org/claims/primaryChallengeQuestion" => "N/A",
			"http://wso2.org/claims/role" => "Internal/everyone",
			"http://wso2.org/claims/title" => "N/A"
		);
		$jwt_body = base64_encode(json_encode($jwt_claims));
		
		$header_field = 'x-jwt-assertion-'. str_replace('.', '-', $tenant_id);
		$header_value = sprintf("%s.%s.%s", $jwt_prefix, $jwt_body, $jwt_suffix);
		if ($config['debug']) error_log($header_field . ": " . $header_value);
		$headers[] = $header_field . ": " . $header_value;
	}
	
	curl_setopt( $ch, CURLOPT_HTTPHEADER, $headers );
	
	//execute post
	curl_exec($ch);
	
	if ($config['debug']) error_log(curl_error($ch));
	
	//close connection
	curl_close($ch);
}

/**
 * Verify the postit is not expired, used up, or empty. Dies on failure. Returns true on success.
 */
function is_valid_postit($postit) 
{
	if (!$postit)
	{
		format_error_response('Unknown postit key.', ERROR_404);
	}
	else if (strtotime($postit['expires_at']) <= strtotime('now'))
	{
		format_error_response('Postit key has expired.', ERROR_403);
	}
	else if ($postit['remaining_uses'] == 0 || $postit['remaining_uses'] < -1)
	{	
		format_error_response('Postit key has already been redeemed.', ERROR_403);
	}
	return true;
}

function serialize_form_data($form_vars)
{
	$post_data = '';
	foreach($form_vars as $key=>$value) 
	{ 
		$post_data .= $key.'='.$value.'&'; 
	}
	$post_data = rtrim($post_data,'&');
	
	return $post_data;
}

if(!function_exists('apache_request_headers')) {
// Function is from: http://www.electrictoolbox.com/php-get-headers-sent-from-browser/
    function apache_request_headers() {
        $headers = array();
        foreach($_SERVER as $key => $value) {
            if(substr($key, 0, 5) == 'HTTP_') {
                $headers[str_replace(' ', '-', ucwords(str_replace('_', ' ', strtolower(substr($key, 5)))))] = $value;
            }
        }
        return $headers;
    }
}
?>