package org.iplantc.service.tags.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.tags.model.constraints.ValidAgaveUuid;
import org.iplantc.service.tags.exceptions.TagException;

public class ValidAgaveUuidValidator implements ConstraintValidator<ValidAgaveUuid, Object> {
	
	public void initialize(ValidAgaveUuid constraintAnnotation) {
        
    }

	@Override
	public boolean isValid(Object uuid, ConstraintValidatorContext constraintContext) {

    	boolean isValid = false;
        
        try {
        	AgaveUUID agaveUUID = new AgaveUUID((String)uuid);
    		if (StringUtils.isEmpty(agaveUUID.getObjectReference())) {
    			isValid = false;
    			throw new TagException("{Invalid associatedUuid. " + uuid + " is not a valid uuid.}");
    		}
    		else {
    			isValid = true;
    		}
        } catch (TagException e) {
        	isValid = false;
        	constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( e.getMessage() )
                .addConstraintViolation();
        } catch (Exception e) {
        	isValid = false;
		}
        
        return isValid;
    }
}