/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class JobResubmissionRequestInputProcessor extends JobRequestInputProcessor{

	private boolean ignoreInputConflicts = false;
	
	public JobResubmissionRequestInputProcessor(String jobRequestOwner, String internalUsername, Software software, boolean isIgnoreInputConflicts) {
		super(jobRequestOwner, internalUsername, software);
		setIgnoreInputConflicts(isIgnoreInputConflicts);
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
		setJobInputs(getMapper().createObjectNode());
		
		for (SoftwareInput softwareInput : getSoftware().getInputs()) {
			
			ArrayNode jobRequestInputValue = null;
			
			try {
				// add hidden inputs into the input array so we have a full
				// record of all inputs for this job in the history.
				if (!softwareInput.isVisible()) {
					
					// if overriding is present, then force it here. this will 
					// replace whatever was there before.
					if (isIgnoreInputConflicts()) { 
						jobRequestInputValue = doProcessSoftwareInputValue(softwareInput, softwareInput.getDefaultValueAsJsonArray());
					}
					// if the hidden input wasn't there before. The app has changed. Alert them. 
					else if (!jobRequestMap.containsKey(softwareInput.getKey())) {
						
						throw new JobProcessingException(400,
								"Invalid value for " + softwareInput.getKey()
								+ ". No value for the hidden input " + softwareInput.getKey()
								+ " was present in the original job. This usually indicates that "
								+ "the app definition has changed since the job was last run. "
								+ "To override hidden input on job resubmissions, set the ignoreInputConflicts "
								+ "field to true in the request. ");
					} 
					// the hidden field was there before and now. Verify the value is identical since it's 
					// it's possible it was not hidden when they ran the job originally. 
					else {
						
						// resolve the current default value for this input
						jobRequestInputValue = doProcessSoftwareInputValue(softwareInput, softwareInput.getDefaultValueAsJsonArray());
						
						// resolve the previous value from the job 
						ArrayNode previousJobInputValue = doProcessSoftwareInputValue(softwareInput, jobRequestMap.get(softwareInput.getKey()));
						
						// if the sizes are off, that's a red flag
						if (jobRequestInputValue.size() != previousJobInputValue.size()) {
							throw new JobProcessingException(400,
									"Invalid value for " + softwareInput.getKey()
									+ ". The value of the hidden input " + softwareInput.getKey()
									+ " has changed since the job was last run. To override hidden "
									+ "inputs on job resubmissions, set the ignoreInputConflicts "
									+ "field to true in the request. ");
						}
						// make sure the composition of the two arrays is identical. We don't
						// care about ordering per se, just composition.
						else {
							List<String> newValues = new ArrayList<String>();
							for (int i=0; i<jobRequestInputValue.size(); i++) {
								newValues.add(jobRequestInputValue.get(i).asText());
							}
							
							List<String> oldValues = new ArrayList<String>();
							for (int i=0; i<previousJobInputValue.size(); i++) {
								oldValues.add(previousJobInputValue.get(i).asText());
							}
							
							// this will make sure that the cardinality of repeated values 
							// is identical as well
							if (!CollectionUtils.subtract(newValues, oldValues).isEmpty() && 
									!CollectionUtils.subtract(oldValues, newValues).isEmpty()) {
								throw new JobProcessingException(400,
										"Invalid value for " + softwareInput.getKey()
										+ ". The value of the hidden input " + softwareInput.getKey()
										+ " has changed since the job was last run. To ignore hidden "
										+ "input conflicts on job resubmissions, set the ignoreInputConflicts "
										+ "field to true in the request. ");
							}
						}
					}
				} 
				// missing fields could be completely optional
				else if (!jobRequestMap.containsKey(softwareInput.getKey())) {
					// if it's requried, that's a problem
					if (softwareInput.isRequired()) {
						throw new JobProcessingException(400,
								"Missing required input " + softwareInput.getKey()
								+ ". The  input field " + softwareInput.getKey()
								+ " has changed since the job was last run and is now required. "
								+ "Resubmission of this job will not be possible due to this "
								+ "change in the app definition. ");
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
				
				getJobInputs().put(softwareInput.getKey(), jobRequestInputValue);
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
		
		return getJobInputs();
	}

	/**
	 * @return the ignoreInputConflicts
	 */
	public boolean isIgnoreInputConflicts() {
		return ignoreInputConflicts;
	}

	/**
	 * @param ignoreInputConflicts the ignoreInputConflicts to set
	 */
	public void setIgnoreInputConflicts(boolean ignoreInputConflicts) {
		this.ignoreInputConflicts = ignoreInputConflicts;
	}
	
}
