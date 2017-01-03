###############################################################
# Migration: V2.1.9.5__Add_job_queues_table.sql
#
# Adding new job_queues table to track queues used by job schedulers.
# A new index is also added to the tenants table on its tenants_id
# field to allow that field to be a foriegn key target.
#
# Also added job_interrupts, job_published and job_leases tables.
#                                      
# Database changes:
#
# Table changes:
# + job_queues
# + job_interrupts
# + job_published
# + job_leases
#
# Index changes:
# + tenants_tenant_id
# + jobs_status
#
# Column changes:
# 
# Data changes:
# + job_leases records
#
#################################################################

# Create a unique index in the tenants.tenant_id field
# so that field can be referenced as a foreign key. 
CREATE UNIQUE INDEX `tenants_tenant_id`
ON `tenants` (`tenant_id`);

# Create a non-unique index on the status field of a job
# to avoid full table scans on scheduler queuries.
CREATE INDEX `jobs_status` ON `jobs` (`status`);

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
  UNIQUE INDEX (`tenant_id`,`phase`,`priority` DESC),
  FOREIGN KEY `fk_tenants_tenant_id` (`tenant_id`)
    REFERENCES tenants(`tenant_id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

# Jobs are identified by their uuids and can have multiple
# outstanding interrupts at a time.  Worker threads remove
# interrupts when they are finish processing them or, as 
# a failsafe, a reaper thread periodically removes expired
# interrupts.
CREATE TABLE IF NOT EXISTS `job_interrupts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_uuid` varchar(64) NOT NULL,
  `tenant_id` varchar(64) NOT NULL,
  `interrupt_type` enum('DELETE','PAUSE','STOP') NOT NULL,
  `created` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `job_uuid` (`job_uuid`,`tenant_id`,`created`),
  KEY `expires_at` (`expires_at`),
  FOREIGN KEY `fk_tenants_tenant_id` (`tenant_id`)
    REFERENCES tenants(`tenant_id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

# Create the published jobs table that tracks the jobs
# already queue by a scheduler and should not be queued again.
CREATE TABLE `job_published` (
  `phase` ENUM('ARCHIVING','MONITORING','STAGING','SUBMITTING') NOT NULL,
  `job_uuid` varchar(64) NOT NULL,
  `created` datetime NOT NULL,
  `creator` varchar(64) NOT NULL,
  PRIMARY KEY (`phase`, `job_uuid`)
) ENGINE=InnoDB;

# Create the scheduler lease table that is used to limit
# the number of active schedulers for each phase to 1.
CREATE TABLE `job_leases` (
    `lease` varchar(32) NOT NULL,
    `last_updated` datetime NOT NULL,
    `expires_at` datetime,
    `lessee` varchar(128),
    PRIMARY KEY (`lease`),
    UNIQUE INDEX (`lease`,`last_updated`,`expires_at`,`lessee`)
) ENGINE=InnoDB;

# Populate the lease table with the names of each phase. 
INSERT IGNORE INTO `job_leases` (lease, last_updated, expires_at, lessee) VALUES ('STAGING', now(), NULL, NULL);
INSERT IGNORE INTO `job_leases` (lease, last_updated, expires_at, lessee) VALUES ('SUBMITTING', now(), NULL, NULL);
INSERT IGNORE INTO `job_leases` (lease, last_updated, expires_at, lessee) VALUES ('MONITORING', now(), NULL, NULL);
INSERT IGNORE INTO `job_leases` (lease, last_updated, expires_at, lessee) VALUES ('ARCHIVING', now(), NULL, NULL);

