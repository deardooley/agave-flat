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
import org.iplantc.service.jobs.model.enumerations.JobDependencyCondition;
import org.iplantc.service.jobs.model.enumerations.JobDependencyOperator;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.iplantc.service.systems.Settings;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * Class to represent individual shared permissions for jobs
 * 
 * @author dooley
 * 
 */
//@Entity
//@Table(name = "job_dependencies")
//@FilterDef(name="jobDependencyTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
//@Filters(@Filter(name="jobDependencyTenantFilter", condition="tenant_id=:tenantId"))
public class JobDependency {

	private Long				id;
	private Long				jobId;
	private String				referenceUuid; // uuid of referenced object
	private JobDependencyCondition	condition;
	private JobDependencyOperator	operator;
	private String				value;	
	private String				tenantId;
	private Date				lastUpdated = new Date();

	public JobDependency() {
		tenantId = TenancyHelper.getCurrentTenantId();
	}

	public JobDependency(Job job, String referenceUuid, JobDependencyCondition condition, JobDependencyOperator operator, String value)
	throws JobException
	{
		this();
		setJobId(job.getId());
		this.referenceUuid = referenceUuid;
		this.condition = condition;
		this.operator = operator;
		this.value = value;
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
	 * @return the referenceUuid
	 */
	@Column(name = "permission", nullable = false, length = 64)
	public String getReferenceUuid()
	{
		return referenceUuid;
	}

	/**
	 * @param referenceUuid the referenceUuid to set
	 */
	public void setReferenceUuid(String referenceUuid)
	{
		this.referenceUuid = referenceUuid;
	}

	/**
	 * @return the condition
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "condition", nullable = false, length = 16)
	public JobDependencyCondition getCondition()
	{
		return condition;
	}

	/**
	 * @param condition the condition to set
	 */
	public void setCondition(JobDependencyCondition condition)
	{
		this.condition = condition;
	}

	/**
	 * @return the operator
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "operator", nullable = false, length = 16)
	public JobDependencyOperator getOperator()
	{
		return operator;
	}

	/**
	 * @param operator the operator to set
	 */
	public void setOperator(JobDependencyOperator operator)
	{
		this.operator = operator;
	}

	/**
	 * @return the value
	 */
	@Column(name = "value", nullable = false, length = 16)
	public String getValue()
	{
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value)
	{
		this.value = value;
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
//
//	public String toJSON(Job job) throws JSONException 
//	{
//		JSONWriter writer = new JSONStringer();
//		writer.object()
//			.key("username").value(username)
//			.key("internalUsername").value(job.getInternalUsername())
//			.key("permission").object()
//				.key("read").value(canRead())
//				.key("write").value(canWrite())
//			.endObject()
//			.key("_links").object()
//	        	.key("self").object()
//	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid() + "/pems/" + username)
//	        	.endObject()
//	        	.key("parent").object()
//	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())
//	        	.endObject()
//	        	.key("profile").object()
//	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + username)
//	        	.endObject()
//	        .endObject()
//        .endObject();
//			
//		return writer.toString();
//	}
//	
//	public String toString()
//	{
//		return "[" + jobId + "] " + username + " " + permission;
//	}
//
//	public boolean equals(Object o)
//	{
//		if (o instanceof JobPermission) { 
//			return ( 
//				( (JobPermission) o ).jobId.equals(jobId) &&
//				( (JobPermission) o ).username.equals(username) && 
//				( (JobPermission) o ).permission.equals(permission) ); 
//		}
//		return false;
//	}

}
