/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.jobs.exceptions.JobProcessingException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author dooley
 *
 */
public class JobResubmissionRequestParameterProcessor extends JobRequestParameterProcessor {

	private boolean ignoreParameterConflicts = false;
	
	public JobResubmissionRequestParameterProcessor(Software software, boolean ignoreParameterConflicts) {
		super(software);
		setIgnoreParameterConflicts(ignoreParameterConflicts);
	}
	/**
	 * Validates the {@link SoftwareParameter} values passed into
	 * a job request.
	 * @param software
	 * @param jobRequestMap
	 * @throws JobProcessingException
	 */
	public void process(Map<String, Object> jobRequestMap) 
	throws JobProcessingException {
		
		setJobParameters(getMapper().createObjectNode());
		
		for (SoftwareParameter softwareParameter : getSoftware().getParameters())
		{
			ArrayNode validatedJobParamValueArray = getMapper().createArrayNode();

			try
			{
				// add hidden parameters into the parameter array so we have a full record
				// of all parameters for this job in the history.
				if (!softwareParameter.isVisible())
				{
					// if overriding is present, then force it here. this will 
					// replace whatever was there before.
					if (isIgnoreParameterConflicts()) { 
						getJobParameters().put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray());
					}
					// if overriding is present, then force it here. this will 
					// replace whatever was there before.
					else if (!jobRequestMap.containsKey(softwareParameter.getKey())) {
						
						throw new JobProcessingException(400,
								"Invalid value for " + softwareParameter.getKey()
								+ ". No value for the hidden parameter " + softwareParameter.getKey()
								+ " was present in the original job. This usually indicates that "
								+ "the app definition has changed since the job was last run. "
								+ "To override hidden parameters on job resubmissions, set the ignoreParameterConflicts "
								+ "field to true in the request. ");
					}
					else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
							softwareParameter.getType().equals(SoftwareParameterType.flag)) {
						
						boolean oldParameterValue = Boolean.valueOf(jobRequestMap.get(softwareParameter.getKey()).toString()); 
						boolean newParameterValue = false;
						
						if (softwareParameter.getDefaultValueAsJsonArray().size() > 0) {
							newParameterValue = softwareParameter.getDefaultValueAsJsonArray().asBoolean();
						} 
						
						if (oldParameterValue != newParameterValue) {
							throw new JobProcessingException(400,
									"Invalid value for " + softwareParameter.getKey()
									+ ". The value of the hidden parameter " + softwareParameter.getKey()
									+ " has changed since the job was last run. To override hidden "
									+ "parameters on job resubmissions, set the ignoreParameterConflicts "
									+ "field to true in the request. ");
						}
						else {
							getJobParameters().put(softwareParameter.getKey(), newParameterValue);
						}
					}
					// the hidden field was there before and now. Verify the value is identical since it's 
					// it's possible it was not hidden when they ran the job originally. 
					else {
						
						// resolve the current default value for this parameter
						ArrayNode jobRequestParameterValue = softwareParameter.getDefaultValueAsJsonArray();
						
						// resolve the previous value from the job 
						String sOldJobRequestParameterValue = (String)jobRequestMap.get(softwareParameter.getKey());
						if (StringUtils.isEmpty(sOldJobRequestParameterValue)) {
							throw new JobProcessingException(400,
									"Invalid value for " + softwareParameter.getKey()
									+ ". The value of the hidden parameter " + softwareParameter.getKey()
									+ " has changed since the job was last run. To ignore hidden "
									+ "parameter conflicts on job resubmissions, set the ignoreParameterConflicts "
									+ "field to true in the request. ");
						} 
						else {
							try {
								JsonNode tmpParam = getMapper().readTree(sOldJobRequestParameterValue);
								if (tmpParam.isArray() && tmpParam.size() == 0) {
									throw new JobProcessingException(400,
											"Invalid value for " + softwareParameter.getKey()
											+ ". The value of the hidden parameter " + softwareParameter.getKey()
											+ " has changed since the job was last run. To ignore hidden "
											+ "parameter conflicts on job resubmissions, set the ignoreParameterConflicts "
											+ "field to true in the request. ");
								}
								else if (tmpParam.isValueNode() && jobRequestParameterValue.size() == 1 && 
									!StringUtils.equals(jobRequestParameterValue.get(0).textValue(), tmpParam.textValue())) { 
									throw new JobProcessingException(400,
											"Invalid value for " + softwareParameter.getKey()
											+ ". The value of the hidden parameter " + softwareParameter.getKey()
											+ " has changed since the job was last run. To ignore hidden "
											+ "parameter conflicts on job resubmissions, set the ignoreParameterConflicts "
											+ "field to true in the request. ");
								}
								else {
									getJobParameters().put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray());
								}
							}
							catch (IOException e) {
								if (!StringUtils.equals(sOldJobRequestParameterValue, 
										softwareParameter.getDefaultValueAsJsonArray().toString())) {
									throw new JobProcessingException(400,
											"Invalid value for " + softwareParameter.getKey()
											+ ". The value of the hidden parameter " + softwareParameter.getKey()
											+ " has changed since the job was last run. To ignore hidden "
											+ "parameter conflicts on job resubmissions, set the ignoreParameterConflicts "
											+ "field to true in the request. ");
								}
							}
							 
						}
					}
				}
				else if (!jobRequestMap.containsKey(softwareParameter.getKey())) {
					
					if (softwareParameter.isRequired()) {
						throw new JobProcessingException(400,
								"Missing required parameter " + softwareParameter.getKey()
								+ ". The parameter field " + softwareParameter.getKey()
								+ " has changed since the job was last run and is now required. "
								+ "Resubmission of this job will not be possible due to this "
								+ "change in the app definition. ");
					}
					else {
						continue;
					}
				}
				else {
					
					String[] explodedParameters = null;
					if (jobRequestMap.get(softwareParameter.getKey()) == null) {
//						explodedParameters = new String[]{};
						continue;
					} else if (jobRequestMap.get(softwareParameter.getKey()) instanceof String[]) {
						explodedParameters = (String[])jobRequestMap.get(softwareParameter.getKey());

					} else {
						explodedParameters = StringUtils.split((String)jobRequestMap.get(softwareParameter.getKey()), ";");
//						explodedParameters = new String[]{(String)pTable.get(softwareParameter.getKey())};
					}

					if (softwareParameter.getMinCardinality() > explodedParameters.length)
					{
						throw new JobProcessingException(400,
								softwareParameter.getKey() + " requires at least " +
										softwareParameter.getMinCardinality() + " values");
					}
					else if (softwareParameter.getMaxCardinality() != -1 &&
						softwareParameter.getMaxCardinality() < explodedParameters.length)
					{
						throw new JobProcessingException(400,
								softwareParameter.getKey() + " may have at most " +
								softwareParameter.getMaxCardinality() + " values");
					}
					else if (softwareParameter.getType().equals(SoftwareParameterType.enumeration))
					{
						List<String> validParamValues = null;
						try {
							validParamValues = softwareParameter.getEnumeratedValuesAsList();
						} catch (SoftwareException e) {
							throw new JobProcessingException(400,
									"Unable to validate parameter value for " + softwareParameter.getKey() +
									" against the enumerated values defined for this parameter.", e);
						}

						if (validParamValues.isEmpty())
						{
							throw new JobProcessingException(400,
									"Invalid parameter value for " + softwareParameter.getKey() +
									". Value must be one of: " + ServiceUtils.explode(",  ", validParamValues));
						}
						else if (explodedParameters.length == 0) {
							continue;
						}
						else
						{
							for (String jobParam: explodedParameters)
							{
								if (validParamValues.contains(jobParam))
								{
									if (explodedParameters.length == 1) {
										getJobParameters().put(softwareParameter.getKey(), jobParam);
									} else {
										validatedJobParamValueArray.add(jobParam);
									}
								}
								else
								{
									throw new JobProcessingException(400,
											"Invalid parameter value, " + jobParam + ", for " + softwareParameter.getKey() +
											". Value must be one of: " + ServiceUtils.explode(",  ", validParamValues));
								}
							}

							if (validatedJobParamValueArray.size() > 1) {
								getJobParameters().put(softwareParameter.getKey(), validatedJobParamValueArray);
							}
						}
					}
					else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
							softwareParameter.getType().equals(SoftwareParameterType.flag))
					{
						if (explodedParameters.length > 1)
						{
							throw new JobProcessingException(400,
									"Invalid parameter value for " + softwareParameter.getKey() +
									". Boolean and flag parameters do not support multiple values.");
						}
						else if (explodedParameters.length == 0) {
							continue;
						}
						else
						{
							String inputValue = explodedParameters[0];
							if (inputValue.toString().equalsIgnoreCase("true")
									|| inputValue.toString().equals("1")
									|| inputValue.toString().equalsIgnoreCase("on"))
							{
								getJobParameters().put(softwareParameter.getKey(), true);
							}
							else if (inputValue.toString().equalsIgnoreCase("false")
									|| inputValue.toString().equals("0")
									|| inputValue.toString().equalsIgnoreCase("off"))
							{
								getJobParameters().put(softwareParameter.getKey(), false);
							}
							else
							{
								throw new JobProcessingException(400,
										"Invalid parameter value for " + softwareParameter.getKey() +
										". Value must be a boolean value. Use 1,0 or true/false as available values.");
							}
						}
					}
					else if (softwareParameter.getType().equals(SoftwareParameterType.number))
					{
						if (explodedParameters.length == 0) {
							continue;
						}
						else
						{
							for (String jobParam: explodedParameters)
							{
								try
								{
									if (NumberUtils.isDigits(jobParam))
									{
										if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
											if (explodedParameters.length == 1) {
												getJobParameters().put(softwareParameter.getKey(), new Long(jobParam));
											} else {
												validatedJobParamValueArray.add(new Long(jobParam));
											}
										} else {
											throw new JobProcessingException(400,
													"Invalid parameter value for " + softwareParameter.getKey() +
													". Value must match the regular expression " +
													softwareParameter.getValidator());
										}

									}
									else if (NumberUtils.isNumber(jobParam))
									{
										if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
											if (explodedParameters.length == 1) {
												getJobParameters().put(softwareParameter.getKey(), new BigDecimal(jobParam).toPlainString());
											} else {
												validatedJobParamValueArray.add(new BigDecimal(jobParam).toPlainString());
											}

										} else {
											throw new JobProcessingException(400,
													"Invalid parameter value for " + softwareParameter.getKey() +
													". Value must match the regular expression " +
													softwareParameter.getValidator());
										}
									}
								} catch (NumberFormatException e) {
									throw new JobProcessingException(400,
											"Invalid parameter value for " + softwareParameter.getKey() +
											". Value must be a number.");
								}
							}

							if (validatedJobParamValueArray.size() > 1) {
								getJobParameters().put(softwareParameter.getKey(), validatedJobParamValueArray);
							}
						}
					}
					else // string parameter
					{
						if (explodedParameters.length == 0) {
							continue;
						}
						else
						{
							for (String jobParam: explodedParameters)
							{
								if (jobParam == null)
								{
									continue;
								}
								else
								{
									if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator()))
									{
										validatedJobParamValueArray.add(jobParam);
									}
									else
									{
										throw new JobProcessingException(400,
												"Invalid parameter value for " + softwareParameter.getKey() +
												". Value must match the regular expression " +
												softwareParameter.getValidator());
									}
								}
							}

							if (validatedJobParamValueArray.size() == 1) {
								getJobParameters().put(softwareParameter.getKey(), validatedJobParamValueArray.iterator().next().asText());
							} else {
								getJobParameters().put(softwareParameter.getKey(), validatedJobParamValueArray);
							}
						}
					}
				}
			}
			catch (JobProcessingException e) {
				throw e;
			}
			catch (Exception e)
			{
				throw new JobProcessingException(500,
						"Failed to parse parameter "+ softwareParameter.getKey(), e);
			}
		}
	}

	/**
	 * @return the ignoreParameterConflicts
	 */
	public boolean isIgnoreParameterConflicts() {
		return ignoreParameterConflicts;
	}
	/**
	 * @param ignoreParameterConflicts the ignoreParameterConflicts to set
	 */
	public void setIgnoreParameterConflicts(boolean ignoreParameterConflicts) {
		this.ignoreParameterConflicts = ignoreParameterConflicts;
	}
}