package org.iplantc.service.systems.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Index;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entity class for persisting events which occur on {@link AgaveDomainEntity}. 
 * This creates a history log per job that can be queried and/or mined.
 * 
 * @author dooley
 *
 */
/**
 * @author dooley
 *
 */
@Entity
public class RemoteSystemEvent { //extends AbstractEventEntity<Tag> {

    protected UUIDType getEventCode() {
        return UUIDType.SYSTEM_EVENT;
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
	private Date created;
    
	/**
	 * UUID of the resource on which the event occurred
	 */
	@Index(name = "entity_uuid")
    @Column(name = "entity_uuid", length = 64, nullable = false)
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
    private String ipAddress;

    private RemoteSystemEvent() {
        this.tenantId = TenancyHelper.getCurrentTenantId();
        this.uuid = new AgaveUUID(UUIDType.SYSTEM_EVENT).toString();
        this.created = new Timestamp(System.currentTimeMillis());
    }

    public RemoteSystemEvent(SystemEventType eventType, String description, String createdBy) {
        this();
        this.status = eventType.name();
        this.description = description;
        this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
        this.createdBy = createdBy;
    }

    public RemoteSystemEvent(String entityUuid, SystemEventType eventType, String description, String createdBy) {
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
    public String getLinks() { 	
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode linksObject = mapper.createObjectNode();
        try {
        	AgaveUUID uuid = new AgaveUUID(getUuid());
	        linksObject.put("self", (ObjectNode)mapper.createObjectNode()
	            .put("href", TenancyHelper.resolveURLToCurrentTenant(uuid.getObjectReference())));
        } catch (UUIDException e) {
        	linksObject.put("self", (ObjectNode)mapper.createObjectNode()
		            .putNull("href"));
    	}
        
        linksObject.put("tag", (ObjectNode)mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getEntity()));
        
        return mapper.createObjectNode().put("_links", linksObject).toString();  	
    }

//    @JsonValue
//    public String toJSON() throws SerializationException {
//        ObjectMapper mapper = new ObjectMapper();
//        
//        ObjectNode json = mapper.createObjectNode().put("status", getStatus())
//                .put("createdBy", getCreatedBy()).put("description", getDescription())
//                .put("created", new DateTime(getCreated()).toString()).put("id", getUuid());
//
//        ObjectNode linksObject = json.putObject("_links");
//        linksObject.put(
//                "self",
//                (ObjectNode) mapper.createObjectNode().put(
//                        "href",
//                        TenancyHelper.resolveURLToCurrentTenant(uuid.getObjectReference())
//                                + "/history/" + getUuid()));
//        linksObject.put(
//                uuid.getResourceType().name().toLowerCase(),
//                (ObjectNode) mapper.createObjectNode().put("href",
//                        TenancyHelper.resolveURLToCurrentTenant(uuid.getObjectReference())));
//        linksObject.put(
//                "profile",
//                (ObjectNode) mapper.createObjectNode().put(
//                        "href",
//                        TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE)
//                                + getCreatedBy()));
//        json.put("_links", linksObject);
//
//        return json.toString();
//    }

    public String toString() {
        return getEntity() + " " + getStatus() + " " + new DateTime(getCreated()).toString();
    }
}
