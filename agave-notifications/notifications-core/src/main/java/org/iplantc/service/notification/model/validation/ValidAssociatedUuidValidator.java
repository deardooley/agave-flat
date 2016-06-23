package org.iplantc.service.notification.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.constraints.ValidAssociatedUuid;
import org.iplantc.service.notification.util.ServiceUtils;

public class ValidAssociatedUuidValidator implements ConstraintValidator<ValidAssociatedUuid, Object> {
	
	public void initialize(ValidAssociatedUuid constraintAnnotation) {
        
    }

    public boolean isValid(Object notification, ConstraintValidatorContext constraintContext) {

        boolean isValid = true;
        
        try
		{
        	final String associatedUuid = BeanUtils.getProperty(notification, "associatedUuid");
            final String callbackUrl = BeanUtils.getProperty(notification, "callbackUrl");
        	final String tenantId = BeanUtils.getProperty(notification, "tenantId");
        	final String owner = BeanUtils.getProperty(notification, "owner");
            

			// check for wildcard subscriptions
			if (StringUtils.equalsIgnoreCase(associatedUuid, "*")) {
				// websockets are good for the firehose and tenant admins, but not for GA
				if (ServiceUtils.isValidRealtimeChannel(callbackUrl, tenantId)) {
					// websocks get the firehose
				}
				else if (AuthorizationHelper.isTenantAdmin(owner)) {
					// as can tenant admins
				}
				// everyone else gets sawbucks
				else {
					throw new NotificationException("Invalid associatedUuid. " +
	    					"Wildcard notification subscriptions are reserved for administrators.");
				}
			}
//			else if (StringUtils.isEmpty(associatedUuid)) {
//				throw new NotificationException("No associated entity provided. " +
//    					"Please provide an associatedUuid for which this notification should apply.");
//			}
			// check that the associated uuid type is valid
			else {
				AgaveUUID agaveUUID = new AgaveUUID(associatedUuid);
	    		if (StringUtils.isEmpty(agaveUUID.getObjectReference())) {
	    			throw new NotificationException("Invalid associatedUuid. The provided uuid was not recognized "
	    					+ "as a valid resource uuid type. Please provide an associatedUuid for which this notification should apply.");
	    		}
	    		else {
	    			// boat drinks
	    		}
			}
		}
        catch (UUIDException e) {
        	isValid = false;
			
			constraintContext.disableDefaultConstraintViolation();
        	constraintContext
                .buildConstraintViolationWithTemplate("Invalid associatedUuid. The provided uuid was not recognized "
    					+ "as a valid resource uuid type. Please provide an associatedUuid for which this notification should apply.")
                .addConstraintViolation();
        }
		catch (NotificationException e) {
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