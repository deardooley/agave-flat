###############################################################
# Migration: V2.1.8.4__Insert_UUID_Usage_key.sql
#
# Adding in new Usage Activities to track uuid api reqeusts
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
# + UsageServices
#   + UUID02
# + UsageActivities
#   + UuidGen
#   + UuidLookup
#
#################################################################

INSERT INTO `UsageServices` (`ID`, `ServiceKey`, `Description`) 
SELECT NULL, 'UUID02', 'UUID Service V2'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageServices` 
                      WHERE `ServiceKey` = 'UUID02'
                      		AND `Description` = 'UUID Service V2');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'UuidGen', NULL, 'UUID02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'UuidGen'
                      		AND `ServiceKey` = 'UUID02');

                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'UuidLookup', NULL, 'UUID02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'UuidLookup'
                      		AND `ServiceKey` = 'UUID02');