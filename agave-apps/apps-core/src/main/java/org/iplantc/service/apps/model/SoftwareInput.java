/**
 *
 */
package org.iplantc.service.apps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.stevesoft.pat.Regex;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "software_inputs")
public class SoftwareInput implements SoftwareAttribute<SoftwareInput>
{
	private Long		id;
	private Software	software;				// the software associated with
												// this input
	private String		key;					// Mandatory. The name of the
												// parameter. This is passed to
												// the execute environment
	private String		defaultValue;			// Mandatory. Comma-separated
												// list of default values. Files
												// are described as URIs.
												// Omitting URL schema is a
												// shortcut for looking relative
												// to the submitting user's
												// $IPLANTHOME directory.
	private int			minCardinality = 1;		// Mandatory. Minumim of one inputFile specified
	private int			maxCardinality = 1;	// Mandatory. Minumim of one

	private String		label;					// Optional. Human-readable text
												// label
	private String		description;			// Optional. Supplemental text
												// description
	private String		argument;				// name as it appears when used on the command line
	private String		fileTypes;				// Optional. Comma-separated
												// list of valid fileTypes
												// provided by
												// /data/transforms/list
	private String		ontology;				// Optional. Comma-separated
												// list of fully qualified URLs
												// pointing to ontology
	private Integer		order 		= 0;		// order in which this input appears
	private String 		validator;
	private boolean		showArgument = false;
	private boolean		repeatArgument = false;
	private boolean		visible		= true;
	private boolean		required	= false;
	private boolean		enquote		= false;	// should this value(s) be enquoted prior to injecting in the wrapper template
	private Date		lastUpdated = new Date();
	private Date		created		= new Date();

	public SoftwareInput() {}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the software
	 */
	@ManyToOne(fetch = FetchType.LAZY)
    public Software getSoftware()
	{
		return software;
	}

	/**
	 * @param software
	 *            the software to set
	 */
	public void setSoftware(Software software)
	{
		this.software = software;
	}

	/**
	 * @return the key
	 */
	@Column(name = "output_key", nullable = false, length = 64)
	public String getKey()
	{
		return key;
	}

	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(String key)
	{
		if (StringUtils.isEmpty(key)) {
			throw new SoftwareException("No key specified");
		} else if (key.length() > 64) {
			throw new SoftwareException("'software.input.id' must be less than 64 characters");
		}

//		if (!key.equals(key.replaceAll( "[^0-9a-zA-Z\\.\\-\\_\\(\\)]" , ""))) {
//			throw new SoftwareException("'software.input.id' may only contain alphanumeric characters, periods, and dashes.");
//		}

		this.key = key;
	}

	/**
	 * @return the defaultValue
	 */
	@Column(name = "default_value", nullable = true, length = 255)
	private String getDefaultValue()
	{
		return defaultValue;
	}

	/**
	 * Returns an ArrayNode containing the default values for this SoftwareInput.
	 * It will parse a valid JSON array, a semicolon delimited list, or a single string value.
	 * Null default values will return an empty ArrayNode.
	 *
	 * @return ArrayNode with the default values or empty if there are none.
	 */
	@Transient
	public ArrayNode getDefaultValueAsJsonArray()
	{
		ArrayNode arrayNode = new ObjectMapper().createArrayNode();
		if (StringUtils.isEmpty(getDefaultValue())) {
			return arrayNode;
		}
		else if (ServiceUtils.isJsonArrayOfStrings(getDefaultValue()))
		{
			try
			{
				for (String value: ServiceUtils.getStringValuesFromJsonArray(getDefaultValue(), false)) {
					arrayNode.add(value);
				}
			}
			catch (Exception e) {
				arrayNode.add(getDefaultValue());
			}
		}
		else if (StringUtils.contains(getDefaultValue(), ";"))
		{
			for (String value: StringUtils.split(getDefaultValue(), ";")) {
				if (StringUtils.isNotBlank(value)) {
					arrayNode.add(value);
				}
			}
		}
		else {
			arrayNode.add(getDefaultValue());
		}

		return arrayNode;
	}

	/**
	 * @param value
	 *            the default value to set
	 */
	public void setDefaultValue(String defaultValue)
	{
		if (StringUtils.length(defaultValue) > 255) {
			throw new SoftwareException("'software.input.value.default' must be less than 255 characters");
		}
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the minCardinality
	 */
	@Column(name = "min_cardinality", nullable = true, length = 3)
	public int getMinCardinality()
	{
		return minCardinality;
	}

	/**
	 * @param minCardinality
	 *            the minCardinality to set
	 */
	public void setMinCardinality(int minCardinality)
	{
		this.minCardinality = minCardinality;
	}

	/**
	 * @return the maxCardinality
	 */
	@Column(name = "max_cardinality", nullable = true, length = 3)
	public int getMaxCardinality()
	{
		return maxCardinality;
	}

	/**
	 * @param maxCardinality
	 *            the maxCardinality to set
	 */
	public void setMaxCardinality(int maxCardinality)
	{
		this.maxCardinality = maxCardinality;
	}

	/**
	 * @return the label
	 */
	@Column(name = "label", nullable = true, length = 128)
	public String getLabel()
	{
		if (StringUtils.isEmpty(label)) {
			return key;
		}

		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label)
	{
		if (StringUtils.length(label) > 128) {
			throw new SoftwareException("'software.input.details.label' must be less than 128 characters");
		}

		this.label = label;
	}

	/**
	 * @return the description
	 */
	@Column(name = "description", nullable = true, length = 1024)
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description)
	{
		if (StringUtils.length(description) > 1024) {
			throw new SoftwareException("'software.input.details.description' must be less than 1024 characters");
		}

		this.description = description;
	}

	/**
	 * @return the fileTypes scrubbed and inserted into a list.
	 */
	@Transient
	public List<String> getFileTypesAsList()
	{
		if (StringUtils.isNotEmpty(getFileTypes()))
		{
			List<String> filteredFileTypes = new ArrayList<String>();
			String[] ontologies = StringUtils.split(getFileTypes(), ",");
			for(String fileType: ontologies) {
				fileType = StringUtils.trimToNull(fileType);
				if (fileType != null) {
					filteredFileTypes.add(fileType);
				}
			}
			return filteredFileTypes;
		}
		else
		{
			return new ArrayList<String>();
		}
	}

	/**
	 * @return the fileTypes
	 */
	@Column(name = "file_types", nullable = true, length = 128)
	private String getFileTypes()
	{
		return fileTypes;
	}

	/**
	 * @param fileTypes
	 *            the fileTypes to set
	 */
	public void setFileTypes(String fileTypes)
	{
		if (StringUtils.length(fileTypes) > 128) {
			throw new SoftwareException("'software.input.semantics.fileTypes' must be less than 128 characters");
		}

		this.fileTypes = fileTypes;
	}

	/**
	 * @return the ontologies scrubbed and inserted into a list.
	 */
	@Transient
	public List<String> getOntologyAsList()
	{
		if (StringUtils.isNotEmpty(getOntology()))
		{
			List<String> filteredOntologies = new ArrayList<String>();
			String[] ontologies = StringUtils.split(getOntology(), ",");
			for(String term: ontologies) {
				term = StringUtils.replace(StringUtils.trimToNull(term),"\"", "");
				if (term != null) {
					filteredOntologies.add(term);
				}
			}
			return filteredOntologies;
		}
		else
		{
			return new ArrayList<String>();
		}
	}

	/**
	 * @return the ontology
	 */
	@Column(name = "ontology", nullable = true, length = 255)
	private String getOntology()
	{
		return ontology;
	}

	/**
	 * @param ontology
	 *            the ontology to set
	 */
	public void setOntology(String ontology)
	{
		if (StringUtils.length(ontology) > 255) {
			throw new SoftwareException("'software.input.semantics.ontology' must be less than 255 characters");
		}

//		try {
//			new URL(ontology);
//		} catch (MalformedURLException e) {
//			throw new SoftwareException("Invalid 'software.input.semantics.ontology' value. " +
//					"If specified, 'software.input.semantics.ontology' must be a valid URL");
//		}

		this.ontology = ontology;
	}

	/**
	 * @return the validator
	 */
	@Column(name = "validator", nullable = true, length = 255)
	public String getValidator()
	{
		return validator;
	}

	/**
	 * @param validator the validator to set
	 */
	public void setValidator(String validator)
	{
		if (StringUtils.length(validator) > 255) {
			throw new SoftwareException("'software.input.value.validator' must be less than 255 characters");
		}
		// verify the regex. We use Perl regex
		else if (!StringUtils.isEmpty(validator))
		{
			try {
				new Regex().compile(validator);
			} catch (Exception e) {
				throw new SoftwareException("'software.input.value.validator' is not a valid regular expression.", e);
			}
		}

		this.validator = validator;
	}

	/**
	 * Argument as it appears on the cli, ex -n or --name
	 * @return argument String value of the argument name
	 */
	@Column(name = "cli_argument", nullable = true, length = 64)
	public String getArgument() {
		return argument;
	}

	/**
	 * Set the argument as it appears on the cli, ex -n or --name
	 *
	 * @param argument
	 */
	public void setArgument(String argument) {
		if (StringUtils.length(argument) > 64) {
			throw new SoftwareException("'software.parameter.details.argument' must be less than 64 characters");
		}
		this.argument = argument;
	}

	/**
	 * Whether the argument text will be injected into the wrapper
	 * script at run time.
	 * @return showArgument true if the argument text will be injected, false otherwise
	 */
	@Column(name = "show_cli_argument", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isShowArgument() {
		return showArgument;
	}

	/**
	 * Sets whether the argument text will be injected into the wrapper
	 * script at run time.
	 *
	 * @param showArgument
	 */
	public void setShowArgument(boolean showArgument) {
		this.showArgument = showArgument;
	}

	/**
	 * Whether the argument text will be injected into the wrapper
	 * template once for each user-supplied input value or once
	 * per input.
	 *
	 * @return repeatArgument true if the argument text will repeated
	 * for each user-supplied parameter.
	 */
	@Column(name = "repeat_cli_argument", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isRepeatArgument() {
		return this.repeatArgument;
	}

	/**
	 * Sets whether the argument text will be injected into the wrapper template
	 * once for each user-supplied input value or once per input.
	 *
	 * @param repeatArgument
	 */
	public void setRepeatArgument(boolean repeatArgument) {
		this.repeatArgument = repeatArgument;
	}

	/**
	 * @param visible
	 *            the visible to set
	 */
	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	/**
	 * @return the visible
	 */
	@Column(name = "visible", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isVisible()
	{
		return visible;
	}

	/**
	 * @return the order
	 */
	@Column(name = "display_order", nullable=false, length=3)
	public Integer getOrder()
	{
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(Integer order)
	{
		this.order = order;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(boolean required)
	{
		this.required = required;
	}

	/**
	 * @return the required
	 */
	@Column(name = "required", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isRequired()
	{
		return required;
	}

	/**
	 * @return the enquote
	 */
	@Column(name = "enquote", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isEnquote()
	{
		return enquote;
	}

	/**
	 * @param enquote the enquote to set
	 */
	public void setEnquote(boolean enquote)
	{
		this.enquote = enquote;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	public String toString()
	{
		return String.format("%s - %s, %s",
	    		getKey(),
	    		isVisible() ? "visible" : "hidden",
	    		isRequired() ? "required" : "optional");
	}

	public boolean equals(Object o)
	{
		if (o instanceof SoftwareInput)
		{
			return ( (SoftwareInput) o ).key.equalsIgnoreCase(key);
		}
		return false;
	}

	public SoftwareInput clone(Software software)
	{
		SoftwareInput input = new SoftwareInput();
		input.setArgument(getArgument());
		input.setCreated(new Date());
		input.setDefaultValue(getDefaultValue());
		input.setDescription(getDescription());
		input.setEnquote(isEnquote());
		input.setFileTypes(getFileTypes());
		input.setKey(getKey());
		input.setLabel(getLabel());
		input.setLastUpdated(new Date());
		input.setMaxCardinality(getMaxCardinality());
		input.setMinCardinality(getMinCardinality());
		input.setOntology(getOntology());
		input.setOrder(getOrder());
		input.setRepeatArgument(isRepeatArgument());
		input.setRequired(isRequired());
		input.setShowArgument(isShowArgument());
		input.setSoftware(software);
		input.setValidator(getValidator());
		input.setVisible(isVisible());

		return input;
	}

	public String toJSON() throws JSONException
	{
		JSONWriter writer = new JSONStringer();

		printJSON(writer);

		return writer.toString();
	}

	public void printJSON(JSONWriter writer) throws JSONException
	{
		writer.object()
			.key("id").value(getKey())
			.key("value").object()
				.key("validator").value(getValidator())
				.key("visible").value(isVisible())
				.key("required").value(isRequired())
				.key("order").value(getOrder())
				.key("enquote").value(isEnquote());
				ArrayNode defaultValueArrayNode = getDefaultValueAsJsonArray();
				if (defaultValueArrayNode.size() == 0)
				{
					writer.key("default").value(null);
				}
				else if (getMaxCardinality() == 1) {
					writer.key("default").value(defaultValueArrayNode.get(0).textValue());
				}
				else
				{
					writer.key("default").array();
					JsonNode child = null;
					for(Iterator<JsonNode> iter = defaultValueArrayNode.iterator(); iter.hasNext(); )
					{
						child = iter.next();
						writer.value(child.asText());
					}
					writer.endArray();
				}
				writer.endObject()
			.key("details").object()
				.key("label").value(getLabel())
				.key("description").value(getDescription())
				.key("argument").value(getArgument())
				.key("showArgument").value(isShowArgument())
				.key("repeatArgument").value(isRepeatArgument())
				.endObject()
			.key("semantics").object()
				.key("minCardinality").value(getMinCardinality())
				.key("maxCardinality").value(getMaxCardinality())
				.key("ontology").array();
					for(String term: getOntologyAsList()) {
						writer.value(term);
					}
					writer.endArray()
				.key("fileTypes").array();
					for(String term: getFileTypesAsList()) {
						writer.value(term);
					}
					writer.endArray()
				.endObject()
			.endObject();
	}

	public static SoftwareInput fromJSON(JSONObject json)
	throws SoftwareException, JSONException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = null;
		try
		{
			jsonNode = objectMapper.readTree(json.toString());
		}
		catch (Exception e) {
			throw new SoftwareException("Invalid software parameter value", e);
		}

		SoftwareInput input = new SoftwareInput();
		if (jsonNode.has("id") && jsonNode.get("id").isTextual())
		{
			input.setKey(json.getString("id"));
		}
		else
		{
			throw new SoftwareException("No id attribute found for software input " + input.getKey() + " description");
		}

		// visible fields do not need a default value
		if (jsonNode.path("value").has("visible"))
		{
			if (ServiceUtils.isBooleanOrNumericBoolean(jsonNode.path("value").get("visible"))) {
				input.setVisible(jsonNode.path("value").get("visible").asBoolean());
			} else {
				throw new SoftwareException("Invalid visible value found for input " + input.getKey() +
						". If specified, visible must be a boolean value, 1, or 0.");
			}
		}
		else {
			input.setVisible(true);
		}

		if (jsonNode.path("value").has("required"))
		{
			if (ServiceUtils.isBooleanOrNumericBoolean(jsonNode.path("value").get("required"))) {
				input.setRequired(jsonNode.path("value").get("required").asBoolean());
			} else {
				throw new SoftwareException("Invalid required value found for input " + input.getKey() +
						". If specified, required must be a boolean value, 1, or 0.");
			}
		}
		else
		{
			input.setRequired(!input.isVisible());
		}

		if (jsonNode.has("semantics"))
		{
			JsonNode semantics = jsonNode.get("semantics");
			if (semantics.isObject())
			{
				if (semantics.has("ontology") && !semantics.get("ontology").isNull() )
				{
					if (semantics.get("ontology").isArray())
					{
						ArrayNode ontologyArrayNode = (ArrayNode)semantics.get("ontology");
						List<String> ontologyList = new ArrayList<String>();
						for(int i=0; i<ontologyArrayNode.size(); i++)
						{
							JsonNode child = ontologyArrayNode.get(i);
							if (!child.isTextual()) {
								 throw new SoftwareException("Invalid ontology value for input " + input.getKey() +
											". If specified, ontologies should be an array of valid string values");
							} else {
								if (StringUtils.isNotEmpty(child.asText()) &&
										!ontologyList.contains(ServiceUtils.enquote(child.asText()))) {
									ontologyList.add(ServiceUtils.enquote(child.asText()));
								}
							}
						}

						input.setOntology(StringUtils.join(ontologyList, ","));
					}
					else
					{
						throw new SoftwareException("ontology attribute for input " + input.getKey() +
								" is not a valid json array.");
					}
				}
				else {
					input.setOntology(null);
				}

				if (semantics.has("minCardinality") && !semantics.get("minCardinality").isNull())
				{
					if (semantics.get("minCardinality").isIntegralNumber()) {
						if (semantics.get("minCardinality").asInt() >= 0 ) {
							input.setMinCardinality(semantics.get("minCardinality").asInt());
						} else {
							throw new SoftwareException("Invalid minCardinality value found for input " + input.getKey() +
									". If specified, the minCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid minCardinality value found for input " + input.getKey() +
								". If specified, the minCardinality value must be an integer value greater than zero.");
					}
				}
				else
				{
					input.setMinCardinality(input.isRequired() || !input.isVisible() ? 1 : 0);
				}

				if (semantics.has("maxCardinality"))
				{
					if (semantics.get("maxCardinality").isIntegralNumber()) {
						int maxCard = semantics.get("maxCardinality").asInt();
						if (maxCard == -1 || maxCard > 0) {
							input.setMaxCardinality(maxCard);
						} else {
							throw new SoftwareException("Invalid maxCardinality value found for input " + input.getKey() +
									". If specified, the maxCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid maxCardinality value found for input " + input.getKey() +
								". If specified, the maxCardinality value must be an integer value greater than zero.");
					}
				}
				else
				{
					input.setMaxCardinality(1);
				}

				if (input.getMaxCardinality() < input.getMinCardinality() && input.getMaxCardinality() != -1) {
					throw new SoftwareException("Invalid maxCardinality value found for input " + input.getKey() +
							". If specified, the maxCardinality value must be greater than or equal to the "
							+ "minCardinality value.");
				}
				else if (input.getMaxCardinality() == 0) {
					throw new SoftwareException("Invalid maxCardinality value found for input " + input.getKey() +
							". If specified, the maxCardinality value must be greater than zero.");
				}

				if (semantics.has("fileTypes") && !semantics.get("fileTypes").isNull())
				{
					if (semantics.get("fileTypes").isArray())
					{
						ArrayNode fileTypesArrayNode = (ArrayNode)semantics.get("fileTypes");
						List<String> fileTypesList = new ArrayList<String>();
						for (int i=0; i< fileTypesArrayNode.size(); i++)
						{
							JsonNode child = fileTypesArrayNode.get(i);
							if (!child.isTextual()) {
								throw new SoftwareException("Invalid fileTypes value found for input " + input.getKey() +
										". If specified, the fileTypes value must be an array of names of valid file types.");
							}
							else
							{
								if (!fileTypesList.contains(child.asText())) {
									fileTypesList.add(child.asText());
								}
							}
						}
						input.setFileTypes(StringUtils.join(fileTypesList, ","));
					} else {
						throw new SoftwareException("Invalid fileTypes value found for input " + input.getKey() +
								". If specified, the fileTypes value must be an array of names of valid file types.");
					}
				}
				else {
					input.setFileTypes(null);
				}
			}
			else if (!semantics.isNull())
			{
				throw new SoftwareException("Invalid semantics attribute value found for input " +
						input.getKey() + ". If specified, the semantics attribute value must be a JSON object.");
			}
			else
			{
				input.setFileTypes(null);
				input.setMaxCardinality(1);
				input.setMinCardinality(input.isRequired() || !input.isVisible() ? 1 : 0);
				input.setOntology(null);
			}
		}
		else
		{
			input.setFileTypes(null);
			input.setMaxCardinality(1);
			input.setMinCardinality(input.isRequired() || !input.isVisible() ? 1 : 0);
			input.setOntology(null);
		}

		if (jsonNode.has("value"))
		{
			JsonNode value = jsonNode.get("value");

			if (value.isObject())
			{
				if (value.has("validator") && !value.get("validator").isNull())
				{
					if (value.get("validator").isTextual()) {
						String validator = value.get("validator").asText();
						input.setValidator(validator);
					} else {
						throw new SoftwareException("Invalid validator found for input " + input.getKey() +
								". If provided, validator must be a string regular expression.");
					}
				}
				else {
					input.setValidator(null);
				}

				if (value.has("default") && !value.get("default").isNull())
				{
					JsonNode defaultInputValue = value.get("default");

					if (defaultInputValue.isNull()) {
						input.setDefaultValue(null);
					}
					else if (defaultInputValue.isArray())
					{
						if (ServiceUtils.isJsonArrayOfStrings(defaultInputValue.toString()))
						{
							JsonNode child = null;
							List<String> uniqueDefaultInputs = new ArrayList<String>();
							for (Iterator<JsonNode> iter = defaultInputValue.iterator(); iter.hasNext();)
							{
								child = iter.next();
								if (child != null && !child.isNull())
								{
									if (ServiceUtils.doesValueMatchValidatorRegex(child.asText(), input.getValidator())) {
										if (!uniqueDefaultInputs.contains(child.asText())) {
											uniqueDefaultInputs.add(child.asText());
										}
									}
									else {
										throw new SoftwareException("Invalid default value found for input " + input.getKey() +
												". Default value " + child.asText() + " does not match the validator for this input.");
									}
								}
							}

							input.setDefaultValue(objectMapper.valueToTree(uniqueDefaultInputs).toString());
						}
						else {
							throw new SoftwareException("Invalid default value found for input " + input.getKey() +
									". If specified, default must be string or array of strings.");
						}
					}
					else if (defaultInputValue.isTextual())
					{
						if (ServiceUtils.doesValueMatchValidatorRegex(defaultInputValue.asText(), input.getValidator())) {
							ArrayNode prunedDefaultInputValues = objectMapper.createArrayNode();
							prunedDefaultInputValues.add(defaultInputValue.asText());
							input.setDefaultValue(prunedDefaultInputValues.toString());
						} else {
							throw new SoftwareException("Invalid default value found for input " + input.getKey() +
									". Default value does not match the validator for this input.");
						}
					}
					else
					{
						throw new SoftwareException("Invalid default value found for input " + input.getKey() +
								". If specified, default must be string or array of strings.");
					}
				}
				else {
					input.setDefaultValue(null);
				}

				// validate the combinations of cardinality, visible, required, and default value
				if (!input.isVisible())
				{
					// hidden inputs must have a default value
					if (input.getDefaultValueAsJsonArray().size() == 0) {
						throw new SoftwareException("No default field found for input " + input.getKey() +
								". default field is required for hidden inputs.");
					}

					// hidden inputs must have a cardinality greater than 0 to allow a default value
					if (input.getMinCardinality() == 0) {
						throw new SoftwareException("Invalid minCardinality value found for input " + input.getKey() +
								". If specified, the minCardinality must be greater than zero when the input is hidden.");
					}
				}

				// required inputs must have a cardinality greater than 0 to allow any value
				if (input.isRequired() && input.getMinCardinality() == 0) {
					throw new SoftwareException("Invalid minCardinality value found for input " + input.getKey() +
							". If specified, the minCardinality must be greater than zero when the input is required.");
				}

				// make sure the number of default honors the cardinality settings
				int defaultinputCount = input.getDefaultValueAsJsonArray().size();
				if (defaultinputCount > input.getMaxCardinality() && input.getMaxCardinality() != -1) {
					throw new SoftwareException("Invalid default value found for input " + input.getKey() +
							". If specified, the total number of default values must be less than the maxCardinality of this input.");
				}
				else if (defaultinputCount < input.getMinCardinality()) {
					throw new SoftwareException("Invalid default value found for input " + input.getKey() +
							". If specified, the total number of default values must be greater than the minCardinality of this input.");
				}

				if (value.has("enquote") && !value.get("enquote").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(value.get("enquote"))) {
						input.setEnquote(value.get("enquote").asBoolean());
					} else {
						throw new SoftwareException("Invalid enquote value found for parameter " + input.getKey() +
								". If specified, the enquote must be a boolean value, 0, or 1.");
					}
				} else {
					input.setEnquote(false);
				}

				// check for an ordering value. If none is found, set to zero
				if (value.has("order") && !value.get("order").isNull())
				{
					JsonNode orderNode = value.get("order");
					if (orderNode.isIntegralNumber())
					{
						int order = orderNode.asInt();
						if (order >= 0) {
							input.setOrder(order);
						} else {
							throw new SoftwareException("Invalid order value found for input " + input.getKey() +
									". If specified, the order value must be an integer value greater than or equal to zero.");
						}
					}
					else
					{
						throw new SoftwareException("Invalid order value found for input " + input.getKey() +
								". If specified, the order value must be an integer value greater than or equal to zero.");
					}
				}
				else
				{
					input.setOrder(0);
				}
			}
			else if (!value.isNull())
			{
				throw new SoftwareException("Invalid values attribute found for software input " + input.getKey() +
						". If specified, the values attribute should be a JSON object.");
			}
			else
			{
				input.setOrder(0);
				input.setDefaultValue(null);
				input.setRequired(false);
				input.setVisible(true);
				input.setValidator(null);
				input.setEnquote(false);
			}
		}
		else
		{
			input.setOrder(0);
			input.setDefaultValue(null);
			input.setRequired(false);
			input.setVisible(true);
			input.setValidator(null);
			input.setEnquote(false);
		}
//		else if (!jsonNode.get("value").isNull())
//		{
//			throw new SoftwareException("No values attribute found for software input " + input.getKey() + " description");
//		}

		if (jsonNode.has("details"))
		{
			JsonNode details = jsonNode.get("details");
			if (details.isObject())
			{
				if (details.has("label") && !details.get("label").isNull())
				{
					if (details.get("label").isTextual()) {
						input.setLabel(details.get("label").asText());
					} else {
						throw new SoftwareException("Invalid label value found for input " + input.getKey() +
								". If specified, the label must be a string value.");
					}
				} else {
					input.setLabel(null);
				}

				if (details.has("description") && !details.get("description").isNull())
				{
					if (details.get("description").isTextual()) {
						input.setDescription(details.get("description").asText());
					} else {
						throw new SoftwareException("Invalid description value found for input " + input.getKey() +
								". If specified, the description must be a string value.");
					}
				} else {
					input.setDescription(null);
				}

				if (details.has("showArgument") && !details.get("showArgument").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(details.get("showArgument"))) {
						input.setShowArgument(details.get("showArgument").asBoolean());
					} else {
						throw new SoftwareException("Invalid showArgument value found for input " + input.getKey() +
								". If specified, the showArgument must be a boolean value, 1, or 0.");
					}
				} else {
					input.setShowArgument(false);
				}

				if (details.has("argument") && !details.get("argument").isNull())
				{
					if (!details.get("argument").isTextual()) {
						throw new SoftwareException("Invalid argument value found for input " + input.getKey() +
								". If specified, the argument must be a string value.");

					} else if (details.get("argument").asText() == null && input.isShowArgument()) {
						throw new SoftwareException("Invalid argument value found for input " + input.getKey() +
								". argument cannot be empty or null when showArgument is true.");
					} else {
						input.setArgument(details.get("argument").asText());
					}
				}
				else if (input.isShowArgument())
				{
					throw new SoftwareException("Invalid argument value found for input " + input.getKey() +
							". A valid string value must be supplied when showArgument is true.");
				}
				else
				{
					input.setArgument(null);
				}

				if (details.has("repeatArgument") && !details.get("repeatArgument").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(details.get("repeatArgument"))) {
						input.setRepeatArgument(details.get("repeatArgument").asBoolean());
					} else {
						throw new SoftwareException("Invalid repeatArgument value found for input " + input.getKey() +
								". If specified, the repeatArgument must be a boolean value, 1, or 0.");
					}
				} else {
					input.setRepeatArgument(false);
				}
			}
			else if (!details.isNull()) {
				throw new SoftwareException("Invalid details value found for input " + input.getKey() +
						". If specified, the details value must be a JSON object.");
			}
			else
			{
				input.setArgument(null);
				input.setShowArgument(false);
				input.setRepeatArgument(false);
				input.setLabel(null);
				input.setDescription(null);
			}
		}
		else
		{
			input.setArgument(null);
			input.setShowArgument(false);
			input.setRepeatArgument(false);
			input.setLabel(null);
			input.setDescription(null);
		}

		return input;
	}

	@Override
	public int compareTo(SoftwareInput o)
	{
		return this.getOrder().compareTo(o.getOrder());
	}

	@Override
	public int compare(SoftwareInput a, SoftwareInput b)
	{
		return a.compareTo(b);
	}
}
