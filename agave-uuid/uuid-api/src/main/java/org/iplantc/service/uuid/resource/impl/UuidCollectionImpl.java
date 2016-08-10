/**
 *
 */
package org.iplantc.service.uuid.resource.impl;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.uuid.resource.UuidCollection;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Path("")
public class UuidCollectionImpl extends AbstractUuidCollection implements UuidCollection {

	private static final Logger log = Logger.getLogger(UuidCollectionImpl.class);

	public UuidCollectionImpl() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.uuid.resource.UuidCollection#createUuid(java.lang.String, java.util.List)
	 */
	@Override
	public Response createUuid(Representation input) {

		logUsage(AgaveLogServiceClient.ActivityKeys.UuidGen);

	    try
	    {
	    	JsonNode contentJson = getPostedContentAsJsonNode(input);
			UUIDType type = null;
	    	if (contentJson.hasNonNull("type")) {
				try {
					String stype = contentJson.get("type").asText();
					type = UUIDType.getInstance(stype);
				}
				catch (UUIDException e) {
					throw e;
				} 
				catch (Exception e) {
					throw new UUIDException("Invalid resource type", e);
				}
			}
			else {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"No resource type provided. Please provide a valid resource " +
					"type for which to generate a UUID.");
			}
	
			ObjectNode json = new ObjectMapper().createObjectNode();
			json.put("uuid", new AgaveUUID(type).toString());
			json.put("type", type.name());
			
			return Response.ok().entity(new AgaveSuccessRepresentation(json.toString())).build();
	    }
	    catch (UUIDException e) {
	    	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
		}
	    catch (Exception e) {
	    	log.error("Failed to generate uuid", e);
	    	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
	                "Failed to generate uuid.", e);
	    
		}
	}
}
