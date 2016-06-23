/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;

import com.fasterxml.jackson.databind.JsonNode;
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
//		// Verify the parameters by their keys given in the
//		// SoftwareParameter in the Software object.
//		this.jobParameters = mapper.createObjectNode();
//		
//		for (SoftwareParameter softwareParameter : software.getParameters())
//		{
////			ArrayNode jobRequestParamValue = null;
//			
//			doProcessSoftwareParameterValue(softwareParameter, 
//					jobRequestMap.get(softwareParameter.getKey()));
//			
//			try
//			{
//				// add hidden parameters into the input array so we have a full record
//				// of all parameters for this job in the history.
//				if (!softwareParameter.isVisible())
//				{
//					// hidden fields are required
//					if (jobRequestMap.containsKey(softwareParameter.getKey())) {
//						throw new JobProcessingException(400,
//								"Invalid hidden parameter value for " + softwareParameter.getKey() +
//								". " + softwareParameter.getKey() + " is a fixed value that "
//								+ "cannot be set manually. Pleaes contact the app owner to fix the "
//								+ "app definition.");
//					} 
//					// validate the default value to ensure it's still valid
//					else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
//							softwareParameter.getType().equals(SoftwareParameterType.flag)) {
//						
//						// process just the first value. doesn't make sense to do anyting else.
//						if (softwareParameter.getDefaultValueAsJsonArray().size() > 0) {
//							boolean defaultValue = doProcessSingleValue(softwareParameter, (boolean)softwareParameter.getDefaultValueAsJsonArray().get(0));
//							jobParameters.put(softwareParameter.getKey(), defaultValue);
//						} 
//						// no default, set false
//						else {
//							jobParameters.put(softwareParameter.getKey(), false);
//						}
//					} else {
//						ArrayNode defaultValue = doProcessSingleValue(softwareParameter, softwareParameter.getDefaultValueAsJsonArray());
//						jobParameters.put(softwareParameter.getKey(), defaultValue);
//					}
//				}
//				// missing fields could be completely optional
//				else if (!jobRequestMap.containsKey(softwareParameter.getKey()))
//				{
//					if (softwareParameter.isRequired()) {
//						throw new JobProcessingException(400,
//								"No input parameter specified for required parameter " 
//								+ softwareParameter.getKey());
//					}
//					else {
//						continue;
//					}
//				}
//				else
//				{	
//					ArrayNode validatedValueArray = doProcessSingleValue(softwareParameter, jobRequestMap.get(softwareParameter.getKey());
//					jobParameters.put(softwareParameter.getKey(), validatedValueArray);
//				}
//			}
//		}
//		
//		/**
//		 * Processes a single value provided for a {@link SoftwareParameter} field in a 
//		 * job request 
//		 * @param softwareParameter
//		 * @param value a single parameter value
//		 * @return
//		 * @throws PermissionException
//		 */
//		protected ArrayNode doProcessSoftwareParameterValue(SoftwareParameter softwareParameter, Object value) 
//		throws PermissionException, JobProcessingException
//		{
//			if (value != null) {
//				if (value instanceof String) {
//					return doProcessSoftwareParameterValue(softwareParameter, (String)value);
//				}
//				else if (value instanceof String[]) {
//					return doProcessSoftwareParameterValue(softwareParameter, (String)value);
//				}
//				else if (value instanceof ArrayNode) {
//					return doProcessSoftwareParameterValue(softwareParameter, (ArrayNode)value);
//				}
//				else {
//					throw new JobProcessingException(400,
//							"Unsupported parameter type for input " + softwareParameter.getKey());
//				}
//			}
//			else {
//				return doProcessSoftwareParameterValue(softwareParameter, this.mapper.createArrayNode());
//			}
//			
//		}
//		
//		/**
//		 * Processes a single value provided for a {@link SoftwareParameter} field in a 
//		 * job request 
//		 * @param softwareParameter
//		 * @param value a single serialized parameter value
//		 * @return
//		 * @throws PermissionException
//		 */
//		protected ArrayNode doProcessSoftwareParameterValue(SoftwareParameter softwareParameter, String value) 
//		throws PermissionException, JobProcessingException
//		{
//			ArrayNode jobRequestParameterValue = this.mapper.createArrayNode();
//			
//			if (value != null) {
//				for (String token: StringUtils.split(value, ";")) {
//					jobRequestParameterValue.add(token);
//				}
//			}
//			
//			return doProcessSoftwareParameterValue(softwareParameter, jobRequestParameterValue);
//			
//		}
//			
//		/**
//		 * Processes a single value provided for a {@link SoftwareParameter} field in a 
//		 * job request 
//		 * @param softwareParameter
//		 * @param value a single serialized parameter value
//		 * @return
//		 * @throws PermissionException
//		 */
//		protected ArrayNode doProcessSoftwareParameterValue(SoftwareParameter softwareParameter, String[] values) 
//		throws PermissionException, JobProcessingException
//		{
//			ArrayNode jobRequestParameterValue = this.mapper.createArrayNode();
//			
//			if (values != null) {
//				for (String value: values) {
//					for (String token: StringUtils.split(value, ";")) {
//						jobRequestParameterValue.add(token);
//					}
//				}
//			}
//			
//			return doProcessSoftwareParameterValue(softwareParameter, jobRequestParameterValue);
//		}
//		
//		/**
//		 * Processes a single value provided for a {@link SoftwareParameter} field in a 
//		 * job request 
//		 * @param softwareInput
//		 * @param valueArray an JSON {@link ArrayNode} of single input string values
//		 * @return
//		 * @throws PermissionException
//		 */
//		protected ArrayNode doProcessSoftwareParameterValue(SoftwareParameter softwareParameter, ArrayNode valueArray) 
//		throws PermissionException, JobProcessingException
//		{	
//			ArrayNode jobRequestParameterValue = mapper.createArrayNode();
//			
//			if (softwareParameter.getMinCardinality() > valueArray.size())
//			{
//				throw new JobProcessingException(400,
//						softwareParameter.getKey() + " requires at least " +
//								softwareParameter.getMinCardinality() + " values");
//			}
//			else if (softwareParameter.getMaxCardinality() != -1 &&
//				softwareParameter.getMaxCardinality() < valueArray.size())
//			{
//				throw new JobProcessingException(400,
//						softwareParameter.getKey() + " may have at most " +
//						softwareParameter.getMaxCardinality() + " values");
//			}
//			else if (softwareParameter.getType().equals(SoftwareParameterType.enumeration))
//			{
//				List<String> validParamValues = null;
//				try {
//					validParamValues = softwareParameter.getEnumeratedValuesAsList();
//				} catch (SoftwareException e) {
//					throw new JobProcessingException(400,
//							"Unable to validate parameter value for " + softwareParameter.getKey() +
//							" against the enumerated values defined for this parameter.", e);
//				}
//
//				if (validParamValues.isEmpty()) {
//					throw new JobProcessingException(400,
//							"Invalid parameter value for " + softwareParameter.getKey() +
//							". No valid enumerated values are defined for this app");
//				}
//				else if (valueArray.size() == 0) {
//					continue;
//				}
//				else {
//					
//					for (Iterator<JsonNode> iter = valueArray.iterator(); iter.hasNext(); ) {
//						JsonNode json = iter.next();
//						
//						// only string values are supported
//						if (!json.isValueNode()) {
//							throw new JobProcessingException(400,
//									"Invalid parameter value, "
//									+ json.toString()
//									+ ", for "
//									+ softwareParameter.getKey()
//									+ ". Value must be a string value representing "
//									+ "a file or folder for which you have access.");
//						}
//						// ignore empty values. default values are not autofilled by agave.
//						// users must supply them on their own. The long exception being
//						// when a SoftwareInput is not visible. In that situation, the 
//						// default value is always injected into the job request and
//						// already present here.
//						else if (json.isNull()) {
//							continue;
//						}
//						
//						String sValue = doProcessSingleValue(softwareParameter, json);
//						
//						jobRequestParameterValue.add(sValue);
//						
//					for (String jobParam: explodedParameters)
//					{
//						if (validParamValues.contains(jobParam))
//						{
//							if (explodedParameters.length == 1) {
//								jobParameters.put(softwareParameter.getKey(), jobParam);
//							} else {
//								validatedJobParamValueArray.add(jobParam);
//							}
//						}
//						else
//						{
//							throw new JobProcessingException(400,
//									"Invalid parameter value, " + jobParam + ", for " + softwareParameter.getKey() +
//									". Value must be one of: " + ServiceUtils.explode(",  ", validParamValues));
//						}
//					}
//
//					if (validatedJobParamValueArray.size() > 1) {
//						jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//					}
//				}
//			}
//			else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
//					softwareParameter.getType().equals(SoftwareParameterType.flag))
//			{
//				if (explodedParameters.length > 1)
//				{
//					throw new JobProcessingException(400,
//							"Invalid parameter value for " + softwareParameter.getKey() +
//							". Boolean and flag parameters do not support multiple values.");
//				}
//				else if (explodedParameters.length == 0) {
//					continue;
//				}
//				else
//				{
//					String inputValue = explodedParameters[0];
//					if (inputValue.toString().equalsIgnoreCase("true")
//							|| inputValue.toString().equals("1")
//							|| inputValue.toString().equalsIgnoreCase("on"))
//					{
//						jobParameters.put(softwareParameter.getKey(), true);
//					}
//					else if (inputValue.toString().equalsIgnoreCase("false")
//							|| inputValue.toString().equals("0")
//							|| inputValue.toString().equalsIgnoreCase("off"))
//					{
//						jobParameters.put(softwareParameter.getKey(), false);
//					}
//					else
//					{
//						throw new JobProcessingException(400,
//								"Invalid parameter value for " + softwareParameter.getKey() +
//								". Value must be a boolean value. Use 1,0 or true/false as available values.");
//					}
//				}
//			}
//			else if (softwareParameter.getType().equals(SoftwareParameterType.number))
//			{
//				if (explodedParameters.length == 0) {
//					continue;
//				}
//				else
//				{
//					for (String jobParam: explodedParameters)
//					{
//						try
//						{
//							if (NumberUtils.isDigits(jobParam))
//							{
//								if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
//									if (explodedParameters.length == 1) {
//										jobParameters.put(softwareParameter.getKey(), new Long(jobParam));
//									} else {
//										validatedJobParamValueArray.add(new Long(jobParam));
//									}
//								} else {
//									throw new JobProcessingException(400,
//											"Invalid parameter value for " + softwareParameter.getKey() +
//											". Value must match the regular expression " +
//											softwareParameter.getValidator());
//								}
//
//							}
//							else if (NumberUtils.isNumber(jobParam))
//							{
//								if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
//									if (explodedParameters.length == 1) {
//										jobParameters.put(softwareParameter.getKey(), new BigDecimal(jobParam).toPlainString());
//									} else {
//										validatedJobParamValueArray.add(new BigDecimal(jobParam).toPlainString());
//									}
//
//								} else {
//									throw new JobProcessingException(400,
//											"Invalid parameter value for " + softwareParameter.getKey() +
//											". Value must match the regular expression " +
//											softwareParameter.getValidator());
//								}
//							}
//						} catch (NumberFormatException e) {
//							throw new JobProcessingException(400,
//									"Invalid parameter value for " + softwareParameter.getKey() +
//									". Value must be a number.");
//						}
//					}
//
//					if (validatedJobParamValueArray.size() > 1) {
//						jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//					}
//				}
//			}
//			else // string parameter
//			{
//				if (explodedParameters.length == 0) {
//					continue;
//				}
//				else
//				{
//					for (String jobParam: explodedParameters)
//					{
//						if (jobParam == null)
//						{
//							continue;
//						}
//						else
//						{
//							if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator()))
//							{
//								validatedJobParamValueArray.add(jobParam);
//							}
//							else
//							{
//								throw new JobProcessingException(400,
//										"Invalid parameter value for " + softwareParameter.getKey() +
//										". Value must match the regular expression " +
//										softwareParameter.getValidator());
//							}
//						}
//					}
//
//					if (validatedJobParamValueArray.size() == 1) {
//						jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray.iterator().next().asText());
//					} else {
//						jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//					}
//				}
//			}
//		}
//	}
//	catch (JobProcessingException e) {
//		throw e;
//	}
//	catch (Exception e)
//	{
//		throw new JobProcessingException(500,
//				"Failed to parse parameter "+ softwareParameter.getKey(), e);
//	}
//}
//
//	private boolean doProcessSingleValue(SoftwareParameter softwareParameter,
//			boolean b) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	}
//
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