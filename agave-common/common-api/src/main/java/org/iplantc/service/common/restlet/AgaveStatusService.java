package org.iplantc.service.common.restlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.representation.AgaveErrorRepresentation;
import org.iplantc.service.common.representation.AgaveRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.service.StatusService;

public class AgaveStatusService extends StatusService 
{
    
    private static final Logger log = Logger.getLogger(AgaveStatusService.class);
    
	/* (non-Javadoc)
	 * @see org.restlet.service.StatusService#getStatus(java.lang.Throwable, org.restlet.data.Request, org.restlet.data.Response)
	 */
	@Override
	public Status toStatus(Throwable throwable, Request request,
			Response response)
	{
	    if (throwable == null) {
	        return response.getStatus();
	    }
	    else if (throwable instanceof ResourceException) {
	        return super.toStatus(throwable, request, response);
	    } else {
	        return new Status(Status.SERVER_ERROR_INTERNAL, throwable, throwable.getMessage(),
	                Status.SERVER_ERROR_INTERNAL.getDescription());
	    }
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.service.StatusService#getRepresentation(org.restlet.data.Status, org.restlet.data.Request, org.restlet.data.Response)
	 */
	@Override
	public Representation toRepresentation(Status status,
			Request request, Response response)
	{
		try {
			Representation currentRepresentation = response.getEntity();
			
			if (currentRepresentation instanceof AgaveRepresentation) {
				return currentRepresentation;
			} else if (status.isSuccess()) {
				return new AgaveSuccessRepresentation();
			} else if (status.getThrowable() instanceof ResourceException) {
			    return new AgaveErrorRepresentation(status.getThrowable().getMessage());
			} else if (response.getStatus().getThrowable() instanceof ResourceException) {
			    return new AgaveErrorRepresentation(response.getStatus().getThrowable().getMessage());
			} else {
				String message = null;
				if (status.getCode() == 401) {
					if (request.getChallengeResponse() == null) {
						message = "Permission denied. No authentication credentials found.";
					} else {
						message = "Permission denied. Invalid authentication credentials";
					}
				} else {
				    Status defaultStatusForCode = Status.valueOf(status.getCode());
				    
				    if (StringUtils.equals(defaultStatusForCode.getDescription(), status.getDescription())) {
				        if (StringUtils.equals(defaultStatusForCode.getReasonPhrase(), status.getReasonPhrase())) {
				            if (response.getStatus().getThrowable() != null) {
				                message = response.getStatus().getThrowable().getMessage();
				            } else {
				                message = status.getDescription();
				            }
				        } else {
				            message = status.getReasonPhrase();
				        }
				    } else {
				        message = status.getDescription();
				    }
				    log.error(message, status.getThrowable());
				}
				
                
				return new AgaveErrorRepresentation(message);
			}
		} finally {
			//try { HibernateUtil.closeSession(); } catch(Exception e) {}
		}
	}
}