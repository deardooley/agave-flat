package org.iplantc.service.common.restlet.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

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
public class AbstractAgaveResource {
	
	private static final Logger log = Logger.getLogger(AbstractAgaveResource.class);

	@Context
	protected UriInfo uriInfo;
	
	@Context
	protected Request request;
	
	@Context 
	protected HttpHeaders headers;

	@Context
	protected SecurityContext securityContext;
	
	/**
	 * Should the response be pretty printed.
	 * @return
	 */
	public boolean isPrettyPrint() {
		String pretty = getUriInfo().getQueryParameters().getFirst("pretty");
		return Boolean.parseBoolean(pretty);
	}
	
	/**
	 * Returns the max number of items to return.
	 * @return 
	 */
	public int getLimit() {
		String count = getUriInfo().getQueryParameters().getFirst("limit");
		if (NumberUtils.isDigits(count)) {
			return Math.min(Integer.parseInt(count), Settings.MAX_PAGE_SIZE);
            
		} else {
			return Math.min(Settings.DEFAULT_PAGE_SIZE, Settings.MAX_PAGE_SIZE);
		}
	}
	
	/**
	 * Returns the number of items to offset the response set.
	 * @return
	 */
	public int getOffset() {
		String offset = getUriInfo().getQueryParameters().getFirst("offset");
		return NumberUtils.toInt(offset);
	}
	
	/**
	 * Returns the response filter fields provided in the query header.
	 * @return
	 */
	public String[] getJsonPathFilters() {
		String sFilters = getUriInfo().getQueryParameters().getFirst("filter");
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
		String debugJWT = getUriInfo().getQueryParameters().getFirst("debugjwt");
		return BooleanUtils.toBoolean(debugJWT);
	}
	
	public String getAuthenticatedUsername() {
		return TenancyHelper.getCurrentEndUser();
	}
	
	protected void checkClientPrivileges(String username) 
	{
		if (StringUtils.isEmpty(username)) {
			throw new WebApplicationException(
					new PermissionException("No client id provided."),
					Status.NOT_FOUND);
		}
		else if (!StringUtils.equals(getAuthenticatedUsername(), username)) {
			if (!this.getSecurityContext().isUserInRole("admin")) {
				throw new WebApplicationException(
						new PermissionException("Client does not have permission to view the requested resource."), 
						Status.UNAUTHORIZED);
			}			 
		}
	}

	/**
	 * @return the uriInfo
	 */
	public UriInfo getUriInfo()
	{
		return uriInfo;
	}

	/**
	 * @param uriInfo the uriInfo to set
	 */
	public void setUriInfo(UriInfo uriInfo)
	{
		this.uriInfo = uriInfo;
	}

	/**
	 * @return the securityContext
	 */
	public SecurityContext getSecurityContext()
	{
		return securityContext;
	}

	/**
	 * @param securityContext the securityContext to set
	 */
	public void setSecurityContext(SecurityContext securityContext)
	{
		this.securityContext = securityContext;
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
		try 
		{
			if (org.restlet.data.MediaType.APPLICATION_JSON.equals(input.getMediaType(), true))
			{
				json = mapper.readTree(input.getStream());
			}
			else if (org.restlet.data.MediaType.APPLICATION_WWW_FORM.equals(input.getMediaType(), true))
			{
			    Form form = new Form(input);
                for (Iterator<Parameter> iter = form.iterator(); iter.hasNext();) {
                    Parameter param = iter.next();
                    ((ObjectNode)json).put(param.getName(), param.getValue());
                }
                return json;
			}
			else if (org.restlet.data.MediaType.MULTIPART_FORM_DATA.equals(input.getMediaType(), true))
			{
				RestletFileUpload fileUpload = new RestletFileUpload(new DiskFileItemFactory());
				
				List<FileItem> items;
				try {
					items = fileUpload.parseRequest(org.restlet.Request.getCurrent());
				} catch (Exception e) {
					throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL,
							"Failed to parse file upload. Please make sure you included "
							+ "a file in your POST request.");
				}
		        
		        for (FileItem item : items) {
		            if (!item.isFormField()) {
		            	json = mapper.readTree(item.getInputStream());
		            	break;
		            }
		            else if (item.isFormField())
			        {
		            	((ObjectNode)json).put(item.getName(), item.getString("utf-8"));
			        }
		        }
			}
			
			
			if (json.size() == 0) {
				throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, 
						"No content found. Please POST your content as a " + 
						org.restlet.data.MediaType.MULTIPART_FORM_DATA.getName() + 
						" file upload or as " + org.restlet.data.MediaType.APPLICATION_JSON.getName() + 
	        			" with the content as the POST body.");
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
		InputStream mimeTypesStream = null;
		try {
			mimeTypesStream = AbstractAgaveResource.class.getResourceAsStream("mime.types");
			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap(mimeTypesStream);
			return mimeTypesMap.getContentType(filename);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (mimeTypesStream != null) try {mimeTypesStream.close();} catch (Exception e){}
		}
	}
}