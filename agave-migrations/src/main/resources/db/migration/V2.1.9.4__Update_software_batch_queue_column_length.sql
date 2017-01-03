###############################################################
# Migration: V2.1.9.4__Update_software_batch_queue_column_length.sql
#
# Updating softwares.batch_queues column to the same length as the 
# batchqueues.name column length.
# 									   
# Database changes:
#
# Table changes:
# - softwares
#
# Index changes:
#
# Column changes:
# ~ softwares.batch_queue DOUBLE
# 
# Data changes:
#
#################################################################

# Convert charge default_queue in jobs table from VARCHAR(12) to VARCHAR(128)

# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'softwares' AND column_name = 'default_queue' AND table_schema = DATABASE() ) > 0,
    "ALTER TABLE `softwares` CHANGE `default_queue` `default_queue` VARCHAR(128)  NULL  DEFAULT NULL;",
    "SELECT 1"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;
