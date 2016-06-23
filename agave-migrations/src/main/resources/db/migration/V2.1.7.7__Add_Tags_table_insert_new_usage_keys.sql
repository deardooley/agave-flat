###############################################################
# Migration: V2.1.7.3__Insert_new_usage_keys.sql
#
# Adding in new Usage Activities to track new file operations
# 									   
# Database changes:
# + tags
# + tags_tagged_resources
# + tagdedresources
# + tagpermissions
# + tagedresources
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

CREATE TABLE IF NOT EXISTS `tags` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(32) NOT NULL,
  `owner` varchar(32) NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `name` (`name`,`owner`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tag_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `created_by` varchar(32) NOT NULL,
  `description` varchar(128) DEFAULT NULL,
  `entity_uuid` varchar(64) NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `entity_uuid` (`entity_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `taggedresources` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `associated_uuid` varchar(64) NOT NULL,
  `tag_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK105E2639772DD7B0` (`tag_id`),
  CONSTRAINT `FK105E2639772DD7B0` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tags_tagged_resources` (
  `tags` bigint(20) NOT NULL,
  `tagged_resources` bigint(20) NOT NULL,
  UNIQUE KEY `tagged_resources` (`tagged_resources`),
  KEY `FKF26A1678ABEAB7E9` (`tags`),
  KEY `FKF26A167883AA5EB6` (`tagged_resources`),
  CONSTRAINT `FKF26A167883AA5EB6` FOREIGN KEY (`tagged_resources`) REFERENCES `taggedresources` (`id`),
  CONSTRAINT `FKF26A1678ABEAB7E9` FOREIGN KEY (`tags`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tagpermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `entity_id` varchar(64) NOT NULL,
  `last_updated` datetime NOT NULL,
  `permission` varchar(32) NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `username` varchar(32) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `username` (`username`,`permission`,`entity_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `UsageServices` (`ID`, `ServiceKey`, `Description`) 
SELECT NULL, 'TAGS02', 'Tags Service V2'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageServices` 
                      WHERE `ServiceKey` = 'TAGS02'
                      		AND `Description` = 'Tags Service V2');

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsAdd', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsAdd'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsUpdate', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsUpdate'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsDelete', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsDelete'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsGetByID', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsGetByID'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsList', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsList'
                      		AND `ServiceKey` = 'TAGS02');
                      		

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagPermissionAdd', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagPermissionAdd'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagPermissionUpdate', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagPermissionUpdate'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagPermissionDelete', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagPermissionDelete'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagPermissionGetByUsername', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagPermissionGetByUsername'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagPermissionsList', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagPermissionsList'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagResourceAdd', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagResourceAdd'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagResourceUpdate', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagResourceUpdate'
                      		AND `ServiceKey` = 'TAGS02');
                      		 
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagResourceDelete', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagResourceDelete'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagResourceGetById', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagResourceGetById'
                      		AND `ServiceKey` = 'TAGS02');

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagResourcesList', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagResourcesList'
                      		AND `ServiceKey` = 'TAGS02');

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsUsage', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsUsage'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsSearch', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsSearch'
                      		AND `ServiceKey` = 'TAGS02');
                      		
INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`) 
SELECT NULL, 'TagsHistoryList', NULL, 'TAGS02'
	FROM DUAL
	WHERE NOT EXISTS (SELECT 1 
                      FROM `UsageActivities` 
                      WHERE `ActivityKey` = 'TagsHistoryList'
                      		AND `ServiceKey` = 'TAGS02');
                      		