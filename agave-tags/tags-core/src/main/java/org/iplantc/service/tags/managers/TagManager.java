package org.iplantc.service.tags.managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.events.TagEventProcessor;
import org.iplantc.service.tags.exceptions.TagEventProcessingException;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.exceptions.TagValidationException;
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
	
	public Tag updateTagAssociationId(Tag existingTag, JsonNode json, String username) 
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
    		if (json.has("associationIds")) {
    			if (json.get("associationIds").isNull() || 
    					(json.get("associationIds").isArray() && json.get("associationIds").size() == 0)) {
    				List<TaggedResource> oldTaggedResources = existingTag.getTaggedResources();
    				existingTag.getTaggedResources().clear();
    				dao.persist(existingTag);
    				
    				
    				try {
						eventProcessor.processAssociatedUuidUpdateEvent(existingTag, oldTaggedResources,  
								new TagEvent(existingTag.getUuid(),
										TagEventType.UPDATED,
										"The following resources were untagged: " + 
												StringUtils.join(existingTag.getTaggedResourcesAsArray()),
										username));
					} catch (TagEventProcessingException e) {
						log.error("Failed to send tag resource removal event for " + existingTag.getUuid(), e);
					}
    			}
    			else if (json.get("associationIds").isArray()) {
    				
    				List<TaggedResource> newTaggedResources = new ArrayList<TaggedResource>();
    				for(Iterator<JsonNode> iter = json.get("associationIds").iterator(); iter.hasNext();) {
    					TaggedResource tr = new TaggedResource(iter.next().textValue(), existingTag);
    					if (StringUtils.isNotBlank(tr.getUuid())) {
    						newTaggedResources.add(tr);
    					}
    				}
    				
    				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    		        Validator validator = factory.getValidator();
    		        
    		        Set<ConstraintViolation<List<TaggedResource>>> violations = validator.validate(newTaggedResources);
    				if (violations.isEmpty()) {
    					List<TaggedResource> oldTaggedResources = existingTag.getTaggedResources();
    					existingTag.getTaggedResources().clear();
    					for (TaggedResource taggedResource: newTaggedResources) {
    						if (StringUtils.isNotBlank(taggedResource.getUuid())) {
    							existingTag.addTaggedResource(taggedResource);
    						}
    					}
        				dao.persist(existingTag);
//        				existingTag.setTaggedResources(newTaggedResources);
//        				dao.persist(tag);
        				
        				try {
							eventProcessor.processAssociatedUuidUpdateEvent(existingTag, oldTaggedResources,  
									new TagEvent(existingTag.getUuid(),
											TagEventType.UPDATED,
											"The following resources were tagged: " + 
													StringUtils.join(existingTag.getTaggedResourcesAsArray()),
											username));
        				} catch (TagEventProcessingException e) {
    						log.error("Failed to send tag resource update event for " + existingTag.getUuid(), e);
    					}
    	        	} 
    				else {
    	        		throw new TagValidationException(violations.iterator().next().getMessage()); 
    	        	}
    			}
    			else {
    				throw new TagValidationException("Invalid associationIds. "
    						+ "Please provide an array of uuid for the associationIds field.");
    			}
    		}
    		else if (json.isArray()) {
    			if(json.isArray() && json.size() == 0) {
    				List<TaggedResource> oldTaggedResources = tag.getTaggedResources();
    				existingTag.getTaggedResources().clear();
    				dao.persist(existingTag);
    				
    				
    				try {
						eventProcessor.processAssociatedUuidUpdateEvent(existingTag, oldTaggedResources,  
								new TagEvent(existingTag.getUuid(),
										TagEventType.UPDATED,
										"The following resources were untagged: " + 
												StringUtils.join(existingTag.getTaggedResourcesAsArray()),
										username));
    				} catch (TagEventProcessingException e) {
						log.error("Failed to send tag resource removal event for " + existingTag.getUuid(), e);
					}
    			}
    			else {
    				List<TaggedResource> newTaggedResources = new ArrayList<TaggedResource>();
    				for(Iterator<JsonNode> iter = json.get("associationIds").iterator(); iter.hasNext();) {
    					TaggedResource tr = new TaggedResource(iter.next().textValue(), existingTag);
    					if (StringUtils.isNotBlank(tr.getUuid())) {
    						newTaggedResources.add(tr);
    					}
    				}
    				
    				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    		        Validator validator = factory.getValidator();
    		        
    		        Set<ConstraintViolation<List<TaggedResource>>> violations = validator.validate(newTaggedResources);
    				if (violations.isEmpty()) {
    					List<TaggedResource> oldTaggedResources = existingTag.getTaggedResources();
        				existingTag.getTaggedResources().clear();
        				for (TaggedResource taggedResource: newTaggedResources) {
    						if (StringUtils.isNotBlank(taggedResource.getUuid())) {
    							existingTag.addTaggedResource(taggedResource);
    						}
    					}
        				dao.persist(existingTag);
        				
        				try {
							eventProcessor.processAssociatedUuidUpdateEvent(existingTag, oldTaggedResources,  
									new TagEvent(existingTag.getUuid(),
											TagEventType.UPDATED,
											"The following resources were tagged: " + 
													StringUtils.join(existingTag.getTaggedResourcesAsArray()),
											username));
        				} catch (TagEventProcessingException e) {
    						log.error("Failed to send tag resource update event for " + existingTag.getUuid(), e);
    					}
    	        	} 
    				else {
    	        		throw new TagValidationException(violations.iterator().next().getMessage()); 
    	        	}
    			}
    		}
    		else {
				throw new TagValidationException("Invalid associationIds. "
						+ "Please provide an array of uuid for the associationIds field.");
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
