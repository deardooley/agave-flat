#!/usr/bin/perl -w

use DBI;

$db_host = 'dbi:mysql:iplant-api:foundation.iplantc.org';
$db_username = 'jobmap';
$db_password = 'qu3ryj0bmap';
$resource_id = 'ranger.tacc.teragrid.org';
$DEBUG = TRUE;

$dbh = DBI->connect($db_host, $db_username, $db_password) or die "Connection Error: $DBI::errstr\n";

$sql = "select id, localId, updateToken from Jobs where status in ('RUNNING', 'QUEUED', 'ARCHIVING', 'PAUSED') and system = '" . $resource_id . "'";

if ($DEBUG) { print ($sql . "\n"); }

$select = $dbh->prepare($sql);

$select->execute or die "SQL Error: $DBI::errstr\n";

while (@row = $select->fetchrow_array) {
	
	if ($DEBUG) { print ("$row[0] -> $row[1]\n"); }
	
	$command = "qstat -j " . $row[1] . " 2>&1";
	
	$fork_response = `$command`;
	
	if ($DEBUG) { print ($fork_response . "\n"); }
	
	if ($fork_response =~ /Following jobs do not exist/)
	{
		if ($DEBUG) { print ("Job ". $row[0] . " is a ghost, updating the db\n"); }
		
		$callback = 'curl -k "https://foundation.iplantc.org/apps-v1/trigger/job/'.$row[0].'/token/'.$row[2].'/status/FAILED" 2>&1';

		$callback_response = `$callback`;
		
		if ($DEBUG) { print ($callback_response . "\n"); }

		$sql = "update Jobs set status = 'FAILED' where id = " . $row[0];
	
		if ($DEBUG) { print ($sql . "\n"); }
		
		$update = $dbh->prepare($sql);
	
		$update->execute or die "SQL Error: $DBI::errstr\n";
	} 
	else
	{
		if ($DEBUG) { print ("Job ". $row[0] . " is still alive. Ignoring\n"); }
	}
}
