###############################################################
# Migration: V2.1.7.4__Add_owner_column_to_data_task_tables.sql
#
# Adding owner column to each of the data processing tasks
# 									   
# Database changes:
#
# Table changes:
#
# Index changes:
# 
# Column changes:
# + staging_tasks.owner
# + encoding_tasks.owner
# + decoding_tasks.owner
#
# Data changes:
#
#################################################################

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'decoding_tasks' AND column_name = 'owner' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `decoding_tasks` ADD `owner` VARCHAR(32)  NOT NULL;" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'encoding_tasks' AND column_name = 'owner' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `encoding_tasks` ADD `owner` VARCHAR(32)  NOT NULL;" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'staging_tasks' AND column_name = 'owner' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `staging_tasks` ADD `owner` VARCHAR(32)  NOT NULL;" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    
--ALTER TABLE `staging_tasks`  ADD COLUMN `owner` VARCHAR(32)  NOT NULL;
--ALTER TABLE `encoding_tasks` ADD COLUMN `owner` VARCHAR(32)  NOT NULL;
--ALTER TABLE `decoding_tasks` ADD COLUMN `owner` VARCHAR(32)  NOT NULL;