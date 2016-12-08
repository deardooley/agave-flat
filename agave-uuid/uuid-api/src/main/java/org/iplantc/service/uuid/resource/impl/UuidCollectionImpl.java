/**
 *
 */
package org.iplantc.service.uuid.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.uuid.resource.UuidCollection;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

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
					// make uuid type case insensitive
					stype = StringUtils.upperCase(stype);
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
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.uuid.resource.UuidCollection#searchUuid(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response searchUuid(@QueryParam("uuids") String uuids, 
								@QueryParam("expand") String expand, 
								@QueryParam("filter") String filter) {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.UuidLookup);
	    
    	if (StringUtils.isEmpty(uuids)) {
    		return Response.ok().entity(new AgaveSuccessRepresentation("[]")).build();
    	}
    	else {
    		ArrayNode jsonArray = null;
    		String[] uuidArray = StringUtils.split(uuids, ",");
    		
    		if (BooleanUtils.toBoolean(expand)) {
    			jsonArray = resolveAndExpandUuids(uuidArray, filter);
    		}
    		else {
    			jsonArray = resolveUuids(uuidArray);
    		}
    		
    		return Response.ok().entity(new AgaveSuccessRepresentation(jsonArray.toString())).build();
    	}
	}

	/**
	 * Looks up metadata for an array of uuid. 
	 * @param uuids
	 * @return ArrayNode of uuid metadata objects.
	 */
	protected ArrayNode resolveUuids(String[] uuids) {
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode jsonArray = mapper.createArrayNode();
		
		for (String uuid: uuids) {
			try {
		    	AgaveUUID agaveUuid = getAgaveUUIDInPath(uuid);
		    	
				ObjectNode json = mapper.createObjectNode();
					json.put("uuid", uuid);
					json.put("type", agaveUuid.getResourceType().name().toLowerCase());

				ObjectNode linksObject = mapper.createObjectNode();
		  		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
		  							.put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference())));

		  		json.set("_links", linksObject);
		  		
		  		jsonArray.add(json);
		    }
		    catch (UUIDException e) {
		    	jsonArray.add(mapper.createObjectNode()
		    			.put("status", "error")
		    			.put("message", "Failed to resolve uuid " + uuid + ". " + e.getMessage()));
			}
			catch (Throwable e) {
				log.error("Failed to resolve resource for uuid " + uuid, e);
				jsonArray.add(mapper.createObjectNode()
		    			.put("status", "error")
		    			.put("message", "Failed to resolve resource for uuid " + uuid + ". " + e.getMessage()));
			}
		}
		return jsonArray;
	}
    	
    /**
     * Fetches the resource representation of one or more uuid in parallel
     * using an executor service. 
     * @param uuids array of uuid to resolve and expand
     * @param filter the filter to use in the response objects
     * @return
     */
    protected ArrayNode resolveAndExpandUuids(String[] uuids, String filter) {
    	
    	List<Object> urls = new ArrayList<Object>();
    	ObjectMapper mapper = new ObjectMapper();
		
    	for (String uuid: uuids) {
    		try {
		    	AgaveUUID agaveUuid = getAgaveUUIDInPath(uuid);
		    	urls.add(TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference()));
		    	
    		}
    		catch (UUIDException e) {
    			log.error("Failed to resolve resource representations for " + uuid, e);
    			
    			urls.add(mapper.createObjectNode()
		    			.put("status", "error")
		    			.put("message", "Failed to resolve " + uuid + ". " + e.getMessage()));
			}
    	}
    	
    	try {
    		return batchResolveResourceUrls(urls, filter);
    	}
    	catch (Throwable e) {
    		log.error("Failed to resolve resource representations for " + StringUtils.join(uuids, ","), e);
    	
    		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
    				"Unable to resolve resource representations for uuids", e);
    	}
    }
	
	/**
	 * @throws TenantException 
	 */
	/**
	 * @param urls
	 * @param filter
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TenantException
	 */
	protected ArrayNode batchResolveResourceUrls(List<Object> urls, String filter) 
	throws InterruptedException, ExecutionException, TenantException 
	{
		ArrayNode jsonArray = new ObjectMapper().createArrayNode();
		
		Map<String,String> headerMap = getRequestAuthCredentials();
		
		Collection<Callable<JsonNode>> tasks = new ArrayList<>();
		for (Object url : urls) {
			if (url instanceof JsonNode) {
				tasks.add(new ResourceResolutionTask((JsonNode)url, filter, headerMap));
			}
			else {
				tasks.add(new ResourceResolutionTask((String)url, filter, headerMap));
			}
		}
		int numThreads = urls.size() > 4 ? 4 : urls.size(); // max 4 threads
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<Future<JsonNode>> results = executor.invokeAll(tasks);
		for (Future<JsonNode> result : results) {
			JsonNode resourceRepresentation = result.get();
			jsonArray.add(resourceRepresentation);
		}
		executor.shutdown(); // always reclaim resources
		
		return jsonArray;
	}
}
