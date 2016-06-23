/**
 * 
 */
package org.iplantc.service.common.arn;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.arn.exceptions.AgaveResourceNameValidationException;

/**
 * @author dooley
 *
 */
public class AgaveResourceNameBuilder {

	private AgaveResourceName arn;
	
	public static AgaveResourceNameBuilder getInstance() {
		return new AgaveResourceNameBuilder();
	}
	
	/**
	 * Constructs a new {@link AgaveResourceName} from a colon-delimited string
	 * representation of the arn as would be given by the {@link AgaveResourceName#toString()}
	 * method.
	 *  
	 * @param arnString
	 * @return
	 * @throws AgaveResourceNameValidationException if the string is malformed
	 */
	public static AgaveResourceNameBuilder getInstance(String arnString) throws AgaveResourceNameValidationException {
		
		if (StringUtils.isEmpty(arnString)) {
			throw new AgaveResourceNameValidationException("ARN cannot be empty");
		}
//		<pre>agave:tenant_id:[resource_type]:[region]:[resourcePath]</pre>
		String[] tokens = StringUtils.split(arnString, ":");
		if (tokens.length != 5) {
			throw new AgaveResourceNameValidationException("Invalid arn format. Valid arn must conform to the format agave:tenant:service:region:resourcePath");
		} 
		else {
			AgaveResourceNameBuilder builder = new AgaveResourceNameBuilder();
			builder.arn = new AgaveResourceName(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]);
			return builder;
		}
		
	}
	
	/**
	 * Private constructor initializing a new {@link AgaveResourceName} for building. 
	 * 
	 * @param serviceType
	 * @return
	 */
	private AgaveResourceNameBuilder() {
		arn = new AgaveResourceName();
	}
	
	/**
	 * Adds a region the {@link AgaveResourceName} configuration. 
	 * 
	 * @param serviceType
	 * @return
	 */
	public AgaveResourceNameBuilder withRegion(String region) throws AgaveResourceNameValidationException {
		if (region == null) {
			region = "";
		}
		if (StringUtils.isAlpha(region)) {
			arn.setRegion(region);
		}
		
		return this;
	}
	
	/**
	 * Adds a service name to the {@link AgaveResourceName} configuration. This 
	 * call makes a null-safe delegation to {@link #withService(String)} 
	 * 
	 * @param serviceType
	 * @return
	 */
	public AgaveResourceNameBuilder withService(AgaveServiceType serviceType) {
		
		String serviceName = serviceType == null ? null : serviceType.name();
		
		arn.setService(serviceName);
		
		return this;
	}
	
	/**
	 * Adds a service name to the {@link AgaveResourceName} configuration. 
	 * 
	 * @param serviceType
	 * @return
	 */
	public AgaveResourceNameBuilder withService(String serviceName) {
		
		arn.setService(serviceName);
		
		return this;
	}
	
	/**
	 * Adds a tenant identifier to the {@link AgaveResourceName} configuration.
	 * @param tenantCode
	 * @return
	 */
	public AgaveResourceNameBuilder withTenant(String tenantCode) {
		
		arn.setTenant(tenantCode);
		
		return this;
	}
	
	/**
	 * Adds a resourcePath identifier to the {@link AgaveResourceName} configuration.
	 * @param resourcePath
	 * @return
	 */
	public AgaveResourceNameBuilder withResourcePath(String resourcePath) {
		
		arn.setResourcePath(resourcePath);
		
		return this;
	}
	
	/**
	 * Completes the building process of the underlying {@link AgaveResourceName} and validates
	 * the resulting object for completeness. This operation will <em>NOT</em> run permission
	 * checks on the accessibility of the resulting {@link AgaveResourceName}. 
	 * 
	 * @return
	 * @throws AgaveResourceNameValidationException
	 */
	public AgaveResourceName build() throws AgaveResourceNameValidationException {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        try {
        	Set<ConstraintViolation<AgaveResourceName>> violations = validator.validate(arn);
        	if (violations.isEmpty()) {
        		return arn;
        	} else {
        		throw new AgaveResourceNameValidationException(violations.iterator().next().getMessage()); 
        	}
        } catch (AgaveResourceNameValidationException e) {
        	throw e;
        } catch (Exception e) {
        	throw new AgaveResourceNameValidationException("Unexpected error while validating arn.", e); 
        }
	}
}
