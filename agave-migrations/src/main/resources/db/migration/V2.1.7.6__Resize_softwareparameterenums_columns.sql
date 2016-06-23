###############################################################
# Migration: V2.1.7.6__Resize_softwareparameterenums_columns.sql
#
# Resize softwareparameterenums columns to save unused space
# 									   
# Database changes:
#
# Table changes:
#
# Index changes:
# 
# Column changes:
# ~ softwareparameterenums.label 255
# ~ softwareparameterenums.value 128
#
# Data changes:
#
#################################################################

# Reduce size of label column in softwareparameterenums table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'softwareparameterenums' AND column_name = 'label' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `softwareparameterenums` MODIFY COLUMN `label` VARCHAR(255);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of value column in softwareparameterenums table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'softwareparameterenums' AND column_name = 'value' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `softwareparameterenums` MODIFY COLUMN `value` VARCHAR(128);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;
