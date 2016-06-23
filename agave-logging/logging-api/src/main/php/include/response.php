<?php  if ( ! defined('BASEPATH')) exit('No direct script access allowed');


/* Print a success response */
function format_success_response($result='')
{
	format_response('success', '', $result);
}

/* Print an error response */
function format_error_response($message, $http_response_code = ERROR_500)
{
	format_response('error', $message, '', $http_response_code);
}

/* Print a default response */
function format_response($status='success', $message='', $result='', $http_response_code = false)
{
	global $db;
	
	if ($http_response_code) header($http_response_code);
	
	$content = array("status" => $status, "message" => $message, "result" => $result);
	
	echo json_encode($content);
	
	mysql_close();
	
	die();
}

?>