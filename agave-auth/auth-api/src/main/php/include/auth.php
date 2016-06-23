<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require_once "ldap.php";
require_once "irods.php";

// Force HTTP Basic Auth on all requests
//header('Authentication Required');

/*if (!isset($_SERVER['PHP_AUTH_USER'])) {
    header('WWW-Authenticate: Basic realm="'.$config['iplant.auth.realm'].'"');
    header(ERROR_401);
    format_error_response("Please authenticate to access the iPlant Foundation API");
	die();
} else {
	
	$username = $_SERVER['PHP_AUTH_USER'];
	$password = $_SERVER['PHP_AUTH_PW'];
	
	// run the auth chain
	if ( !ldap_authenticate($username, $password) ) 
	{
		// see if they're passing in a token as pw
		if ( !is_valid_token_for_user($username, $password) )
		{
			return format_error_response('Authentication failed', ERROR_401);
		}
	}
    
}


$url_action = (empty($_REQUEST['action'])) ? 'logIn' : $_REQUEST['action'];
$auth_realm = (isset($auth_realm)) ? $auth_realm : '';

if (isset($url_action)) {
    if (is_callable($url_action)) {
        call_user_func($url_action);
    } else {
        format_error_response('Function does not exist, request terminated');
    };
};

function logIn() {
    global $config;
    if (!isset($_SESSION['username'])) {
        if (!isset($_SESSION['login'])) {
            $_SESSION['login'] = TRUE;
            header('WWW-Authenticate: Basic realm="'.$config['iplant.auth.realm'].'"');
            header('HTTP/1.0 403 Permission Denied');
            //format_error_response("Please authenticate to access the iPlant Foundation API");
			echo "You are unauthorized ... ";
			echo "[<a href='" . $_SERVER['PHP_SELF'] . "'>Login</a>]";
            exit;
        } else {
            $username = isset($_SERVER['PHP_AUTH_USER']) ? $_SERVER['PHP_AUTH_USER'] : '';
            $password = isset($_SERVER['PHP_AUTH_PW']) ? $_SERVER['PHP_AUTH_PW'] : '';
            $result = authenticate($username, $password);
            if ($result === TRUE) {
                $_SESSION['username'] = $username;
            } else {
				session_unset($_SESSION['login']);
                format_error_response("Invalid login. Try again.");
				exit;
            };
        };
    };
}*/


	$username = isset($_SERVER['PHP_AUTH_USER']) ? $_SERVER['PHP_AUTH_USER'] : '';
	$password = isset($_SERVER['PHP_AUTH_PW']) ? $_SERVER['PHP_AUTH_PW'] : '';
	$result = authenticate($username, $password);
	if ($result === TRUE) {
		$_SESSION['username'] = $username;
	} else {
		session_unset($_SESSION['login']);
		format_error_response("Invalid login. Try again.");
		exit;
	}
	
	

function authenticate($username, $password) 
{
	global $config;
	
	$auth_function = "{$config['iplant.auth.source']}_authenticate";
	error_log($auth_function);
	
	if ($config['debug']) error_log("trying $username, $password");
	
	if ($_SERVER['REQUEST_METHOD'] == "PUT" or $_SERVER['REQUEST_METHOD'] == "POST") // creations and renewals must be done with the original password
	{
		// see if they're passing in a token as pw
		if ( !$auth_function($username, $password) ) 
		{
			return format_error_response('Authentication failed', ERROR_401);
		}
	} 
	else if ($_SERVER['REQUEST_METHOD'] == "DELETE")  // delete operation makes no sense for passwords
	{
		// see if they're passing in a token as pw
		if ( !is_valid_token_for_user($username, $_REQUEST['token']) )
		{
			return format_error_response('Authentication failed', ERROR_401);
		}
	}
	else if ($_SERVER['REQUEST_METHOD'] == "GET") // original auth method.
	{
		if ( !$auth_function($username, $password) ) 
		{
			// see if they're passing in a token as pw
			if ( is_valid_token_for_user($username, $password) )
			{
				// decrement the number of uses of the token if they're doing a get
				redeem_token($username, $password);
			}
			else
			{
				return format_error_response('Authentication failed', ERROR_401);
			}
		}
	}
	return TRUE;
}

function logOut() {

    session_destroy();
    if (isset($_SESSION['username'])) {
        session_unset($_SESSION['username']);
        echo "You've successfully logged out<br>";
        echo '<p><a href="?action=logIn">LogIn</a></p>';
    } else {
        header("Location: ?action=logIn", TRUE, 301);
    };
    if (isset($_SESSION['login'])) { session_unset($_SESSION['login']); };
    exit;
}