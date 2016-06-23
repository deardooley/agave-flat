/**
 * 
 */
package org.iplantc.service.clients;

import org.iplantc.service.clients.exceptions.APIClientException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper to hold responses from the API. Responses come in the form
 * <code>
 * {
 * 		"status": "success",
 * 		"message": "",
 * 		"result": {}
 * }
 * </code>
 * @author dooley
 *
 */
public class APIResponse 
{
	public enum APIResponseStatus { SUCCESS, ERROR }
	private APIResponseStatus status;
	private JsonNode result;
	private String message;
	
	public APIResponse(String text) throws APIClientException 
	{	
		if (APIServiceUtils.isEmpty(text)) 
		{
			throw new APIClientException("Empty response received.");
		} 
		else
		{
			try 
			{
				ObjectMapper mapper = new ObjectMapper();
				JsonFactory factory = mapper.getFactory();
				JsonParser jp = factory.createJsonParser(text);
				JsonNode response = mapper.readTree(jp);
				
				if (response.has("status") && !response.get("status").isNull()) {
					if ("success".equals(response.get("status").asText())) {
						this.status = APIResponseStatus.SUCCESS;
					} else {
						this.status = APIResponseStatus.ERROR;
					}
					this.message = response.get("message").asText();
					this.result = response.get("result");
				} 
				else
				{
					throw new APIClientException("Unrecognized response from the server.\n" + text);
				}
			} catch (Exception e) {
				throw new APIClientException("Unable to parse response from the server.\n" + text, e);
			}
		}
	}

	/**
	 * @return the status
	 */
	public APIResponseStatus getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(APIResponseStatus status)
	{
		this.status = status;
	}

	/**
	 * @return the result
	 */
	public JsonNode getResult()
	{
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(JsonNode result)
	{
		this.result = result;
	}

	/**
	 * @return the message
	 */
	public String getMessage()
	{
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message)
	{
		this.message = message;
	}

	public boolean isSuccess()
	{
		return (status != null && APIResponseStatus.SUCCESS.equals(status));
	}
}
