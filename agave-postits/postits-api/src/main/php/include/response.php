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
	global $db, $config;
	
	header("Content-Type: application/json");
	
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
		$content = array("status" => $status, "message" => $message, "result" => $result, "version" => $config['service.version'] );
	}

	if (is_array($content))
	{
		
        $content = apply_request_response_filter($content, $_GET['filter']);

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
 * Filters the api response with the fields listed in $filter. If
 * $filter is empty or '*', the raw response is returned. Unmatched
 * fields are ignored.
 *
 * @param array $item
 * @param string $filter
 * @return array filtered array or the original if $filter is a '*'
 */
function apply_request_response_filter($content, $filter='') {
    if (empty($filter) || $filter == "*") {
        return $content;
    }
    else if (empty($content)) {
        return $content;
    }
    // test for an wrapped response object
    else if (array_key_exists("result", $content)) {
        $content['result'] = apply_request_response_filter($content['result'], $filter);
        return $content;
    }
    // test for an object
    else if (array_key_exists("id", $content)) {
        return filter_response_item($content, $filter);
    }
    // multidimensional array
    else {

        $filtered_content = array();
        foreach ($content as $item) {
            $filtered_content[] = filter_response_item($item, $filter);
        }

        return $filtered_content;
    }
}

/**
 * Filters an single response item with the comma-separated list of
 * $filter values.
 * @param array $item
 * @param string $filter
 * @return array filtered array or the original if $filter is a '*'
 */
function filter_response_item($item, $filter='') {
    $fields = explode(',',$filter);
    $filtered_content = array();
    foreach($fields as $field) {
        if (array_key_exists($field, $item)) {
            $filtered_content[$field] = $item[$field];
        }
    }
    return $filtered_content;
}

/**
 * Builds hal object for the postit responses
 */
function get_hal_links_for_token($postit)
{
	global $config;
	
	$hal = array(
			'self' => array('href' => resolve_tenant_url($config['iplant.foundation.services']['postit']) . $postit['postit_key']),
			'profile' => array('href' => resolve_tenant_url($config['iplant.foundation.services']['profile']) . $postit['creator'])
	);
	
	$redirect_path = parse_url($postit['target_url'], PHP_URL_PATH);
	$path_tokens = explode( '/', $redirect_path);
	
	if (count($path_tokens) > 1) {
// 		error_log(print_r($path_tokens, 1));
		$resource_type = $path_tokens[1];
		if ( substr($resource_type, -1) === 's' ) {
			$resource_type = substr($resource_type, 0, -1);
		}
		$hal[$resource_type] = array('href' => $postit['target_url']);
	
	}
	
	if (!empty($postit['internalUsername'])) {
		$hal['internalUser'] = array('href' => resolve_tenant_url($config['iplant.foundation.services']['profile']) . $postit['creator'] . '/users/' . $postit['internalUsername']);
	} else if (!empty($postit['internal_username'])) {
		$hal['internalUser'] = array('href' => resolve_tenant_url($config['iplant.foundation.services']['profile']) . $postit['creator'] . '/users/' . $postit['internal_username']);
	}

	return $hal;
}

/**
 * Adjusts the default API resource URLs to tenant-specific URLs given
 * in the tenants table in the db. This should be called for every
 * HAL url returned from the API. Say Settings.IPLANT_APPS_SERVICE = https://agave.iplantc.org/apps/2.0
 * was the default url from the service settings. If there was a tenant
 * called xsede.org in the db with tenantId = https://api.xsede.org,
 * then after calling this method, the returned url would be
 * https://api.xsede.org/apps/2.0
 *
 * @param url default url to resolve
 * @return string tenant-specific url for the given resource
 */
function resolve_tenant_url($url)
{
	global $tenant_id;

	$tenant = get_tenant_info($tenant_id);

	if (empty($tenant))
	{
		return $url;
	}
	else
	{
		$tenant_base_url = $tenant['base_url'];
		$tenant_base_url = rtrim($tenant_base_url, '/');
		$tenant_base_url .= '/';

		$resolvedUrl = substr($url, strpos($url, '://') + 3);
		$resolvedUrl = substr($resolvedUrl, strpos($resolvedUrl, '/') + 1);
		$resolvedUrl = $tenant_base_url . $resolvedUrl;

		return $resolvedUrl;
	}
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
