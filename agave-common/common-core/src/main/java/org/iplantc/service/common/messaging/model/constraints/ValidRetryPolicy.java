package org.iplantc.service.common.messaging.model.constraints;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.iplantc.service.common.messaging.model.validation.ValidRetryPolicyValidator;

@Target( { TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidRetryPolicyValidator.class)
@Documented
public @interface ValidRetryPolicy {

    String message() default "{org.iplantc.service.common.validation.ValidNotifiationPolicy.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}