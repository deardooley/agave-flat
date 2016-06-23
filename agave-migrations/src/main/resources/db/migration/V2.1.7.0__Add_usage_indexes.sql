# ************************************************************
# Migration: V2.1.7.0__Add_usage_indexes.sql
#
# Database changes:
#
# Table changes:
#
# Index changes:
# + Usage.Username
# + Usage.CreatedAt
# + fileevents.uuid
# + softwareevents.uuid
# + transfertasks.created
# 
# Column changes:
# + fileevents.uuid
# + fileevents.tenant_id
# + fileevents.created_by
# ~ jobevents.uuid 
#
# ************************************************************

# Blow away this unused table 
# ------------------------------------------------------------

DROP TABLE IF EXISTS `transfer_events`;

/* Add indexes to the Usage table for better query performance */
set @exist := (select count(*) from information_schema.statistics where table_name = 'Usage' and index_name = 'Username' and table_schema = database());
set @sqlstmt := if( @exist > 0, 'SELECT 1', 'ALTER TABLE `Usage` ADD INDEX (`Username`)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

set @exist := (select count(*) from information_schema.statistics where table_name = 'Usage' and index_name = 'CreatedAt' and table_schema = database());
set @sqlstmt := if( @exist > 0, 'SELECT 1', 'ALTER TABLE `Usage` ADD INDEX (`CreatedAt`)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


# Add a uuid column to the fileevents table. 
# We will programmatically backfill the table with valid
# uuid valies and add a unique index.
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'uuid' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` ADD COLUMN `uuid`  VARCHAR(64)  NOT NULL;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;
  
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'tenant_id' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` ADD COLUMN `tenant_id`  VARCHAR(64)  NOT NULL;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;  

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'created_by' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` ADD COLUMN `created_by`  VARCHAR(32)  NOT NULL;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

set @exist := (select count(*) from information_schema.statistics where table_name = 'fileevents' and index_name = 'uuid' and table_schema = database());
set @sqlstmt := if( @exist > 0, 'SELECT 1', 'ALTER TABLE `fileevents` ADD INDEX (`uuid`)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

# Add a unique indexed uuid column to the 
# softwareevents table
# ------------------------------------------------------------

set @exist := (select count(*) from information_schema.statistics where table_name = 'softwareevents' and index_name = 'uuid' and table_schema = database());
set @sqlstmt := if( @exist > 0, 'SELECT 1', 'ALTER TABLE `softwareevents` ADD INDEX (`uuid`)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

# Reduce size of uuid column in jobevents table 
# to be consistent with uuid across the api
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobevents' AND column_name = 'uuid' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `jobevents` MODIFY COLUMN `uuid` VARCHAR(64);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Add new indexes on source, dest, and owner + tenant_id to the table. 
# These are the most queried upon fields 
# ------------------------------------------------------------

set @exist := (select count(*) from information_schema.statistics where table_name = 'transfertasks' and index_name = 'created' and table_schema = database());
set @sqlstmt := if( @exist > 0, 'SELECT 1', 'ALTER TABLE `transfertasks` ADD INDEX (`created`)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    
--
--/* Blow away this unused table */
--DROP TABLE IF EXISTS `transfer_events`;
--
--/* Add indexes to the Usage table for better query performance */
--ALTER TABLE `Usage` ADD INDEX (`Username`), ADD INDEX (`CreatedAt`);
--
--/* Add a uuid column to the fileevents table. 
-- * We will programmatically backfill the table with valid
-- * uuid valies and add a unique index. */ 
--ALTER table `fileevents` ADD COLUMN `uuid` VARCHAR(64), 
--						 ADD COLUMN `tenant_id` VARCHAR(64), 
--						 ADD COLUMN `created_by` VARCHAR(64),
--						 ADD INDEX (`uuid`);
--
--/* Add a unique indexed uuid column to the 
-- * softwareevents table */
--ALTER table `softwareevents` ADD INDEX (`uuid`);
--
--/* Reduce size of uuid column in jobevents table 
-- * to be consistent with uuid across the api */
--ALTER table `jobevents` MODIFY COLUMN `uuid` VARCHAR(64);
--
--/* Add new indexes on source, dest, and owner + tenant_id to the table. 
-- * These are the most queried upon fields 
-- */
--ALTER table `transfertasks` ADD INDEX (`created`);
