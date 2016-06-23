###############################################################
# Migration: V2.1.7.9__Insert_new_systemevents.sql
#
# Adding in new System History table and usage Activities to track 
# history events
# 									   
# Database changes:
# 
# Table changes:
# + systemevents
#
# Index changes:
#
# Column changes:
# 
# Data changes:
# + UsageActivities
#	+ SystemHistoryGet
# 	+ SystemHistoryList
#   + SystemHistoryGetById
#	+ SystemHistoryGet
# 	+ SystemHistoryList
#   + SystemHistoryGetById
#
#################################################################

CREATE TABLE IF NOT EXISTS `systemevents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `created_by` varchar(32) CHARACTER SET latin1 NOT NULL,
  `description` varchar(128) CHARACTER SET latin1 DEFAULT NULL,
  `entity_uuid` varchar(64) CHARACTER SET latin1 NOT NULL,
  `ip_address` varchar(15) CHARACTER SET latin1 NOT NULL,
  `status` varchar(32) CHARACTER SET latin1 NOT NULL,
  `tenant_id` varchar(64) CHARACTER SET latin1 NOT NULL,
  `uuid` varchar(64) CHARACTER SET latin1 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `entity_uuid` (`entity_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryGet', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryGet'
                      		AND `ServiceKey` = 'SYSTEMS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryList', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryList'
                      		AND `ServiceKey` = 'SYSTEMS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryGetById', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryGetById'
                      		AND `ServiceKey` = 'SYSTEMS02');

                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryGet', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryGet'
                      		AND `ServiceKey` = 'SYSTEMS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryList', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryList'
                      		AND `ServiceKey` = 'SYSTEMS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'SystemHistoryGetById', NULL, 'SYSTEMS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'SystemHistoryGetById'
                      		AND `ServiceKey` = 'SYSTEMS02');