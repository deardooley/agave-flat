/**
 * 
 */
package org.iplantc.service.common.representation;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Wrapper for error responses
 * 
 * @author dooley
 *
 */
public class AgaveErrorRepresentation extends AgaveRepresentation {

	/**
	 * @param jsonObject
	 */
	public AgaveErrorRepresentation(String errorMessage)
	{
		super("error", errorMessage, (JsonNode)null);
	}

}
