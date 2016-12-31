package org.iplantc.service.common.representation;


import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.util.JsonPropertyFilter;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.data.Status;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Lf2SpacesIndenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * Wrapper class for all responses from the api
 * 
 * @author dooley
 *
 */
public abstract class AgaveRepresentation extends StringRepresentation {

	private Boolean prettyPrint = null;
	
	/**
	 * Constructor for legacy services building their JSON responses
	 * as {@link String} values. 
	 * 
	 * @deprecated
	 * @see {@link #AgaveRepresentation(String, String, JsonNode)
	 * @param status
	 * @param message
	 * @param json
	 */
	protected AgaveRepresentation(String status, String message, String json) 
	{	
		super("", MediaType.APPLICATION_JSON, null, CharacterSet.UTF_8);
		
		setText(formatResponse(status, message, json));
	}

	/**
	 * Default constructor for services generating {@link JsonNode} naked 
	 * responses. This saves a couple serialization and deserialization steps
	 * along the say, which reduces load on the server, memory utilization, 
	 * and processing time. 
	 * 
	 * @param status
	 * @param message
	 * @param json
	 */
	protected AgaveRepresentation(String status, String message, JsonNode jsonNode) 
	{	
		super("", MediaType.APPLICATION_JSON, null, CharacterSet.UTF_8);
		
		setText(formatResponse(status, message, jsonNode));
	}
	
	/**
	 * Formats the response both successful and unsuccessful calls into a 
	 * JSON wrapper object with attributes status, message, and result 
	 * where result contains the JSON output of the call.
	 * 
	 * @param status
	 * @param message
	 * @param json
	 */
	protected String formatResponse(String status, String message, String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
            
            JsonNode jsonNode = mapper.readTree(json);
			
            return formatResponse(status, message, jsonNode);
		}
		catch (Exception e) {
			if (isNaked()) {
		        return json;
		    }
		    else
		    {
    			status = status.replaceAll("\"", "\\\"");
    			
    			if (message == null) {
    				message = "";
    			} else {
    				message = message.replaceAll("\"", "\\\"");
    			}
    			
    			StringBuilder builder = new StringBuilder();
    			builder.append("{\"status\":\"" + status + "\",");
    			builder.append("\"message\":\"" + message + "\",");
    			builder.append("\"version\":\"" + Settings.SERVICE_VERSION + "\",");
    			builder.append("\"result\":" + json + "}");
    			return builder.toString();
		    }
		}
	}

	/**
	 * Formats the response both successful and unsuccessful calls into a 
	 * JSON wrapper object with attributes status, message, and result 
	 * where result contains the JSON output of the call.
	 * 
	 * @param status
	 * @param message
	 * @param jsonNode
	 */
	protected String formatResponse(String status, String message, JsonNode jsonNode) {
		try
		{
			DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
            
            ObjectMapper mapper = new ObjectMapper();
			
			if (isNaked()) 
			{
			    if (Response.getCurrent().getStatus() == Status.SUCCESS_NO_CONTENT || jsonNode == null) {
			       return null;
			    } else {
			    	if (isPrettyPrint()) {
	    			    return mapper.writer(pp).writeValueAsString(filterOutput(jsonNode));
	    			} else {
	    				return filterOutput(jsonNode).toString();
	    			}
			    }
			}
			else 
			{
    			ObjectNode jsonWrapper = mapper.createObjectNode()
    				.put("status", status)
    				.put("message", message)
    				.put("version", Settings.SERVICE_VERSION);
    			
    			if (jsonNode != null) {
    				jsonWrapper.set("result", filterOutput(jsonNode));
    			}
    			
    			if (isPrettyPrint()) {
    			    return mapper.writer(pp).writeValueAsString(jsonWrapper);
    			} else {
    				return jsonWrapper.toString();
    			}
			}
		} 
		catch (Exception e)
		{
		    if (isNaked()) {
		        return jsonNode.toString();
		    }
		    else
		    {
    			status = status.replaceAll("\"", "\\\"");
    			
    			if (message == null) {
    				message = "";
    			} else {
    				message = message.replaceAll("\"", "\\\"");
    			}
    			
    			StringBuilder builder = new StringBuilder();
    			builder.append("{\"status\":\"" + status + "\",");
    			builder.append("\"message\":\"" + message + "\",");
    			builder.append("\"version\":\"" + Settings.SERVICE_VERSION + "\",");
    			builder.append("\"result\":" + jsonNode.toString() + "}");
    			return builder.toString();
		    }
		}
	}

	/**
	 * @return the prettyPrint
	 */
	public Boolean isPrettyPrint()
	{
	    if (prettyPrint == null) {
	        Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
	        prettyPrint = Boolean.valueOf(form.getFirstValue("pretty"));
	    }
	    
	    return prettyPrint;
	}

	/**
	 * @param prettyPrint the prettyPrint to set
	 */
	public void setPrettyPrint(Boolean prettyPrint)
	{
		this.prettyPrint = prettyPrint;
	}
	
	/**
	 * Returns true if the {@code naked} url query parameter was truthy. This
	 * will cause only the result to be returned to the client, excluding
	 * the traditional response object fields.
	 * 
	 * @return true if {@code naked} url query parameter was set to a truthy value
	 */
	public boolean isNaked() {
	    Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
        return Boolean.valueOf(form.getFirstValue("naked"));
	}
	
	/**
	 * Returns the response filter fields provided in the query header.
	 * @return
	 */
	public String[] getJsonPathFilters() {
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
		String sFilters = form.getFirstValue("filter");
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
	 * Applies user-supplied path filters to the response json 
	 * 
	 * @param nakedJsonResponse
	 * @return the properly filetered response
	 * @throws IOException
	 */
	public JsonNode filterOutput(JsonNode nakedJsonResponse) throws IOException {
		String[] sFilters = getJsonPathFilters();
		try {
			return new JsonPropertyFilter().getFilteredContent(
					nakedJsonResponse, sFilters);
		} catch (Exception e) {
			return nakedJsonResponse;
		}
	}
}
