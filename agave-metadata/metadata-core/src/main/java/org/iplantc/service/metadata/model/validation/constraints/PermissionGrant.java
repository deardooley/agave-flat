package org.iplantc.service.metadata.model.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.iplantc.service.metadata.model.validation.MetadataSchemaComplianceValidator;

@Target( { ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@CheckAtLeastOneNotNull(fieldNames={"username","group","role"})
@Constraint(validatedBy = MetadataSchemaComplianceValidator.class)
@Documented
public @interface PermissionGrant {

    String message() default "Metadata value is not compliant with the given schema";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    
    /**
     * @return The first field
     */
    String valueField();

    /**
     * @return The second field
     */
    String schemaIdField();

}