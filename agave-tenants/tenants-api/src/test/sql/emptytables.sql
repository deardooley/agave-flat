CREATE DATABASE  IF NOT EXISTS `${foundation.db.test.database}` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `${foundation.db.test.database}`;
-- MySQL dump 10.13  Distrib 5.5.34, for debian-linux-gnu (x86_64)
--
-- Host: bunker.tacc.utexas.edu    Database: agave-api
-- ------------------------------------------------------
-- Server version	5.5.30

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Usage`
--

DROP TABLE IF EXISTS `Usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Usage` (
  `UID` int(11) NOT NULL AUTO_INCREMENT,
  `Username` varchar(64) NOT NULL,
  `ServiceKey` varchar(30) NOT NULL DEFAULT '',
  `ActivityKey` varchar(32) NOT NULL DEFAULT '',
  `ActivityContext` varchar(64) DEFAULT NULL,
  `CreatedAt` datetime NOT NULL,
  `CallingIP` varchar(15) DEFAULT NULL,
  `UserIP` varchar(15) DEFAULT NULL,
  `ClientApplication` varchar(64) NOT NULL DEFAULT '',
  `TenantId` varchar(64) NOT NULL DEFAULT '',
  `UserAgent` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`UID`),
  KEY `ServiceKey` (`ServiceKey`),
  KEY `ActivityKey` (`ActivityKey`)
) ENGINE=InnoDB AUTO_INCREMENT=23089 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UsageActivities`
--

DROP TABLE IF EXISTS `UsageActivities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UsageActivities` (
  `id` int(200) NOT NULL AUTO_INCREMENT,
  `ActivityKey` varchar(32) NOT NULL DEFAULT '',
  `Description` text,
  `ServiceKey` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ServiceKey` (`ServiceKey`)
) ENGINE=InnoDB AUTO_INCREMENT=513 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UsageDeveloper`
--

DROP TABLE IF EXISTS `UsageDeveloper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UsageDeveloper` (
  `Username` varchar(64) NOT NULL DEFAULT '',
  `ServiceKey` varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (`Username`,`ServiceKey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UsageServices`
--

DROP TABLE IF EXISTS `UsageServices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UsageServices` (
  `ID` int(200) NOT NULL AUTO_INCREMENT,
  `ServiceKey` varchar(30) NOT NULL DEFAULT '',
  `Description` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authconfigs`
--

DROP TABLE IF EXISTS `authconfigs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `authconfigs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `credential` varchar(32768) DEFAULT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `password` varchar(128) DEFAULT NULL,
  `system_default` bit(1) DEFAULT NULL,
  `login_credential_type` varchar(16) NOT NULL,
  `username` varchar(32) DEFAULT NULL,
  `authentication_system_id` bigint(20) DEFAULT NULL,
  `remote_config_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKAB65DAC9D0F7341D` (`authentication_system_id`),
  KEY `FKAB65DAC98B60DEA6` (`remote_config_id`),
  CONSTRAINT `FKAB65DAC98B60DEA6` FOREIGN KEY (`remote_config_id`) REFERENCES `remoteconfigs` (`id`),
  CONSTRAINT `FKAB65DAC9D0F7341D` FOREIGN KEY (`authentication_system_id`) REFERENCES `credentialservers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=178 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authentication_tokens`
--

DROP TABLE IF EXISTS `authentication_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `authentication_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `creator` varchar(32) NOT NULL,
  `expires_at` datetime NOT NULL,
  `internal_username` varchar(32) NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `renewed_at` datetime NOT NULL,
  `remaining_uses` int(11) NOT NULL,
  `token` varchar(64) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batchqueues`
--

DROP TABLE IF EXISTS `batchqueues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `batchqueues` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `custom_directives` varchar(32768) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `max_jobs` bigint(20) NOT NULL,
  `max_memory` bigint(20) NOT NULL,
  `name` varchar(128) NOT NULL,
  `system_default` bit(1) DEFAULT NULL,
  `execution_system_id` bigint(20) DEFAULT NULL,
  `max_nodes` bigint(20) NOT NULL,
  `max_procesors` bigint(20) NOT NULL,
  `max_requested_time` varchar(255) DEFAULT NULL,
  `max_user_jobs` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK2F730D3CD7AE66CC` (`execution_system_id`),
  CONSTRAINT `FK2F730D3CD7AE66CC` FOREIGN KEY (`execution_system_id`) REFERENCES `executionsystems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=347 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `credentialservers`
--

DROP TABLE IF EXISTS `credentialservers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `credentialservers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `endpoint` varchar(255) NOT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(64) NOT NULL,
  `port` int(11) DEFAULT NULL,
  `protocol` varchar(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `decoding_tasks`
--

DROP TABLE IF EXISTS `decoding_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `decoding_tasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `callback_key` varchar(64) NOT NULL,
  `created` datetime NOT NULL,
  `current_filter` varchar(64) NOT NULL,
  `dest_path` varchar(255) NOT NULL,
  `dest_transform` varchar(64) NOT NULL,
  `destination_uri` varchar(255) NOT NULL,
  `source_path` varchar(255) NOT NULL,
  `src_transform` varchar(64) NOT NULL,
  `status` varchar(8) NOT NULL,
  `logical_file_id` bigint(20) DEFAULT NULL,
  `system_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKAE027D7A1DCDC7B0` (`logical_file_id`),
  KEY `FKAE027D7ABBBF083F` (`system_id`),
  CONSTRAINT `FKAE027D7A1DCDC7B0` FOREIGN KEY (`logical_file_id`) REFERENCES `logical_files` (`id`),
  CONSTRAINT `FKAE027D7ABBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `encoding_tasks`
--

DROP TABLE IF EXISTS `encoding_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `encoding_tasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `callback_key` varchar(64) NOT NULL,
  `created` datetime NOT NULL,
  `dest_path` varchar(255) NOT NULL,
  `source_path` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `transform_name` varchar(32) NOT NULL,
  `transform_filter_name` varchar(32) NOT NULL,
  `logical_file_id` bigint(20) DEFAULT NULL,
  `system_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF27B81A21DCDC7B0` (`logical_file_id`),
  KEY `FKF27B81A2BBBF083F` (`system_id`),
  CONSTRAINT `FKF27B81A21DCDC7B0` FOREIGN KEY (`logical_file_id`) REFERENCES `logical_files` (`id`),
  CONSTRAINT `FKF27B81A2BBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=682 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `executionsystems`
--

DROP TABLE IF EXISTS `executionsystems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `executionsystems` (
  `environment` varchar(32768) DEFAULT NULL,
  `execution_type` varchar(16) NOT NULL,
  `max_system_jobs` int(11) DEFAULT NULL,
  `max_system_jobs_per_user` int(11) DEFAULT NULL,
  `scheduler_type` varchar(16) NOT NULL,
  `scratch_dir` varchar(255) DEFAULT NULL,
  `startup_script` varchar(255) DEFAULT NULL,
  `type` varchar(16) NOT NULL,
  `work_dir` varchar(255) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `login_config` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK29629E0C7871F82F` (`id`),
  KEY `FK29629E0C8DC88804` (`login_config`),
  CONSTRAINT `FK29629E0C7871F82F` FOREIGN KEY (`id`) REFERENCES `systems` (`id`),
  CONSTRAINT `FK29629E0C8DC88804` FOREIGN KEY (`login_config`) REFERENCES `loginconfigs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fileevents`
--

DROP TABLE IF EXISTS `fileevents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fileevents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `ip_address` varchar(15) NOT NULL,
  `status` varchar(32) NOT NULL,
  `logicalfile_id` bigint(20) DEFAULT NULL,
  `transfertask` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK8A30C99573DE1B78` (`transfertask`),
  KEY `FK8A30C99541C615BD` (`logicalfile_id`),
  CONSTRAINT `FK8A30C99541C615BD` FOREIGN KEY (`logicalfile_id`) REFERENCES `logical_files` (`id`),
  CONSTRAINT `FK8A30C99573DE1B78` FOREIGN KEY (`transfertask`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5898 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `internalusers`
--

DROP TABLE IF EXISTS `internalusers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `internalusers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `currently_active` bit(1) DEFAULT NULL,
  `city` varchar(32) DEFAULT NULL,
  `country` varchar(32) DEFAULT NULL,
  `created` datetime NOT NULL,
  `created_by` varchar(32) NOT NULL,
  `department` varchar(64) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `fax` varchar(32) DEFAULT NULL,
  `first_name` varchar(32) DEFAULT NULL,
  `gender` int(11) DEFAULT NULL,
  `institution` varchar(64) DEFAULT NULL,
  `last_name` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `position` varchar(32) DEFAULT NULL,
  `research_area` varchar(64) DEFAULT NULL,
  `state` varchar(32) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `username` varchar(32) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `username` (`username`,`created_by`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job_permissions`
--

DROP TABLE IF EXISTS `job_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_permissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_id` bigint(20) NOT NULL,
  `last_updated` datetime NOT NULL,
  `permission` varchar(16) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `job_id` (`job_id`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jobevents`
--

DROP TABLE IF EXISTS `jobevents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobevents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `created_by` varchar(128) NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `ip_address` varchar(15) NOT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `job_id` bigint(20) NOT NULL,
  `transfertask` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK6222FB1673DE1B78` (`transfertask`),
  KEY `FK6222FB1678E880CD` (`job_id`),
  CONSTRAINT `FK6222FB1673DE1B78` FOREIGN KEY (`transfertask`) REFERENCES `transfertasks` (`id`),
  CONSTRAINT `FK6222FB1678E880CD` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5736 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jobs`
--

DROP TABLE IF EXISTS `jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `archive_output` bit(1) DEFAULT NULL,
  `archive_path` varchar(255) DEFAULT NULL,
  `callback_url` varchar(255) DEFAULT NULL,
  `charge` float DEFAULT NULL,
  `created` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `error_message` varchar(16384) DEFAULT NULL,
  `inputs` varchar(16384) DEFAULT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `local_job_id` varchar(255) DEFAULT NULL,
  `memory_request` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `output_path` varchar(255) DEFAULT NULL,
  `owner` varchar(32) NOT NULL,
  `parameters` varchar(16384) DEFAULT NULL,
  `processor_count` int(11) NOT NULL,
  `requested_time` varchar(19) DEFAULT NULL,
  `retries` int(11) DEFAULT NULL,
  `scheduler_job_id` varchar(255) DEFAULT NULL,
  `software_name` varchar(80) NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `submit_time` datetime DEFAULT NULL,
  `execution_system` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `update_token` varchar(64) DEFAULT NULL,
  `uuid` varchar(64) NOT NULL,
  `optlock` int(11) DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `work_path` varchar(255) DEFAULT NULL,
  `archive_system` bigint(20) DEFAULT NULL,
  `queue_request` varchar(80) NOT NULL,
  `node_count` bigint(20) NOT NULL,
  `status_checks` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FK31DC56AC7D7B60` (`archive_system`),
  CONSTRAINT `FK31DC56AC7D7B60` FOREIGN KEY (`archive_system`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=171 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `logical_files`
--

DROP TABLE IF EXISTS `logical_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logical_files` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(64) NOT NULL,
  `native_format` varchar(32) DEFAULT NULL,
  `owner` varchar(32) NOT NULL,
  `path` varchar(255) NOT NULL,
  `source` varchar(255) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `uuid` varchar(255) NOT NULL,
  `system_id` bigint(20) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FKBB45CEC1BBBF083F` (`system_id`),
  CONSTRAINT `FKBB45CEC1BBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=424 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `logicalfilenotifications`
--

DROP TABLE IF EXISTS `logicalfilenotifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logicalfilenotifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_sent` datetime NOT NULL,
  `status` varchar(32) NOT NULL,
  `still_pending` bit(1) DEFAULT NULL,
  `callback` varchar(1024) DEFAULT NULL,
  `logicalfile_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK2ECF400341C615BD` (`logicalfile_id`),
  CONSTRAINT `FK2ECF400341C615BD` FOREIGN KEY (`logicalfile_id`) REFERENCES `logical_files` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loginconfigs`
--

DROP TABLE IF EXISTS `loginconfigs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `loginconfigs` (
  `protocol` varchar(16) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKC32B7DE85C950942` (`id`),
  CONSTRAINT `FKC32B7DE85C950942` FOREIGN KEY (`id`) REFERENCES `remoteconfigs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metadata_permissions`
--

DROP TABLE IF EXISTS `metadata_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `metadata_permissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_updated` datetime NOT NULL,
  `permission` varchar(16) NOT NULL,
  `username` varchar(32) NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metadata_schema_permissions`
--

DROP TABLE IF EXISTS `metadata_schema_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `metadata_schema_permissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_updated` datetime NOT NULL,
  `permission` varchar(16) NOT NULL,
  `schema_id` varchar(255) NOT NULL,
  `username` varchar(32) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `schema_id` (`schema_id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `associated_uuid` varchar(64) NOT NULL,
  `attempts` int(11) NOT NULL,
  `callback_url` varchar(1024) NOT NULL,
  `created` datetime NOT NULL,
  `last_sent` datetime DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `notification_event` varchar(32) NOT NULL,
  `owner` varchar(32) NOT NULL,
  `is_persistent` bit(1) DEFAULT NULL,
  `response_code` int(11) DEFAULT NULL,
  `is_success` bit(1) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=156 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `postits`
--

DROP TABLE IF EXISTS `postits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `postits` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_url` varchar(32768) NOT NULL,
  `target_method` varchar(6) NOT NULL DEFAULT 'GET',
  `postit_key` varchar(64) NOT NULL,
  `creator` varchar(32) NOT NULL,
  `token` varchar(64) NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `remaining_uses` int(7) NOT NULL DEFAULT '-1',
  `internal_username` varchar(32) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL DEFAULT 'iplantc.org',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `proxyservers`
--

DROP TABLE IF EXISTS `proxyservers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxyservers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `host` varchar(256) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `remote_config_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKA72DF7628B60DEA6` (`remote_config_id`),
  CONSTRAINT `FKA72DF7628B60DEA6` FOREIGN KEY (`remote_config_id`) REFERENCES `remoteconfigs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remoteconfigs`
--

DROP TABLE IF EXISTS `remoteconfigs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `remoteconfigs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `host` varchar(256) NOT NULL,
  `last_updated` datetime NOT NULL,
  `port` int(11) DEFAULT NULL,
  `proxy_server_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF431326BE2764978` (`proxy_server_id`),
  CONSTRAINT `FKF431326BE2764978` FOREIGN KEY (`proxy_server_id`) REFERENCES `proxyservers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `remotefilepermissions`
--

DROP TABLE IF EXISTS `remotefilepermissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `remotefilepermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `logical_file_id` bigint(20) NOT NULL,
  `permission` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=172 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `software_inputs`
--

DROP TABLE IF EXISTS `software_inputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `software_inputs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) DEFAULT NULL,
  `file_types` varchar(128) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(64) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `min_cardinality` int(11) DEFAULT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  `required` bit(1) DEFAULT NULL,
  `validator` varchar(255) DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  `cli_argument` varchar(64) DEFAULT NULL,
  `show_cli_argument` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF4C1638159B3FD5F` (`software`),
  CONSTRAINT `FKF4C1638159B3FD5F` FOREIGN KEY (`software`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=393 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `software_outputs`
--

DROP TABLE IF EXISTS `software_outputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `software_outputs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) NOT NULL,
  `file_types` varchar(128) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(64) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `max_cardinality` int(11) DEFAULT NULL,
  `min_cardinality` int(11) DEFAULT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `pattern` varchar(255) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKECF878FA59B3FD5F` (`software`),
  CONSTRAINT `FKECF878FA59B3FD5F` FOREIGN KEY (`software`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `software_parameters`
--

DROP TABLE IF EXISTS `software_parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `software_parameters` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(64) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  `required` bit(1) DEFAULT NULL,
  `value_type` varchar(16) NOT NULL,
  `validator` varchar(255) DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  `cli_argument` varchar(64) DEFAULT NULL,
  `show_cli_argument` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKEE3EF78259B3FD5F` (`software`),
  CONSTRAINT `FKEE3EF78259B3FD5F` FOREIGN KEY (`software`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1078 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `software_permissions`
--

DROP TABLE IF EXISTS `software_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `software_permissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_updated` datetime NOT NULL,
  `permission` varchar(16) NOT NULL,
  `username` varchar(32) NOT NULL,
  `software_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKCD9271EC41F2F66B` (`software_id`),
  CONSTRAINT `FKCD9271EC41F2F66B` FOREIGN KEY (`software_id`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `softwares`
--

DROP TABLE IF EXISTS `softwares`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `softwares` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `available` bit(1) DEFAULT NULL,
  `checkpointable` bit(1) DEFAULT NULL,
  `checksum` varchar(64) DEFAULT NULL,
  `created` datetime NOT NULL,
  `deployment_path` varchar(255) NOT NULL,
  `executable_path` varchar(255) NOT NULL,
  `execution_type` varchar(8) NOT NULL,
  `helpuri` varchar(128) DEFAULT NULL,
  `icon` varchar(128) DEFAULT NULL,
  `label` varchar(64) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `long_description` varchar(32768) DEFAULT NULL,
  `modules` varchar(255) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `owner` varchar(32) NOT NULL,
  `parallelism` varchar(8) NOT NULL,
  `publicly_available` bit(1) DEFAULT NULL,
  `revision_count` int(11) DEFAULT NULL,
  `short_description` varchar(255) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `test_path` varchar(255) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `version` varchar(16) NOT NULL,
  `system_id` bigint(20) DEFAULT NULL,
  `storage_system_id` bigint(20) DEFAULT NULL,
  `default_memory` float DEFAULT NULL,
  `default_procesors` int(11) DEFAULT NULL,
  `default_queue` varchar(12) DEFAULT NULL,
  `default_requested_time` varchar(19) DEFAULT NULL,
  `default_nodes` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `name` (`name`,`version`,`publicly_available`,`tenant_id`),
  KEY `FK85C8D3AC62ED13D2` (`storage_system_id`),
  KEY `FK85C8D3AC4B955F33` (`system_id`),
  CONSTRAINT `FK85C8D3AC4B955F33` FOREIGN KEY (`system_id`) REFERENCES `executionsystems` (`id`),
  CONSTRAINT `FK85C8D3AC62ED13D2` FOREIGN KEY (`storage_system_id`) REFERENCES `storagesystems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=330 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `softwares_inputs`
--

DROP TABLE IF EXISTS `softwares_inputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `softwares_inputs` (
  `softwares` bigint(20) NOT NULL,
  `inputs` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`inputs`),
  UNIQUE KEY `inputs` (`inputs`),
  KEY `FKA75D91DC90D96F64` (`softwares`),
  KEY `FKA75D91DCD5BC00DB` (`inputs`),
  CONSTRAINT `FKA75D91DC90D96F64` FOREIGN KEY (`softwares`) REFERENCES `softwares` (`id`),
  CONSTRAINT `FKA75D91DCD5BC00DB` FOREIGN KEY (`inputs`) REFERENCES `software_inputs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `softwares_outputs`
--

DROP TABLE IF EXISTS `softwares_outputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `softwares_outputs` (
  `softwares` bigint(20) NOT NULL,
  `outputs` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`outputs`),
  UNIQUE KEY `outputs` (`outputs`),
  KEY `FK8DE215FF90D96F64` (`softwares`),
  KEY `FK8DE215FF35F2FE6B` (`outputs`),
  CONSTRAINT `FK8DE215FF35F2FE6B` FOREIGN KEY (`outputs`) REFERENCES `software_outputs` (`id`),
  CONSTRAINT `FK8DE215FF90D96F64` FOREIGN KEY (`softwares`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `softwares_parameters`
--

DROP TABLE IF EXISTS `softwares_parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `softwares_parameters` (
  `softwares` bigint(20) NOT NULL,
  `parameters` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`parameters`),
  UNIQUE KEY `parameters` (`parameters`),
  KEY `FK8016805D90D96F64` (`softwares`),
  KEY `FK8016805D7A7FA8BB` (`parameters`),
  CONSTRAINT `FK8016805D7A7FA8BB` FOREIGN KEY (`parameters`) REFERENCES `software_parameters` (`id`),
  CONSTRAINT `FK8016805D90D96F64` FOREIGN KEY (`softwares`) REFERENCES `softwares` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staging_tasks`
--

DROP TABLE IF EXISTS `staging_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `staging_tasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bytes_transferred` bigint(20) DEFAULT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `retry_count` int(11) NOT NULL,
  `status` varchar(32) NOT NULL,
  `total_bytes` bigint(20) DEFAULT NULL,
  `logical_file_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKB9B09E8A1DCDC7B0` (`logical_file_id`),
  CONSTRAINT `FKB9B09E8A1DCDC7B0` FOREIGN KEY (`logical_file_id`) REFERENCES `logical_files` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storageconfigs`
--

DROP TABLE IF EXISTS `storageconfigs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storageconfigs` (
  `home_dir` varchar(255) DEFAULT NULL,
  `mirror_permissions` bit(1) NOT NULL,
  `protocol` varchar(16) NOT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `root_dir` varchar(255) DEFAULT NULL,
  `zone` varchar(255) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK99C2F2965C950942` (`id`),
  CONSTRAINT `FK99C2F2965C950942` FOREIGN KEY (`id`) REFERENCES `remoteconfigs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storagesystems`
--

DROP TABLE IF EXISTS `storagesystems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storagesystems` (
  `type` varchar(16) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKF983E1497871F82F` (`id`),
  CONSTRAINT `FKF983E1497871F82F` FOREIGN KEY (`id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systempermissions`
--

DROP TABLE IF EXISTS `systempermissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systempermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `permission` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systemroles`
--

DROP TABLE IF EXISTS `systemroles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systemroles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `role` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systems`
--

DROP TABLE IF EXISTS `systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systems` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `available` bit(1) DEFAULT NULL,
  `created` datetime NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `global_default` bit(1) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(64) NOT NULL,
  `owner` varchar(32) NOT NULL,
  `publicly_available` bit(1) DEFAULT NULL,
  `revision` int(11) DEFAULT NULL,
  `site` varchar(64) DEFAULT NULL,
  `status` varchar(8) NOT NULL,
  `system_id` varchar(64) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `type` varchar(32) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `storage_config` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `system_id_tenant` (`system_id`,`tenant_id`),
  KEY `FK9871D424DA9BF604` (`storage_config`),
  CONSTRAINT `FK9871D424DA9BF604` FOREIGN KEY (`storage_config`) REFERENCES `storageconfigs` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systems_systemroles`
--

DROP TABLE IF EXISTS `systems_systemroles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systems_systemroles` (
  `systems` bigint(20) NOT NULL,
  `roles` bigint(20) NOT NULL,
  UNIQUE KEY `roles` (`roles`),
  UNIQUE KEY `roles_2` (`roles`),
  KEY `FK3363E5328A8DAC1` (`roles`),
  KEY `FK3363E5310E3BF38` (`systems`),
  CONSTRAINT `FK3363E5310E3BF38` FOREIGN KEY (`systems`) REFERENCES `systems` (`id`),
  CONSTRAINT `FK3363E5328A8DAC1` FOREIGN KEY (`roles`) REFERENCES `systemroles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenants`
--

DROP TABLE IF EXISTS `tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tenants` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `base_url` varchar(255) NOT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `status` varchar(64) DEFAULT NULL,
  `tenant_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transfertasks`
--

DROP TABLE IF EXISTS `transfertasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transfertasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `attempts` int(11) DEFAULT NULL,
  `bytes_transferred` double DEFAULT NULL,
  `created` datetime NOT NULL,
  `dest` varchar(2048) NOT NULL DEFAULT '',
  `end_time` datetime DEFAULT NULL,
  `event_id` varchar(255) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `owner` varchar(32) NOT NULL,
  `source` varchar(2048) NOT NULL DEFAULT '',
  `start_time` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `total_size` double DEFAULT NULL,
  `transfer_rate` double DEFAULT NULL,
  `parent_task` bigint(20) DEFAULT NULL,
  `root_task` bigint(20) DEFAULT NULL,
  `uuid` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK8914FE833015DB82` (`parent_task`),
  KEY `FK8914FE83BFE5C64A` (`root_task`),
  CONSTRAINT `FK8914FE833015DB82` FOREIGN KEY (`parent_task`) REFERENCES `transfertasks` (`id`),
  CONSTRAINT `FK8914FE83BFE5C64A` FOREIGN KEY (`root_task`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15422 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userdefaultsystems`
--

DROP TABLE IF EXISTS `userdefaultsystems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userdefaultsystems` (
  `system_id` bigint(20) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  KEY `FKC1EA8F4EBBBF083F` (`system_id`),
  CONSTRAINT `FKC1EA8F4EBBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-04-10 20:24:03
