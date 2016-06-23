# ************************************************************
# Database: agavecore
# Generation Time: 2016-03-24 15:13:24 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table authconfigs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `authconfigs`;

CREATE TABLE `authconfigs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `credential` varchar(32768) DEFAULT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `password` varchar(128) DEFAULT NULL,
  `system_default` tinyint(1) DEFAULT NULL,
  `login_credential_type` varchar(16) NOT NULL,
  `username` varchar(32) DEFAULT NULL,
  `authentication_system_id` bigint(20) DEFAULT NULL,
  `remote_config_id` bigint(20) DEFAULT NULL,
  `private_key` varchar(8196) DEFAULT NULL,
  `public_key` varchar(8196) DEFAULT NULL,
  `trusted_ca_url` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKAB65DAC9D0F7341D` (`authentication_system_id`),
  KEY `FKAB65DAC98B60DEA6` (`remote_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table authentication_tokens
# ------------------------------------------------------------

DROP TABLE IF EXISTS `authentication_tokens`;

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



# Dump of table batchqueues
# ------------------------------------------------------------

DROP TABLE IF EXISTS `batchqueues`;

CREATE TABLE `batchqueues` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `custom_directives` varchar(32768) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `max_jobs` bigint(20) NOT NULL,
  `max_memory` bigint(20) NOT NULL,
  `name` varchar(128) NOT NULL,
  `system_default` tinyint(1) DEFAULT NULL,
  `execution_system_id` bigint(20) DEFAULT NULL,
  `max_nodes` bigint(20) NOT NULL,
  `max_procesors` bigint(20) NOT NULL,
  `max_requested_time` varchar(255) DEFAULT NULL,
  `max_user_jobs` bigint(20) NOT NULL,
  `uuid` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK2F730D3CD7AE66CC` (`execution_system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table credentialservers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `credentialservers`;

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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table decoding_tasks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `decoding_tasks`;

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
  `last_updated` datetime NOT NULL,
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKAE027D7A1DCDC7B0` (`logical_file_id`),
  KEY `FKAE027D7ABBBF083F` (`system_id`),
  CONSTRAINT `FKAE027D7A1DCDC7B0` FOREIGN KEY (`logical_file_id`) REFERENCES `logical_files` (`id`),
  CONSTRAINT `FKAE027D7ABBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table discoverableservices
# ------------------------------------------------------------

DROP TABLE IF EXISTS `discoverableservices`;

CREATE TABLE `discoverableservices` (
  `dtype` varchar(31) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `host` varchar(128) NOT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(128) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `uuid` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table discoverableservices_capabilities
# ------------------------------------------------------------

DROP TABLE IF EXISTS `discoverableservices_capabilities`;

CREATE TABLE `discoverableservices_capabilities` (
  `discoverableservices` bigint(20) NOT NULL,
  `capabilities` bigint(20) NOT NULL,
  UNIQUE KEY `capabilities` (`capabilities`),
  KEY `FK95A6A914F8585983` (`capabilities`),
  KEY `FK95A6A914FE739CF6` (`discoverableservices`),
  CONSTRAINT `FK95A6A914F8585983` FOREIGN KEY (`capabilities`) REFERENCES `servicecapabilities` (`id`),
  CONSTRAINT `FK95A6A914FE739CF6` FOREIGN KEY (`discoverableservices`) REFERENCES `discoverableservices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table encoding_tasks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `encoding_tasks`;

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
  `optlock` int(11) DEFAULT NULL,
  `last_updated` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF27B81A21DCDC7B0` (`logical_file_id`),
  KEY `FKF27B81A2BBBF083F` (`system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table executionsystems
# ------------------------------------------------------------

DROP TABLE IF EXISTS `executionsystems`;

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
  KEY `FK29629E0C8DC88804` (`login_config`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table fileevents
# ------------------------------------------------------------

DROP TABLE IF EXISTS `fileevents`;

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
  KEY `FK8A30C99541C615BD` (`logicalfile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table internalusers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `internalusers`;

CREATE TABLE `internalusers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `currently_active` tinyint(1) DEFAULT NULL,
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
  `street1` varchar(64) DEFAULT NULL,
  `street2` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `username` (`username`,`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table job_permissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job_permissions`;

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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table jobevents
# ------------------------------------------------------------

DROP TABLE IF EXISTS `jobevents`;

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
  `uuid` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK6222FB1673DE1B78` (`transfertask`),
  KEY `FK6222FB1678E880CD` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table jobs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `jobs`;

CREATE TABLE `jobs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `archive_output` tinyint(1) DEFAULT NULL,
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
  `execution_system` varchar(64) NOT NULL DEFAULT '',
  `tenant_id` varchar(128) NOT NULL,
  `update_token` varchar(64) DEFAULT NULL,
  `uuid` varchar(64) NOT NULL,
  `optlock` int(11) DEFAULT NULL,
  `visible` tinyint(1) DEFAULT NULL,
  `work_path` varchar(255) DEFAULT NULL,
  `archive_system` bigint(64) DEFAULT NULL,
  `queue_request` varchar(80) NOT NULL,
  `node_count` bigint(20) NOT NULL,
  `status_checks` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FK31DC56AC7D7B60` (`archive_system`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table logical_files
# ------------------------------------------------------------

DROP TABLE IF EXISTS `logical_files`;

CREATE TABLE `logical_files` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `name` varchar(256) NOT NULL DEFAULT '',
  `native_format` varchar(32) DEFAULT NULL,
  `owner` varchar(32) NOT NULL,
  `path` varchar(2048) NOT NULL DEFAULT '',
  `source` varchar(2048) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `uuid` varchar(255) NOT NULL,
  `system_id` bigint(20) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FKBB45CEC1BBBF083F` (`system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table logicalfilenotifications
# ------------------------------------------------------------

DROP TABLE IF EXISTS `logicalfilenotifications`;

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
  KEY `FK2ECF400341C615BD` (`logicalfile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table loginconfigs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `loginconfigs`;

CREATE TABLE `loginconfigs` (
  `protocol` varchar(16) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKC32B7DE85C950942` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table metadata_permissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `metadata_permissions`;

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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table metadata_schema_permissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `metadata_schema_permissions`;

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



# Dump of table monitor_checks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `monitor_checks`;

CREATE TABLE `monitor_checks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `message` varchar(2048) DEFAULT NULL,
  `result` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `monitor` bigint(20) DEFAULT NULL,
  `type` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FK83E322F026AC90B` (`monitor`),
  CONSTRAINT `FK83E322F026AC90B` FOREIGN KEY (`monitor`) REFERENCES `monitors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table monitors
# ------------------------------------------------------------

DROP TABLE IF EXISTS `monitors`;

CREATE TABLE `monitors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `is_active` tinyint(1) DEFAULT '0',
  `created` datetime NOT NULL,
  `frequency` int(11) DEFAULT NULL,
  `internal_username` varchar(64) DEFAULT NULL,
  `last_success` datetime DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `next_update_time` datetime NOT NULL,
  `owner` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `update_system_status` tinyint(1) DEFAULT NULL,
  `uuid` varchar(64) NOT NULL,
  `system` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `system` (`system`,`owner`,`tenant_id`),
  KEY `FKEC66EE59438E5D43` (`system`),
  CONSTRAINT `FKEC66EE59438E5D43` FOREIGN KEY (`system`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table notifications
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notifications`;

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
  `is_persistent` tinyint(1) DEFAULT NULL,
  `response_code` int(11) DEFAULT NULL,
  `is_success` bit(1) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `is_terminated` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table postits
# ------------------------------------------------------------

DROP TABLE IF EXISTS `postits`;

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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table proxyservers
# ------------------------------------------------------------

DROP TABLE IF EXISTS `proxyservers`;

CREATE TABLE `proxyservers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `host` varchar(256) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `remote_config_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKA72DF7628B60DEA6` (`remote_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table remoteconfigs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `remoteconfigs`;

CREATE TABLE `remoteconfigs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `host` varchar(256) NOT NULL,
  `last_updated` datetime NOT NULL,
  `port` int(11) DEFAULT NULL,
  `proxy_server_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF431326BE2764978` (`proxy_server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table remotefilepermissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `remotefilepermissions`;

CREATE TABLE `remotefilepermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `internal_username` varchar(32) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `logical_file_id` bigint(20) NOT NULL,
  `permission` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `username` varchar(32) NOT NULL,
  `is_recursive` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table servicecapabilities
# ------------------------------------------------------------

DROP TABLE IF EXISTS `servicecapabilities`;

CREATE TABLE `servicecapabilities` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity_type` varchar(32) DEFAULT NULL,
  `api_name` varchar(32) DEFAULT NULL,
  `definition` text,
  `group_name` varchar(64) DEFAULT NULL,
  `system_id` varchar(64) DEFAULT NULL,
  `system_queue` varchar(64) DEFAULT NULL,
  `tenant_id` varchar(64) DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `batch_queue` varchar(64) DEFAULT NULL,
  `dest_system` varchar(64) DEFAULT NULL,
  `execution_system` varchar(64) DEFAULT NULL,
  `source_system` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table software_inputs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `software_inputs`;

CREATE TABLE `software_inputs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) DEFAULT NULL,
  `file_types` varchar(128) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(128) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `min_cardinality` int(11) DEFAULT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  `required` tinyint(1) DEFAULT NULL,
  `validator` varchar(255) DEFAULT NULL,
  `visible` tinyint(1) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  `cli_argument` varchar(64) DEFAULT NULL,
  `show_cli_argument` tinyint(1) NOT NULL,
  `enquote` tinyint(1) NOT NULL,
  `max_cardinality` int(11) NOT NULL DEFAULT '-1',
  `repeat_cli_argument` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKF4C1638159B3FD5F` (`software`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table software_outputs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `software_outputs`;

CREATE TABLE `software_outputs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) DEFAULT NULL,
  `file_types` varchar(128) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(128) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `max_cardinality` int(11) DEFAULT NULL,
  `min_cardinality` int(11) DEFAULT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `pattern` varchar(255) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKECF878FA59B3FD5F` (`software`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table software_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `software_parameters`;

CREATE TABLE `software_parameters` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `default_value` varchar(255) DEFAULT '',
  `description` varchar(32768) DEFAULT NULL,
  `output_key` varchar(64) NOT NULL,
  `label` varchar(128) DEFAULT NULL,
  `last_updated` datetime NOT NULL,
  `ontology` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  `required` tinyint(1) DEFAULT NULL,
  `value_type` varchar(16) NOT NULL,
  `validator` varchar(255) DEFAULT NULL,
  `visible` tinyint(1) DEFAULT NULL,
  `software` bigint(20) DEFAULT NULL,
  `cli_argument` varchar(64) DEFAULT NULL,
  `show_cli_argument` tinyint(1) NOT NULL,
  `enquoted` tinyint(1) NOT NULL,
  `max_cardinality` int(11) DEFAULT '1',
  `min_cardinality` int(11) DEFAULT NULL,
  `repeat_cli_argument` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKEE3EF78259B3FD5F` (`software`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table software_permissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `software_permissions`;

CREATE TABLE `software_permissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_updated` datetime NOT NULL,
  `permission` varchar(16) NOT NULL,
  `username` varchar(32) NOT NULL,
  `software_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKCD9271EC41F2F66B` (`software_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwareevents
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwareevents`;

CREATE TABLE `softwareevents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `created_by` varchar(128) NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `ip_address` varchar(15) NOT NULL,
  `software_uuid` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `transfertask` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FKFA0F298073DE1B78` (`transfertask`),
  KEY `software_uuid` (`software_uuid`),
  CONSTRAINT `FKFA0F298073DE1B78` FOREIGN KEY (`transfertask`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwareparameterenums
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwareparameterenums`;

CREATE TABLE `softwareparameterenums` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `label` varchar(512) NOT NULL,
  `value` varchar(512) NOT NULL,
  `software_parameter` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK7AA72AF0E2B651E2` (`software_parameter`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwares
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwares`;

CREATE TABLE `softwares` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `available` tinyint(1) DEFAULT NULL,
  `checkpointable` tinyint(1) DEFAULT NULL,
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
  `publicly_available` tinyint(1) DEFAULT NULL,
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
  UNIQUE KEY `name` (`name`,`version`,`publicly_available`,`revision_count`,`tenant_id`),
  KEY `FK85C8D3AC62ED13D2` (`storage_system_id`),
  KEY `FK85C8D3AC4B955F33` (`system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwares_inputs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwares_inputs`;

CREATE TABLE `softwares_inputs` (
  `softwares` bigint(20) NOT NULL,
  `inputs` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`inputs`),
  UNIQUE KEY `inputs` (`inputs`),
  KEY `FKA75D91DC90D96F64` (`softwares`),
  KEY `FKA75D91DCD5BC00DB` (`inputs`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwares_outputs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwares_outputs`;

CREATE TABLE `softwares_outputs` (
  `softwares` bigint(20) NOT NULL,
  `outputs` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`outputs`),
  UNIQUE KEY `outputs` (`outputs`),
  KEY `FK8DE215FF90D96F64` (`softwares`),
  KEY `FK8DE215FF35F2FE6B` (`outputs`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table softwares_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `softwares_parameters`;

CREATE TABLE `softwares_parameters` (
  `softwares` bigint(20) NOT NULL,
  `parameters` bigint(20) NOT NULL,
  PRIMARY KEY (`softwares`,`parameters`),
  UNIQUE KEY `parameters` (`parameters`),
  KEY `FK8016805D90D96F64` (`softwares`),
  KEY `FK8016805D7A7FA8BB` (`parameters`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table staging_tasks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `staging_tasks`;

CREATE TABLE `staging_tasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bytes_transferred` bigint(20) DEFAULT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `retry_count` int(11) NOT NULL,
  `status` varchar(32) NOT NULL,
  `total_bytes` bigint(20) DEFAULT NULL,
  `logical_file_id` bigint(20) DEFAULT NULL,
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKB9B09E8A1DCDC7B0` (`logical_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table storageconfigs
# ------------------------------------------------------------

DROP TABLE IF EXISTS `storageconfigs`;

CREATE TABLE `storageconfigs` (
  `home_dir` varchar(255) DEFAULT NULL,
  `mirror_permissions` tinyint(1) NOT NULL,
  `protocol` varchar(16) NOT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `root_dir` varchar(255) DEFAULT NULL,
  `zone` varchar(255) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  `public_apps_dir` varchar(255) DEFAULT NULL,
  `container` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK99C2F2965C950942` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table storagesystems
# ------------------------------------------------------------

DROP TABLE IF EXISTS `storagesystems`;

CREATE TABLE `storagesystems` (
  `type` varchar(16) NOT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKF983E1497871F82F` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table systempermissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `systempermissions`;

CREATE TABLE `systempermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `permission` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table systemroles
# ------------------------------------------------------------

DROP TABLE IF EXISTS `systemroles`;

CREATE TABLE `systemroles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `role` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table systems
# ------------------------------------------------------------

DROP TABLE IF EXISTS `systems`;

CREATE TABLE `systems` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `available` tinyint(1) NOT NULL DEFAULT '1',
  `created` datetime NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `global_default` tinyint(1) NOT NULL DEFAULT '0',
  `last_updated` datetime NOT NULL,
  `name` varchar(64) NOT NULL,
  `owner` varchar(32) NOT NULL,
  `publicly_available` tinyint(1) NOT NULL DEFAULT '0',
  `revision` int(11) DEFAULT NULL,
  `site` varchar(64) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT '',
  `system_id` varchar(64) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `type` varchar(32) NOT NULL,
  `uuid` varchar(128) NOT NULL,
  `storage_config` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `system_id_tenant` (`system_id`,`tenant_id`),
  KEY `FK9871D424DA9BF604` (`storage_config`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table systems_systemroles
# ------------------------------------------------------------

DROP TABLE IF EXISTS `systems_systemroles`;

CREATE TABLE `systems_systemroles` (
  `systems` bigint(20) NOT NULL,
  `roles` bigint(20) NOT NULL,
  UNIQUE KEY `roles` (`roles`),
  UNIQUE KEY `roles_2` (`roles`),
  KEY `FK3363E5328A8DAC1` (`roles`),
  KEY `FK3363E5310E3BF38` (`systems`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table tenants
# ------------------------------------------------------------

DROP TABLE IF EXISTS `tenants`;

CREATE TABLE `tenants` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL,
  `base_url` varchar(255) NOT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `status` varchar(64) DEFAULT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `uuid` varchar(128) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;

INSERT INTO `tenants` (`id`, `name`, `base_url`, `contact_email`, `contact_name`, `created`, `last_updated`, `status`, `tenant_id`, `uuid`)
VALUES
	(1,'Agave Development Tenant','https://apim.tenants.dev.example.com/','admin@example.com','Agave Admin','2011-01-01 08:01:00','2011-01-01 08:01:00','LIVE','agave.dev','0001411570898814-b0b0b0bb0b-0001-016');

/*!40000 ALTER TABLE `tenants` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table transfer_events
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transfer_events`;

CREATE TABLE `transfer_events` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `owner` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `transfertask` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FK47E777ED73DE1B78` (`transfertask`),
  CONSTRAINT `FK47E777ED73DE1B78` FOREIGN KEY (`transfertask`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table transferevents
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transferevents`;

CREATE TABLE `transferevents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `description` varchar(32768) DEFAULT NULL,
  `ip_address` varchar(15) NOT NULL,
  `owner` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `uuid` varchar(64) NOT NULL,
  `transfertask` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `FK8113BCC473DE1B78` (`transfertask`),
  CONSTRAINT `FK8113BCC473DE1B78` FOREIGN KEY (`transfertask`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table transfertaskpermissions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transfertaskpermissions`;

CREATE TABLE `transfertaskpermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  `permission` varchar(32) NOT NULL,
  `is_recursive` tinyint(1) DEFAULT NULL,
  `tenant_id` varchar(128) NOT NULL,
  `transfertask_id` bigint(20) NOT NULL,
  `username` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table transfertasks
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transfertasks`;

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
  `total_files` bigint(20) NOT NULL DEFAULT '0',
  `total_skipped` bigint(20) NOT NULL DEFAULT '0',
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK8914FE833015DB82` (`parent_task`),
  KEY `FK8914FE83BFE5C64A` (`root_task`),
  KEY `root_task` (`root_task`),
  KEY `uuid` (`uuid`),
  CONSTRAINT `FK8914FE83BFE5C64A` FOREIGN KEY (`root_task`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table transfertasks_myisam
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transfertasks_myisam`;

CREATE TABLE `transfertasks_myisam` (
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
  `total_files` bigint(20) NOT NULL DEFAULT '0',
  `total_skipped` bigint(20) NOT NULL DEFAULT '0',
  `optlock` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK8914FE833015DB82` (`parent_task`),
  KEY `FK8914FE83BFE5C64A` (`root_task`),
  KEY `root_task` (`root_task`),
  KEY `uuid` (`uuid`),
  KEY `created` (`created`),
  CONSTRAINT `transfertasks_myisam_ibfk_1` FOREIGN KEY (`root_task`) REFERENCES `transfertasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table Usage
# ------------------------------------------------------------

DROP TABLE IF EXISTS `Usage`;

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
  KEY `ActivityKey` (`ActivityKey`),
  KEY `Username` (`Username`),
  KEY `CreatedAt` (`CreatedAt`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



# Dump of table UsageActivities
# ------------------------------------------------------------

DROP TABLE IF EXISTS `UsageActivities`;

CREATE TABLE `UsageActivities` (
  `id` int(200) NOT NULL AUTO_INCREMENT,
  `ActivityKey` varchar(32) NOT NULL DEFAULT '',
  `Description` text,
  `ServiceKey` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ServiceKey` (`ServiceKey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `UsageActivities` WRITE;
/*!40000 ALTER TABLE `UsageActivities` DISABLE KEYS */;

INSERT INTO `UsageActivities` (`id`, `ActivityKey`, `Description`, `ServiceKey`)
VALUES
	(1,'AppsAdd','Publish application to apps service','Apps01'),
	(2,'AppsDelete','Delete application from apps service','Apps01'),
	(3,'AppsForm','Generate html form for a published app','Apps01'),
	(4,'AppsGetByID','Get application by specific id','Apps01'),
	(5,'AppsListPublic','List public apps','Apps01'),
	(6,'AppsListShared','List private apps','Apps01'),
	(7,'AppsSeachPublicByName','Find public app by name','Apps01'),
	(8,'AppsSeachPublicByTag','Find public app by tag','Apps01'),
	(9,'AppsSeachPublicByTerm','Find public app by term','Apps01'),
	(10,'AppsSeachSharedByName','Find shared app by name','Apps01'),
	(11,'AppsSeachSharedByTag','Find shared app by tag','Apps01'),
	(12,'AppsSeachSharedByTerm','Find shared app by term','Apps01'),
	(13,'AppsUsage','View apps service usage page','Apps01'),
	(14,'AtmoLaunch','Launch an Instance','Atm02'),
	(15,'AuthCreate','Create an auth token','Auth01'),
	(16,'AuthList','List all valid auth tokens','Auth01'),
	(17,'AuthRenew','Renew an auth token','Auth01'),
	(18,'AuthRevoke','Revoke an auth token','Auth01'),
	(19,'AuthVerify','Verify an auth token','Auth01'),
	(20,'claimit','redeem a postit','Postit01'),
	(21,'DataImpliedExport','Transform from native format to specified format and export','Data01'),
	(22,'DataImpliedTransform','Transform from native format to specified format','Data01'),
	(23,'DataList','List all available transforms','Data01'),
	(24,'DataSearchByFile','Search for transforms available for a file','Data01'),
	(25,'DataSearchByName','Search for transform by name','Data01'),
	(26,'DataSearchByTag','Search for transform by tag','Data01'),
	(27,'DataSpecifiedExport','Transform from specified format to specified format and export','Data01'),
	(28,'DataSpecifiedTransform','Transform from specified format to specified format','Data01'),
	(29,'DataViewCloud','View word cloud of data transform usage','Data01'),
	(30,'de-analysis-close','The Analysis window is closed','de-prod'),
	(31,'de-analysis-delete','','de-prod'),
	(32,'de-analysis-open','The Analysis window is opened','de-prod'),
	(33,'de-analysis-view-output','','de-prod'),
	(34,'de-apps-close','','de-prod'),
	(35,'de-apps-copy','','de-prod'),
	(36,'de-apps-delete','','de-prod'),
	(37,'de-apps-edit','','de-prod'),
	(38,'de-apps-fail','An app ran unsuccessfully','de-prod'),
	(39,'de-apps-fav','User rates or rerates an app','de-prod'),
	(40,'de-apps-launch-app','User launches an app','de-prod'),
	(41,'de-apps-make-public','','de-prod'),
	(42,'de-apps-new-app','','de-prod'),
	(43,'de-apps-new-workflow','','de-prod'),
	(44,'de-apps-open','','de-prod'),
	(45,'de-apps-open-app','User brings up the launch dialog for an app','de-prod'),
	(46,'de-apps-success','An app ran successfully','de-prod'),
	(47,'de-apps-unfav','User removes an app rating','de-prod'),
	(48,'de-data-close','The Data window is closed','de-prod'),
	(49,'de-data-file-delete','','de-prod'),
	(50,'de-data-file-download','','de-prod'),
	(51,'de-data-file-rename','','de-prod'),
	(52,'de-data-file-view','','de-prod'),
	(53,'de-data-folder-delete ','A folder is deleted','de-prod'),
	(54,'de-data-folder-new','A new folder is created','de-prod'),
	(55,'de-data-folder-rename','A folder is renamed','de-prod'),
	(56,'de-data-import-desktop','','de-prod'),
	(57,'de-data-import-url','','de-prod'),
	(58,'de-data-open','The Data window is opened','de-prod'),
	(59,'de-error','An error window appears','de-prod'),
	(60,'de-help-doc','User clicks the Documentation link','de-prod'),
	(61,'de-help-forums','User clicks the Forums link','de-prod'),
	(62,'de-help-support','User clicks the Support link','de-prod'),
	(63,'de-login','User logs into the DE','de-prod'),
	(64,'de-logout','User logs out of the DE','de-prod'),
	(65,'de-notif-close','','de-prod'),
	(66,'de-notif-new','User receives a new notification','de-prod'),
	(67,'de-notif-open','','de-prod'),
	(68,'de-notif-view','','de-prod'),
	(69,'IOCopy','Copy a file or folder','IO01'),
	(70,'IODelete','Delete a file or folder','IO01'),
	(71,'IODownload','Download a file','IO01'),
	(72,'IOExport','Async export a file','IO01'),
	(73,'IOImport','Async import a file','IO01'),
	(74,'IOList','List file or folder','IO01'),
	(75,'IOMakeDir','Create a new folder','IO01'),
	(76,'IOMove','Move file or folder','IO01'),
	(77,'IOPublicDownload','Download a public file','IO01'),
	(78,'IORename','Rename a file or folder','IO01'),
	(79,'IOShare','Manage share permissions on a file or folder','IO01'),
	(80,'IOUpload','Upload a file','IO01'),
	(81,'IOUsage','View io service usage page','IO01'),
	(82,'JobsDelete','Delete a job','Jobs01'),
	(83,'JobsGetByID','Get job by specific id','Jobs01'),
	(84,'JobsGetInput','Download input file from a job','Jobs01'),
	(85,'JobsGetOutput','Download output file from a job','Jobs01'),
	(86,'JobsKill','Kill a job','Jobs01'),
	(87,'JobsList','List a user\'s jobs','Jobs01'),
	(88,'JobsListInputs','List inputs for a job','Jobs01'),
	(89,'JobsListOutputs','List output path for a job','Jobs01'),
	(90,'JobsResubmit','Download output file from a job','Jobs01'),
	(91,'JobsShare','Manage share permissions on a job','Jobs01'),
	(92,'JobsSubmit','Submit a job','Jobs01'),
	(93,'JobsUsage','View jobs service usage page','Jobs01'),
	(94,'postit','create a new postit','Postit01'),
	(95,'ProfileSearchEmail','Search profile service with email address','Profile01'),
	(96,'ProfileSearchName','Search profile service with name','Profile01'),
	(97,'ProfileSearchUsername','Search profile service with username','Profile01'),
	(98,'ProfileUsage','View profile service usage page','Profile01'),
	(99,'ProfileUsername','Query profile service with username','Profile01'),
	(100,'revokeit','revoke a postit','Postit01'),
	(101,'tito-main-window','User clicks \"back\" for the landing screen','tito-prod'),
	(102,'de-login','User logs into the DE','de-qa'),
	(103,'de-logout','User logs out of the DE','de-qa'),
	(104,'de-data-open','The Data window is opened','de-qa'),
	(105,'de-data-close','The Data window is closed','de-qa'),
	(106,'de-data-folder-new','A new folder is created','de-qa'),
	(107,'de-data-folder-delete ','A folder is deleted','de-qa'),
	(108,'de-data-folder-rename','A folder is renamed','de-qa'),
	(109,'de-data-file-view','','de-qa'),
	(110,'de-data-file-rename','','de-qa'),
	(111,'de-data-file-delete','','de-qa'),
	(112,'de-data-file-download','','de-qa'),
	(113,'de-data-import-desktop','','de-qa'),
	(114,'de-data-import-url','','de-qa'),
	(115,'de-analysis-open','The Analysis window is opened','de-qa'),
	(116,'de-analysis-close','The Analysis window is closed','de-qa'),
	(117,'de-analysis-delete','','de-qa'),
	(118,'de-analysis-view-output','','de-qa'),
	(119,'de-apps-open','','de-qa'),
	(120,'de-apps-close','','de-qa'),
	(121,'de-apps-open-app','User brings up the launch dialog for an app','de-qa'),
	(122,'de-apps-launch-app','User launches an app','de-qa'),
	(123,'de-apps-success','An app ran successfully','de-qa'),
	(124,'de-apps-fail','An app ran unsuccessfully','de-qa'),
	(125,'de-apps-new-app','','de-qa'),
	(126,'de-apps-new-workflow','','de-qa'),
	(127,'de-apps-copy','','de-qa'),
	(128,'de-apps-edit','','de-qa'),
	(129,'de-apps-delete','','de-qa'),
	(130,'de-apps-make-public','','de-qa'),
	(131,'de-apps-fav','User rates or rerates an app','de-qa'),
	(132,'de-apps-unfav','User removes an app rating','de-qa'),
	(133,'de-notif-new','User receives a new notification','de-qa'),
	(134,'de-notif-open','','de-qa'),
	(135,'de-notif-close','','de-qa'),
	(136,'de-notif-view','','de-qa'),
	(137,'de-help-doc','User clicks the Documentation link','de-qa'),
	(138,'de-help-forums','User clicks the Forums link','de-qa'),
	(139,'de-help-support','User clicks the Support link','de-qa'),
	(140,'de-error','An error window appears','de-qa'),
	(141,'de-login','User logs into the DE','de-dev'),
	(142,'de-logout','User logs out of the DE','de-dev'),
	(143,'de-data-open','The Data window is opened','de-dev'),
	(144,'de-data-close','The Data window is closed','de-dev'),
	(145,'de-data-folder-new','A new folder is created','de-dev'),
	(146,'de-data-folder-delete ','A folder is deleted','de-dev'),
	(147,'de-data-folder-rename','A folder is renamed','de-dev'),
	(148,'de-data-file-view','','de-dev'),
	(149,'de-data-file-rename','','de-dev'),
	(150,'de-data-file-delete','','de-dev'),
	(151,'de-data-file-download','','de-dev'),
	(152,'de-data-import-desktop','','de-dev'),
	(153,'de-data-import-url','','de-dev'),
	(154,'de-analysis-open','The Analysis window is opened','de-dev'),
	(155,'de-analysis-close','The Analysis window is closed','de-dev'),
	(156,'de-analysis-delete','','de-dev'),
	(157,'de-analysis-view-output','','de-dev'),
	(158,'de-apps-open','','de-dev'),
	(159,'de-apps-close','','de-dev'),
	(160,'de-apps-open-app','User brings up the launch dialog for an app','de-dev'),
	(161,'de-apps-launch-app','User launches an app','de-dev'),
	(162,'de-apps-success','An app ran successfully','de-dev'),
	(163,'de-apps-fail','An app ran unsuccessfully','de-dev'),
	(164,'de-apps-new-app','','de-dev'),
	(165,'de-apps-new-workflow','','de-dev'),
	(166,'de-apps-copy','','de-dev'),
	(167,'de-apps-edit','','de-dev'),
	(168,'de-apps-delete','','de-dev'),
	(169,'de-apps-make-public','','de-dev'),
	(170,'de-apps-fav','User rates or rerates an app','de-dev'),
	(171,'de-apps-unfav','User removes an app rating','de-dev'),
	(172,'de-notif-new','User receives a new notification','de-dev'),
	(173,'de-notif-open','','de-dev'),
	(174,'de-notif-close','','de-dev'),
	(175,'de-notif-view','','de-dev'),
	(176,'de-help-doc','User clicks the Documentation link','de-dev'),
	(177,'de-help-forums','User clicks the Forums link','de-dev'),
	(178,'de-help-support','User clicks the Support link','de-dev'),
	(179,'de-error','An error window appears','de-dev'),
	(180,'de-login','User logs into the DE','de-staging'),
	(181,'de-logout','User logs out of the DE','de-staging'),
	(182,'de-data-open','The Data window is opened','de-staging'),
	(183,'de-data-close','The Data window is closed','de-staging'),
	(184,'de-data-folder-new','A new folder is created','de-staging'),
	(185,'de-data-folder-delete ','A folder is deleted','de-staging'),
	(186,'de-data-folder-rename','A folder is renamed','de-staging'),
	(187,'de-data-file-view','','de-staging'),
	(188,'de-data-file-rename','','de-staging'),
	(189,'de-data-file-delete','','de-staging'),
	(190,'de-data-file-download','','de-staging'),
	(191,'de-data-import-desktop','','de-staging'),
	(192,'de-data-import-url','','de-staging'),
	(193,'de-analysis-open','The Analysis window is opened','de-staging'),
	(194,'de-analysis-close','The Analysis window is closed','de-staging'),
	(195,'de-analysis-delete','','de-staging'),
	(196,'de-analysis-view-output','','de-staging'),
	(197,'de-apps-open','','de-staging'),
	(198,'de-apps-close','','de-staging'),
	(199,'de-apps-open-app','User brings up the launch dialog for an app','de-staging'),
	(200,'de-apps-launch-app','User launches an app','de-staging'),
	(201,'de-apps-success','An app ran successfully','de-staging'),
	(202,'de-apps-fail','An app ran unsuccessfully','de-staging'),
	(203,'de-apps-new-app','','de-staging'),
	(204,'de-apps-new-workflow','','de-staging'),
	(205,'de-apps-copy','','de-staging'),
	(206,'de-apps-edit','','de-staging'),
	(207,'de-apps-delete','','de-staging'),
	(208,'de-apps-make-public','','de-staging'),
	(209,'de-apps-fav','User rates or rerates an app','de-staging'),
	(210,'de-apps-unfav','User removes an app rating','de-staging'),
	(211,'de-notif-new','User receives a new notification','de-staging'),
	(212,'de-notif-open','','de-staging'),
	(213,'de-notif-close','','de-staging'),
	(214,'de-notif-view','','de-staging'),
	(215,'de-help-doc','User clicks the Documentation link','de-staging'),
	(216,'de-help-forums','User clicks the Forums link','de-staging'),
	(217,'de-help-support','User clicks the Support link','de-staging'),
	(218,'de-error','An error window appears','de-staging'),
	(219,'tito-login','','tito-prod'),
	(220,'tito-logout','','tito-prod'),
	(221,'tito-create-new','','tito-prod'),
	(222,'tito-edit','','tito-prod'),
	(223,'tito-copy','','tito-prod'),
	(224,'tito-publish','','tito-prod'),
	(225,'tito-request-install','','tito-prod'),
	(226,'tito-delete','','tito-prod'),
	(227,'tito-save','','tito-prod'),
	(228,'tito-preview-ui','','tito-prod'),
	(229,'tito-preview-json','','tito-prod'),
	(230,'tito-help-doc','User clicks the Documentation link','tito-prod'),
	(231,'tito-help-forums','User clicks the Forums link','tito-prod'),
	(232,'tito-help-support','User clicks the Support link','tito-prod'),
	(233,'tito-error','An error window appears','tito-prod'),
	(234,'tito-login','','tito-de'),
	(235,'tito-logout','','tito-de'),
	(236,'tito-create-new','','tito-de'),
	(237,'tito-edit','','tito-de'),
	(238,'tito-copy','','tito-de'),
	(239,'tito-publish','','tito-de'),
	(240,'tito-request-install','','tito-de'),
	(241,'tito-delete','','tito-de'),
	(242,'tito-save','','tito-de'),
	(243,'tito-preview-ui','','tito-de'),
	(244,'tito-preview-json','','tito-de'),
	(245,'tito-main-window','User clicks \"back\" for the landing screen','tito-de'),
	(246,'tito-help-doc','User clicks the Documentation link','tito-de'),
	(247,'tito-help-forums','User clicks the Forums link','tito-de'),
	(248,'tito-help-support','User clicks the Support link','tito-de'),
	(249,'tito-error','An error window appears','tito-de'),
	(250,'tito-login','','tito-qa'),
	(251,'tito-logout','','tito-qa'),
	(252,'tito-create-new','','tito-qa'),
	(253,'tito-edit','','tito-qa'),
	(254,'tito-copy','','tito-qa'),
	(255,'tito-publish','','tito-qa'),
	(256,'tito-request-install','','tito-qa'),
	(257,'tito-delete','','tito-qa'),
	(258,'tito-save','','tito-qa'),
	(259,'tito-preview-ui','','tito-qa'),
	(260,'tito-preview-json','','tito-qa'),
	(261,'tito-main-window','User clicks \"back\" for the landing screen','tito-qa'),
	(262,'tito-help-doc','User clicks the Documentation link','tito-qa'),
	(263,'tito-help-forums','User clicks the Forums link','tito-qa'),
	(264,'tito-help-support','User clicks the Support link','tito-qa'),
	(265,'tito-error','An error window appears','tito-qa'),
	(266,'tito-login','','tito-staging'),
	(267,'tito-logout','','tito-staging'),
	(268,'tito-create-new','','tito-staging'),
	(269,'tito-edit','','tito-staging'),
	(270,'tito-copy','','tito-staging'),
	(271,'tito-publish','','tito-staging'),
	(272,'tito-request-install','','tito-staging'),
	(273,'tito-delete','','tito-staging'),
	(274,'tito-save','','tito-staging'),
	(275,'tito-preview-ui','','tito-staging'),
	(276,'tito-preview-json','','tito-staging'),
	(277,'tito-main-window','User clicks \"back\" for the landing screen','tito-staging'),
	(278,'tito-help-doc','User clicks the Documentation link','tito-staging'),
	(279,'tito-help-forums','User clicks the Forums link','tito-staging'),
	(280,'tito-help-support','User clicks the Support link','tito-staging'),
	(281,'tito-error','An error window appears','tito-staging'),
	(282,'belph-categ-add','','belphegor-prod'),
	(283,'belph-categ-delete','','belphegor-prod'),
	(284,'belph-categ-rename','','belphegor-prod'),
	(285,'belph-categ-move','User moves a subcategory to a different parent','belphegor-prod'),
	(286,'belph-categ-expand','User expands the categories panel','belphegor-prod'),
	(287,'belph-categ-collapse','User collapses the categories panel','belphegor-prod'),
	(288,'belph-app-delete','','belphegor-prod'),
	(289,'belph-app-edit','User opens the edit dialog','belphegor-prod'),
	(290,'belph-app-recateg','User moves an app to a different category','belphegor-prod'),
	(291,'belph-app-expand','User expands an app','belphegor-prod'),
	(292,'belph-app-save','User clicks \"save\" in the edit dialog','belphegor-prod'),
	(293,'belph-app-doc','User clicks the Documentation link for an app','belphegor-prod'),
	(294,'belph-app-forum','User clicks the Forum link for an app','belphegor-prod'),
	(295,'belph-categ-add','','belphegor-qa'),
	(296,'belph-categ-delete','','belphegor-qa'),
	(297,'belph-categ-rename','','belphegor-qa'),
	(298,'belph-categ-move','User moves a subcategory to a different parent','belphegor-qa'),
	(299,'belph-categ-expand','User expands the categories panel','belphegor-qa'),
	(300,'belph-categ-collapse','User collapses the categories panel','belphegor-qa'),
	(301,'belph-app-delete','','belphegor-qa'),
	(302,'belph-app-edit','User opens the edit dialog','belphegor-qa'),
	(303,'belph-app-recateg','User moves an app to a different category','belphegor-qa'),
	(304,'belph-app-expand','User expands an app','belphegor-qa'),
	(305,'belph-app-save','User clicks \"save\" in the edit dialog','belphegor-qa'),
	(306,'belph-app-doc','User clicks the Documentation link for an app','belphegor-qa'),
	(307,'belph-app-forum','User clicks the Forum link for an app','belphegor-qa'),
	(308,'belph-error','An error window appears','belphegor-qa'),
	(309,'belph-categ-add','','belphegor-dev'),
	(310,'belph-categ-delete','','belphegor-dev'),
	(311,'belph-categ-rename','','belphegor-dev'),
	(312,'belph-categ-move','User moves a subcategory to a different parent','belphegor-dev'),
	(313,'belph-categ-expand','User expands the categories panel','belphegor-dev'),
	(314,'belph-categ-collapse','User collapses the categories panel','belphegor-dev'),
	(315,'belph-app-delete','','belphegor-dev'),
	(316,'belph-app-edit','User opens the edit dialog','belphegor-dev'),
	(317,'belph-app-recateg','User moves an app to a different category','belphegor-dev'),
	(318,'belph-app-expand','User expands an app','belphegor-dev'),
	(319,'belph-app-save','User clicks \"save\" in the edit dialog','belphegor-dev'),
	(320,'belph-app-doc','User clicks the Documentation link for an app','belphegor-dev'),
	(321,'belph-app-forum','User clicks the Forum link for an app','belphegor-dev'),
	(322,'belph-error','An error window appears','belphegor-dev'),
	(323,'belph-categ-add','','belphegor-staging'),
	(324,'belph-categ-delete','','belphegor-staging'),
	(325,'belph-categ-rename','','belphegor-staging'),
	(326,'belph-categ-move','User moves a subcategory to a different parent','belphegor-staging'),
	(327,'belph-categ-expand','User expands the categories panel','belphegor-staging'),
	(328,'belph-categ-collapse','User collapses the categories panel','belphegor-staging'),
	(329,'belph-app-delete','','belphegor-staging'),
	(330,'belph-app-edit','User opens the edit dialog','belphegor-staging'),
	(331,'belph-app-recateg','User moves an app to a different category','belphegor-staging'),
	(332,'belph-app-expand','User expands an app','belphegor-staging'),
	(333,'belph-app-save','User clicks \"save\" in the edit dialog','belphegor-staging'),
	(334,'belph-app-doc','User clicks the Documentation link for an app','belphegor-staging'),
	(335,'belph-app-forum','User clicks the Forum link for an app','belphegor-staging'),
	(336,'belph-error','An error window appears','belphegor-staging'),
	(337,'JobSearch','Search for foundation jobs','Jobs01'),
	(338,'JobsListAttribute','List specific attribute of foundation jobs','Jobs01'),
	(339,'SystemsList','List iPlant HPC systems','Systems01'),
	(340,'MonitorDaily','Show daily test results','Monitor01'),
	(341,'MonitorTest','Show individual test results','Monitor01'),
	(342,'MonitorSummary','Show summary results','Monitor01'),
	(343,'MonitorSuite','Show results from a test suite','Monitor01'),
	(344,'MonitorService','Show results from an test group','Monitor01'),
	(345,'PostItCreate','Create a new PostIt link','PostIt01'),
	(346,'PostItClaim','Claim a PostIt link','PostIt01'),
	(347,'PostItRevoke','Revoke a PostIt link','PostIt01'),
	(348,'AppsPublish','Publish an app to the public space','Apps01'),
	(349,'AppsListPermissions','List app permissions','Apps01'),
	(350,'AppsEditPermissions','Edit app permissions','Apps01'),
	(351,'SystemsUsage','Systems service usage info','Systems01'),
	(352,'AppsDelete','','APPS02'),
	(353,'AppsForm','','APPS02'),
	(354,'AppsGetByID','','APPS02'),
	(355,'AppsList','','APPS02'),
	(356,'AppsListPublic','','APPS02'),
	(357,'AppsListShared','','APPS02'),
	(358,'AppsAdd','','APPS02'),
	(359,'AppsSearchPublicByName','','APPS02'),
	(360,'AppsSearchPublicByTag','','APPS02'),
	(361,'AppsSearchPublicByTerm','','APPS02'),
	(362,'AppsSearchSharedByName','','APPS02'),
	(363,'AppsSearchSharedByTag','','APPS02'),
	(364,'AppsSearchSharedByTerm','','APPS02'),
	(365,'AppsUsage','','APPS02'),
	(366,'AppsListPermissions','','APPS02'),
	(367,'AppsPublish','','APPS02'),
	(368,'AppsSearchPublicBySystem','','APPS02'),
	(369,'AppsUpdatePermissions','','APPS02'),
	(370,'AppsRemovePermissions','','APPS02'),
	(371,'AuthCreate','','AUTH02'),
	(372,'AuthList','','AUTH02'),
	(373,'AuthRenew','','AUTH02'),
	(374,'AuthRevoke','','AUTH02'),
	(375,'DataImpliedExport','','TRANSFORMS02'),
	(376,'DataImpliedTransform','','TRANSFORMS02'),
	(377,'DataList','','TRANSFORMS02'),
	(378,'DataSearchByFile','','TRANSFORMS02'),
	(379,'DataSearchByName','','TRANSFORMS02'),
	(380,'DataSearchByTag','','TRANSFORMS02'),
	(381,'DataSpecifiedExport','','TRANSFORMS02'),
	(382,'DataSpecifiedTransform','','TRANSFORMS02'),
	(383,'DataViewCloud','','TRANSFORMS02'),
	(384,'IOPublicDownload','','FILES02'),
	(385,'IOMove','','FILES02'),
	(386,'IOMakeDir','','FILES02'),
	(387,'IOCopy','','FILES02'),
	(388,'IODelete','','FILES02'),
	(389,'IODownload','','FILES02'),
	(390,'IOExport','','FILES02'),
	(391,'IOImport','','FILES02'),
	(392,'IOList','','FILES02'),
	(393,'IORename','','FILES02'),
	(394,'IOShare','','FILES02'),
	(395,'IOUpload','','FILES02'),
	(396,'IOUsage','','FILES02'),
	(397,'FilesGetHistory','','FILES02'),
	(398,'JobsDelete','','JOBS02'),
	(399,'JobsGetByID','','JOBS02'),
	(400,'JobsGetOutput','','JOBS02'),
	(401,'JobsKill','','JOBS02'),
	(402,'JobsList','','JOBS02'),
	(403,'JobsListInputs','','JOBS02'),
	(404,'JobsResubmit','','JOBS02'),
	(405,'JobsListOutputs','','JOBS02'),
	(406,'JobsShare','','JOBS02'),
	(407,'JobsSubmit','','JOBS02'),
	(408,'JobsGetInput','','JOBS02'),
	(409,'JobsUsage','','JOBS02'),
	(410,'JobStatus','','JOBS02'),
	(411,'JobSearch','','JOBS02'),
	(412,'JobAttributeList','','JOBS02'),
	(413,'JobsGetHistory','','JOBS02'),
	(414,'JobSubmissionForm','','JOBS02'),
	(415,'InternalUsersList','','PROFILES02'),
	(416,'InternalUsersRegistration','','PROFILES02'),
	(417,'InternalUserDelete','','PROFILES02'),
	(418,'InternalUserUpdate','','PROFILES02'),
	(419,'InternalUserGet','','PROFILES02'),
	(420,'InternalUserSearchName','','PROFILES02'),
	(421,'InternalUserSearchEmail','','PROFILES02'),
	(422,'InternalUserSearchUsername','','PROFILES02'),
	(423,'InternalUserSearchStatus','','PROFILES02'),
	(424,'SystemGetCredentials','','SYSTEMS02'),
	(425,'SystemAddCredential','','SYSTEMS02'),
	(426,'SystemRemoveCredential','','SYSTEMS02'),
	(427,'SystemsListPublic','','SYSTEMS02'),
	(428,'SystemsGetByID','','SYSTEMS02'),
	(429,'SystemsAdd','','SYSTEMS02'),
	(430,'SystemsDelete','','SYSTEMS02'),
	(431,'SystemsPublish','','SYSTEMS02'),
	(432,'SystemsUnpublish','','SYSTEMS02'),
	(433,'SystemsClone','','SYSTEMS02'),
	(434,'SystemsSetDefault','','SYSTEMS02'),
	(435,'SystemsUnsetDefault','','SYSTEMS02'),
	(436,'SystemsSetGlobalDefault','','SYSTEMS02'),
	(437,'SystemsUnsetGlobalDefault','','SYSTEMS02'),
	(438,'SystemListRoles','','SYSTEMS02'),
	(439,'SystemEditRoles','','SYSTEMS02'),
	(440,'SystemRemoveRoles','','SYSTEMS02'),
	(441,'SystemsDefaultListPublic','','SYSTEMS02'),
	(442,'SystemsErase','','SYSTEMS02'),
	(443,'UsageList','','USAGE02'),
	(444,'UsageSearch','','USAGE02'),
	(445,'UsageAddTrigger','','USAGE02'),
	(446,'UsageDeleteTrigger','','USAGE02'),
	(447,'UsageUpdateTrigger','','USAGE02'),
	(448,'SchemaList','','METADATA02'),
	(449,'SchemaListRelated','','METADATA02'),
	(450,'SchemaGetById','','METADATA02'),
	(451,'SchemaSearch','','METADATA02'),
	(452,'SchemaCreate','','METADATA02'),
	(453,'SchemaDelete','','METADATA02'),
	(454,'SchemaEdit','','METADATA02'),
	(455,'SchemaPemsCreate','','METADATA02'),
	(456,'SchemaPemsAdd','','METADATA02'),
	(457,'SchemaPemsUpdate','','METADATA02'),
	(458,'SchemaPemsDelete','','METADATA02'),
	(459,'SchemaPemsList','','METADATA02'),
	(460,'NotifList','','NOTIFICATIONS02'),
	(461,'NotifAdd','','NOTIFICATIONS02'),
	(462,'NotifUpdate','','NOTIFICATIONS02'),
	(463,'NotifDelete','','NOTIFICATIONS02'),
	(464,'NotifTrigger','','NOTIFICATIONS02'),
	(465,'NotifListRelated','','NOTIFICATIONS02'),
	(466,'NotifGetById','','NOTIFICATIONS02'),
	(467,'PostItList','','POSTITS02'),
	(468,'PostItsDelete','','POSTITS02'),
	(469,'PostItRedeem','','POSTITS02'),
	(470,'PostItsAdd','','POSTITS02'),
	(471,'TransfersList','','TRANSFERS02'),
	(472,'TransfersAdd','','TRANSFERS02'),
	(473,'TransfersUpdate','','TRANSFERS02'),
	(474,'TransfersDelete','','TRANSFERS02'),
	(475,'TransfersStop','','TRANSFERS02'),
	(476,'TransfersSearch','','TRANSFERS02'),
	(477,'TransfersGetById','','TRANSFERS02'),
	(478,'TriggerListAll','','TRIGGERS02'),
	(479,'TriggerCreate','','TRIGGERS02'),
	(480,'TriggerList','','TRIGGERS02'),
	(481,'TriggerDelete','','TRIGGERS02'),
	(482,'MyProxyList','','MYPROXY02'),
	(483,'MyProxyStore','','MYPROXY02'),
	(484,'MyProxyGetByName','','MYPROXY02'),
	(485,'MyProxyDelete','','MYPROXY02'),
	(486,'InternalUserClear','','PROFILES02'),
	(487,'AuthVerify','','AUTH02'),
	(488,'SystemsListAll','','SYSTEMS02'),
	(489,'SystemsListType','','SYSTEMS02'),
	(490,'SystemsSearch','','SYSTEMS02'),
	(491,'SystemsUpdate','','SYSTEMS02'),
	(492,'MetaCreate','','METADATA02'),
	(493,'MetaList','','METADATA02'),
	(494,'MetaListRelated','','METADATA02'),
	(495,'MetaGetById','','METADATA02'),
	(496,'MetaSearch','','METADATA02'),
	(497,'MetaDelete','','METADATA02'),
	(498,'MetaEdit','','METADATA02'),
	(499,'MetaPemsCreate','','METADATA02'),
	(500,'MetaPemsAdd','','METADATA02'),
	(501,'MetaPemsUpdate','','METADATA02'),
	(502,'MetaPemsDelete','','METADATA02'),
	(503,'MetaPemsList','','METADATA02'),
	(504,'ProfileSearchUsername','','PROFILES02'),
	(505,'ProfileSearchEmail','','PROFILES02'),
	(506,'ProfileSearchName','','PROFILES02'),
	(507,'ProfileUsage','','PROFILES02'),
	(508,'ProfileUsername','','PROFILES02'),
	(510,'AppsClone','','APPS02'),
	(511,'AppsClonePrivate','','APPS02'),
	(512,'AppsClonePublic','','APPS02'),
	(513,'MonitorGetById',' ','MONITORS02'),
	(514,'MonitorsList',NULL,'MONITORS02'),
	(515,'MonitorAdd',NULL,'MONITORS02'),
	(516,'MonitorDelete',NULL,'MONITORS02'),
	(517,'MonitorUpdate',NULL,'MONITORS02'),
	(518,'MonitorChecksList',NULL,'MONITORS02'),
	(519,'MonitorCheckGetById',NULL,'MONITORS02'),
	(520,'MonitorTrigger',NULL,'MONITORS02'),
	(521,'ClientsList',NULL,'CLIENTS02'),
	(522,'ClientsDelete',NULL,'CLIENTS02'),
	(523,'ClientsUpdate',NULL,'CLIENTS02'),
	(524,'ClientsAdd',NULL,'CLIENTS02'),
	(525,'ClientsApiList',NULL,'CLIENTS02'),
	(526,'ClientsApiSubscribe',NULL,'CLIENTS02'),
	(527,'ClientsApiDelete',NULL,'CLIENTS02'),
	(528,'TenantsList',NULL,'TENANTS02'),
	(529,'JobsMonthly',NULL,'USAGE02'),
	(530,'UsersMonthly',NULL,'USAGE02'),
	(531,'DataMonthly',NULL,'USAGE02'),
	(532,'RequestsMonthly',NULL,'USAGE02'),
	(533,'IPMonthly',NULL,'USAGE02'),
	(534,'HoursMonthly',NULL,'USAGE02'),
	(535,'IOPemsUpdate',NULL,'FILES02'),
	(536,'IOPemsList',NULL,'FILES02'),
	(537,'IOPemsDelete',NULL,'FILES02'),
	(538,'ClientsGetByUsername',NULL,'CLIENTS02'),
	(539,'ClientsGetById',NULL,'CLIENTS02'),
	(540,'ProfileUpdate',NULL,'PROFILES02'),
	(541,'ProfileAdd',NULL,'PROFILES02'),
	(542,'ProfileDelete',NULL,'PROFILES02'),
	(543,'ProfileList',NULL,'PROFILES02'),
	(544,'AppsEnable',NULL,'APPS02'),
	(545,'AppsDisable',NULL,'APPS02'),
	(546,'AppsUpdate',NULL,'APPS02'),
	(547,'SystemBatchQueueListRoles','','SYSTEMS02'),
	(548,'SystemBatchQueueUpdateRoles','','SYSTEMS02'),
	(549,'SystemBatchQueueAddRoles','','SYSTEMS02'),
	(550,'SystemBatchQueueDeleteRoles','','SYSTEMS02'),
	(551,'AppsHistoryList',NULL,'APPS02'),
	(552,'SystemsErase','','SYSTEMS02'),
	(553,'SystemBatchQueueList','','SYSTEMS02'),
	(554,'SystemBatchQueueUpdate','','SYSTEMS02'),
	(555,'SystemBatchQueueDelete','','SYSTEMS02'),
	(556,'SystemEnable','','SYSTEMS02'),
	(557,'SystemDisable','','SYSTEMS02'),
	(558,'TransfersList','','TRANSFERS02'),
	(559,'TransfersAdd','','TRANSFERS02'),
	(560,'TransfersUpdate','','TRANSFERS02'),
	(561,'TransfersDelete','','TRANSFERS02'),
	(562,'TransfersStop','','TRANSFERS02'),
	(563,'TransfersSearch','','TRANSFERS02'),
	(564,'TransfersGetById','','TRANSFERS02'),
	(565,'TransfersHistoryGet','','TRANSFERS02'),
	(566,'TransfersHistoryList','','TRANSFERS02'),
	(567,'AppsErase',NULL,'APPS02'),
	(568,'AppsIdLookup',NULL,'APPS02');

/*!40000 ALTER TABLE `UsageActivities` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table UsageDeveloper
# ------------------------------------------------------------

DROP TABLE IF EXISTS `UsageDeveloper`;

CREATE TABLE `UsageDeveloper` (
  `Username` varchar(64) NOT NULL DEFAULT '',
  `ServiceKey` varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (`Username`,`ServiceKey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `UsageDeveloper` WRITE;
/*!40000 ALTER TABLE `UsageDeveloper` DISABLE KEYS */;

INSERT INTO `UsageDeveloper` (`Username`, `ServiceKey`)
VALUES
	('dennis','belphegor-dev'),
	('dennis','belphegor-prod'),
	('dennis','belphegor-qa'),
	('dennis','belphegor-staging'),
	('dennis','de-dev'),
	('dennis','de-prod'),
	('dennis','de-qa'),
	('dennis','de-staging'),
	('dennis','tito-de'),
	('dennis','tito-prod'),
	('dennis','tito-qa'),
	('dennis','tito-staging'),
	('hariolf','belphegor-dev'),
	('hariolf','belphegor-prod'),
	('hariolf','belphegor-qa'),
	('hariolf','belphegor-staging'),
	('hariolf','de-dev'),
	('hariolf','de-prod'),
	('hariolf','de-qa'),
	('hariolf','de-staging'),
	('hariolf','tito-de'),
	('hariolf','tito-prod'),
	('hariolf','tito-qa'),
	('hariolf','tito-staging'),
	('ipctest','Atm02'),
	('jerry','belphegor-dev'),
	('jerry','belphegor-prod'),
	('jerry','belphegor-qa'),
	('jerry','belphegor-staging'),
	('jerry','de-dev'),
	('jerry','de-prod'),
	('jerry','de-qa'),
	('jerry','de-staging'),
	('jerry','tito-de'),
	('jerry','tito-prod'),
	('jerry','tito-qa'),
	('jerry','tito-staging'),
	('lenards','belphegor-dev'),
	('lenards','belphegor-prod'),
	('lenards','belphegor-qa'),
	('lenards','belphegor-staging'),
	('lenards','de-dev'),
	('lenards','de-prod'),
	('lenards','de-qa'),
	('lenards','de-staging'),
	('lenards','tito-de'),
	('lenards','tito-prod'),
	('lenards','tito-qa'),
	('lenards','tito-staging'),
	('psarando','belphegor-dev'),
	('psarando','belphegor-prod'),
	('psarando','belphegor-qa'),
	('psarando','belphegor-staging'),
	('psarando','de-dev'),
	('psarando','de-prod'),
	('psarando','de-qa'),
	('psarando','de-staging'),
	('psarando','tito-de'),
	('psarando','tito-prod'),
	('psarando','tito-qa'),
	('psarando','tito-staging'),
	('sriram','belphegor-dev'),
	('sriram','belphegor-prod'),
	('sriram','belphegor-qa'),
	('sriram','belphegor-staging'),
	('sriram','de-dev'),
	('sriram','de-prod'),
	('sriram','de-qa'),
	('sriram','de-staging'),
	('sriram','tito-de'),
	('sriram','tito-prod'),
	('sriram','tito-qa'),
	('sriram','tito-staging');

/*!40000 ALTER TABLE `UsageDeveloper` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table UsageServices
# ------------------------------------------------------------

DROP TABLE IF EXISTS `UsageServices`;

CREATE TABLE `UsageServices` (
  `ID` int(200) NOT NULL AUTO_INCREMENT,
  `ServiceKey` varchar(30) NOT NULL DEFAULT '',
  `Description` text,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `UsageServices` WRITE;
/*!40000 ALTER TABLE `UsageServices` DISABLE KEYS */;

INSERT INTO `UsageServices` (`ID`, `ServiceKey`, `Description`)
VALUES
	(1,'Apps01','Foundation Apps Service v1'),
	(2,'Atm01','iPlant Cloud Environment'),
	(3,'Atm02','atmosphere'),
	(4,'Auth01','Foundation Auth Service v1'),
	(5,'belphegor','DE Admin GUI  - Production'),
	(6,'belphegor-dev','DE Admin GUI  - Dev'),
	(7,'belphegor-qa','DE Admin GUI  - QA'),
	(8,'belphegor-staging','DE Admin GUI - Staging'),
	(9,'Data01','Foundation Data Service v1'),
	(10,'de-dev','Discovery Environment - Dev'),
	(11,'de-prod','Discovery Environment Production'),
	(12,'de-qa','Discovery Environment - QA'),
	(13,'de-staging','Discovery Environment Production'),
	(14,'DE002','Bioinformatic Workflows'),
	(15,'IO01','Foundation IO Service v1'),
	(16,'Jobs01','Foundation Job service v1'),
	(17,'Postit01','foundation api postit service'),
	(18,'Profile01','Foundation Profile Service v1'),
	(19,'tito-de','Tools Integration - Discovery Environment'),
	(20,'tito-prod','Tools Integration - Production'),
	(21,'tito-qa','Tools Integration - QA'),
	(22,'tito-staging','Tools Integration - Staging'),
	(23,'Systems01','Foundation Systems Service v1'),
	(24,'Monitor01','Foundation monitoring service v1'),
	(25,'PostIt01','Foundation PostIt service v1'),
	(26,'APPS02','Apps Service V2'),
	(27,'AUTH02','Auth Service V2'),
	(28,'TRANSFORMS02','Transforms Service V2'),
	(29,'FILES02','Files Service V2'),
	(30,'JOBS02','Jobs Service V2'),
	(31,'PROFILES02','Profiles Service V2'),
	(32,'SYSTEMS02','Systems Service V2'),
	(33,'USAGE02','Usage Service V2'),
	(34,'METADATA02','Metadata Service V2'),
	(35,'NOTIFICATIONS02','Notifications Service V2'),
	(36,'POSTITS02','Postits Service V2'),
	(37,'TRANSFERS02','Transfers Service V2'),
	(38,'TRIGGERS02','Triggers Service V2'),
	(39,'MYPROXY02','Myproxy Service V2'),
	(40,'MONITORS02','Monitoring Service V2'),
	(41,'CLIENTS02','Client registration service v2'),
	(42,'TENANTS02','Tenants listing and admin service.'),
	(43,'USAGE02','Usage and reporting service');

/*!40000 ALTER TABLE `UsageServices` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table userdefaultsystems
# ------------------------------------------------------------

DROP TABLE IF EXISTS `userdefaultsystems`;

CREATE TABLE `userdefaultsystems` (
  `system_id` bigint(20) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  KEY `FKC1EA8F4EBBBF083F` (`system_id`),
  CONSTRAINT `FKC1EA8F4EBBBF083F` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
