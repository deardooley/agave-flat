###############################################################
# Migration: V2.1.8.1__Update_batchqueue_nullable_description.sql
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
# + batchqueues.description
#
# Data changes:
#
#################################################################

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'batchqueues' AND column_name = 'description' AND table_schema = DATABASE() ) > 0,
    "ALTER TABLE `batchqueues` MODIFY COLUMN `description` VARCHAR(512) NULL DEFAULT NULL;", "SELECT 1" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    
--ALTER TABLE `batchqueues` MODIFY COLUMN `description` VARCHAR(512) NULL DEFAULT NULL;