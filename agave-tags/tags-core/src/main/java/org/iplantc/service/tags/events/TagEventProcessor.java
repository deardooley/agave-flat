package org.iplantc.service.tags.events;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.tags.dao.TagEventDao;
import org.iplantc.service.tags.exceptions.TagEventPersistenceException;
import org.iplantc.service.tags.exceptions.TagEventProcessingException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.TaggedResource;
import org.iplantc.service.tags.model.enumerations.TagEventType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles sending and propagation of events on {@link Tag} objects.
 * Currently it only sends notification events and does not persist the 
 * {@link TagEvent} with the {@link Tag} due to the hibernate
 * managed relationship.
 * 
 * TODO: Remove {@link Tag#getEvents()} and the relationship.
 * TODO: Make this class the default mechansim for adding events
 * TODO: Refactor this as an async factory using vert.x
 *
 * @author dooley
 *
 */
public class TagEventProcessor {
	private static final Logger log = Logger.getLogger(TagEventProcessor.class);
	private ObjectMapper mapper = new ObjectMapper();
	private TagEventDao dao = new TagEventDao();
	
	public TagEventProcessor(){}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link Tag}. This could be a CRUD operation or a change
	 * to the associations.
	 * 
	 * @param tag
	 * @param event
	 * @return the {@link TagEvent} updated with the association to the {@link Tag}
	 */
	public TagEvent processContentEvent(Tag tag, TagEvent event) 
	throws TagEventProcessingException {
		
		if (tag == null) {
			throw new TagEventProcessingException("Valid tag must be provided to process event.");
		}
		
		if (event == null) {
			throw new TagEventProcessingException("Valid tag event must be provided to process event.");
		}
		
		JsonNode json = null;
		try {
		    json = mapper.createObjectNode().set("tag", mapper.readTree(tag.toJSON()));
		} catch (Throwable e) {
		    log.error(String.format("Failed to serialize tag "
		            + "%s to json for %s event notification", 
		            tag.getUuid(), event.getStatus()), e);
		    json = null;
		}
		
		try {
			dao.persist(event);
		}
		catch (TagEventPersistenceException e) {
			log.error("Failed to save tag event " + event.getStatus() + " on tag " + tag.getUuid(), e);
		}
		
		NotificationManager.process(tag.getUuid(), event.getStatus(), event.getCreatedBy(), json.toString());
		
		// We fire delegated tag permission events on associated uuids. We will 
		// send the entire fire event to the delgation method for simplicity
		TagEventType delegatedEvent = TagEventType.CREATED == TagEventType.valueOf(event.getStatus().toUpperCase()) ? 
				TagEventType.RESOURCE_ADDED : TagEventType.RESOURCE_REMOVED;
		
		for (TaggedResource tr: tag.getTaggedResources()) {
			fireAssociatedNotification(tr.getUuid(), delegatedEvent, event.getCreatedBy(), json);
		}
		
		return event;
	}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link Tag}. This could be a CRUD operation or a change
	 * to the associations.
	 * 
	 * @param tag
	 * @param event
	 * @return the {@link TagEvent} updated with the association to the {@link Tag}
	 */
	public TagEvent processAssociatedUuidUpdateEvent(Tag tag, List<TaggedResource> oldTaggedResources, TagEvent event) 
	throws TagEventProcessingException {
		
		if (tag == null) {
			throw new TagEventProcessingException("Valid tag must be provided to process event.");
		}
		
		if (event == null) {
			throw new TagEventProcessingException("Valid tag event must be provided to process event.");
		}
		
		JsonNode json = null;
		try {
		    json = mapper.createObjectNode().set("tag", mapper.readTree(tag.toJSON()));
		} catch (Throwable e) {
		    log.error(String.format("Failed to serialize tag "
		            + "%s to json for %s event notification", 
		            tag.getUuid(), event.getStatus()), e);
		    json = null;
		}
		
		try {
			dao.persist(event);
		}
		catch (TagEventPersistenceException e) {
			log.error("Failed to save tag event " + event.getStatus() + " on tag " + tag.getUuid(), e);
		}
		
		NotificationManager.process(tag.getUuid(), event.getStatus(), event.getCreatedBy(), json.toString());
		
		// We fire delegated tag permission events on associated uuids. We will 
		// send the entire fire event to the delgation method for simplicity.
		// We start with the deletions
		for (TaggedResource tr: oldTaggedResources) {
			if (!tag.getTaggedResources().contains(tr)) {
				fireAssociatedNotification(tr.getUuid(), TagEventType.RESOURCE_REMOVED, event.getCreatedBy(), json);
			}
		}
		
		// We start with the deletions
		for (TaggedResource tr: tag.getTaggedResources()) {
			if (!oldTaggedResources.contains(tr)) {
				fireAssociatedNotification(tr.getUuid(), TagEventType.RESOURCE_ADDED, event.getCreatedBy(), json);
			}
		}
		
		return event;
	}
	
	/**
	 * Generates notification events for permission-related changes to a 
	 * {@link Tag}. Permission changes are not propagated to the associated
	 * resources.
	 * 
	 * @param tag
	 * @param permission
	 * @param event
	 * @return the {@link TagEvent}
	 * 
	 * @throws TagEventProcessingException
	 */
	public TagEvent processPermissionEvent(Tag tag, TagPermission permission, TagEvent event) 
	throws TagEventProcessingException 
	{
		if (tag == null) {
			throw new TagEventProcessingException("Validtage must be provided to process event.");
		}
		
		if (permission == null) {
			throw new TagEventProcessingException("Valid tag permission must be provided to process event.");
		}
		
		if (event == null) {
			throw new TagEventProcessingException("Valid tag event must be provided to process event.");
		}
		
		ObjectNode json = null;
		try {
		    json = mapper.createObjectNode();
	    	json.set("tag", mapper.readTree(tag.toJSON()));
		    json.set("permission", mapper.readTree(permission.toJSON()));
		} catch (Throwable e) {
		    log.error(String.format("Failed to serialize tag "
		            + "%s to json for %s event notification", 
		            tag.getUuid(), event.getStatus()), e);
		    json = null;
		}
		
		try {
			dao.persist(event);
		}
		catch (TagEventPersistenceException e) {
			log.error("Failed to save tag event " + event.getStatus() + " on tag " + tag.getUuid(), e);
		}
		
		NotificationManager.process(tag.getUuid(), event.getStatus(), event.getCreatedBy(), json.toString());
		
		// We do not fire delegated tag permission events on associated uuids 
		
		return event;
	}
	
	
	/**
	 * Publishes a FILE_CONTENT_CHANGE or FILE_PERMISSION_* event to the  
	 * system on which the {@link Tag} resides. Only events where
	 * {@link TagEventType#isContentChangeEvent()} or {@link TagEventType#isPermissionChangeEvent()}
	 * are true get published.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return
	 */
	private void fireAssociatedNotification(String associatedUuid, TagEventType event, String createdBy, JsonNode json) {
		try {
            NotificationManager.process(associatedUuid, "TAG_" + event.name(), createdBy, json.toString());
        }
        catch (Throwable e) {
            log.error(String.format("Failed to send content tag change notification "
                    + "to %s to on a %s event", 
                    associatedUuid, event.name()), e);
        }
	}

}
