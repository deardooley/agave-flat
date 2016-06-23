/**
 * 
 */
package org.iplantc.service.common.discovery.providers.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.iplantc.service.common.discovery.PlatformService;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.uuid.AgaveUUID;
/**
 * @author dooley
 *
 */
import org.iplantc.service.common.uuid.UUIDType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "discoverableservices")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class DiscoverableService implements PlatformService<DiscoverableServiceCapability>
{
	private Long id;
	private String host;
	private Integer port;
	private String name;
	private String uuid;
	private Date created;
	private Date lastUpdated;
	private List<DiscoverableServiceCapability> capabilities;
	
	public DiscoverableService() {
		this.created = new Date();
		this.lastUpdated = new Date();
		this.capabilities = new ArrayList<DiscoverableServiceCapability>();
		this.uuid = new AgaveUUID(UUIDType.WORKER).toString();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getId()
	 */
	@Override
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getName()
	 */
	@Override
	@Column(name = "name", nullable = true, unique = false, length = 128)
	public String getName()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getHost()
	 */
	@Override
	@Column(name = "host", nullable = false, length = 128)
	public String getHost()
	{
		return host;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setHost(java.lang.String)
	 */
	@Override
	public void setHost(String host)
	{
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getPort()
	 */
	@Override
	@Column(name = "port")
	public Integer getPort()
	{
		return port;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setPort(java.lang.Integer)
	 */
	@Override
	public void setPort(Integer port)
	{
		this.port = port;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getCapabilities()
	 */
	@Override
	@OneToMany(cascade = {CascadeType.ALL}, fetch=FetchType.EAGER, orphanRemoval=true)
	public List<DiscoverableServiceCapability> getCapabilities()
	{
		return capabilities;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setCapabilities(java.util.List)
	 */
	@Override
	public void setCapabilities(List<DiscoverableServiceCapability> capabilities)
	{
		this.capabilities = capabilities;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getUuid()
	 */
	@Override
	@Column(name = "uuid", nullable = false, length = 128)
	public String getUuid()
	{
		return uuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setUuid(java.lang.String)
	 */
	@Override
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getLastUpdated()
	 */
	@Override
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setLastUpdated(java.util.Date)
	 */
	@Override
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#getCreated()
	 */
	@Override
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated() {
		return created;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#setCreated(java.util.Date)
	 */
	@Override
	public void setCreated(Date created) {
		this.created = created;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#hasExclusivity(org.iplantc.service.common.discovery.ServiceCapability)
	 */
	@Override
	public boolean hasExclusivity(DiscoverableServiceCapability capability) 
	{
		for (ServiceCapability existingCapability: getCapabilities()) 
		{
			if (existingCapability.invalidates(capability)) {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.model.DiscoverableService#toJSON()
	 */
	@Override
	public String toJSON()
	{
		ObjectMapper mapper = new ObjectMapper();
		
		ObjectNode json = mapper.createObjectNode()
				.put("id", getUuid())
				.put("name", getName())
				.put("host", getHost())
				.put("port", getPort());
		
		ArrayNode capabilitiesArray = json.putArray("capabilities");
		for(ServiceCapability capability: getCapabilities()) {
			capabilitiesArray.add(mapper.valueToTree(capability));
		}
		
		json.put("lastUpdated", new DateTime(getLastUpdated()).toString())
			.put("created", new DateTime(getCreated()).toString());
		
		return json.toString();
	}

}