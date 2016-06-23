package org.iplantc.service.common.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Index;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract Entity class for persisting events which occur on {@link AgaveDomainEntity}. 
 * This persists select events in a history log for a {@link AgaveDomainEntity} which 
 * can then be queried for more information.
 * 
 * @author dooley
 *
 */
@MappedSuperclass
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractEntityEvent<T extends Enum<?>> implements AgaveEntityEvent {

	/**
	 * Returns the {@link UUIDType} of the concrete implementation class.
	 * @return
	 */
	protected abstract UUIDType getEntityEventUUIDType();
	
	/**
	 * Returns the {@link UUIDType} of the entity to which this 
	 * event is triggered.
	 * @return
	 */
	protected abstract UUIDType getEntityUUIDType();
	
	/**
     * Returns the public facing URL to the Agave resource
     * identified by {@link #getEntity()} by introspecting the
     * UUID and getting the object reference. In most cases, 
     * this can be deterministically calculated much faster 
     * by overriding this method in subclasses with the following: 
     * <code>
     * @Override
     * protected String getResolvedEntityUrl() {
     *    return TenancyHelper.resolveURLToCurrentTenant(
     *    		Settings.IPLANT_MONITOR_SERVICE) + getEntity());
     * }
     * </code>
     * 
     * Not all API routes will support such a construction. In 
     * those cases it is still faster to construct the URL from
     * known information than obtain it using this default 
     * implementation.
     * 
     * @return 
     */
    protected String getResolvedEntityUrl() {
    	try {
        	AgaveUUID uuid = new AgaveUUID(getEntity());
	        return TenancyHelper.resolveURLToCurrentTenant(uuid.getObjectReference());
        } catch (UUIDException e) {
        	return "";
    	}
    }
    
    /**
     * Returns the public facing URL to this event resource. 
     * In most API resources, this will simply hang off the 
     * end of the parent resoruce and, this this default
     * implementation will be fastest. In other cases, such 
     * as when consuming dynamic endpoints and/or file paths,
     * this method should be overridden.
     * 
     * @return
     */
    protected String getResolvedEntityEventUrl() {
    	return getResolvedEntityUrl() + "/history/" + getUuid();
    }

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
	@Column(name = "created_by", nullable = false, length = 128)
    @Size(min=3,max=32, message="Invalid event owner. Usernames must be between {min} and {max} characters.")
	private String createdBy;
	
	/**
	 * The tenant in which this notification was created.
	 */
	@Column(name = "tenant_id", nullable=false, length = 64)
	@JsonIgnore
	private String tenantId;
	
	/**
	 * The creation date of this entity 
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	@NotNull(message="Invalid event created. Notification created cannot be null.")
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date created;
    
	/**
	 * UUID of the resource on which the event occurred
	 */
	@Index(name = "entity_uuid")
    @Column(name = "entity_uuid", length = 64, nullable = false)
	@JsonIgnore
    private String entity;
    
	/**
	 * The event which was thrown 
	 */
	@Column(name = "`status`", nullable = false, length = 32)
    private String status;
    
    /**
     * A human readable description of the event which occurred 
     */
    @Column(name = "description", length = 128)
    private String description;
    
    /**
     * IP address of the client who created the event 
     */
    @Column(name = "ip_address", nullable = false, length = 15)
    @JsonIgnore
	private String ipAddress;

    
    /**
     * Default no-args contructor 
     */
    public AbstractEntityEvent() {
        this.tenantId = TenancyHelper.getCurrentTenantId();
        this.uuid = new AgaveUUID(getEntityEventUUIDType()).toString();
        this.created = new Timestamp(System.currentTimeMillis());
    }

    public AbstractEntityEvent(T eventType, String description, String createdBy) {
        this();
        this.status = eventType.name();
        this.description = description;
        this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
        this.createdBy = createdBy;
    }

    public AbstractEntityEvent(String entityUuid, T eventType, String description, String createdBy) {
        this(eventType, description, createdBy);
        setEntity(entityUuid);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the softwareUuid
     */
    public String getEntity() {
        return entity;
    }

    /**
     * @param entity
     *            the uuid of the entity to set
     */
    public void setEntity(String entityUuid) {
        this.entity = entityUuid;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param username
     *            the creator to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the message
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * @return the tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId
     *            the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(Timestamp created) {
        this.created = created;
    }
    
    
    @Transient
    @JsonProperty("_links")
    public ObjectNode getLinks() { 	
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode linksObject = mapper.createObjectNode();
        linksObject.set("self", 
        		(ObjectNode)mapper.createObjectNode()
            		.put("href", getResolvedEntityEventUrl()));
        
        linksObject.set(getEntityEventUUIDType().name().toLowerCase(), 
        		mapper.createObjectNode()
                	.put("href", getResolvedEntityUrl()));
        
        return linksObject;  	
    }

    public String toString() {
        return String.format("%s[%s] %s %s",
        		getEntityUUIDType(),
        		getEntity(),
        		getStatus(),
        		new DateTime(getCreated()).toString());
    }
}
