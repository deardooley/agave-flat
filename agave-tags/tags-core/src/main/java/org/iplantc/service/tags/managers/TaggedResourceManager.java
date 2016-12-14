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
import org.iplantc.service.tags.exceptions.DuplicateTaggedResourceException;
import org.iplantc.service.tags.exceptions.TagEventProcessingException;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.iplantc.service.tags.exceptions.UnknownTaggedResourceException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.model.TaggedResource;
import org.iplantc.service.tags.model.enumerations.TagEventType;

import com.fasterxml.jackson.databind.JsonNode;

public class TaggedResourceManager {
	
	private static final Logger log = Logger.getLogger(TaggedResourceManager.class);
	
	private TagDao dao;
	private TagEventProcessor eventProcessor;
	private Tag tag;
	
	public TaggedResourceManager(Tag tag) {
		this.setTag(tag);
		this.setDao(new TagDao());
		this.setEventProcessor(new TagEventProcessor());
	}
	
	/**
	 * Adds a {@link TaggedResource} to an existing {@link Tag}. In the event of a duplicate,
	 * nothing is added.
	 * 
	 * @param uuid a uuid to add to the given tag
	 * @param username
	 * @return the newly added {@link TaggedResource} for the {@code uuid}
	 * @throws TagValidationException
	 * @throws TagException
	 * @throws DuplicateTaggedResourceException 
	 */
	public TaggedResource addToTag(String uuid, String username) 
	throws TagValidationException, TagException, DuplicateTaggedResourceException 
	{	
		if (tag == null) {
			throw new TagException("No tag specifed");
		}
		
		if (StringUtils.isBlank(uuid)) {
			throw new TagValidationException("Resource id cannot be null. "
					+ "Please provide a valid uuid to tag with " + getTag().getName());
		}
		
		// create and validate the new TaggedResource
		TaggedResource newTaggedResource = new TaggedResource(uuid, getTag());
		
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        Set<ConstraintViolation<TaggedResource>> violations = validator.validate(newTaggedResource);
		if (violations.isEmpty()) {
			
			List<TaggedResource> oldTaggedResources = getTag().getTaggedResources();
			
			// returns true on insert, false on duplicate
			if (getTag().addTaggedResource(newTaggedResource)) {
			
				getDao().persist(getTag());
				
				try {
					// process the events for the new associated UUIDS
					eventProcessor.processAssociatedUuidUpdateEvent(getTag(), oldTaggedResources,  
							new TagEvent(getTag().getUuid(),
									TagEventType.UPDATED,
									"The following resources were tagged: " + 
											StringUtils.join(getTag().getTaggedResourcesAsArray()),
									username));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send tag resource update event for " + getTag().getUuid(), e);
				}
			}
			else {
				return getTag().getTaggedResource(uuid);
//				throw new DuplicateTaggedResourceException("Resource " + uuid + " is already tagged with " + getTag().getName());
			}
    	} 
		else {
    		throw new TagValidationException(violations.iterator().next().getMessage()); 
    	}
		
		return newTaggedResource;
	}
	
	/**
	 * Removes all associationIds from the given tag.
	 * 
	 * @param username
	 * @throws TagException
	 */
	public void clearAllFromTag(String username) 
	throws TagException 
	{
		if (tag == null) {
			throw new TagException("No tag specifed");
		}
		
		// remember old TaggedResource for event propagation after delete
		List<TaggedResource> oldTaggedResources = getTag().getTaggedResources();
		
		// clear all TaggedResource for the tag and save the empty list with the tag
		getTag().getTaggedResources().clear();
		getDao().persist(getTag());
		
		try {
			// process the events for the new associated UUIDS
			eventProcessor.processAssociatedUuidUpdateEvent(getTag(), oldTaggedResources,  
					new TagEvent(getTag().getUuid(),
							TagEventType.UPDATED,
							"The following resources were untagged: " + 
									StringUtils.join(getTag().getTaggedResourcesAsArray()),
							username));
		} catch (TagEventProcessingException e) {
			log.error("Failed to send tag resource removal event for " + getTag().getUuid(), e);
		}
	}
	
	/**
	 * Adds a {@link List<String>} of uuid to an existing {@link Tag}. In the event of a duplicate,
	 * nothing is added.
	 * 
	 * @param json an JSON array of uuid to add to the given tag
	 * @param username
	 * @return the newly added {@link TaggedResource}s
	 * @throws TagValidationException
	 * @throws TagException
	 * @throws DuplicateTaggedResourceException 
	 */
	public List<TaggedResource> addAllToTag(JsonNode json, String username) 
	throws TagValidationException, TagException, DuplicateTaggedResourceException 
	{		
		if (tag == null) {
			throw new TagException("No tag specifed");
		}
		
		List<TaggedResource> newTaggedResources = new ArrayList<TaggedResource>();
		
		// we are expecting a json array of tags
		if (json.isArray()) {
			
			// provided there are actually values present, we proceed
			if(json.size() > 0) {
				
				// validate each uuid, ignoring blanks prior to adding
				for(Iterator<JsonNode> iter = json.iterator(); iter.hasNext();) {
					TaggedResource tr = new TaggedResource(iter.next().textValue(), getTag());
					if (StringUtils.isNotBlank(tr.getUuid())) {
						newTaggedResources.add(tr);
					}	
				}
				
				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		        Validator validator = factory.getValidator();
		        
		        Set<ConstraintViolation<List<TaggedResource>>> violations = validator.validate(newTaggedResources);
				if (violations.isEmpty()) {
					// remember the old TaggedResource list so we don't send out unnecesary events
					List<TaggedResource> oldTaggedResources = getTag().getTaggedResources();
					for(TaggedResource tr: newTaggedResources) {
						getTag().addTaggedResource(tr);
					}
    				getDao().persist(getTag());
    				
    				try {
    					// process the events for the new associated UUIDS
						eventProcessor.processAssociatedUuidUpdateEvent(getTag(), oldTaggedResources,  
								new TagEvent(getTag().getUuid(),
										TagEventType.UPDATED,
										"The following resources were tagged: " + 
												newTaggedResources.toString(),
										username));
    				} catch (TagEventProcessingException e) {
						log.error("Failed to send tag resource update event for " + getTag().getUuid(), e);
					}
	        	} 
				else {
	        		throw new TagValidationException(violations.iterator().next().getMessage()); 
	        	}
			}
			else {
				// we do nothing if no values were provided, the 
				// returned array will be empty
			}
		}
		else {
			throw new TagValidationException("Invalid associationIds. "
					+ "Please provide an array of uuid for the associationIds field.");
		}
		
		return newTaggedResources;
	}
	
	
	/**
	 * Deletes a tag and sends the appropriate notifications. Permissions
	 * are verified prior to deletion.
	 * 
	 * @param uuid the uuid to untag
	 * @param username the requesting username
	 * @return true if deletion occurred, false otherwise
	 * @throws TagException
	 * @throws TagPermissionException
	 */
	public boolean deleteFromTag(String uuid, String username) 
	throws TagException, TagPermissionException, UnknownTaggedResourceException
	{	
		if (tag == null) {
			throw new TagException("No tag specifed");
		}
		
		if (StringUtils.isBlank(uuid)) {
			throw new TagException("No resource uuid specifed for removal");
		}
		
		TagPermissionManager pm = new TagPermissionManager(tag);
		
		if (!pm.canWrite(username)) {
    		throw new TagPermissionException("User does not have permission to view this tag");
    	}
    	else {
    		
    		TaggedResource match = tag.getTaggedResource(uuid);
    		
			if (match != null) {
				tag.getTaggedResources().remove(match);
				getDao().persist(tag);
				
	    		try {
					eventProcessor.processContentEvent(tag, 
							new TagEvent(tag.getUuid(),
									TagEventType.DELETED,
									"Resource " + uuid + " was removed by " + username,
									username));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send resource " + uuid + " removal event for tag " + tag.getUuid(), e);
				}
			}
			else {
				return false;
//				throw new UnknownTaggedResourceException("Resource " + associatedId + " is not associated tag " + tag.getName());
			}
			
			return true;
    	}
	}

	/**
	 * @return the dao
	 */
	public TagDao getDao() {
		return dao;
	}

	/**
	 * @param dao the dao to set
	 */
	public void setDao(TagDao dao) {
		this.dao = dao;
	}

	/**
	 * @return the eventProcessor
	 */
	public TagEventProcessor getEventProcessor() {
		return eventProcessor;
	}

	/**
	 * @param eventProcessor the eventProcessor to set
	 */
	public void setEventProcessor(TagEventProcessor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	/**
	 * @return the tag
	 */
	public Tag getTag() {
		return tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(Tag tag) {
		this.tag = tag;
	}

}
