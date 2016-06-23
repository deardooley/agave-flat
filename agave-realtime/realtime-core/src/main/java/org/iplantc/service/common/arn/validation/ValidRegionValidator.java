package org.iplantc.service.common.arn.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.arn.constraints.ValidRegion;

public class ValidRegionValidator implements ConstraintValidator<ValidRegion, String> {
	
	private static final Logger log = Logger.getLogger(ValidRegionValidator.class);
	
	public void initialize(ValidRegion constraintAnnotation) {
        
    }

    public boolean isValid(String regionName, ConstraintValidatorContext constraintContext) {

        boolean isValid = StringUtils.isEmpty(regionName);
        
        return isValid;
        
    }

}