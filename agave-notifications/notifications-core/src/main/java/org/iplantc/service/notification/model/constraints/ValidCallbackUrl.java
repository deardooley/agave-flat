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
import org.iplantc.service.notification.model.validation.ValidCallbackUrlValidator;

@NotNull(message = "Invalid url value. Valid notification url are an email address, "
					+ "phone number, or web address to which the API will send notification of the event.")
@NotEmpty(message = "Invalid url value. Valid notification url are an email address, "
					+ "phone number, or web address to which the API will send notification of the event.")
@Size(min=5,
	  max=1024, 
	  message = "Invalid url value. Notification urls must be between {min} and {max} characters long.")
@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCallbackUrlValidator.class)
@Documented
public @interface ValidCallbackUrl {

    String message() default "{org.iplantc.service.common.validation.ValidCallbackUrl.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}