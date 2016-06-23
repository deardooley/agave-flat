package org.iplantc.service.metadata.model.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.model.validation.ValidAgaveUUIDValidator;

@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAgaveUUIDValidator.class)
@Documented
public @interface ValidAgaveUUID {

    String message() default "UUID is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    
    /**
     * @return the type that this uuid should comply with or null
     */
    UUIDType type();
    
    /**
     * @return the value of the field or method level annotation
     */
    String value();

}