/**
 *
 */
package org.iplantc.service.uuid.resource.impl;

import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.uuid.exceptions.UUIDResolutionException;
import org.iplantc.service.uuid.resource.UuidResource;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Concrete implementation class resolving a given serialized
 * {@link AgaveUUID} to a valid type and resource url.
 * @author dooley
 *
 */
@Path("{uuid}")
public class UuidResourceImpl extends AbstractUuidResource implements
		UuidResource {

	private static final Logger log = Logger.getLogger(UuidResourceImpl.class);

	public UuidResourceImpl() {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.iplantc.service.tags.resource.UuidResource#getUuid()
	 */
	@Get
	@Override
	public Response getUuid(@PathParam("uuid") String uuid, 
			@QueryParam("expand") String expand, 
			@QueryParam("filter") String filter)
	throws Exception 
	{
		
		logUsage(AgaveLogServiceClient.ActivityKeys.UuidLookup);
		
		Boolean expandResources = BooleanUtils.toBoolean(expand);
		
		try
		{
			AgaveUUID agaveUuid = getAgaveUUIDInPath(uuid);
			
			// if expand=true, return the resource representation for the uuid
			if (expandResources) {
				
				Map<String,String> headerMap = getRequestAuthCredentials();
				
				String resourceUrl = TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference());
				
				// resolve the file media url to the file listing url as needed
				JsonNode json = null;
				if (UUIDType.FILE == agaveUuid.getResourceType()) {
					resourceUrl = StringUtils.replaceOnce(resourceUrl,  "/media/", "/listings/") + "?limit=1";
				}
			
				json = fetchResource(resourceUrl, filter, headerMap);
				
	    		return Response.ok(new AgaveSuccessRepresentation(json.toString())).build();
	    	}
			// otherwise, just return the metadata
	    	else {
	    		
		    	ObjectMapper mapper = new ObjectMapper();
				ObjectNode json = mapper.createObjectNode();
					json.put("uuid", uuid);
					json.put("type", agaveUuid.getResourceType().name().toLowerCase());
	
				ObjectNode linksObject = mapper.createObjectNode();
	      		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
	      							.put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference())));
	
	      		json.set("_links", linksObject);
	      		
				return Response.ok(new AgaveSuccessRepresentation(json.toString())).build();
	    	}
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (UUIDResolutionException e) {
			log.error(e);
			throw new ResourceException(Status.SERVER_ERROR_BAD_GATEWAY,
                  e.getMessage(), e);
		}
		catch (Throwable e) {
			log.error("Failed to resolve resource for uuid " + uuid, e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                  "Failed to resolve resource for uuid " + uuid, e);
		}
	}
}
