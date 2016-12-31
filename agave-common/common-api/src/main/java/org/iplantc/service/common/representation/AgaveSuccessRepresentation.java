/**
 * 
 */
package org.iplantc.service.common.representation;

import com.fasterxml.jackson.databind.JsonNode;



/**
 * @author dooley
 *
 */
public class AgaveSuccessRepresentation extends AgaveRepresentation {

    /**
     * Creates a success response with no message and empty response body
     */
    public AgaveSuccessRepresentation()
    {
        this(null, (JsonNode)null);
    }
    
    /**
     * Create success representation with the JSON response
     * @param json serialized response object
     */
    public AgaveSuccessRepresentation(String json)
    {
        this(null, json);
    }

    /**
     * Create success representation with the JSON response
     * @param jsonNode response object as a serialized node
     */
    public AgaveSuccessRepresentation(JsonNode jsonNode)
    {
        this(null, jsonNode);
    }
    
    /**
     * Create success representation with the message and JSON response
     * @param message
     * @param json serialized response object
     */
    public AgaveSuccessRepresentation(String message, String json)
    {
        super("success", message, json);
    }
    
    /**
     * Create success representation with the message and JSON response
     * @param message
     * @param json serialized response object
     */
    public AgaveSuccessRepresentation(String message, JsonNode jsonNode)
    {
        super("success", message, jsonNode);
    }
}
