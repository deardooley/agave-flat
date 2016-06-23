package org.iplantc.service.notification.model.validation;

import java.net.URI;
import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.constraints.ValidCallbackUrl;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;
import org.iplantc.service.notification.util.ServiceUtils;

public class ValidCallbackUrlValidator implements ConstraintValidator<ValidCallbackUrl, String> {
	
	public void initialize(ValidCallbackUrl constraintAnnotation) {
        
    }

    public boolean isValid(String callbackUrl, ConstraintValidatorContext constraintContext) {

        boolean isValid = true;
        
        callbackUrl = StringUtils.trimToEmpty(callbackUrl);
        
		try
		{
			NotificationCallbackProviderType.getInstanceForUri(callbackUrl);
		}
		catch (BadCallbackException e) {
			isValid = false;
			
			constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( "Invalid callback url value, " + callbackUrl + ". Valid callback urls may be an email address, "
        				+ "phone number, web url, or agave url.")
                .addConstraintViolation();
		}
		catch (Throwable e) {
			isValid = false;
		}
        
        return isValid;
    }

}