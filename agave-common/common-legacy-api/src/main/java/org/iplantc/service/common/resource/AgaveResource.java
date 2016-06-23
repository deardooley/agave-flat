/**
 * 
 */
package org.iplantc.service.common.resource;

import static org.restlet.data.MediaType.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * Abstract class to handle pagination and user and tenant lookup.
 * 
 * @author dooley
 *
 */
public abstract class AgaveResource extends Resource 
{
    private static final Logger log = Logger.getLogger(AgaveResource.class);
            
    protected Integer limit = Settings.DEFAULT_PAGE_SIZE;
    protected Integer offset = 0;
    protected boolean debugjwt = false;
    protected boolean forceDownload = false;
    protected String[] jsonPathFilters;
    
    public AgaveResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        
        Form form = request.getOriginalRef().getQueryAsForm();
        if (form != null) {
            limit = NumberUtils.toInt(form.getFirstValue("limit"),Settings.DEFAULT_PAGE_SIZE);
            limit = Math.abs(limit);
            limit = Math.min(limit, Settings.MAX_PAGE_SIZE);
            
            offset = NumberUtils.toInt(form.getFirstValue("offset"), 0);
            offset = Math.abs(offset);
            forceDownload = Boolean.parseBoolean(form.getFirstValue("force"));
            debugjwt = Boolean.parseBoolean(form.getFirstValue("debugjwt"));
            
            String sFilters = form.getFirstValue("filter");
            if (StringUtils.isEmpty(sFilters)) {
            	jsonPathFilters = new String[]{};
    		}
    		else {
				Splitter splitter = Splitter.on(CharMatcher.anyOf(",")).trimResults()
				            .omitEmptyStrings();
				Iterable<String> splitFilters = splitter.split(sFilters);
				if (splitFilters == null) {
					jsonPathFilters = new String[]{};
				} else {
					jsonPathFilters = Iterables.toArray(splitFilters, String.class);
				}
    		}
        }
        
        if (debugjwt) {
            log.debug(JWTClient.getCurrentJWSObject().toString());
        }
    }
    
    /**
	 * 
	 * @return true if the current request has a {@code filter} query paramter
	 * with json path fields to include in the response.
	 */
	public boolean hasJsonPathFilters() {
		return ArrayUtils.isNotEmpty(jsonPathFilters);
	}
    
    protected String getAuthenticatedUsername() 
    {
        if (Settings.DEBUG) 
        {
            return Settings.DEBUG_USERNAME;
        } 
        else if (StringUtils.equals("wso2", Settings.AUTH_SOURCE)) 
        {
            return TenancyHelper.getCurrentEndUser();
        }
        else
        {
            return getRequest().getChallengeResponse().getIdentifier();
        }
    }
    
    /**
     * Generic parser to handle the different POST and PUT content types accepted and return
     * it as a Map<String, String>. Defaults to AgaveResource.getPostedEntityAsMap(true);
     * 
     * @return Map<String,String> of form values
     * @throws ResourceException
     */
    protected Map<String,String> getPostedEntityAsMap() throws ResourceException
    {
        return getPostedEntityAsMap(true);
    }
    
    /**
     * Generic parser to handle the different POST and PUT content types accepted and return
     * it as a Map<String, String>
     * 
     * @param allowUrlFormEncoded
     * @return Map<String,String> of form values
     * @throws ResourceException
     */
    protected Map<String,String> getPostedEntityAsMap(boolean allowUrlFormEncoded) throws ResourceException
    {
        Map<String,String> map = new HashMap<String, String>();
        byte[] bytes = null;
        try 
        {
            Representation entity = getRequest().getEntity();
            if (entity != null) 
            {
            	if (entity.getMediaType() == null || StringUtils.isEmpty(entity.getMediaType().getName())) {
            		// this will pass through as an empty body and throw an exception
            	}
            	else if (MULTIPART_FORM_DATA.getName().equalsIgnoreCase(entity.getMediaType().getName()) || 
                		MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) 
                {
                    // parse the form to get the job specs
                    if (entity.getCharacterSet() == null) {
                        entity.setCharacterSet(CharacterSet.UTF_8);
                    }
                    
                    DiskFileItemFactory factory = new DiskFileItemFactory();
                    factory.setSizeThreshold((int)(Math.pow(2, 20) * 5));
                    RestletFileUpload upload = new RestletFileUpload(factory);
                    
                    List<FileItem> items;
                    try {
                        items = upload.parseRequest(getRequest());
                    } catch (Exception e) {
                        throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                                "Failed to parse file upload. Please make sure you included a file in your POST request.");
                    }
                    
                    for (final Iterator<FileItem> it = items.iterator(); it.hasNext();) 
                    {
                        FileItem fi = it.next();
                        if (fi.getFieldName().equals("fileToUpload")) 
                        {    
                            String text = new String(StreamUtils.getBytes(fi.getInputStream()));
                            
                            if (StringUtils.isEmpty(text)) {
                                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "No content found in file. Please POST your content as a " + 
                                        MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + MediaType.APPLICATION_JSON.getName() + 
                                        " with the content as the POST body.");
                            }
                            map.put("fileToUpload", text);
                            // we only care about the file. the rest of the info is meaningless to us 
                            // except on file uploads which we handle differently.
                            break;
                        }
                        else if (fi.isFormField())
                        {
                            map.put(fi.getFieldName(), fi.getString());
                        }
                    }
                }
                else if (APPLICATION_WWW_FORM.getName().equalsIgnoreCase(entity.getMediaType().getName()) || 
                		APPLICATION_WWW_FORM.equals(entity.getMediaType(), true) && allowUrlFormEncoded) 
                {
                    Form form = new Form(entity);
                    map = form.getValuesMap();
                }
                else if (APPLICATION_JSON.getName().equalsIgnoreCase(entity.getMediaType().getName()) || 
                		APPLICATION_JSON.equals(entity.getMediaType(), true)) 
                {
                    bytes = StreamUtils.getBytes(entity.getStream());
                    if (ArrayUtils.isEmpty(bytes))
                    {
                        // POST request with no entity.
                        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                        		"No content found. Please send your request as a " + 
                                MULTIPART_FORM_DATA.getName() + " file upload or as " + 
                				APPLICATION_JSON.getName() + 
                                " with valid JSON content in the request body.");
                    }
                    else 
                    {   
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(bytes);
                        map = new HashMap<String, String>();
                        for(Iterator<String> iter = json.fieldNames(); iter.hasNext();)
                        {
                            String field = iter.next();
                            map.put(field, json.get(field).asText());
                        }
                    }
                }
                else
                {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    		"Unsupported content type. Please send your request as a " + 
                            MULTIPART_FORM_DATA.getName() + " file upload or as " + 
            				APPLICATION_JSON.getName() + 
                            " with valid JSON content in the request body.");
                }
            }
            else
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                		"No content found. Please send your request as a " + 
                        MULTIPART_FORM_DATA.getName() + " file upload or as " + 
        				APPLICATION_JSON.getName() + 
                        " with valid JSON content in the request body.");
            }
        } 
        catch (ResourceException e) 
        {
            throw e;
        } 
        catch (JsonProcessingException e) 
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid JSON found. " + e.getOriginalMessage(), e);
        } 
        catch (IOException e) 
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to parse request content.", e);
        }
        finally {
            bytes = null;
        }
        
        if (map.isEmpty()) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
            		"No content found. Please send your request as a " + 
                    MULTIPART_FORM_DATA.getName() + " file upload or as " + 
    				APPLICATION_JSON.getName() + 
                    " with valid JSON content in the request body.");
        }
        
        return map;
    }
    
    /**
     * Generic parser to handle the different POST and PUT content types accepted and return
     * it as an ObjectNode
     * 
     * @return ObjectNode
     * @throws ResourceException
     */
    protected JsonNode getPostedEntityAsObjectNode(boolean allowUrlFormEncoded) throws ResourceException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.createObjectNode();
        byte[] bytes = null;
        try 
        {   
            Representation entity = getRequest().getEntity();
            if (entity != null) 
            {
                if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) 
                {
                    // parse the form to get the job specs
                    if (entity.getCharacterSet() == null) {
                        entity.setCharacterSet(CharacterSet.UTF_8);
                    }
                    
                    DiskFileItemFactory factory = new DiskFileItemFactory();
                    factory.setSizeThreshold((int)(Math.pow(2, 20) * 5));
                    RestletFileUpload upload = new RestletFileUpload(factory);
                    
                    List<FileItem> items;
                    try {
                        items = upload.parseRequest(getRequest());
                    } catch (Exception e) {
                        throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                                "Failed to parse file upload. Please make sure you included a file in your POST request.");
                    }
                    
                    for (final Iterator<FileItem> it = items.iterator(); it.hasNext();) 
                    {
                        FileItem fi = it.next();
                        if (fi.getFieldName().equals("fileToUpload")) 
                        {    
                            json = mapper.readTree(fi.getInputStream());
                            
                            if (json == null || json.size() == 0) {
                                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "No content found in file. Please POST your content as a " + 
                                        MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + MediaType.APPLICATION_JSON.getName() + 
                                        " with the content as the POST body.");
                            }
                            break;
                        }
                        else if (fi.isFormField())
                        {
                            ((ObjectNode)json).put(fi.getFieldName(), fi.getString());
                        }
                    }
                }
                else if (MediaType.APPLICATION_WWW_FORM.equals(entity.getMediaType(), true) && allowUrlFormEncoded) 
                {
                    Form form = new Form(entity);
                    for (Iterator<Parameter> iter = form.iterator(); iter.hasNext();) {
                        Parameter param = iter.next();
                        ((ObjectNode)json).put(param.getName(), param.getValue());
                    }
                }
                else if (MediaType.APPLICATION_JSON.equals(entity.getMediaType(), true)) 
                {
                    bytes = StreamUtils.getBytes(entity.getStream());
                    if (ArrayUtils.isEmpty(bytes))
                    {
                        // POST request with no entity.
                        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No content found. Please POST your content as a " + 
                                MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + MediaType.APPLICATION_JSON.getName() + 
                                " with the content as the POST body.");
                    }
                    else 
                    {   
                        json = mapper.readTree(bytes);
                    }
                }
                else
                {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unsupported content type. Please POST your system description as a " + 
                            MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + MediaType.APPLICATION_JSON.getName() + " with the JSON description as the POST body.");
                }
            }
            else
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                        "No content found. Please POST your content as a " + 
                        MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + 
                        MediaType.APPLICATION_JSON.getName() + 
                        " with the content as the POST body.");
            }
        } 
        catch (ResourceException e) 
        {
            throw e;
        } 
        catch (JsonProcessingException e) 
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    "Invalid JSON found. " + e.getMessage(), e);
        } 
        catch (IOException e) 
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to parse request content.", e);
        }
        finally {
            bytes = null;
        }
        
        if (json.size() == 0) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    "No content found. Please POST your content as a " + 
                    MediaType.MULTIPART_FORM_DATA.getName() + " file upload or as " + 
                    MediaType.APPLICATION_JSON.getName() + 
                    " with the content as the POST body.");
        }
        
        return json;
    }
    
    /**
     * Generic parser to handle the different POST and PUT content types accepted and return
     * it as an JSONObject. Falls back on AgaveResource.getPostedEntityAsObjectNode(allowUrlFormEncoded).
     * 
     * @return JSONObject
     * @throws ResourceException
     */
    protected JSONObject getPostedEntityAsJsonObject(boolean allowUrlFormEncoded) throws ResourceException
    {
        JsonNode json = getPostedEntityAsObjectNode(allowUrlFormEncoded);
        
        try 
        {
            return new JSONObject(json.toString());
        } 
        catch (JSONException e) 
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    "Invalid JSON found. " + e.getMessage(), e);
        }
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public boolean isDebugjwt() {
        return debugjwt;
    }

    public void setDebugjwt(boolean debugjwt) {
        this.debugjwt = debugjwt;
    }

    public boolean isForceDownload() {
        return forceDownload;
    }

    public void setForceDownload(boolean forceDownload) {
        this.forceDownload = forceDownload;
    }
}
