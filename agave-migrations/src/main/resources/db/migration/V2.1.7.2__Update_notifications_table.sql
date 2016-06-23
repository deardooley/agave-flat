###############################################################
# Migration: V2.1.7.2__Update_notifications_table.sql
#
# Update notifications table with support for retry policy 
# field. 
# 									   
# Database changes:
#
# Table changes:
#
# Index changes:
#
# Column changes:
# - is_terminated
# - is_success
# - response_code
# - attempts
# + retry_strategy
# + retry_delay
# + retry_interval
# + retry_limit
# + save_on_failure
#
#################################################################

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'status' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "ALTER TABLE `notifications` ADD `status` VARCHAR(12)  NOT NULL  DEFAULT 'ACTIVE';" )); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_terminated' AND table_schema = DATABASE() ) < 1, "SELECT 1", 
    "UPDATE `notifications` set `status` = 'ACTIVE' where `is_terminated` = 0;")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_terminated' AND table_schema = DATABASE() ) < 1, "SELECT 1", 
    "UPDATE `notifications` set `status` = 'COMPLETE' where `is_terminated` = 1 and (`response_code` > 199 or `response_code` < 300);")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_terminated' AND table_schema = DATABASE() ) < 1, "SELECT 1", 
    "UPDATE `notifications` set `status` = 'FAILED' where `is_terminated` = 1 and (`response_code` < 200 or `response_code` > 299);")); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'attempts' 
    AND table_schema = DATABASE() ) < 1, "SELECT 1", "ALTER TABLE `notifications` DROP `attempts`"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'last_sent' 
    AND table_schema = DATABASE() ) < 1, "SELECT 1", "ALTER TABLE `notifications` DROP `last_sent`"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'response_code' 
    AND table_schema = DATABASE() ) < 1, "SELECT 1", "ALTER TABLE `notifications` DROP `response_code`"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;
    
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_success' 
    AND table_schema = DATABASE() ) < 1, "SELECT 1", "ALTER TABLE `notifications` DROP `is_success`"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_terminated' 
    AND table_schema = DATABASE() ) < 1, "SELECT 1", "ALTER TABLE `notifications` DROP `is_terminated`"));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


/* add the new fields for retry policy */
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'retry_strategy' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `retry_strategy`  VARCHAR(12)  NOT NULL  DEFAULT 'IMMEDIATE' AFTER `status`;"
	)); 
  PREPARE stmt FROM @s; 
  EXECUTE stmt; 
  DEALLOCATE PREPARE stmt;
	
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'retry_limit' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `retry_limit` 	 INT(12)  	  NOT NULL  DEFAULT '5' 		AFTER `retry_strategy`"
	));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'retry_rate' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `retry_rate` 	 INT(12)  	  NOT NULL  DEFAULT '5' 		AFTER `retry_limit`"
	));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'retry_delay' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `retry_delay` 	 INT(12)  	  NOT NULL  DEFAULT '0' 		AFTER `retry_rate`"
	));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'save_on_failure' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `save_on_failure` TINYINT(1)   NOT NULL  DEFAULT '0' 		AFTER `retry_delay`;"
	));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'notifications' AND column_name = 'is_visible' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `notifications` ADD COLUMN `is_visible` TINYINT(1)   NOT NULL  DEFAULT '1' 		AFTER `save_on_failure`;"
	));
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    

# add the status column 
# ------------------------------------------------------------
--
--ALTER TABLE table_name ADD col_name VARCHAR(12)  NOT NULL  DEFAULT 'ACTIVE';

# Update the old values for finished and success to the new status field 
# ------------------------------------------------------------
--
--UPDATE `notifications` set `status` = 'ACTIVE' where `is_terminated` = 0;
--UPDATE `notifications` set `status` = 'COMPLETE' where `is_terminated` = 1 and (`response_code` > 199 or `response_code` < 300);
--UPDATE `notifications` set `status` = 'FAILED' where `is_terminated` = 1 and (`response_code` < 200 or `response_code` > 299);

# drop the deprecated columns 
# ------------------------------------------------------------
--
--   ALTER TABLE `notifications` DROP `attempts`,
--						    DROP `last_sent`,
--						    DROP `response_code`,
--						    DROP `is_success`,
--						    DROP `is_terminated`;

# add the new fields for retry policy
# ------------------------------------------------------------
--
--ALTER TABLE `notifications` ADD COLUMN `retry_strategy`  VARCHAR(12)  NOT NULL  DEFAULT 'IMMEDIATE' AFTER `status`,
--					   		ADD COLUMN `retry_limit` 	 INT(12)  	  NOT NULL  DEFAULT '5' 		AFTER `retry_strategy`,
--					   		ADD COLUMN `retry_rate` 	 INT(12)  	  NOT NULL  DEFAULT '5' 		AFTER `retry_limit`,
--					   		ADD COLUMN `retry_delay` 	 INT(12)  	  NOT NULL  DEFAULT '0' 		AFTER `retry_rate`,
--					   		ADD COLUMN `save_on_failure` TINYINT(1)   NOT NULL  DEFAULT '0' 		AFTER `retry_delay`;
					   