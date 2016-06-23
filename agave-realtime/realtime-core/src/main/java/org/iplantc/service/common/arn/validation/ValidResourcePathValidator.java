package org.iplantc.service.common.arn.validation;

import java.lang.reflect.InvocationTargetException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.arn.AgaveResourceName;
import org.iplantc.service.common.arn.AgaveServiceType;
import org.iplantc.service.common.arn.constraints.ValidResourcePath;

/**
 * Validates the {@link AgaveResourceName#getResourcePath} for the annotated
 * class instance.
 *  
 * @author dooley
 *
 */
public class ValidResourcePathValidator implements ConstraintValidator<ValidResourcePath, Object> {
	
	private static final Logger log = Logger.getLogger(ValidResourcePathValidator.class);
	
	public void initialize(ValidResourcePath constraintAnnotation) {
        
    }

	@Override
	public boolean isValid(final Object value, ConstraintValidatorContext constraintContext) {

        boolean isValid;
        
        try {
        	final AgaveServiceType serviceType = AgaveServiceType.valueOf((String)BeanUtils.getProperty(value, "service"));
        	final String resourcePath = BeanUtils.getProperty(value, "resourcePath");
            
        	isValid = serviceType.hasResourcePath(resourcePath);
            
        } catch (IllegalArgumentException e) {
        	isValid = false;
            
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			isValid = false;
		}
        
        return isValid;
        
    }
}