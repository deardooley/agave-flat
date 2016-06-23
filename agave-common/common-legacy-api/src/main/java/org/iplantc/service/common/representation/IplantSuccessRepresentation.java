/**
 * 
 */
package org.iplantc.service.common.representation;


/**
 * @author dooley
 *
 */
public class IplantSuccessRepresentation extends IplantRepresentation {

    /**
     * Creates a success response with no message and empty response body
     */
    public IplantSuccessRepresentation()
    {
        this(null, null);
    }
    
    /**
     * Create success representation with the JSON response
     * @param jsonObject serialized response object
     */
    public IplantSuccessRepresentation(String json)
    {
        this(null, json);
    }

    /**
     * Create success representation with the message and JSON response
     * @param message
     * @param json serialized response object
     */
    public IplantSuccessRepresentation(String message, String json)
    {
        super("success", message, json);
    }
}
