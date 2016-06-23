package org.iplantc.service.metadata.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.dao.MetadataSchemaDao;
import org.iplantc.service.metadata.model.validation.constraints.SchemaExists;

public class SchemaExistsValidator implements ConstraintValidator<SchemaExists, Object> {
    
    private static final Logger log = Logger.getLogger(SchemaExistsValidator.class);
    
    @Override
    public void initialize(final SchemaExists constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object target, final ConstraintValidatorContext constraintContext) {
        
//        if (StringUtils.isEmpty((String)target)) {
//            return false;
//        }
//        
//        try {
//            return (MetadataSchemaDao.getInstance().findByUuidAndTenant((String) target, TenancyHelper.getCurrentTenantId()) != null);
//        } 
//        catch (Exception e) {
//            log.error("Failed to fetch metadata schema", e);
//            constraintContext.disableDefaultConstraintViolation();
//            constraintContext.buildConstraintViolationWithTemplate( 
//                        "Invalid uuid value")
//                    .addConstraintViolation();
//        }
//        
//        return false;
    	return true;
    }
}
