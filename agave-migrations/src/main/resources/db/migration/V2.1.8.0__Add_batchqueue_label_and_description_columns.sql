###############################################################
# Migration: V2.1.8.0__Add_batchqueue_label_and_description_columns.sql
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
# + batchqueues.mappedName
# + batchqueues.description
#
# Data changes:
#
#################################################################

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'batchqueues' AND column_name = 'mapped_name' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `batchqueues` ADD `mapped_name` VARCHAR(128);" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'batchqueues' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `batchqueues` ADD `description` VARCHAR(512)  NOT NULL;" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    
--ALTER TABLE `batchqueues`  ADD COLUMN `label` VARCHAR(128);
--ALTER TABLE `batchqueues` ADD COLUMN `description` VARCHAR(512);