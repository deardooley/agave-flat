###############################################################
# Migration: V2.1.9.1__Add_job_queues_table.sql
#
# Adding new job_queues table to track queues used by job schedulers.
# A new index is also added to the tenants table on its tenants_id
# field to allow that field to be a foriegn key target.
#                                      
# Database changes:
#
# Table changes:
# + job_queues
#
# Index changes:
#
# Column changes:
# 
# Data changes:
#
#################################################################

# Create a unique index in the tenants.tenant_id field
# so that field can be referenced as a foreign key. 
CREATE UNIQUE INDEX `tenants_tenant_id`
ON `tenants` (`tenant_id`);

# All queue names begin with phase.tenant_id and must be unique
# The phases are listed in alphabetic order so that enum sorting
# matches alphabetic sorting.
CREATE TABLE IF NOT EXISTS `job_queues` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL,
  `name` varchar(150) NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `phase` ENUM('ARCHIVING','MONITORING','STAGING','SUBMITTING') NOT NULL,
  `priority` int NOT NULL,
  `num_workers` int UNSIGNED NOT NULL,
  `max_messages` int UNSIGNED NOT NULL,
  `filter` varchar(2048) NOT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX (`name`),
  UNIQUE INDEX (`uuid`),
  UNIQUE INDEX (`tenant_id`,`phase`,`priority` DESC)
#  FOREIGN KEY `fk_tenants_tenant_id` (`tenant_id`)
#    REFERENCES tenants(`tenant_id`)
#    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;