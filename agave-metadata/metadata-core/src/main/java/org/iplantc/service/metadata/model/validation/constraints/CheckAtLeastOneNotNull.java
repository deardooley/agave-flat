package org.iplantc.service.metadata.model.validation.constraints;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = CheckAtLeastOneNotNull.CheckAtLeastOneNotNullValidator.class)
@Documented
public @interface CheckAtLeastOneNotNull {

    
    String message() default "At least one of %s must be a non null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] fieldNames();

    public static class CheckAtLeastOneNotNullValidator implements
            ConstraintValidator<CheckAtLeastOneNotNull, Object> {
    	
    	private static final Logger log = Logger.getLogger(CheckAtLeastOneNotNull.class);

        private String[] fieldNames;

        public void initialize(CheckAtLeastOneNotNull constraintAnnotation) {
            this.fieldNames = constraintAnnotation.fieldNames();
        }

        public boolean isValid(Object object, ConstraintValidatorContext constraintContext) {

            if (object == null) return true;

            try {

                for (String fieldName : fieldNames) {
                    Object property = PropertyUtils.getProperty(object, fieldName);

                    if (property != null) return true;
                }

                return false;

            } catch (Exception e) {
                log.error("Failed to validate that one of " + fieldNames.toString()
                        + "was not null", e);
                return false;
            }
        }

    }

}