###############################################################
# Migration: V2.1.9.6__Add_notifications_index.sql
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
CREATE INDEX `notifications_status_plus` ON `notifications` 
(`status`, `tenant_id`, `associated_uuid`, `notification_event`);
