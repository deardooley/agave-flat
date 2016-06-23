package org.iplantc.service.realtime.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.SerializationException;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelEventType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entity class for persisting events which occur on {@link AgaveDomainEntity}. 
 * This creates a history log per job that can be queried and/or mined.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "realtimechannelevents")
@FilterDef(name="realtimeChannelEventTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="realtimeChannelEventTenantFilter", condition="tenant_id=:tenantId"))
public class RealtimeChannelEvent {

    private Long id;
    private String entity;
    private String status;
    private String description;
    private String ipAddress;
    private String sourceEvent;
    private String createdBy;
    private Timestamp created;
    private String tenantId;
    private String uuid;

    private RealtimeChannelEvent() {
        this.tenantId = TenancyHelper.getCurrentTenantId();
        this.uuid = new AgaveUUID(UUIDType.APP_EVENT).toString();
        this.created = new Timestamp(System.currentTimeMillis());
    }

    public RealtimeChannelEvent(String status, String description, String createdBy) {
        this();
        this.status = status;
        this.description = description;
        this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
        this.createdBy = createdBy;
    }

    public RealtimeChannelEvent(String entityUuid, RealtimeChannelEventType eventType, String description, String createdBy) {
        this(eventType.name(), description, createdBy);
        setEntity(entityUuid);
    }

//    @Transient
//    @JsonIgnore
//    protected UUIDType getEventCode() {
//        return UUIDType.TAG_EVENT;
//    }
//    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name = "`id`", unique = true, nullable = false)
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
    @Index(name = "entity_uuid")
    @Column(name = "entity_uuid", length = 128, nullable = false)
    @JsonIgnore
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
    @Column(name = "`status`", nullable = false, length = 32)
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
    @Column(name = "created_by", nullable = false, length = 128)
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
	 * @return the sourceEvent
	 */
    @Column(name = "source_event", length = 128, nullable = true, unique = false)
    public String getSourceEvent() {
		return sourceEvent;
	}

	/**
	 * @param sourceEvent the sourceEvent to set
	 */
	public void setSourceEvent(String sourceEvent) {
		this.sourceEvent = sourceEvent;
	}

	/**
     * @return the message
     */
    @Column(name = "description", length = 1024)
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
    @Column(name = "ip_address", nullable = false, length = 15)
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
    @Column(name = "tenant_id", nullable = false, length = 128)
    @JsonIgnore
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
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, length = 19)
    public Date getCreated() {
        return created;
    }
    
    /**
     * @param created
     *            the created to set
     */
    public void setCreated(Timestamp created) {
        this.created = created;
    }

    /**
     * @return the uuid
     */
    @Column(name = "uuid", length = 128, nullable = false, unique = true)
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


    @JsonValue
    public String toJSON() throws SerializationException {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode json = mapper.createObjectNode()
        		.put("status", getStatus())
                .put("createdBy", getCreatedBy())
                .put("description", getDescription())
                .put("created", new DateTime(getCreated()).toString())
                .put("id", getUuid());

        ObjectNode linksObject = json.putObject("_links");
        linksObject.put(
                "self",
                (ObjectNode) mapper.createObjectNode().put(
                        "href",
                        TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_REALTIME_SERVICE) + getEntity() + "/history/" + getUuid()));
        
        linksObject.put("channel", (ObjectNode)mapper.createObjectNode()
                .put("href", 
                		TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_REALTIME_SERVICE) + getEntity()));
        
        linksObject.put(
                "profile",
                (ObjectNode) mapper.createObjectNode().put(
                        "href",
                        TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE)
                                + getCreatedBy()));
        json.put("_links", linksObject);

        return json.toString();
    }

    public String toString() {
        return getEntity() + " " + getStatus() + " " + new DateTime(getCreated()).toString();
    }
}
