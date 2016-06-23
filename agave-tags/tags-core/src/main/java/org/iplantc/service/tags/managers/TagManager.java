package org.iplantc.service.tags.managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.events.TagEventProcessor;
import org.iplantc.service.tags.exceptions.TagEventProcessingException;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.iplantc.service.tags.managers.TagPermissionManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.model.TaggedResource;
import org.iplantc.service.tags.model.enumerations.TagEventType;

import com.fasterxml.jackson.databind.JsonNode;

public class TagManager {
	
	private static final Logger log = Logger.getLogger(TagManager.class);
	
	private TagDao dao;
	public TagManager() {
		dao = new TagDao();
	}
	
	/**
	 * Inserts a tag and sends the appropriate notifications. Permissions
	 * are verified prior to deletion.
	 * 
	 * @param tag
	 * @param username
	 * @throws TagException
	 * @throws TagPermissionException
	 */
	public Tag addTagForUser(JsonNode json, String username) 
	throws TagValidationException, TagException 
	{	
		Tag tag = Tag.fromJSON(json);
		tag.setOwner(username);
    	
    	if (dao.doesTagNameExistForUser(tag.getName(), username)) {
    		throw new TagValidationException("Tag with the name " + 
    				tag.getName() + " already exists. " +
    				"Tag names must be unique per user.");
    	}
    	else {
    		dao.persist(tag);
    		TagEventProcessor eventProcessor = new TagEventProcessor();
    		
    		// alert any subscribers
    		try {
				eventProcessor.processContentEvent(tag, 
						new TagEvent(tag.getUuid(),
								TagEventType.CREATED,
								"Tag was created by " + username,
								username));
			} catch (TagEventProcessingException e) {
				log.error("Failed to send tag creation event for " + tag.getUuid(), e);
			}
    	}
    	
    	return tag;
	}
	
	public Tag updateTagAssociatedUuid(Tag existingTag, JsonNode json, String username) 
	throws TagValidationException, TagException 
	{
		Tag tag = Tag.fromJSON(json);
		
		if (dao.doesUserTagExistWithName(existingTag.getName())) {
    		throw new TagValidationException("Tag with the name " + 
    				existingTag.getName() + " already exists. " +
    				"Tag names must be unique per user.");
    	}
    	else {
    		TagEventProcessor eventProcessor = new TagEventProcessor();
    		if (json.has("associatedIds")) {
    			if (json.get("associatedIds").isNull() || 
    					(json.get("associatedIds").isArray() && json.get("associatedIds").size() == 0)) {
    				List<TaggedResource> oldTaggedResources = tag.getTaggedResources();
    				tag.getTaggedResources().clear();
    				dao.persist(tag);
    				
    				
    				try {
						eventProcessor.processAssociatedUuidUpdateEvent(tag, oldTaggedResources,  
								new TagEvent(tag.getUuid(),
										TagEventType.UPDATED,
										"The following resources were untagged: " + 
												StringUtils.join(tag.getTaggedResourcesAsArray()),
										username));
					} catch (TagEventProcessingException e) {
						log.error("Failed to send tag resource removal event for " + tag.getUuid(), e);
					}
    			}
    			else if (json.get("associatedIds").isArray()) {
    				
    				List<TaggedResource> newTaggedResources = new ArrayList<TaggedResource>();
    				for(Iterator<JsonNode> iter = json.get("associatedIds").iterator(); iter.hasNext();) {
    					newTaggedResources.add(new TaggedResource(iter.next().textValue(), tag));
    				}
    				
    				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    		        Validator validator = factory.getValidator();
    		        
    		        Set<ConstraintViolation<List<TaggedResource>>> violations = validator.validate(newTaggedResources);
    				if (violations.isEmpty()) {
    					List<TaggedResource> oldTaggedResources = tag.getTaggedResources();
        				tag.getTaggedResources().clear();
        				dao.persist(tag);
        				tag.setTaggedResources(newTaggedResources);
        				dao.persist(tag);
        				
        				try {
							eventProcessor.processAssociatedUuidUpdateEvent(tag, oldTaggedResources,  
									new TagEvent(tag.getUuid(),
											TagEventType.UPDATED,
											"The following resources were to: " + 
													StringUtils.join(tag.getTaggedResourcesAsArray()),
											username));
        				} catch (TagEventProcessingException e) {
    						log.error("Failed to send tag resource update event for " + tag.getUuid(), e);
    					}
    	        	} 
    				else {
    	        		throw new TagValidationException(violations.iterator().next().getMessage()); 
    	        	}
    			}
    			else {
    				throw new TagValidationException("Invalid associatedIds. "
    						+ "Please provide an array of uuid for the associatedIds field.");
    			}
    		}
    		else if (json.isArray()) {
    			if(json.isArray() && json.size() == 0) {
    				List<TaggedResource> oldTaggedResources = tag.getTaggedResources();
    				tag.getTaggedResources().clear();
    				dao.persist(tag);
    				
    				
    				try {
						eventProcessor.processAssociatedUuidUpdateEvent(tag, oldTaggedResources,  
								new TagEvent(tag.getUuid(),
										TagEventType.UPDATED,
										"The following resources were untagged: " + 
												StringUtils.join(tag.getTaggedResourcesAsArray()),
										username));
    				} catch (TagEventProcessingException e) {
						log.error("Failed to send tag resource removal event for " + tag.getUuid(), e);
					}
    			}
    			else {
    				List<TaggedResource> newTaggedResources = new ArrayList<TaggedResource>();
    				for(Iterator<JsonNode> iter = json.get("associatedIds").iterator(); iter.hasNext();) {
    					newTaggedResources.add(new TaggedResource(iter.next().textValue(), tag));
    				}
    				
    				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    		        Validator validator = factory.getValidator();
    		        
    		        Set<ConstraintViolation<List<TaggedResource>>> violations = validator.validate(newTaggedResources);
    				if (violations.isEmpty()) {
    					List<TaggedResource> oldTaggedResources = tag.getTaggedResources();
        				tag.getTaggedResources().clear();
        				dao.persist(tag);
        				tag.setTaggedResources(newTaggedResources);
        				dao.persist(tag);
        				
        				try {
							eventProcessor.processAssociatedUuidUpdateEvent(tag, oldTaggedResources,  
									new TagEvent(tag.getUuid(),
											TagEventType.UPDATED,
											"The following resources were to: " + 
													StringUtils.join(tag.getTaggedResourcesAsArray()),
											username));
        				} catch (TagEventProcessingException e) {
    						log.error("Failed to send tag resource update event for " + tag.getUuid(), e);
    					}
    	        	} 
    				else {
    	        		throw new TagValidationException(violations.iterator().next().getMessage()); 
    	        	}
    			}
    		}
    		else {
				throw new TagValidationException("Invalid associatedIds. "
						+ "Please provide an array of uuid for the associatedIds field.");
			}
    	}
		
		return tag;
	}

	/**
	 * Deletes a tag and sends the appropriate notifications. Permissions
	 * are verified prior to deletion.
	 * 
	 * @param tag
	 * @param username
	 * @throws TagException
	 * @throws TagPermissionException
	 */
	public void deleteUserTag(Tag tag, String username) 
	throws TagException, TagPermissionException 
	{	
		if (tag == null) {
			throw new TagException("No tag specifed for deletion");
		}
		
		TagPermissionManager pm = new TagPermissionManager(tag);
		
		if (!pm.canWrite(username)) {
    		throw new TagPermissionException("User does not have permission to view this tag");
    	}
    	else {
    		dao.delete(tag);
    		TagEventProcessor eventProcessor = new TagEventProcessor();
    		
    		try {
				eventProcessor.processContentEvent(tag, 
						new TagEvent(tag.getUuid(),
								TagEventType.DELETED,
								"Tag was deleted by " + username,
								username));
			} catch (TagEventProcessingException e) {
				log.error("Failed to send tag deletion event for " + tag.getUuid(), e);
			}
    	}
	}
	
	/**
	 * Deletes a tag and sends the appropriate notifications. Permissions
	 * are verified prior to deletion.
	 * 
	 * @param tag
	 * @param username
	 * @return true if deletion occurred, false otherwise
	 * @throws TagException
	 * @throws TagPermissionException
	 */
	public Tag deleteUserTagAssociatedId(Tag tag, String associatedId, String username) 
	throws TagException, TagPermissionException 
	{	
		if (tag == null) {
			throw new TagException("No tag specifed for deletion");
		}
		
		TagPermissionManager pm = new TagPermissionManager(tag);
		
		if (!pm.canWrite(username)) {
    		throw new TagPermissionException("User does not have permission to view this tag");
    	}
    	else {
    		
    		TaggedResource match = null;
			for (TaggedResource tr: tag.getTaggedResources()) {
				if (StringUtils.equals(tr.getUuid(), associatedId)) {
					match = tr;
					break;
				}
			}
    		
			if (match != null) {
				tag.getTaggedResources().remove(match);
				dao.persist(tag);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
	    		
	    		try {
					eventProcessor.processContentEvent(tag, 
							new TagEvent(tag.getUuid(),
									TagEventType.DELETED,
									"Tag was deleted by " + username,
									username));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send tag deletion event for " + tag.getUuid(), e);
				}
			}
			
			return tag;
    	}
	}

}
