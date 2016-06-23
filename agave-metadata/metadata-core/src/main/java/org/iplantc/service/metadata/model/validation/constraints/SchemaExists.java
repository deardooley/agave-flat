package org.iplantc.service.metadata.model.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.iplantc.service.metadata.model.validation.SchemaExistsValidator;

@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SchemaExistsValidator.class)
@Documented
public @interface SchemaExists {

    String message() default "No schemata found matching the given id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}