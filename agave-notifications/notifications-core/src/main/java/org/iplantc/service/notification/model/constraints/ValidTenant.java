package org.iplantc.service.notification.model.constraints;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.iplantc.service.notification.model.validation.ValidTenantValidator;

@NotNull
@NotEmpty
@Size(min=3,max=128)
@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidTenantValidator.class)
@Documented
public @interface ValidTenant {

    String message() default "{org.iplantc.service.common.validation.ValidTenant.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}