/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.transfer.RemoteDataClientFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class JobRequestInputProcessor {

	private ObjectNode jobInputs;
	private String internalUsername;
	private String username;
	private Software software;
	private ObjectMapper mapper = new ObjectMapper();
	
	public JobRequestInputProcessor(String jobRequestOwner, String internalUsername, Software software) {
		this.username = jobRequestOwner;
		this.internalUsername = internalUsername;
		this.software = software;
		this.mapper = new ObjectMapper();
	}
	
	/**
	 * Validates the {@link SoftwareInput} values passed into a job request.
	 * 
	 * @param jobRequestMap 
	 * @throws JobProcessingException
	 */
	public ObjectNode process(Map<String, Object> jobRequestMap) 
	throws JobProcessingException 
	{
		jobInputs = this.mapper.createObjectNode();
		
		for (SoftwareInput softwareInput : software.getInputs()) {
			
			ArrayNode jobRequestInputValue = null;
			
			try {
				// add hidden inputs into the input array so we have a full
				// record of all inputs for this job in the history.
				if (!softwareInput.isVisible()) {
					
					// hidden fields are required
					if (jobRequestMap.containsKey(softwareInput.getKey())) {
						throw new JobProcessingException(400,
								"Invalid value for " + softwareInput.getKey()
								+ ". " + softwareInput.getKey()
								+ " is a fixed value that "
								+ "cannot be set manually. ");
					} 
					// validate the default value to ensure it's still valid
					else {
						jobRequestInputValue = doProcessSoftwareInputValue(softwareInput, softwareInput.getDefaultValueAsJsonArray());
					}
				} 
				// missing fields could be completely optional
				else if (!jobRequestMap.containsKey(softwareInput.getKey())) {
					// if it's requried, that's a problem
					if (softwareInput.isRequired()) {
						throw new JobProcessingException(400,
								"No input specified for " + softwareInput.getKey());
					}
					// if not, carry on
					else {
						continue;
					}
				} 
				// the field is present and availble to receive input. validate it
				 else {
					jobRequestInputValue = doProcessSoftwareInputValue(softwareInput, jobRequestMap.get(softwareInput.getKey()));
				}
				
				jobInputs.put(softwareInput.getKey(), jobRequestInputValue);
			} 
			catch (PermissionException e) {
				throw new JobProcessingException(400, e.getMessage(), e);
			} 
			catch (JobProcessingException e) {
				throw e;
			} 
			catch (Exception e) {
				throw new JobProcessingException(400,
						"Failed to parse input for " + softwareInput.getKey(), e);
			}
		}
		
		return jobInputs;
	}
	
	/**
	 * Processes a single value provided for a {@link SoftwareInput} field in a 
	 * job request 
	 * @param softwareInput
	 * @param value a single input string value
	 * @return
	 * @throws PermissionException
	 */
	protected ArrayNode doProcessSoftwareInputValue(SoftwareInput softwareInput, Object value) 
	throws PermissionException, JobProcessingException
	{
		if (value != null) {
			if (value instanceof String) {
				return doProcessSoftwareInputValue(softwareInput, (String)value);
			}
			else if (value instanceof String[]) {
				return doProcessSoftwareInputValue(softwareInput, (String[])value);
			}
			else if (value instanceof ArrayNode) {
				return doProcessSoftwareInputValue(softwareInput, (ArrayNode)value);
			}
			else {
				throw new JobProcessingException(400,
						"Unsupported input type for input " + softwareInput.getKey());
			}
		}
		else {
			return doProcessSoftwareInputValue(softwareInput, this.mapper.createArrayNode());
		}
		
	}
	
	/**
	 * Processes a single value provided for a {@link SoftwareInput} field in a 
	 * job request 
	 * @param softwareInput
	 * @param value a single input string value
	 * @return
	 * @throws PermissionException
	 */
	protected ArrayNode doProcessSoftwareInputValue(SoftwareInput softwareInput, String value) 
	throws PermissionException, JobProcessingException
	{
		ArrayNode jobRequestInputValue = this.mapper.createArrayNode();
		
		if (value != null) {
			for (String token: StringUtils.split(value, ";")) {
				jobRequestInputValue.add(token);
			}
		}
		
		return doProcessSoftwareInputValue(softwareInput, jobRequestInputValue);
		
	}
		
	/**
	 * Processes a single value provided for a {@link SoftwareInput} field in a 
	 * job request 
	 * @param softwareInput
	 * @param values an array of single input string values
	 * @return
	 * @throws PermissionException
	 */
	protected ArrayNode doProcessSoftwareInputValue(SoftwareInput softwareInput, String[] values) 
	throws PermissionException, JobProcessingException
	{
		ArrayNode jobRequestInputValue = this.mapper.createArrayNode();
		
		if (values != null) {
			for (String value: values) {
				for (String token: StringUtils.split(value, ";")) {
					jobRequestInputValue.add(token);
				}
			}
		}
		
		return doProcessSoftwareInputValue(softwareInput, jobRequestInputValue);
	}
	
	/**
	 * Processes a single value provided for a {@link SoftwareInput} field in a 
	 * job request 
	 * @param softwareInput
	 * @param valueArray an JSON {@link ArrayNode} of single input string values
	 * @return
	 * @throws PermissionException
	 */
	protected ArrayNode doProcessSoftwareInputValue(SoftwareInput softwareInput, ArrayNode valueArray) 
	throws PermissionException, JobProcessingException
	{
		ArrayNode jobRequestInputValue = mapper.createArrayNode();
		
		if (valueArray == null) {
			valueArray = mapper.createArrayNode();
		}
		
		// check for too many values provided
		if (softwareInput.getMinCardinality() > valueArray.size()) {
			throw new JobProcessingException(400,
					softwareInput.getKey() + " requires at least "
							+ softwareInput.getMinCardinality()
							+ " values");
		} 
		// check for too few values provided
		else if (softwareInput.getMaxCardinality() != -1
				&& softwareInput.getMaxCardinality() < valueArray.size()) {
			throw new JobProcessingException(400,
					softwareInput.getKey() + " may have at most "
							+ softwareInput.getMaxCardinality()
							+ " values");
		}
		// process each value individually
		else {
			for (Iterator<JsonNode> iter = valueArray.iterator(); iter.hasNext(); ) {
				JsonNode json = iter.next();
				
				// only string values are supported
				if (!json.isValueNode()) {
					throw new JobProcessingException(400,
							"Invalid input value, "
							+ json.toString()
							+ ", for "
							+ softwareInput.getKey()
							+ ". Value must be a string value representing "
							+ "a file or folder for which you have access.");
				}
				// ignore empty values. default values are not autofilled by agave.
				// users must supply them on their own. The long exception being
				// when a SoftwareInput is not visible. In that situation, the 
				// default value is always injected into the job request and
				// already present here.
				else if (json.isNull()) {
					continue;
				}
				
				String sValue = doProcessSingleValue(softwareInput, json.textValue());
				
				jobRequestInputValue.add(sValue);
			}
			
			return jobRequestInputValue;			
		}
	}
		
	/**
	 * Validate and sanitize a single string value provided for a {@link SoftwareInput} field in a 
	 * job request 
	 * @param softwareInput
	 * @param sValue
	 * @return the validated value
	 * @throws PermissionException
	 */
	protected String doProcessSingleValue(SoftwareInput softwareInput, String sValue) 
	throws PermissionException, JobProcessingException
	{
		// if a SoftwareInput#validator is present, the input must 
		// match the regex
		if (StringUtils.isNotEmpty(softwareInput.getValidator())
				&& !ServiceUtils.doesValueMatchValidatorRegex(sValue, softwareInput.getValidator())) {
			throw new JobProcessingException(
					400,
					"Invalid input value, " + sValue + ", for " + softwareInput.getKey()
					+ ". Value must match the following expression: "
					+ softwareInput.getValidator());
		} 
		// no validator, so verify the value is a suppported file or folder
		else {
			URI inputUri;
			try {
				// is the uri valid?
				inputUri = new URI(sValue);
				
				// is the schema supported?
				if (!RemoteDataClientFactory.isSchemeSupported(inputUri)) {
					throw new JobProcessingException(400,
							"Invalid value for " + softwareInput.getKey()
							+ ". URI with the " + inputUri.getScheme()
							+ " scheme are not currently supported. "
							+ "Please specify your input as a relative path, "
							+ "an Agave resource URL, "
							+ "or a URL with one of the following schemes: "
							+ "http, https, sftp, or agave.");
				} 
				// can the user read the file/folder?
				else if (!canUserReadUri(this.username, this.internalUsername, inputUri)) {
					throw new JobProcessingException(403,
							"You do not have permission to access this the input file or directory "
									+ "at " + sValue);
				}
			} catch (URISyntaxException e) {
				throw new JobProcessingException(403,
						"Invalid value for " + softwareInput.getKey()
						+ "Please specify your input as a relative path, "
						+ "an Agave resource URL, "
						+ "or a URL with one of the following schemes: "
						+ "http, https, sftp, or agave.");
			}
		}
		
		return sValue;
	}

	/**
	 * Checks the permission and existence of a remote URI. This wraps the 
	 * {@link PermissionManager#canUserReadUri(String, String, URI)} method 
	 * to allow mocking of this class.
	 *  
	 * @param username
	 * @param internalUsername
	 * @param inputUri
	 * @return
	 * @throws PermissionException
	 */
	public boolean canUserReadUri(String username, String internalUsername, URI inputUri) 
	throws PermissionException 
	{
		return PermissionManager.canUserReadUri(this.username, this.internalUsername, inputUri);
	}

	/**
	 * @return the jobInputs
	 */
	public ObjectNode getJobInputs() {
		return jobInputs;
	}



	/**
	 * @param jobInputs the jobInputs to set
	 */
	public void setJobInputs(ObjectNode jobInputs) {
		this.jobInputs = jobInputs;
	}



	/**
	 * @return the internalUsername
	 */
	public String getInternalUsername() {
		return internalUsername;
	}



	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername) {
		this.internalUsername = internalUsername;
	}



	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}



	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}



	/**
	 * @return the software
	 */
	public Software getSoftware() {
		return software;
	}



	/**
	 * @param software the software to set
	 */
	public void setSoftware(Software software) {
		this.software = software;
	}



	/**
	 * @return the mapper
	 */
	public ObjectMapper getMapper() {
		return mapper;
	}



	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}
}
