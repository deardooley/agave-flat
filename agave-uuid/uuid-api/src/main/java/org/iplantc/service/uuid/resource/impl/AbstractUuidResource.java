package org.iplantc.service.uuid.resource.impl;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public class AbstractUuidResource extends AbstractAgaveResource {

	/**
     *
     */
	public AbstractUuidResource() {
	}

	/**
	 * Creates a {@link AgaveUUID} object for the uuid in the URL or throws an
	 * exception that can be re-thrown from the route method.
	 * 
	 * @param uuid
	 * @return AgaveUUID object referenced in the path
	 * @throws AgaveUUID
	 * @throws ResourceException
	 */
	protected AgaveUUID getAgaveUUIDInPath(String uuid) throws UUIDException {
		
		AgaveUUID agaveUuid = null;
		if (StringUtils.isEmpty(uuid)) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No uuid provided");
		} else {

			try {
				agaveUuid = new AgaveUUID(uuid);
			} catch (UUIDException e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No resource found matching " + uuid, e);
			} catch (Throwable e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Unable to resolve uuid " + uuid, e);
			}
		}

		return agaveUuid;
	}

	/**
	 * Convenience class to log usage info per request
	 * 
	 * @param action
	 */
	protected void logUsage(AgaveLogServiceClient.ActivityKeys activityKey) {
		AgaveLogServiceClient.log(getServiceKey().name(), activityKey.name(),
				getAuthenticatedUsername(), "", org.restlet.Request
						.getCurrent().getClientInfo().getUpstreamAddress());
	}

	protected ServiceKeys getServiceKey() {
		return AgaveLogServiceClient.ServiceKeys.UUID02;
	}

}
