/**
 * 
 */
package org.iplantc.service.common.arn;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.iplantc.service.common.arn.constraints.ValidRegion;
import org.iplantc.service.common.arn.constraints.ValidResourcePath;
import org.iplantc.service.common.arn.constraints.ValidService;
import org.iplantc.service.common.arn.constraints.ValidTenant;

/**
 * Represents a resource name which corresponds to the Agave Resource Name (ARN) 
 * scheme. 
 * 
 * Valid ARN follow the general scheme:
 * 
 * <pre>agave:tenant:service:region:resourcePath</pre>
 * 
 * @author dooley
 *
 */
@ValidResourcePath
public class AgaveResourceName {
	
	public static final String AGAVE_NAMESPACE = "agave";
	
	/**
	 * The namespace to which this resource name applies. Use "agave" for all
	 * situations.
	 */
	private String namespace;
	
	/**
	 * The tenant code to which this resource corresponds.
	 */
	private String tenant;
	
	/**
	 * The service to which this resource corresponds. Services and API do not 
	 * necessarily correspond 1-to-1. 
	 */
	private String service;
	
	/**
	 * The region in which the resource should be contextualized. In the hosted or a
	 * private deployment of the platform, this should be left empty. For hybrid 
	 * deployments this can differentiate resources within different data centers, 
	 * containers, etc.
	 */
	private String region;
	
	/**
	 * The resource path. This is a service/specific value and could represent 
	 * something as simple as a wildcard for all resources within a service, an agave 
	 * URI representing a path on a system, a uuid, or sub collection or subresource 
	 * off one or more resources.  
	 */
	private String resourcePath;
	
	public AgaveResourceName() {
		setNamespace(AGAVE_NAMESPACE);
	}
	
	public AgaveResourceName(String tenant,
			String service, String region, String resourcePath) {
		this();
		setTenant(tenant);
		setService(service);
		setRegion(region);
		setResourcePath(resourcePath);
	}
	
	public AgaveResourceName(String namespace, String tenant,
			String service, String region, String resourcePath) {
		this();
		setNamespace(namespace);
		setTenant(tenant);
		setService(service);
		setRegion(region);
		setResourcePath(resourcePath);
	}
	
	/**
	 * @return the namespace
	 */
	@NotNull
	@Pattern(regexp="^agave$")
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace the namespace to set
	 */
	private void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * @return the tenant
	 */
	@ValidTenant
	public String getTenant() {
		return tenant;
	}

	/**
	 * @param tenant the tenant to set
	 */
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	/**
	 * @return the service
	 */
	@ValidService
	public String getService() {
		return service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}

	/**
	 * @return the region
	 */
	@ValidRegion
	public String getRegion() {
		return region;
	}

	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * @return the resourcePath
	 */
	public String getResourcePath() {
		return resourcePath;
	}

	/**
	 * @param resourcePath the resourcePath to set
	 */
	
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}
	
	/** 
	 * Returns the serialized form of this ARN following the
	 * general scheme: 
	 * 
	 * <pre>agave:tenant:service:region:resourcePath</pre>
	 */
	@Override
	public String toString() {
		return String.format("%s:%s:%s:%s:%s", 
				getNamespace(),
				getTenant(),
				getService(),
				getRegion(),
				getResourcePath());
	}
}
