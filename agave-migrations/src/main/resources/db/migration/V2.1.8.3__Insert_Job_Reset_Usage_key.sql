###############################################################
# Migration: V2.1.8.3__Insert_Job_Reset_Usage_key.sql
#
# Adding in new Usage Activities to track job reset requests
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
#	+ JobReset
#
#################################################################

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'JobReset', NULL, 'JOBS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'JobReset'
                      		AND `ServiceKey` = 'JOBS02');
