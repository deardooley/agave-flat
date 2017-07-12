###############################################################
# Migration: V2.1.11.0__Add_notifications_index.sql
#
# NotificationDao.getActiveForAssociatedUuidAndEvent() causes a 
# full scan of the notifications table because no index is 
# available.  This file creates an index that can be used in
# that query and any other query in NotificationDao that filters
# on the status field.
#                                      
# Database changes:
#
# Table changes:
# 
# Index changes:
# + notifications_status_plus
#
# Column changes:
# 
# Data changes:
#
#################################################################

/** Add a multi-use index **/
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'notifications' AND index_name = 'notifications_status_plus' AND table_schema = DATABASE() ) = '1', "SELECT 1",
    "CREATE INDEX `notifications_status_plus` ON `notifications` (`status`, `tenant_id`, `associated_uuid`, `notification_event`)" ));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
