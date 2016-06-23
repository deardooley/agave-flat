<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require_once ("auth.php");

header("Access-Control-Allow-Origin: *");
header("Content-type: application/json");

if ( ! isset($_SERVER['PHP_AUTH_USER']) )
{
	header('WWW-Authenticate: Basic realm="iPlant Foundation API"');
	header('HTTP/1.0 401 Unauthorized');
    echo 'Please authenticate using your iPlant username and password';
    exit;
}

$username = isset($_SERVER['PHP_AUTH_USER']) ? $_SERVER['PHP_AUTH_USER'] : '';
$password = isset($_SERVER['PHP_AUTH_PW']) ? $_SERVER['PHP_AUTH_PW'] : '';
$result = authenticate($username, $password);
if ($result === TRUE) {
	$_SESSION['username'] = $username;
} else {
	format_error_response("Invalid login. Try again.");
	exit;
}
