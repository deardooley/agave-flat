/**
 * 
 */
package org.iplantc.service.realtime.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.realtime.exceptions.RealtimeChannelValidationException;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelProtocolType;
import org.iplantc.service.systems.exceptions.SystemException;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * POJO for representing a websocket or streaming channel 
 * to the Agave event stream.
 *  
 * @author dooley
 *
 */
@Entity
@Table(name = "realtimechannels", uniqueConstraints=
    @UniqueConstraint(columnNames={"name","owner","tenant_id"}))
@FilterDef(name="realtimeChannelTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="realtimeChannelTenantFilter", condition="tenant_id=:tenantId"))
public class RealtimeChannel {
	
	private Long id;
    private String name;
    private String owner;
    private String uuid;
    private RealtimeChannelProtocolType protocol;
    private String tenantId;
    private Integer version = 0;
    private boolean available = true;
    private boolean enabled = true;
    private Set<String> observableEvents = new HashSet<String>();
    private Timestamp lastUpdated;
    private Timestamp created;
	
	public RealtimeChannel() {
		this.created = new Timestamp(new Date().getTime());
		this.lastUpdated = this.created;
		this.uuid = new AgaveUUID(UUIDType.REALTIME_CHANNEL).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.name = UUID.randomUUID().toString();
	}
	
	public RealtimeChannel(String name, String owner) {
		this();
		setName(name);
		setOwner(owner);
	}
	
	/**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name = "id", unique = true, nullable = false)
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
    @Column(name = "name", nullable = true, unique = false, length = 64)
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
	 * @return the protocol
	 */
    @Enumerated(EnumType.STRING)
	@Column(name = "protocol", nullable = true, unique = false, length = 16)
    public RealtimeChannelProtocolType getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(RealtimeChannelProtocolType protocol) {
		this.protocol = protocol;
	}

	/**
     * @return the owner
     */
    @Column(name = "owner", nullable = true, unique = false, length = 64)
     public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) throws RealtimeChannelValidationException {
        if (StringUtils.length(owner) > 32) {
            throw new RealtimeChannelValidationException("Invalid tag owner. " +
                    "Tag owner must be less than 32 characters.");
        }
        this.owner = owner;
    }

    /**
	 * Returns a set of events which will be published to this channel
	 * @return the usersUsingAsDefault
	 */
	@ElementCollection
	@CollectionTable(name="realtimechannel_observableevents", joinColumns=@JoinColumn(name="system_id"))
	@Column(name="observable_events")
	@LazyCollection(LazyCollectionOption.FALSE)
	@JsonManagedReference("events")
	public Set<String> getObservableEvents()
	{
		return observableEvents;
	}

	/**
	 * @param usersUsingAsDefault the usersUsingAsDefault to set
	 */
	public void setObservableEvents(Set<String> observableEvents)
	{
		this.observableEvents = observableEvents;
	}

	/**
	 * @param event the event to set
	 * @throws SystemException
	 */
	public void addObservableEvent(String observableEvent)
	{
		if (!StringUtils.isEmpty(observableEvent) && observableEvent.length() > 32) {
			throw new SystemException("default username must be less than 32 characters.");
		}

		this.observableEvents.add(observableEvent);
	}

    /**
     * @return the tenantCode
     */
    @Column(name = "tenant_id", nullable = false, length = 64)
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
    @Column(name = "uuid", nullable = false, unique = true, length = 128)
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
	 * @return the available
	 */
    @Column(name = "is_available", columnDefinition = "TINYINT(1)")
	public boolean isAvailable() {
		return available;
	}

	/**
	 * @param available the available to set
	 */
	public void setAvailable(boolean available) {
		this.available = available;
	}

	/**
	 * @return the enabled
	 */
	@Column(name = "is_enabled", columnDefinition = "TINYINT(1)")
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * @return the lastUpdated
     */
    @Column(name = "last_updated", nullable = false, length = 19)
    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return the created
     */
    @Column(name = "created", nullable = false, length = 19)
    public Timestamp getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Timestamp created) {
        this.created = created;
    }
    
    /**
     * @return the version
     */
    @Version
    @Column(name="OPTLOCK")
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
        return getName() + " => " + StringUtils.join(getObservableEvents(), ",");
    }
    
    @JsonValue
    public String toJSON()
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode()
            .put("id", getUuid())
            .put("name", getName());
        
        ArrayNode resourceArray = json.putArray("events");
        
        for (String resId: getObservableEvents()) {
            resourceArray.add(resId);
        }
        
        json.put("lastUpdated", new DateTime(getLastUpdated()).toString())
            .put("created", new DateTime(getCreated()).toString());
        
        ObjectNode linksObject = mapper.createObjectNode();
        linksObject.put("self", (ObjectNode)mapper.createObjectNode()
            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_REALTIME_SERVICE) + getUuid()));
        linksObject.put("owner", (ObjectNode)mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner()));
        json.put("_links", linksObject);
        
        return json.toString();
    }

}
