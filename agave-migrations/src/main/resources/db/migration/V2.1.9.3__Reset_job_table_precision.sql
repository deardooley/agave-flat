###############################################################
# Migration: V2.1.9.3__Reset_job_table_precision.sql
#
# Resetting precision on job table.
# 									   
# Database changes:
#
# Table changes:
# - jobs
#
# Index changes:
#
# Column changes:
# ~ jobs.charge DOUBLE
# 
# Data changes:
#
#################################################################

# Convert charge column in jobs table to a DOUBLE precision field

# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'charge' AND table_schema = DATABASE() ) > 0,
	"ALTER TABLE `jobs` MODIFY COLUMN `charge` DOUBLE;",
	"SELECT 1"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'processor_count' AND table_schema = DATABASE() ) > 0,
	"ALTER TABLE `jobs` MODIFY COLUMN `processor_count` INT(11);",
	"SELECT 1"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'node_count' AND table_schema = DATABASE() ) > 0,
	"ALTER TABLE `jobs` MODIFY COLUMN `node_count` INT(11);",
	"SELECT 1"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


