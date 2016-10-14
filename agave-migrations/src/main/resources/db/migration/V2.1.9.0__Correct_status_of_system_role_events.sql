###############################################################
# Migration: V2.1.9.0__Correct_status_of_system_role_events.sql
#
# Fix incorrect status values in the systemevents table for role
# grant and revocation events.
#
# Database changes:
#
# Table changes:
#
# Index changes:
#
# Column changes:
#
# Data changes:
# + systemevents
#	+ status
#
#
#################################################################

UPDATE systemevents SET `status` = "ROLES_GRANT" WHERE `status` = "STATUS_UPDATE" AND `description` LIKE '%was granted%';

UPDATE systemevents SET `status` = "ROLES_REVOKE" WHERE `status` = "STATUS_UPDATE" AND `description` LIKE '%had their roles revoked%';
