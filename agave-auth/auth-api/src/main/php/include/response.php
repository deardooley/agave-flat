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
	global $mysqli, $config;
	
	clear_session();
	
	if ($http_response_code) header($http_response_code);
	
	$content = array(
		"status" => $status, 
		"message" => $message, 
		"version" => $config['service.version'],
		"result" => $result,
		);
	
	if (isset($_GET['callback'])) {
		echo "{$_GET['callback']}(". str_replace('\/', '/', json_encode($content)).")";
	} else if (isset($_GET['pretty']) && strtolower($_GET['pretty']) == 'true') {
		echo prettyPrint(str_replace('\/', '/', json_encode($content)));
	} else {
		echo str_replace('\/', '/', json_encode($content));
	}
	
	$mysqli->close();
	
	die();
}

function clear_session()
{
	$session_name = session_name();
	
	session_destroy();
	
	if ( isset( $_COOKIE[ $session_name ] ) ) {
		setcookie(session_name(), '', time()-3600, '/');
	}	
}

function get_hal_links_for_token($token)
{
	global $config;
	
	$hal = array(
		'self' => array('href' => $config['iplant.foundation.services']['auth'] . 'tokens/' . $token['token']),
		'profile' => array('href' => $config['iplant.foundation.services']['profile'] . $token['username'])
	);
	
	if (!empty($token['internalUsername'])) {
		$hal['internalUser'] = array('href' => $hal['profile']['href'] . '/users/' . $token['internalUsername']);
	} else if (!empty($token['internal_username'])) {
		$hal['internalUser'] = array('href' => $hal['profile']['href'] . '/users/' . $token['internal_username']);
	}
	
	return $hal;
}

/**
 * Indents a flat JSON string to make it more human-readable.
 *
 * @param string $json The original JSON string to process.
 *
 * @return string Indented version of the original JSON string.
 */
function prettyPrint($json) {
    $result = '';
    $level = 0;
    $prev_char = '';
    $in_quotes = false;
    $ends_line_level = NULL;
    $json_length = strlen( $json );

    for( $i = 0; $i < $json_length; $i++ ) {
        $char = $json[$i];
        $new_line_level = NULL;
        $post = "";
        if( $ends_line_level !== NULL ) {
            $new_line_level = $ends_line_level;
            $ends_line_level = NULL;
        }
        if( $char === '"' && $prev_char != '\\' ) {
            $in_quotes = !$in_quotes;
        } else if( ! $in_quotes ) {
            switch( $char ) {
                case '}': case ']':
                    $level--;
                    $ends_line_level = NULL;
                    $new_line_level = $level;
                    break;

                case '{': case '[':
                    $level++;
                case ',':
                    $ends_line_level = $level;
                    break;

                case ':':
                    $post = " ";
                    break;

                case " ": case "\t": case "\n": case "\r":
                    $char = "";
                    $ends_line_level = $new_line_level;
                    $new_line_level = NULL;
                    break;
            }
        }
        if( $new_line_level !== NULL ) {
            $result .= "\n".str_repeat( "\t", $new_line_level );
        }
        $result .= $char.$post;
        $prev_char = $char;
    }

    return $result;
}