/**
 * 
 */
package org.iplantc.service.common.representation;


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
		super("error", errorMessage, null);
	}

}
