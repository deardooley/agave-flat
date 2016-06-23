/**
 * 
 */
package org.iplantc.service.common.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.common.Settings;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * POJO for an API tenant. This is used to resolve the tenant id that comes in 
 * as part of the jwt to a valid url that can be used in the HAL responses.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "tenants")
public class Tenant {

	private Long id;
	private String name;
	private String tenantCode;
	private String baseUrl;
	private String contactEmail;
	private String contactName;
	private String status;
	private String uuid;
	private Date lastUpdated;
	private Date created;
	
	public Tenant() {
		this.setUuid(new AgaveUUID(UUIDType.TENANT).toString());
		this.created = new Date();
		this.lastUpdated = new Date();
	}
	
	public Tenant(String tenantCode, String apiBase, String contactEmail, String contactName)
	{
		this();
		this.tenantCode = tenantCode;
		this.baseUrl = apiBase;
		this.contactEmail = contactEmail;
		this.contactName = contactName; 
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
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the tenantCode
	 */
	@Column(name = "tenant_id", nullable = false, unique = true, length = 64)
	public String getTenantCode() {
		return tenantCode;
	}

	/**
	 * @param tenantCode the tenantCode to set
	 */
	public void setTenantCode(String tenantCode) {
		this.tenantCode = tenantCode;
	}

	/**
	 * @return the apiBase
	 */
	@Column(name = "base_url", nullable = false, length = 255)
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * @param apiBase the apiBase to set
	 */
	public void setBaseUrl(String baseUrl) {
		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}
		this.baseUrl = baseUrl;
	}

	/**
	 * @return the contactEmail
	 */
	@Column(name = "contact_email", nullable = true, length = 128)
	public String getContactEmail() {
		return contactEmail;
	}

	/**
	 * @param contactEmail the contactEmail to set
	 */
	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	/**
	 * @return the contactName
	 */
	@Column(name = "contact_name", nullable = true, length = 64)
	public String getContactName() {
		return contactName;
	}

	/**
	 * @param contactName the contactName to set
	 */
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	/**
	 * @return the status
	 */
	@Column(name = "status", nullable = true, length = 64)
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
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
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
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
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public String toString()
	{
		return getTenantCode() + " " + getBaseUrl() + getContactEmail();
	}
	
	public String toJSON()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode()
				.put("id", getUuid())
				.put("code", getTenantCode())
				.put("baseUrl", getBaseUrl())
				.put("contactEmail", getContactEmail())
				.put("contactName", getContactName())
				.put("status", getStatus())
				.put("lastUpdated", new DateTime(getLastUpdated()).toString())
				.put("created", new DateTime(getCreated()).toString());
		
		ObjectNode linksObject = mapper.createObjectNode();
		linksObject.put("self", (ObjectNode)mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TENANTS_SERVICE) + getUuid()));
		linksObject.put("contacts", (ObjectNode)mapper.createObjectNode()
	    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TENANTS_SERVICE) + getUuid() + "/contacts"));
		
		json.put("_links", linksObject);
	    
		return json.toString();
	}
}
