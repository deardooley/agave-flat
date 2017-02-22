/**
 * 
 */
package org.iplantc.service.jobs.managers;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class JobRequestParameterProcessor {

	private ObjectNode jobParameters;
	private ObjectMapper mapper;
	private Software software;
	
	public JobRequestParameterProcessor(Software software) {
		this.setSoftware(software);
		this.setMapper(new ObjectMapper());
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
		
		this.jobParameters = getMapper().createObjectNode();
		
		for (SoftwareParameter softwareParameter : software.getParameters())
		{
			ArrayNode validatedJobParamValueArray = getMapper().createArrayNode();

			try
			{
				// add hidden parameters into the input array so we have a full record
				// of all parameters for this job in the history.
				if (!softwareParameter.isVisible())
				{
					if (jobRequestMap.containsKey(softwareParameter.getKey())) {
						throw new JobProcessingException(400,
								"Invalid parameter value for " + softwareParameter.getKey() +
								". " + softwareParameter.getKey() + " is a fixed value that "
								+ "cannot be set manually. ");
					} else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
							softwareParameter.getType().equals(SoftwareParameterType.flag)) {
						if (softwareParameter.getDefaultValueAsJsonArray().size() > 0) {
							jobParameters.put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray().get(0));
						} else {
							jobParameters.put(softwareParameter.getKey(), false);
						}
					} else {
						jobParameters.put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray());
					}
				}
				else if (!jobRequestMap.containsKey(softwareParameter.getKey()))
				{
					if (softwareParameter.isRequired())
					{
						throw new JobProcessingException(400,
								"No input parameter specified for " + softwareParameter.getKey());
					}
					else
					{
						continue;
					}
				}
				else
				{
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
										jobParameters.put(softwareParameter.getKey(), jobParam);
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
								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
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
								jobParameters.put(softwareParameter.getKey(), true);
							}
							else if (inputValue.toString().equalsIgnoreCase("false")
									|| inputValue.toString().equals("0")
									|| inputValue.toString().equalsIgnoreCase("off"))
							{
								jobParameters.put(softwareParameter.getKey(), false);
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
												jobParameters.put(softwareParameter.getKey(), new Long(jobParam));
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
												jobParameters.put(softwareParameter.getKey(), new BigDecimal(jobParam).toPlainString());
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
								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
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
								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray.iterator().next().asText());
							} else {
								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
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
	 * @return the jobParameters
	 */
	public ObjectNode getJobParameters() {
		return jobParameters;
	}

	/**
	 * @param jobParameters the jobParameters to set
	 */
	public void setJobParameters(ObjectNode jobParameters) {
		this.jobParameters = jobParameters;
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