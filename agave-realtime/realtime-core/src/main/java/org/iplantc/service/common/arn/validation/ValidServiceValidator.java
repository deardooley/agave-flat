package org.iplantc.service.common.arn.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.arn.AgaveServiceType;
import org.iplantc.service.common.arn.constraints.ValidService;

public class ValidServiceValidator implements ConstraintValidator<ValidService, String> {
	
	private static final Logger log = Logger.getLogger(ValidServiceValidator.class);
	
	public void initialize(ValidService constraintAnnotation) {
        
    }

    public boolean isValid(String serviceName, ConstraintValidatorContext constraintContext) {

        boolean isValid;
        
        try {
        	// verify the serviceName is valid. if no exception is thrown, it is valid
        	AgaveServiceType.valueOf(StringUtils.upperCase(serviceName));
        	isValid = true;
		} catch (Exception e) {
			isValid = false;
		}
        
        if(!isValid) {
        	constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( "{org.iplantc.service.common.arn.validation.ValidService.message}" )
                .addPropertyNode( "service" )
                .addConstraintViolation();
        }
        
        return isValid;
        
    }

}