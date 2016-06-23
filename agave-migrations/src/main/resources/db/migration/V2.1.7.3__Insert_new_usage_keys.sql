###############################################################
# Migration: V2.1.7.3__Insert_new_usage_keys.sql
#
# Adding in new Usage Activities to track new file operations
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
#	+ IOIndex
# 	+ IOTouch
#
#################################################################

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'IOIndex', NULL, 'FILES02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'IOIndex'
                      		AND `ServiceKey` = 'FILES02');

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'IOTouch', NULL, 'FILES02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'IOTouch'
                      		AND `ServiceKey` = 'FILES02');
                      		INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 

SELECT NULL, 'NotifSearch', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifSearch'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
SELECT NULL, 'NotifAttemptSearch', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifAttemptSearch'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
SELECT NULL, 'NotifAttemptList', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifAttemptList'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
SELECT NULL, 'NotifAttemptClear', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifAttemptClear'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
SELECT NULL, 'NotifAttemptDelete', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifAttemptDelete'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
SELECT NULL, 'NotifAttemptDetails', NULL, 'NOTIFICATIONS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'NotifAttemptDetails'
                      		AND `ServiceKey` = 'NOTIFICATIONS02');
                      		
                      		