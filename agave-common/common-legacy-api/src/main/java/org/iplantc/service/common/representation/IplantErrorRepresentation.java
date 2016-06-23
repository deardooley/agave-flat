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
public class IplantErrorRepresentation extends IplantRepresentation {

	/**
	 * Creates an error response message which will be wrapped and 
	 * returned to the user.
	 * 
	 * @param message error message to return in response
	 */
	public IplantErrorRepresentation(String errorMessage)
	{
		super("error", errorMessage, null);
	}

}
