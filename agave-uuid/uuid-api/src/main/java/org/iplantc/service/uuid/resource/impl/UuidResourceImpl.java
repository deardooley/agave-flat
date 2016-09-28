/**
 *
 */
package org.iplantc.service.uuid.resource.impl;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.uuid.resource.UuidResource;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Concrete implementation class resolving a given serialized
 * {@link AgaveUUID} to a valid type and resource url.
 * @author dooley
 *
 */
@Path("{entityId}")
public class UuidResourceImpl extends AbstractUuidResource implements
		UuidResource {

	private static final Logger log = Logger.getLogger(UuidResourceImpl.class);

	public UuidResourceImpl() {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.iplantc.service.tags.resource.UuidCollection#getUuid()
	 */
	@Get
	@Override
	public Response getUuid(@PathParam("uuid") String uuid) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.UuidLookup);

		try
		{
			AgaveUUID agaveUuid = getAgaveUUIDInPath(uuid);

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
				json.put("uuid", uuid);
				json.put("type", agaveUuid.getResourceType().name());

			ObjectNode linksObject = mapper.createObjectNode();
      		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
      							.put("href", agaveUuid.getObjectReference()));

			return Response.ok(new AgaveSuccessRepresentation(json.toString())).build();
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			log.error("Failed to resolve resource for uuid " + uuid, e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                  "Failed to resolve resource for uuid " + uuid, e);
		}
	}
}
