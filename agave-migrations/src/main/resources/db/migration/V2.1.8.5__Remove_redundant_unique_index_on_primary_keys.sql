###############################################################
# Migration: V2.1.8.5__Remove_redundant_unique_index_on_primary_keys.sql
#
# Update notifications table with support for retry policy
# field.
#
# Database changes:
#
# Table changes:
#
# Index changes:
# - Dropping redundant unique index `authconfigs`.`id`
# - Dropping redundant unique index `authentication_tokens`.`id`
# - Dropping redundant unique index `batchqueues`.`id`
# - Dropping redundant unique index `credentialservers`.`id`
# - Dropping redundant unique index `decoding_tasks`.`id`
# - Dropping redundant unique index `discoverableservices`.`id`
# - Dropping redundant unique index `discoverableservices_capabilities`.`id`
# - Dropping redundant unique index `encoding_tasks`.`id`
# - Dropping redundant unique index `executionsystems`.`id`
# - Dropping redundant unique index `fileevents`.`id`
# - Dropping redundant unique index `internalusers`.`id`
# - Dropping redundant unique index `job_permissions`.`id`
# - Dropping redundant unique index `jobevents`.`id`
# - Dropping redundant unique index `jobs`.`id`
# - Dropping redundant unique index `logical_files`.`id`
# - Dropping redundant unique index `logicalfilenotifications`.`id`
# - Dropping redundant unique index `loginconfigs`.`id`
# - Dropping redundant unique index `metadata_permissions`.`id`
# - Dropping redundant unique index `metadata_schema_permissions`.`id`
# - Dropping redundant unique index `monitor_checks`.`id`
# - Dropping redundant unique index `monitors`.`id`
# - Dropping redundant unique index `notifications`.`id`
# - Dropping redundant unique index `postits`.`id`
# - Dropping redundant unique index `proxyservers`.`id`
# - Dropping redundant unique index `remoteconfigs`.`id`
# - Dropping redundant unique index `remotefilepermissions`.`id`
# - Dropping redundant unique index `servicecapabilities`.`id`
# - Dropping redundant unique index `software_inputs`.`id`
# - Dropping redundant unique index `software_outputs`.`id`
# - Dropping redundant unique index `software_parameters`.`id`
# - Dropping redundant unique index `software_permissions`.`id`
# - Dropping redundant unique index `softwareevents`.`id`
# - Dropping redundant unique index `softwareparameterenums`.`id`
# - Dropping redundant unique index `softwares`.`id`
# - Dropping redundant unique index `softwares_inputs`.`id`
# - Dropping redundant unique index `softwares_outputs`.`id`
# - Dropping redundant unique index `softwares_parameters`.`id`
# - Dropping redundant unique index `staging_tasks`.`id`
# - Dropping redundant unique index `storageconfigs`.`id`
# - Dropping redundant unique index `storagesystems`.`id`
# - Dropping redundant unique index `systempermissions`.`id`
# - Dropping redundant unique index `systemroles`.`id`
# - Dropping redundant unique index `systems`.`id`
# - Dropping redundant unique index `systems_systemroles`.`id`
# - Dropping redundant unique index `tags`.`id`
# - Dropping redundant unique index `tag_events`.`id`
# - Dropping redundant unique index `tagpermissions`.`id`
# - Dropping redundant unique index `tenants`.`id`
# - Dropping redundant unique index `transfer_events`.`id`
# - Dropping redundant unique index `transferevents`.`id`
# - Dropping redundant unique index `transfertaskpermissions`.`id`
# - Dropping redundant unique index `transfertasks`.`id`
# - Dropping redundant unique index `transfertasks_myisam`.`id`
# - Dropping redundant unique index `Usage`.`id`
# - Dropping redundant unique index `UsageActivities`.`id`
# - Dropping redundant unique index `UsageDeveloper`.`id`
# - Dropping redundant unique index `UsageServices`.`id`
# - Dropping redundant unique index `userdefaultsystems`.`id`
#
# Column changes:
#
#################################################################

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'authconfigs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `authconfigs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'authentication_tokens' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `authentication_tokens`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'batchqueues' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `batchqueues`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'credentialservers' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `credentialservers`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'decoding_tasks' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `decoding_tasks`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'discoverableservices' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `discoverableservices`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'discoverableservices_capabilities' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `discoverableservices_capabilities`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'encoding_tasks' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `encoding_tasks`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'executionsystems' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `executionsystems`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'fileevents' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `fileevents`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'internalusers' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `internalusers`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'job_permissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `job_permissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'jobevents' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `jobevents`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'jobs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `jobs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'logical_files' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `logical_files`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'logicalfilenotifications' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `logicalfilenotifications`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'loginconfigs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `loginconfigs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'metadata_permissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `metadata_permissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'metadata_schema_permissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `metadata_schema_permissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'monitor_checks' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `monitor_checks`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'monitors' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `monitors`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'notifications' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `notifications`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'postits' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `postits`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'proxyservers' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `proxyservers`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'remoteconfigs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `remoteconfigs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'remotefilepermissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `remotefilepermissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'servicecapabilities' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `servicecapabilities`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'software_inputs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `software_inputs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'software_outputs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `software_outputs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'software_parameters' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `software_parameters`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'software_permissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `software_permissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwareevents' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwareevents`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwareparameterenums' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwareparameterenums`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwares' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwares`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwares_inputs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwares_inputs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwares_outputs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwares_outputs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'softwares_parameters' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `softwares_parameters`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'staging_tasks' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `staging_tasks`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'storageconfigs' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `storageconfigs`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'storagesystems' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `storagesystems`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systempermissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `systempermissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systemroles' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `systemroles`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systems' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `systems`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systems_systemroles' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `systems_systemroles`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'tags' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `tags`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'tags_event' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `tags_event`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'tagpermissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `tagpermissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'tenants' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `tenants`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'transfer_events' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `transfer_events`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'transferevents' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `transferevents`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'transfertaskpermissions' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `transfertaskpermissions`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'transfertasks' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `transfertasks`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'transfertasks_myisam' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `transfertasks_myisam`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'Usage' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `Usage`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'UsageActivities' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `UsageActivities`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'UsageDeveloper' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `UsageDeveloper`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'UsageServices' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `UsageServices`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'userdefaultsystems' AND index_name = 'id' AND table_schema = DATABASE() ) = '0', "SELECT 1",
    "DROP INDEX `id` ON `userdefaultsystems`" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
