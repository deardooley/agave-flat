package org.iplantc.service.common.util;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Just a helper class to simplify usage
 */
public class JsonPropertyFilter {
	
	 public JsonPropertyFilter() {}
    
    /**
     * Applies path filters to a serialized JSON object and returns the result with optional
     * pretty printing.
     * @param content
     * @param filters
     * @param prettyPrint
     * @return
     * @throws IOException
     */
    public String getFilteredContent(String content, String[] filters, boolean prettyPrint) throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	
    	return getFilteredContent(mapper.readTree(content), filters, prettyPrint);
    }
    
    /**
     * Applies path filters to a {@link JsonNode} and returns the result with optional
     * pretty printing.
     * @param json
     * @param filters
     * @param prettyPrint
     * @return
     * @throws IOException
     */
    public String getFilteredContent(JsonNode json, String[] filters, boolean prettyPrint) throws IOException {
    	
    	ObjectMapper mapper = new ObjectMapper();
    	
    	if (ArrayUtils.isEmpty(filters) || ArrayUtils.contains(filters, "*")) {
    		if (prettyPrint) {
	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
	    		return mapper.writer(pp).writeValueAsString(json);
	    	} else {
	    		return json.toString();
	    	}
    	}
    	
    	// iterate over array
    	if (json.isArray()) {
	    	ArrayNode filteredJson = mapper.createArrayNode();
	    	for (Iterator<JsonNode> iter=json.iterator(); iter.hasNext(); ) {
//	    		String childJson = mapper.writer().writeValueAsString(child);
	    		
	    		ObjectNode child = (ObjectNode)iter.next();
	    		ObjectNode filteredChild = mapper.createObjectNode();
	    		for (String filter: filters) {
	    			String[] tokens = StringUtils.split(filter, ".");
	    			if (tokens.length == 2) {
    					if (child.has(tokens[0])) {
    						if (child.get(tokens[0]).has(tokens[1])) {
    							if (filteredChild.has(tokens[0])) {
	    							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
    							}
    							else {
    								filteredChild.putObject(tokens[0]).put(tokens[1], child.get(tokens[0]).get(tokens[1]));
    							}
    						}
    						else {
    							if (filteredChild.has(tokens[0])) {
    								((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
    							}
    							else {
    								filteredChild.putObject(tokens[0]).putNull(tokens[1]);
    							}
    						}
    					}
    				} 
	    			else if (tokens.length == 1) { 
		    			if (child.has(filter)) {
		    				filteredChild.put(filter, child.get(filter));
		    			}
	    			}
	    		}
	    			    		
	    		if (!filteredChild.isNull() && filteredChild.size() > 0) {
	    			filteredJson.add(filteredChild);
	    		}
    		}
	    	
	    	if (prettyPrint) {
	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
	    		return mapper.writer(pp).writeValueAsString(filteredJson);
	    	} else {
	    		return mapper.writeValueAsString(filteredJson);
	    	}
    	}
    	else {
    		ObjectNode filteredChild = mapper.createObjectNode();
    		for (String filter: filters) {
    			String[] tokens = StringUtils.split(filter, ".");
    			if (tokens.length == 2) {
					if (json.has(tokens[0])) {
						if (json.get(tokens[0]).has(tokens[1])) {
							if (filteredChild.has(tokens[0])) {
    							((ObjectNode)filteredChild.get(tokens[0])).put(tokens[1], filteredChild.get(tokens[0]).get(tokens[1]));
							}
							else {
								filteredChild.putObject(tokens[0]).put(tokens[1], json.get(tokens[0]).get(tokens[1]));
							}
						}
						else {
							if (filteredChild.has(tokens[0])) {
								((ObjectNode)filteredChild.get(tokens[0])).putNull(tokens[1]);
							}
							else {
								filteredChild.putObject(tokens[0]).putNull(tokens[1]);
							}
						}
					}
				} 
    			else if (tokens.length == 1) { 
	    			if (json.has(filter)) {
	    				filteredChild.put(filter, json.get(filter));
	    			}
    			}
    		}
    		
    		if (prettyPrint) {
	    		DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
	    		return mapper.writer(pp).writeValueAsString(filteredChild);
	    	} else {
	    		return mapper.writeValueAsString(filteredChild);
	    	}
    	}
    }

}