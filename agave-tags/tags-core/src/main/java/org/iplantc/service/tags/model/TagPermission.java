/**
 * 
 */
package org.iplantc.service.tags.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.validator.constraints.NotEmpty;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.model.CaseInsensitiveEnumDeserializer;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.tags.exceptions.PermissionValidationException;
import org.iplantc.service.tags.model.constraints.ValidAgaveUuid;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Class to represent individual shared permissions for persistable entities
 * 
 * @author dooley
 * 
 */
@Entity
@Table(name = "tagpermissions", uniqueConstraints=
	@UniqueConstraint(columnNames={"username","permission","entity_id", "tenant_id"}))
@FilterDef(name="tagPermissionTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="tagPermissionTenantFilter", condition="tenant_id=:tenantId"))
public class TagPermission { //extends AbstractPermissionEntity<Tag> {

    /**
     * The database identifier of this object
     */
    @Id
    @GeneratedValue
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    private Long id;
	
    /**
     * The entity to which this permissino applies
     */
    
    @Column(name = "entity_id", length = 64, nullable = false, unique = false)
    @JsonIgnore
    @ValidAgaveUuid
    private String entityId;
    
    /**
     * The principal to whom this permission applies
     */
    @Column(name = "username", nullable = false, length = 32)
    @NotNull(message="Missing username. Please provide a username for whom the permission will be granted.")
    @Size(min=3, max=32, message="A valid username must be between 3 and 32 characters")
    private String username;
    
    /**
     * The permission granted by this record
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 32)
    @NotNull(message="Missing permission. Please provide a username for whom the permission will be granted.")
    private PermissionType permission;
	
    /**
     * The uuid of this permission record
     */
    @Column(name = "uuid", length = 64, nullable = false, unique = true)
    @NotEmpty
    @NotNull
    @JsonIgnore
    private String uuid;
	
    /**
	 * The tenant in which this notification was created.
	 */
	@Column(name = "tenant_id", nullable=false, length = 64)
	@JsonIgnore
	private String tenantId;
	
	/**
	 * Field used for optimistic locking
	 */
	@Version
    @Column(name="OPTLOCK")
	@JsonIgnore
    private Integer version = 0;
	
	/**
	 * Timestamp this record was created
	 */
	@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, length = 19)
	@JsonIgnore
	private Date created;
	
	/**
	 * Timestamp this record was last updated.
	 */
	@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false, length = 19)
	@JsonIgnore
	private Date lastUpdated;
	
	public TagPermission() {
	    this.uuid = new AgaveUUID(UUIDType.PERMISSION).toString();
	    this.created = new Date();
	    this.lastUpdated = new Date();
	    this.tenantId = TenancyHelper.getCurrentTenantId();
	}

	public TagPermission(String username,
	        PermissionType permissionType)
	{
		this();
		this.username = username;
		this.permission = permissionType;
	}
	
	public TagPermission(Tag entity, String username,
	        PermissionType permissionType)
	{
		this(username, permissionType);
		this.entityId = entity.getUuid();
	}
	
	/**
	 * @return the id
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the entity associated with this permission
	 */
	public String getEntityId()
	{
		return this.entityId;
	}

	/**
	 * @param entityId
	 *            the entityId to set
	 */
	public void setEntity(String entityId)
	{
		this.entityId = entityId;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}

	/**
	 * @return the permission, {@link PermissionType#NONE} if not set.
	 */
	public PermissionType getPermission() {
		return permission == null ? PermissionType.NONE : permission;
	}

	@Transient
	@JsonSetter("permission")
    private void setPermission(String value) 
	throws IllegalArgumentException {
		
        if (StringUtils.isEmpty(value)) {
        	this.permission = PermissionType.NONE;
        } else {
        	this.permission = PermissionType.valueOf(value.toUpperCase());
        }
    }

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setPermission(PermissionType permission) {
		this.permission = permission;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}
	
	/**
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }
    
    /**
     * The permission uuid
     * @return
     */
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    
    /**
     * @param version the current version
     */
    public void setVersion(Integer version) {
        this.version = version;
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
	
	public TagPermission clone()
	{
		TagPermission pem = new TagPermission();
		pem.setPermission(getPermission());
		pem.setUsername(getUsername());
		return pem;
	}
	
	public static TagPermission fromJSON(JsonNode json, String entityId) 
	throws PermissionValidationException 
	{
		SimpleModule enumModule = new SimpleModule();
        enumModule.setDeserializers(new CaseInsensitiveEnumDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        mapper.registerModule(enumModule);
		
		try {
			TagPermission permission = mapper.treeToValue(json, TagPermission.class);
			permission.setEntity(entityId);
			
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	        Validator validator = factory.getValidator();
	        
        	Set<ConstraintViolation<TagPermission>> violations = validator.validate(permission);
        	if (violations.isEmpty()) {
        		return permission;
        	} else {
        		throw new PermissionValidationException(violations.iterator().next().getMessage()); 
        	}
        } 
		catch (UnrecognizedPropertyException e) {
        	String message = "Unknown property " + e.getPropertyName();
        	if (StringUtils.startsWithIgnoreCase(e.getPropertyName(), "pe")) {
        		message += ". Did you mean permission?";
        	}
        	else if (StringUtils.startsWithIgnoreCase(e.getPropertyName(), "us")) {
        		message += ". Did you mean username?";
        	}
        	throw new PermissionValidationException(message);
		}
		catch (PermissionValidationException e) {
        	throw e;
        } catch (Exception e) {
        	throw new PermissionValidationException("Unexpected error while validating permission.", e); 
        }
	}

	@JsonValue
	public String toJSON() throws JSONException 
	{
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(username)
			.key("permission").object()
				.key("read").value(getPermission().canRead())
				.key("write").value(getPermission().canWrite())
				.key("execute").value(getPermission().canExecute())
			.endObject()
			.key("lastUpdated").value(new DateTime(getLastUpdated()).toString())
			.key("created").value(new DateTime(getCreated()).toString())
			.key("_links").object()
            	.key("self").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getEntityId()+ "/pems/" + getUsername())
	        	.endObject()
	        	.key("tag").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TAGS_SERVICE) + getEntityId())
	        	.endObject()
	        	.key("profile").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getUsername())
	        	.endObject()
	        .endObject()
		.endObject();
		return writer.toString();
	}
	
	public String toString()
	{
		return "[" + getEntityId() + "] " + username + " " + permission;
	}
}
