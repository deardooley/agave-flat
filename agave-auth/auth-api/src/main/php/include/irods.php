<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

require_once "config.php";
require_once("irods/Prods.inc.php");
	
/* Simple LDAP auth class. Just returns true or false */
function irods_authenticate($username, $password) 
{
	global $config;
	
	if ($username != "" && $password != "") {
		try 
		{
			$account = new RODSAccount($config['iplant.irods.host'],$config['iplant.irods.port'], $username, $password);
			
			$conn = RODSConnManager::getConn($account);
			
			//$dir = new ProdsPath($account, '/'.$config['iplant.irods.zone']);
			
			return true;
			
		} catch (RODSException $e) { 
			error_log(print_r($e, true));
			return false;
		}
	}
}

function irods_is_valid_username($lookup_username) 
{
	global $config, $username, $password;
    
    if ($username != "" && $password != "") {
		try 
		{
			$account = new RODSAccount($config['iplant.irods.host'],$config['iplant.irods.port'], $username, $password);
			
			$conn = RODSConnManager::getConn($account);
			
			$user_home = sprintf("/%s/home/%s", $config['iplant.irods.zone'], $lookup_username);
			
			$dir = new ProdsDir($account, $user_home, true);
			
			return true;
			
		} catch (RODSException $e) { 
			return false;
		}
	}
}