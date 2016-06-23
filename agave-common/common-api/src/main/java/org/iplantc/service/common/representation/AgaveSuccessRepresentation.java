/**
 * 
 */
package org.iplantc.service.common.representation;



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
        this(null, null);
    }
    
    /**
     * Create success representation with the JSON response
     * @param jsonObject serialized response object
     */
    public AgaveSuccessRepresentation(String json)
    {
        this(null, json);
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
    
//	/**
//	 * @param prettyPrint
//	 * @deprecated formatting is pulled from query string dynamically now
//	 */
//	public AgaveSuccessRepresentation(boolean prettyPrint)
//	{
//		this(null, "{}", prettyPrint);
//	}
//	/**
//	 * @param jsonObject
//	 * @deprecated formatting is pulled from query string dynamically now
//	 */
//	public AgaveSuccessRepresentation(String json, boolean prettyPrint)
//	{
//		this(null, json, prettyPrint);
//	}
//
//	/**
//	 * @param message
//	 * @param json
//	 * @param prettyPrint
//	 * @deprecated formatting is pulled from query string dynamically now
//	 */
//	public AgaveSuccessRepresentation(String message, String json, boolean prettyPrint)
//	{
//		super("success", message, json, prettyPrint);
//	}
}
