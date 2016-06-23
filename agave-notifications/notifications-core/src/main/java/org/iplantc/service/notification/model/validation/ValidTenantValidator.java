package org.iplantc.service.notification.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.model.constraints.ValidTenant;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;

public class ValidTenantValidator implements ConstraintValidator<ValidTenant, String> {
	
	private static final Logger log = Logger.getLogger(ValidTenantValidator.class);
	
	public void initialize(ValidTenant constraintAnnotation) {
        
    }

    public boolean isValid(String tenantId, ConstraintValidatorContext constraintContext) {

        boolean isValid;
        TenantDao dao = new TenantDao();
        
        try {
			isValid = dao.exists(tenantId);
		} catch (TenantException e) {
			log.error("Unable to validate tenant id during teanant validation check.", e);
			isValid = false;
		}
        
        if(!isValid) {
        	constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( "{org.iplantc.service.common.arn.validation.ValidTenant.message}" )
                .addConstraintViolation();
        }
        
        return isValid;
        
    }

}