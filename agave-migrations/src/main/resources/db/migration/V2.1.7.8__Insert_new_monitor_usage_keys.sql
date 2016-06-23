###############################################################
# Migration: V2.1.7.8__Insert_new_monitor_usage_keys.sql
#
# Adding in new Monitor History table and usage Activities to track 
# history events
# 									   
# Database changes:
# 
# Table changes:
# + monitorevents
#
# Index changes:
#
# Column changes:
# 
# Data changes:
# + UsageActivities
#	+ MonitorHistoryGet
# 	+ MonitorHistoryList
#   + MonitorHistoryGetById
#
#################################################################

CREATE TABLE IF NOT EXISTS `monitorevents` (
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
SELECT NULL, 'MonitorHistoryGet', NULL, 'MONITORS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'MonitorHistoryGet'
                      		AND `ServiceKey` = 'MONITORS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'MonitorHistoryList', NULL, 'MONITORS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'MonitorHistoryList'
                      		AND `ServiceKey` = 'MONITORS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'MonitorHistoryGetById', NULL, 'MONITORS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'MonitorHistoryGetById'
                      		AND `ServiceKey` = 'MONITORS02');

