###############################################################
# Migration: V2.1.9.5__Flatten_system_roles_delete_join_table.sql
#
# Dropping the join table between systems and systemroles. Adding
# bidirectional system_role_id to the systemroles table so 
# orphans can be cleaned up.
# 									   
# Database changes:
# - Drop table systems_systemroles
#
# Table changes:
# - systems
# - systemroles
# 
# Index changes:
#
# Column changes:
# + systemroles.system_role_id INT(11) NOT NULL NULL
# 
# Data changes:
#
#################################################################

/** Add column for remote system id bidirectional relationship to the systmeroles table. **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systemroles' AND column_name = 'remote_system_id' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `systemroles` ADD `remote_system_id` BIGINT(20)  NULL  DEFAULT NULL  AFTER `username`;" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/** Insert system id into existing roles mapped through the join table...if it exists. **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systems_systemroles' AND table_schema = DATABASE() ) > 0,
	"UPDATE `systemroles` SET `systemroles`.`remote_system_id`=( select `systems_systemroles`.`systems` FROM `systems_systemroles` where `systemroles`.`id` = `systems_systemroles`.`roles`)",
	 "SELECT 1"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/** Remove orphan rows from the systemroles table. These are roles that **/
/** did not have a mapping in the join table. Hibernate removed the relation **/
/** but did not clean up after itself **/
/** Add column for remote system id bidirectional relationship to the systmeroles table. **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systemroles' AND column_name = 'remote_system_id' AND table_schema = DATABASE() ) > 0, 
    "DELETE from `systemroles` WHERE `remote_system_id` is NULL;",
    "SELECT 1")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/** Now that the table is populated and orphans removed, add a null constraint to the column **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systemroles' AND column_name = 'remote_system_id' AND table_schema = DATABASE() ) > 0, 
    "ALTER TABLE `systemroles` CHANGE `remote_system_id` `remote_system_id` BIGINT(20) NOT NULL;",
    "SELECT 1")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/** Drop the old systems_systemroles join table as long as we have the  **/
/** systemroles.remote_system_id present **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systemroles' AND column_name = 'remote_system_id' AND table_schema = DATABASE() ) > 0, 
    "DROP TABLE IF EXISTS `systems_systemroles`;",
    "SELECT 1")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;



