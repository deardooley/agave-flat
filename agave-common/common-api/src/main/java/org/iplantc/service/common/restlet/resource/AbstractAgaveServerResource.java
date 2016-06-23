package org.iplantc.service.common.restlet.resource;

import static org.restlet.data.MediaType.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * @author dooley
 *
 */
public class AbstractAgaveServerResource extends ServerResource {
	
	private static final Logger log = Logger
			.getLogger(AbstractAgaveServerResource.class);
	
	protected Form getQueryParameters() {
		return Request.getCurrent().getResourceRef().getQueryAsForm();
	}
	/**
	 * Should the response be pretty printed.
	 * @return
	 */
	public boolean isPrettyPrint() {
		String pretty = getQueryParameters().getFirstValue("pretty");
		return Boolean.parseBoolean(pretty);
	}
	
	/**
	 * Returns the max number of items to return.
	 * @return 
	 */
	public int getLimit() {
		String count = getQueryParameters().getFirstValue("limit");
		if (NumberUtils.isDigits(count)) {
			return Integer.parseInt(count);
		} else {
			return Settings.DEFAULT_PAGE_SIZE;
		}
	}
	
	/**
	 * Returns the number of items to offset the response set.
	 * @return
	 */
	public int getOffset() {
		String offset = getQueryParameters().getFirstValue("offset");
		return NumberUtils.toInt(offset);
	}
	
	/**
	 * Returns the internal username from the request headers
	 * @return
	 */
	public String getInternalUsername() {
		return (String)Request.getCurrent().getAttributes().get("internalUsername");
	}
	
	public boolean isDebugjwt() {
		String pretty = getQueryParameters().getFirstValue("debugjwt");
		return Boolean.parseBoolean(pretty);
    }

    public boolean isForceDownload() {
    	String pretty = getQueryParameters().getFirstValue("force");
		return Boolean.parseBoolean(pretty);
    }

    /**
	 * Returns the response filter fields provided in the query header.
	 * @return
	 */
	public String[] getJsonPathFilters() {
		String sFilters = getQueryParameters().getFirstValue("filter");
		if (StringUtils.isEmpty(sFilters)) {
			return new String[]{};
		}
		else {
			Splitter splitter = Splitter.on(CharMatcher.anyOf(",")).trimResults()
			            .omitEmptyStrings();
			Iterable<String> splitFilters = splitter.split(sFilters);
			if (splitFilters == null || !splitFilters.iterator().hasNext()) {
				return new String[]{};
			} else {
				return Iterables.toArray(splitFilters, String.class);
			}
		}
	}
	
	/**
	 * 
	 * @return true if the current request has a {@code filter} query paramter
	 * with json path fields to include in the response.
	 */
	public boolean hasJsonPathFilters() {
		return ArrayUtils.isNotEmpty(getJsonPathFilters());
	}
	
	/**
	 * Returns the number of items to offset the response set.
	 * @return
	 */
	public boolean getDebugJWT() {
		String debugJWT = getQueryParameters().getFirstValue("debugjwt");
		return BooleanUtils.toBoolean(debugJWT);
	}
	
	public String getAuthenticatedUsername() {
		return TenancyHelper.getCurrentEndUser();
	}
	
	protected void checkClientPrivileges(String username) 
	{
		if (StringUtils.isEmpty(username)) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No client id provided.");
		}
		else if (!AuthorizationHelper.isTenantAdmin(getAuthenticatedUsername())) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"Client does not have permission to view the requested resource.");
		}
	}

	/**
	 * Parses the uploaded data as a file upload or a raw json upload depending
	 * on the content type. We don't serialize the json natively, so we can't handle
	 * this with standard Consumes annotations. Relies on the 
	 * AbstractAgaveResource.getPostedContentAsJsonNode(input) method.
	 * 
	 * @param input the processed input
	 * @return org.json.JSONObject
	 * @throws ResourceException if anything goes wrong
	 */
	public JSONObject getPostedContentAsJsonObject(Representation input) 
	{
		try {
			return new JSONObject(getPostedContentAsJsonNode(input).toString());
		} catch (JSONException e) {
			throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid JSON found. " + e.getMessage(), e);
		}
	}
	
	/**
	 * Parses the uploaded data as a file upload or a raw json upload depending
	 * on the content type. We don't serialize the json natively, so we can't handle
	 * this with standard Consumes annotations.
	 * 
	 * @param input the processed input
	 * @return JsonNode
	 * @throws ResourceException if anything goes wrong
	 */
	public JsonNode getPostedContentAsJsonNode(Representation input) 
	{	
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.createObjectNode();
		byte[] bytes = null;
		try 
		{
			if (input.getMediaType() == null || StringUtils.isEmpty(input.getMediaType().getName())) {
				// this will fall through and return an empty body
			}
			else if (APPLICATION_JSON.getName().equalsIgnoreCase(input.getMediaType().getName()) ||
					APPLICATION_JSON.equals(input.getMediaType(), true))
			{
				bytes = StreamUtils.getBytes(input.getStream());
                
				if (ArrayUtils.isEmpty(bytes)) {
                    // POST request with no entity.
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No content found. " + 
                    		MediaType.APPLICATION_JSON.getName() + " should be sent in the raw as the " +
            				" content of the request body.");
                } 
				else {   
					// sure, why not allow comments
					mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

					json = mapper.readTree(bytes);
				}
			}
			else if (APPLICATION_WWW_FORM.getName().equalsIgnoreCase(input.getMediaType().getName()) ||
					APPLICATION_WWW_FORM.equals(input.getMediaType(), true))
			{
			    Form form = new Form(input);
			    for (Iterator<Parameter> iter = form.iterator(); iter.hasNext();) {
                    Parameter param = iter.next();
                    ((ObjectNode)json).put(param.getName(), param.getValue());
                }
                return json;
			}
			else if (MULTIPART_FORM_DATA.getName().equalsIgnoreCase(input.getMediaType().getName()) ||
					MULTIPART_FORM_DATA.equals(input.getMediaType(), true))
			{
				RestletFileUpload fileUpload = new RestletFileUpload(new DiskFileItemFactory());
				
				List<FileItem> items;
				try {
					items = fileUpload.parseRequest(org.restlet.Request.getCurrent());
				} catch (Exception e) {
					throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL,
							"Failed to parse file upload. Please make sure you included "
							+ "a file in your request.");
				}
		        
		        for (FileItem item : items) {
		            if (!item.isFormField()) {
		            	InputStream in = null;
		            	try {
		            		in = input.getStream();
		            		bytes = StreamUtils.getBytes(in);
			            	mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
			            	json = mapper.readTree(bytes);
			            	break;
		            	}
		            	finally {
		            		try { in.close(); } catch (Exception e) {}
		            	}
		            	
		            }
		            else if (item.isFormField())
			        {
		            	((ObjectNode)json).put(item.getFieldName(), item.getString());
			        }
		        }
			}
			
			
			if (json.size() == 0) {
				throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, 
						"No content found. Please send your content as a " + 
						MULTIPART_FORM_DATA.getName() + 
						" file upload or as " + APPLICATION_JSON.getName() + 
	        			" with valid JSON content in the request body.");
			}
			
			return json;
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (JsonProcessingException e) 
		{
			throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid JSON found. " + e.getOriginalMessage(), e);
		} 
		catch (IOException e) 
		{
			throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL, 
					"Failed to parse request content.", e);
		} 
	}
	
	/**
	 * Resolves a mimetype based on the filename.
	 * @param filename
	 * @return
	 */
	public String resolveMimeTime(String filename) {
		try {
			InputStream mimeTypesStream = this.getClass().getClassLoader().getResourceAsStream("mime.types");
			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap(mimeTypesStream);
			return mimeTypesMap.getContentType(filename);
		} catch (Exception e) {
			return new MimetypesFileTypeMap().getContentType(filename);
		}
	}
	
	
	
}