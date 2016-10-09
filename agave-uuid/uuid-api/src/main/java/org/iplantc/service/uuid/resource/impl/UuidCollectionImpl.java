/**
 *
 */
package org.iplantc.service.uuid.resource.impl;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.uuid.exceptions.UUIDResolutionException;
import org.iplantc.service.uuid.resource.UuidCollection;
import org.restlet.Request;
import org.restlet.data.Header;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
	
	@GET
	@Override
	public Response searchUuid(@QueryParam("uuids") String uuids, 
								@QueryParam("expand") String expand, 
								@QueryParam("filter") String filter) {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.UuidLookup);
	    
    	if (StringUtils.isEmpty(uuids)) {
    		return Response.ok().entity(new AgaveSuccessRepresentation("[]")).build();
    	}
    	else {
    		ObjectMapper mapper = new ObjectMapper();
			
    		ArrayNode jsonArray = mapper.createArrayNode();
    		String message = null;
    		Boolean expandResources = BooleanUtils.toBoolean(expand);
    		
    		for (String uuid: StringUtils.split(uuids, ",")) {
	    		try {
			    	AgaveUUID agaveUuid = getAgaveUUIDInPath(uuid);
			    	
			    	if (expandResources) {
			    		
			    		JsonNode response = fetchResource(TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference()), filter);
			    		jsonArray.add(response);
			    	}
			    	else {
			
						ObjectNode json = mapper.createObjectNode();
							json.put("uuid", uuid);
							json.put("type", agaveUuid.getResourceType().name().toLowerCase());
			
						ObjectNode linksObject = mapper.createObjectNode();
			      		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
			      							.put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference())));
			
			      		json.set("_links", linksObject);
			      		
			      		jsonArray.add(json);
			    	}
			    }
			    catch (UUIDException e) {
			    	jsonArray.add(mapper.createObjectNode());
			    	message = "Failed to resolve " + uuid + ". " + e.getMessage();
//				    	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
				}
	    		catch (UUIDResolutionException e) {
	    			log.error(e);
	    		}
			    catch (Throwable e) {
	    			log.error("Failed to resolve resource for uuid " + uuid, e);
	    			message = "Failed to resolve resource for uuid " + uuid + ". " + e.getMessage();
	    			jsonArray.add(mapper.createObjectNode());
//		    			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
//		                      "Failed to resolve resource for uuid " + uuid, e);
	    		}
    		}
    		
    		return Response.ok().entity(new AgaveSuccessRepresentation(message, jsonArray.toString())).build();
    	}
	}
}
