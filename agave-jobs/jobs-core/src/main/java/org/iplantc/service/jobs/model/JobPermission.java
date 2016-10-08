/**
 * 
 */
package org.iplantc.service.jobs.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.iplantc.service.systems.Settings;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * Class to represent individual shared permissions for jobs
 * 
 * @author dooley
 * 
 */
@Entity
@Table(name = "job_permissions", uniqueConstraints=
@UniqueConstraint(columnNames={"job_id","username"}))
@FilterDef(name="jobPermissionTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="jobPermissionTenantFilter", condition="tenant_id=:tenantId"))
public class JobPermission {

	private Long				id;
	private Long				jobId;
	private String				username;
	private PermissionType		permission;
	private String				tenantId;
	private Date				lastUpdated;

	public JobPermission() {
		lastUpdated = new DateTime().toDate();
		tenantId = TenancyHelper.getCurrentTenantId();
	}

	public JobPermission(Job job, String username, PermissionType permissionType)
	throws JobException
	{
		this();
		setJobId(job.getId());
		setUsername(username);
		setPermission(permissionType);
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
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
	 * @return the jobId
	 */
	@Column(name = "job_id", nullable = false)
	public Long getJobId()
	{
		return jobId;
	}

	/**
	 * @param jobId
	 *            the jobId to set
	 */
	public void setJobId(Long jobId)
	{
		this.jobId = jobId;
	}

	/**
	 * @return the username
	 */
	@Column(name = "username", nullable = false, length = 32)
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 * @throws JobException 
	 */
	public void setUsername(String username) throws JobException
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new JobException("'permission.username' must be less than 32 characters");
		}
		
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "permission", nullable = false, length = 16)
	public PermissionType getPermission()
	{
		return permission;
	}

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setPermission(PermissionType permission)
	{
		this.permission = permission;
	}

	/**
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastUpdated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	public boolean canRead()
	{
		return permission.canRead();
	}

	public boolean canWrite()
	{
		return permission.canWrite();
	}
	
	public boolean canExecute()
	{
		return permission.canExecute();
	}

	public String toJSON(Job job) throws JSONException 
	{
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(username)
			.key("internalUsername").value(job.getInternalUsername())
			.key("permission").object()
				.key("read").value(canRead())
				.key("write").value(canWrite())
			.endObject()
			.key("_links").object()
	        	.key("self").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid() + "/pems/" + username)
	        	.endObject()
	        	.key("parent").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())
	        	.endObject()
	        	.key("profile").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + username)
	        	.endObject()
	        .endObject()
        .endObject();
			
		return writer.toString();
	}
	
	public String toString()
	{
		return "[" + jobId + "] " + username + " " + permission;
	}

	public boolean equals(Object o)
	{
		if (o instanceof JobPermission) { 
			return ( 
				( (JobPermission) o ).jobId.equals(jobId) &&
				( (JobPermission) o ).username.equals(username) && 
				( (JobPermission) o ).permission.equals(permission) ); 
		}
		return false;
	}

}
