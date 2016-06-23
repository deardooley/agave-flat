package org.iplantc.service.metadata.model.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import cz.jirutka.validator.collection.CommonEachValidator;
import cz.jirutka.validator.collection.constraints.EachConstraint;

// common boilerplate
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
// this is important!
@EachConstraint(validateAs = PermissionGrant.class)
@Constraint(validatedBy = CommonEachValidator.class)
public @interface EachPermissionGrant {

    // copy&paste all attributes from Awesome annotation here
    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String someAttribute();
}