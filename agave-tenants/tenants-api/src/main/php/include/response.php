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

	// if a naked response is requested, strip content down to just the result or message (if error)
	if (isset($_GET['naked']) && strtolower($_GET['naked']) == 'true')
	{
		if ($status == 'success') {
			$content = $result;
		} else {
			$content = $message;
		}
	}
	// if not naked, give standard wrapper
	else
	{
		$content = array("status" => $status, "message" => $message, "result" => $result, "version" => $config['service.version']);
	}

	if (is_array($content))
	{
		if (isset($_GET['pretty']) && strtolower($_GET['pretty']) == 'true') {
			//echo str_replace('\/', '/', json_encode($content, JSON_PRETTY_PRINT));
			echo prettyPrint(str_replace('\/', '/', json_encode($content)));
		} else {
			echo str_replace('\/', '/', json_encode($content));
		}
	}
	else
	{
		echo $content;
	}

	die();
}

/**
 * Indents a flat JSON string to make it more human-readable.
 *
 * @param string $json The original JSON string to process.
 *
 * @return string Indented version of the original JSON string.
 */
function prettyPrint($json, $spaces=2) {
    $result = '';
    $level = 0;
    $prev_char = '';
    $in_quotes = false;
    $ends_line_level = NULL;
    $json_length = strlen( $json );

    $indent = str_repeat(' ', $spaces);

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
            $result .= "\n".str_repeat( $indent, $new_line_level );
        }
        $result .= $char.$post;
        $prev_char = $char;
    }

    return $result;
}

?>
