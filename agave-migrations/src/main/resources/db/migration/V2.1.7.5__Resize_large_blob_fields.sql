###############################################################
# Migration: V2.1.7.5__Resize_large_blob_fields.sql
#
# Resizing large blobs to fit in mysql norms
# 									   
# Database changes:
#
# Table changes:
#
# Index changes:
# 
# Column changes:
# ~ executionsystems.environment 1024
# ~ batchqueues.custom_directives 4096
# ~ authconfigs.credential TEXT 8192
# ~ authconfigs.public_key TEXT 8192
# ~ authconfigs.private_key TEXT 16384
# ~ fileevents.description 1024
# ~ softwareevents.description 1024
# ~ jobevents.description 4096
# ~ transferevents.description 4096
# ~ fileevents.description 4096
# ~ postits.target_url 1024
# ~ fileevents.description 4096
# ~ software_inputs.description 1024
# ~ software_outputs.description 1024
# ~ software_parameters.description 1024
# ~ softwares.long_description 4096
# ~ systems.description 4096
#
# Data changes:
#
#################################################################

# Reduce size of environment column in executionsystems table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'error_message' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `jobs` MODIFY COLUMN `error_message` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

# Convert inputs column in jobs table to a TEXT field
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'inputs' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `jobs` MODIFY COLUMN `inputs` TEXT;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;

# Convert parameters column in jobs table to a TEXT field
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobs' AND column_name = 'parameters' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `jobs` MODIFY COLUMN `parameters` TEXT;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of environment column in executionsystems table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'executionsystems' AND column_name = 'environment' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `executionsystems` MODIFY COLUMN `environment` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of custom_directives column in batchqueues table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'batchqueues' AND column_name = 'custom_directives' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `batchqueues` MODIFY COLUMN `custom_directives` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of credential column in authconfigs table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'authconfigs' AND column_name = 'credential' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `authconfigs` MODIFY COLUMN `credential` TEXT;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of public_key column in authconfigs table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'authconfigs' AND column_name = 'public_key' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `authconfigs` MODIFY COLUMN `public_key` TEXT;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of private_key column in authconfigs table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'authconfigs' AND column_name = 'private_key' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `authconfigs` MODIFY COLUMN `private_key` TEXT;"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in fileevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in softwareevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'softwareevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `softwareevents` MODIFY COLUMN `description` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in jobevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'jobevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `jobevents` MODIFY COLUMN `description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in transferevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'transferevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `transferevents` MODIFY COLUMN `description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in fileevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of target_url column in postits table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'postits' AND column_name = 'target_url' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `postits` MODIFY COLUMN `target_url` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in fileevents table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'fileevents' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in software_inputs table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'software_inputs' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `software_inputs` MODIFY COLUMN `description` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in software_outputs table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'software_outputs' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `software_outputs` MODIFY COLUMN `description` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in software_parameters table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'software_parameters' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `software_parameters` MODIFY COLUMN `description` VARCHAR(1024);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of long_description column in softwares table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'softwares' AND column_name = 'long_description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `softwares` MODIFY COLUMN `long_description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


# Reduce size of description column in systems table 
# to cut down on wasted space in db
# ------------------------------------------------------------

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE table_name = 'systems' AND column_name = 'description' AND table_schema = DATABASE() ) > 0, "SELECT 1",
	"ALTER TABLE `systems` MODIFY COLUMN `description` VARCHAR(4096);"
	)); 
PREPARE stmt FROM @s; 
EXECUTE stmt; 
DEALLOCATE PREPARE stmt;


/********************************************************************/
/**                                                                **/
/** For refernce, here is the unchecked SQL we executed 		   **/
/**                                                                **/
/********************************************************************/    
--ALTER TABLE `executionsystems` MODIFY COLUMN `environment` VARCHAR(1024);
--ALTER TABLE `batchqueues` MODIFY COLUMN `custom_directives` VARCHAR(4096);
--ALTER TABLE `authconfigs` MODIFY COLUMN `credential` VARCHAR(4096);
--ALTER TABLE `authconfigs` MODIFY COLUMN `public_key` VARCHAR(4096);
--ALTER TABLE `authconfigs` MODIFY COLUMN `private_key` VARCHAR(8192);
--ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(1024);
--ALTER TABLE `softwareevents` MODIFY COLUMN `description` VARCHAR(1024);
--ALTER TABLE `jobevents` MODIFY COLUMN `description` VARCHAR(4096);
--ALTER TABLE `transferevents` MODIFY COLUMN `description` VARCHAR(4096);
--ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(4096);
--ALTER TABLE `postits` MODIFY COLUMN `target_url` VARCHAR(1024);
--ALTER TABLE `fileevents` MODIFY COLUMN `description` VARCHAR(4096);
--ALTER TABLE `software_inputs` MODIFY COLUMN `description` VARCHAR(1024);
--ALTER TABLE `software_outputs` MODIFY COLUMN `description` VARCHAR(1024);
--ALTER TABLE `software_parameters` MODIFY COLUMN `description` VARCHAR(1024);
--ALTER TABLE `softwares` MODIFY COLUMN `long_description` VARCHAR(4096);
--ALTER TABLE `systems` MODIFY COLUMN `description` VARCHAR(4096);
