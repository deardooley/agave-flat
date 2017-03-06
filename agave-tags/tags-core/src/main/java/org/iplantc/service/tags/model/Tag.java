package org.iplantc.service.tags.model;

// import org.hibernate.validator.constraints.NotEmpty;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.model.CaseInsensitiveEnumDeserializer;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tag domain class
 * 
 * @author dooley
 */
@Entity
@Table(name = "tags", uniqueConstraints=
    @UniqueConstraint(columnNames={"name","owner","tenant_id"}))
@FilterDef(name="tagTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="tagTenantFilter", condition="tenant_id=:tenantId"))
public class Tag  {
    
	/**
	 * Primary key for this entity
	 */
	@Id
	@GeneratedValue
	@Column(name = "`id`", unique = true, nullable = false)
	@JsonIgnore
	private Long id;
	
	/**
	 * UUID of this notification
	 */
	@Column(name = "uuid", nullable = false, length = 64, unique = true)
	@JsonProperty("id")
	@Size(min=3,
		  max=64,
		  message = "Invalid uuid value. uuid must be between {min} and {max} characters long.")
	private String uuid;
	
	/**
	 * Creator of this notification
	 */
	@Column(name = "owner", nullable = false, length = 32)
	@Size(min=3,max=32, message="Invalid notification owner. Usernames must be between {min} and {max} characters.")
	private String owner;
	
	/**
	 * The tenant in which this notification was created.
	 */
	@Column(name = "tenant_id", nullable=false, length = 64)
	@JsonIgnore
	private String tenantId;
	
	/**
	 * The last time this entity was updated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false)
	@NotNull(message="Invalid notification lastUpdated. Notification lastUpdated cannot be null.")
	private Date lastUpdated;	
	
	/**
	 * The creation date of this entity 
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false)
	@NotNull(message="Invalid notification created. Notification created cannot be null.")
	private Date created;
	
	/**
	 * Creator of this notification
	 */
	@Column(name = "name", nullable = false, length = 32)
	@NotNull
	@Size(min=3,max=32, message="Invalid notification owner. Usernames must be between {min} and {max} characters.")
	@Pattern(regexp="[0-9a-zA-Z\\.\\-_]+", flags={Pattern.Flag.CASE_INSENSITIVE})
	private String name;
	
	/**
	 * The tags to which this uuid applies.
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval=true)
	@JsonProperty("associationIds")
	@OptimisticLock(excluded = true)
    private List<TaggedResource> taggedResources = new ArrayList<TaggedResource>();
    
	/**
	 * Optimistic lock on the database row
	 */
	@Version
    @Column(name="OPTLOCK")
    private Integer version = 0;
    
    public Tag() {
        tenantId = TenancyHelper.getCurrentTenantId();
        this.setUuid(new AgaveUUID(UUIDType.TAG).toString());
        this.created = new Timestamp(System.currentTimeMillis());
        this.lastUpdated = new Timestamp(System.currentTimeMillis());
    }
    
    public Tag(String name, String owner, String[] taggedResources) {
        this();
        setName(name);
        setOwner(owner);
        setTaggedResources(taggedResources);
    }
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Descriptive name of the tenant
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) throws TagValidationException {
        if (StringUtils.length(owner) > 32) {
            throw new TagValidationException("Invalid tag owner. " +
                    "Tag owner must be less than 32 characters.");
        }
        this.owner = owner;
    }

    /**
     * @return the taggedResources
     */
    public List<TaggedResource> getTaggedResources() {
        return taggedResources;
    }
    
    /**
     * Returns the {@link TaggedResource} with uuid matching the given
     * value. 
     * @param associatedUuid uuid of the resource to lookup
     * @return the taggedResource with matching uuid, null if no match
     */
    public TaggedResource getTaggedResource(String associatedUuid) {
        for (TaggedResource tr: getTaggedResources()) {
        	if (tr.getUuid().equals(associatedUuid)) {
        		return tr;
        	}
         }
        
        return null;
    }
    
    /**
     * @return the taggedResources
     */
    public String[] getTaggedResourcesAsArray() {
        List<String> uuids = new ArrayList<String>();
        for (TaggedResource taggedResource: taggedResources) {
    		uuids.add(taggedResource.getUuid());
    	}
    	return uuids.toArray(new String[]{});
    }
    
    /**
     * @param taggedResources the taggedResources to set
     */
    public void setTaggedResources(List<TaggedResource> taggedResources) {
    	if (taggedResources == null || taggedResources.isEmpty()) {
    		this.taggedResources.clear();
    		this.taggedResources = new ArrayList<TaggedResource>();
    	} 
    	else {
	    	for (TaggedResource tr: taggedResources) {
				addTaggedResource(tr);
	    	}
    	}
    }
    
    /**
     * @param associatedUuids an array of agave resource uuid to associate with this tag
     */
    @JsonIgnore
    public void setTaggedResources(String[] associatedUuids) {
    	for (String uuid: associatedUuids) {
			addTaggedResource(new TaggedResource(uuid, this));
    	}
    }
    
    /**
     * Add a {@link TaggedResource} and associate with this tag.
     * @param resource
     */
    public boolean addTaggedResource(TaggedResource resource) {
    	for (TaggedResource taggedResource: this.taggedResources) {
    		if (StringUtils.equals(resource.getUuid(), taggedResource.getUuid())) {
    			// don't add duplicates
    			return false;
    		}
    	}
        resource.setTag(this);
        this.taggedResources.add(resource);
        return true;
    }

    /**
     * @return the tenantCode
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantCode the tenantCode to set
     */
    public void setTenantId(String tenantCode) {
        this.tenantId = tenantCode;
    }

    /**
     * Universally unique id for this tenant.
     * @return
     */
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid
     */
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }
    
    /**
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }
    
    /**
     * @param version the current version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String toString()
    {
        return getName() + " => " + StringUtils.join(getTaggedResourceIds(), ",");
    }
    
    /**
     * Get a {@link List} of just uuid of tagged resources.
     * @return list of tagged resources or empty list of no tagged resources
     */
    private List<String> getTaggedResourceIds() {
        List<String> resourceIds = new ArrayList<String>();
        for (TaggedResource tr: getTaggedResources()) {
            resourceIds.add(tr.getUuid());
        }
        return resourceIds;
    }
    
    public static Tag fromJSON(JsonNode json) 
	throws TagValidationException
	{
		SimpleModule enumModule = new SimpleModule();
        enumModule.setDeserializers(new CaseInsensitiveEnumDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        mapper.registerModule(enumModule);
		
		try {
			Tag tag = mapper.treeToValue(json, Tag.class);
			
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	        Validator validator = factory.getValidator();
	        
        	Set<ConstraintViolation<Tag>> violations = validator.validate(tag);
        	if (violations.isEmpty()) {
        		return tag;
        	} else {
        		throw new TagValidationException(violations.iterator().next().getMessage()); 
        	}
		} catch (UnrecognizedPropertyException e) {
        	String message = "Unknown property " + e.getPropertyName();
        	if (StringUtils.startsWithIgnoreCase(e.getPropertyName(), "assoc")) {
        		message += ". Did you mean associationIds?";
        	}
        	else if (StringUtils.startsWithIgnoreCase(e.getPropertyName(), "na")) {
        		message += ". Did you mean name?";
        	}
        	throw new TagValidationException(message);
		}
		catch (TagValidationException e) {
        	throw e;
        } 
		catch (Exception e) {
        	throw new TagValidationException("Unexpected error while validating tag.", e); 
        }
	}

    @JsonValue
    public JsonNode toJSON()
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode()
            .put("id", getUuid())
            .put("name", getName());
        
        ArrayNode resourceArray = mapper.createArrayNode();
        ArrayNode halResourceArray = mapper.createArrayNode();
        for (TaggedResource taggedResource: getTaggedResources()) {
            resourceArray.add(taggedResource.getUuid());
            
//            AgaveUUID agaveUUID = null;
//            try {
//            	agaveUUID = new AgaveUUID(taggedResource.getUuid());
//            	
//                ObjectNode assocResource = mapper.createObjectNode();
//                assocResource.put("rel", taggedResource.getUuid());
//                assocResource.put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference()));
//                assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
//                halResourceArray.add(assocResource);
//            }
//            catch (UUIDException e) {
//            	ObjectNode assocResource = mapper.createObjectNode();
//                assocResource.put("rel", taggedResource.getUuid());
//                assocResource.putNull("href");
//                if (agaveUUID != null) {
//                	assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
//                }
//                halResourceArray.add(assocResource);
//            }
        }
        
        json.put("associationIds", resourceArray);
        
        
        json.put("lastUpdated", new DateTime(getLastUpdated()).toString())
            .put("created", new DateTime(getCreated()).toString());
        
        ObjectNode linksObject = mapper.createObjectNode();
        linksObject.put("self", (ObjectNode)mapper.createObjectNode()
            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getUuid()));
        linksObject.put("associationIds", (ObjectNode)mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getUuid() + "/associations"));
//        linksObject.put("history", (ObjectNode)mapper.createObjectNode()
//                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getUuid() + "/history"));
        linksObject.put("permissions", (ObjectNode)mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getUuid() + "/pems"));
        
        linksObject.put("owner", (ObjectNode)mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner()));
        json.put("_links", linksObject);
        
        return json;
    }
    
    
}
