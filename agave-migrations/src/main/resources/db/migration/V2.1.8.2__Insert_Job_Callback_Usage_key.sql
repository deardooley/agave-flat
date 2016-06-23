###############################################################
# Migration: V2.1.8.2__Insert_Job_Callback_Usage_key.sql
#
# Adding in new Usage Activities to track custom runtime job callbacks
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
#	+ JobsCustomRuntimeEvent
#
#################################################################

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'JobsCustomRuntimeEvent', NULL, 'JOBS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'JobsCustomRuntimeEvent'
                      		AND `ServiceKey` = 'JOBS02');
