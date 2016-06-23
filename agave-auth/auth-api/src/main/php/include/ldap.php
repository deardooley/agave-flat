<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require_once "config.php";

/* Simple LDAP auth class. Just returns true or false */
function ldap_authenticate($username, $password) {
	global $config;
    
    if ($username != "" && $password != "") {
        $ds = ldap_connect($config['iplant.ldap.host'],$config['iplant.ldap.port']);
        $r = ldap_search( $ds, $config['iplant.ldap.base.dn'], 'uid=' . $username);
        if ($r) {
            $result = @ldap_get_entries( $ds, $r);
            if ($result[0]) {
                if (@ldap_bind( $ds, $result[0]['dn'], $password) ) {
                    return TRUE;
                }
            }
        }
    }
	return FALSE;
}

function ldap_is_valid_username($lookup_username) {
	global $config, $username, $password;
    
    if ($username != "" && $password != "") {
        $ds=@ldap_connect($config['iplant.ldap.host'],$config['iplant.ldap.port']);
        $r = @ldap_search( $ds, $config['iplant.ldap.base.dn'], 'uid=' . $lookup_username);
        if ($r) {
            return TRUE;
        }
    }
	return FALSE;
}

/*function count_users() {
	global $config;
	
	$ds=@ldap_connect($config['iplant.ldap.host'],$config['iplant.ldap.port']);
    
	$users = array();
	$alpha = array('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z');
	foreach($alpha as $a) {
		$r = @ldap_search( $ds, $config['iplant.ldap.base.dn'], 'uid=' . $a.'*');
        if ($r) {
            $result = @ldap_get_entries( $ds, $r);
			echo ("Search for {$a} produced " . count($result) . " results\n");
            if (!empty($result)) {
				foreach($result as $entry) {
					$users[$entry['dn']] = true;
				}
			}
        }
	}
	echo ("\nTotal of " . count($users) . " unique results");
};

count_users();*/

/*

if ( !ldap_authenticate($config['iplant.community.username'], $config['iplant.community.password']) ) 
{
    echo('Authorization Failed: ');
    exit(0);
}
echo('Authorization success');
*/
?>
