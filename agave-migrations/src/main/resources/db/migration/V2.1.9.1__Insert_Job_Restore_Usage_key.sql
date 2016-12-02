###############################################################
# Migration: V2.1.9.1__Insert_Job_Restore_Usage_key.sql
#
# Adding in new Usage Activities to track job restore (undelete) 
# requests
# 									   
# Database changes:
#
# Table changes:
#
# Index changes:
#
# Column changes:
# 
# Data changes:
# + UsageActivities
#	+ JobRestore
#
#################################################################

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'JobRestore', NULL, 'JOBS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'JobRestore'
                      		AND `ServiceKey` = 'JOBS02');
