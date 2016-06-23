package org.iplantc.service.common.restlet;

import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.service.StatusService;

public class IPlantStatusService extends StatusService 
{

	/* (non-Javadoc)
	 * @see org.restlet.service.StatusService#getStatus(java.lang.Throwable, org.restlet.data.Request, org.restlet.data.Response)
	 */
	@Override
	public Status getStatus(Throwable throwable, Request request,
			Response response)
	{
		return response.getStatus();
	}

	@Override
	public Representation getRepresentation(Status status, Request request, Response response)
	{
		try {
			Representation currentRepresentation = response.getEntity();
			if (currentRepresentation instanceof IplantRepresentation) {
				return currentRepresentation;
			} else if (status.isSuccess()) {
				return new IplantSuccessRepresentation();
			} else {
				String message = null;
				if (status.getCode() == 401) {
					if (request.getChallengeResponse() == null) {
						message = "Permission denied. No authentication credentials found.";
					} else {
						message = "Permission denied. Invalid authentication credentials";
					}
				} else {
					message = status.getDescription();
				}
				return new IplantErrorRepresentation(message);
			}
		} finally {
			try { HibernateUtil.closeSession(); } catch(Exception e) {}
		}
	}
	
}