package org.iplantc.service.common.arn.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.iplantc.service.common.arn.validation.ValidRegionValidator;

@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidRegionValidator.class)
@Documented
public @interface ValidRegion {

    String message() default "{org.iplantc.service.common.arn.validation.ValidRegion.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}