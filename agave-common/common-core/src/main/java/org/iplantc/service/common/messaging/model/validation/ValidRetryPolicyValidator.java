package org.iplantc.service.common.messaging.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.exceptions.RetryPolicyViolationException;
import org.iplantc.service.common.messaging.model.constraints.ValidRetryPolicy;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;

public class ValidRetryPolicyValidator implements ConstraintValidator<ValidRetryPolicy, Object> {
	
	public void initialize(ValidRetryPolicy constraintAnnotation) {
        
    }

    public boolean isValid(Object retryPolicy, ConstraintValidatorContext constraintContext) {

        boolean isValid = true;
        
        try
		{
        	final String sRetryStrategyType = BeanUtils.getProperty(retryPolicy, "retryStrategyType");
        	
            final int retryRate = NumberUtils.toInt(BeanUtils.getProperty(retryPolicy, "retryRate"));
        	final int retryLimit = NumberUtils.toInt(BeanUtils.getProperty(retryPolicy, "retryLimit"));
        	final int retryDelay = NumberUtils.toInt(BeanUtils.getProperty(retryPolicy, "retryDelay"));
        	RetryStrategyType retryStrategyType = null;
        	
//			if (!StringUtils.isEmpty(sRetryStrategyType) && ) {
//        		throw new NotificationPolicyViolationException("{Invalid retry policy retryStrategy. Strategy must be one of: NONE, IMMEDIATE, DELAYED, or EXPONENTIAL}");
//        	} else {
        	if (!StringUtils.isEmpty(sRetryStrategyType)) {
        		retryStrategyType = RetryStrategyType.valueOf(sRetryStrategyType.toUpperCase());
        		
        		if ((retryStrategyType == RetryStrategyType.NONE || 
        				retryStrategyType == RetryStrategyType.IMMEDIATE) && retryDelay > 0) {
        			throw new RetryPolicyViolationException("Invalid retry policy retryDelay. retryDely cannot be greater than zero when retryStrategy is NONE or IMMEDIATE");
        		} else if (retryStrategyType == RetryStrategyType.DELAYED && retryDelay < 1) {
        			throw new RetryPolicyViolationException("Invalid retry policy retryDelay. retryDely must be a positive integer value when retryStrategy is DELAYED");
        		}
        	}
		}
        catch (IllegalArgumentException e) {
        	isValid = false;
			
			constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate( "Unknown retry policy retryStrategy. Strategy must be one of: NONE, IMMEDIATE, DELAYED, or EXPONENTIAL")
                .addConstraintViolation(); 	
        }
		catch (RetryPolicyViolationException e) {
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