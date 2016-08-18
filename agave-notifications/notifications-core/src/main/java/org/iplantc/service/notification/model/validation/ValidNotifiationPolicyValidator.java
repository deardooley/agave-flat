package org.iplantc.service.notification.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.notification.exceptions.NotificationPolicyViolationException;
import org.iplantc.service.notification.model.constraints.ValidNotifiationPolicy;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;

public class ValidNotifiationPolicyValidator implements ConstraintValidator<ValidNotifiationPolicy, Object> {
	
	public void initialize(ValidNotifiationPolicy constraintAnnotation) {
        
    }

    public boolean isValid(Object notificationPolicy, ConstraintValidatorContext constraintContext) {

        boolean isValid = true;
        
        try
		{
        	final String sRetryStrategyType = BeanUtils.getProperty(notificationPolicy, "retryStrategyType");
        	
            final int retryRate = NumberUtils.toInt(BeanUtils.getProperty(notificationPolicy, "retryRate"));
        	final int retryLimit = NumberUtils.toInt(BeanUtils.getProperty(notificationPolicy, "retryLimit"));
        	final int retryDelay = NumberUtils.toInt(BeanUtils.getProperty(notificationPolicy, "retryDelay"));
        	RetryStrategyType retryStrategyType = null;
        	
//			if (!StringUtils.isEmpty(sRetryStrategyType) && ) {
//        		throw new NotificationPolicyViolationException("{Invalid notification policy retryStrategy. Strategy must be one of: NONE, IMMEDIATE, DELAYED, or EXPONENTIAL}");
//        	} else {
        	if (!StringUtils.isEmpty(sRetryStrategyType)) {
        		retryStrategyType = RetryStrategyType.valueOf(sRetryStrategyType.toUpperCase());
        		
        		if ((retryStrategyType == RetryStrategyType.NONE || 
        				retryStrategyType == RetryStrategyType.IMMEDIATE) && retryDelay > 0) {
        			throw new NotificationPolicyViolationException("Invalid notification policy retryDelay. retryDely cannot be greater than zero when retryStrategy is NONE or IMMEDIATE");
        		} else if (retryStrategyType == RetryStrategyType.DELAYED && retryDelay < 1) {
        			throw new NotificationPolicyViolationException("Invalid notification policy retryDelay. retryDely must be a positive integer value when retryStrategy is DELAYED");
        		}
        	}
		}
        catch (IllegalArgumentException e) {
        	isValid = false;
			
			constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( "Unknown notification policy retryStrategy. Strategy must be one of: NONE, IMMEDIATE, DELAYED, or EXPONENTIAL")
                .addConstraintViolation(); 	
        }
		catch (NotificationPolicyViolationException e) {
			isValid = false;
			
			constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( e.getMessage() )
                .addConstraintViolation();
		}
		catch (Throwable e) {
			isValid = false;
		}
        
        return isValid;
    }

}