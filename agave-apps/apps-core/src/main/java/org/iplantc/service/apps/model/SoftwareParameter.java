/**
 *
 */
package org.iplantc.service.apps.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.stevesoft.pat.Regex;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "software_parameters")
public class SoftwareParameter implements SoftwareAttribute<SoftwareParameter>
{
	private Long		id;
	private String		key;					// Mandatory unique name of the
												// parameter
	private SoftwareParameterType	type;		// Mandatory. xs entity type
	private String		defaultValue;			// Optional default value for this parameter
	private String		validator;				// Optional regex validating expression, enumerated values
	private Integer		order 		= 0;		// order of this parameter in the list
	private boolean		visible	 	= true;
	private boolean		required	= true;
	private boolean		enquote		= false;	// whether to wrap the value in quote prior to injecting into the wrapper template

	private String		description;			// Optional supplemental text description
	private String		label;					// Short human readable label
	private String		argument;				// name as it appears when used on the command line
	private boolean		showArgument = false;
	private boolean		repeatArgument = false;

	private List<SoftwareParameterEnumeratedValue> enumValues = new ArrayList<SoftwareParameterEnumeratedValue>();
	private String		ontology;				// Mandatory xs entity type
	private int			minCardinality = 0;
	private int			maxCardinality = 1;

	private Software	software;
	private Date		lastUpdated;
	private Date		created;

	public SoftwareParameter() {
		this.created = new Date();
		this.lastUpdated = new Date();
	}

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
			throw new SoftwareException("No key value for software parameter");
		} else if (key.length() > 64) {
			throw new SoftwareException("'software.parameter.id' must be less than 64 characters");
		}

//		if (!key.equals(key.replaceAll( "[^0-9a-zA-Z\\.\\-\\_\\(\\)]" , ""))) {
//			throw new SoftwareException("'software.parameter.id' may only contain alphanumeric characters, periods, and dashes.");
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
	 * Returns an ArrayNode containing the default values for this SoftwareParameter.
	 * It will parse a valid JSON array, a semicolon delimited list, or a single string value.
	 * Null default values will return an empty ArrayNode.
	 *
	 * @return ArrayNode with the default values or empty if there are none.
	 */
	@Transient
	public ArrayNode getDefaultValueAsJsonArray()
	{
		ArrayNode arrayNode = new ObjectMapper().createArrayNode();
		if (ServiceUtils.isJsonArrayOfStrings(getDefaultValue()))
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
		else
		{
			arrayNode.add(getDefaultValue());
		}

		return arrayNode;
	}

	/**
	 * @param defaultValue
	 *            the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue)
	{
		if (StringUtils.length(defaultValue) > 255) {
			throw new SoftwareException("'software.parameter.value.default' must be less than 255 characters");
		}

		this.defaultValue = defaultValue;
	}

	/**
	 * @return the type
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "value_type", nullable = false, length = 16)
	public SoftwareParameterType getType()
	{
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type)
	{
		try
		{
			if (!ServiceUtils.isValid(type)) {
				throw new SoftwareException("No type value for software parameter");
			}
			else if (SoftwareParameterType.valueOf(type.toLowerCase()) == null)
			{
				throw new SoftwareException("Invalid type for software parameter. Valid types are [string, num, bool, enumeration]");
			}

			this.type = SoftwareParameterType.valueOf(type.toLowerCase());
		} catch (IllegalArgumentException e) {
			throw new SoftwareException("Invalid type for software parameter. Valid types are [string, num, bool, enumeration]");
		}
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(SoftwareParameterType type)
	{
		if (type == null) {
			throw new SoftwareException("No type value for software parameter");
		}

		this.type = type;
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
//		if (StringUtils.isEmpty(label)) {
//			throw new SoftwareException("Invalid software.parameter.details.label value for software parameter");
//		} else
		if (StringUtils.length(label) > 128) {
			throw new SoftwareException("'software.parameter.details.label' must be less than 128 characters");
		}

		this.label = label;
	}

	@Column(name = "cli_argument", nullable = true, length = 64)
	public String getArgument() {
		return argument;
	}

	/**
	 * @param argument
	 */
	public void setArgument(String argument) {
		if (StringUtils.length(argument) > 64) {
			throw new SoftwareException("'software.parameter.details.argument' must be less than 64 characters");
		}
		this.argument = argument;
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
			throw new SoftwareException("'software.parameter.semantics.ontology' must be less than 255 characters");
		}

//		try {
//			 new URL(ontology);
//		 } catch (MalformedURLException e) {
//			 throw new SoftwareException("Invalid 'software.parameter.semantics.ontology' value. " +
//			 		"If specified, 'software.parameter.semantics.ontology' must be a valid URL");
//		 }

		this.ontology = ontology;
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
			throw new SoftwareException("'software.parameter.details.description' must be less than 1024 characters");
		}

		this.description = description;
	}

	/**
	 * @return the validator
	 */
	@Column(name = "validator", nullable = true, length = 256)
	public String getValidator()
	{
		return validator;
	}

	/**
	 * Returns list of just the enumerated values, not their labels.
	 *
	 * @return List of string values representing the enumeration values
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@Transient
	public List<String> getEnumeratedValuesAsList() throws SoftwareException
	{
		ArrayNode enumeratedValuesArray = getEnumeratedValues();
		List<String> valuesList = new ArrayList<String>();

		JsonNode enumeratedValue = null;
		for(Iterator<JsonNode> iter = enumeratedValuesArray.iterator(); iter.hasNext();)
		{
			enumeratedValue=iter.next();
			if (enumeratedValue.isObject()) {
				valuesList.add(enumeratedValue.get("value").textValue());
			} else {
				valuesList.add(enumeratedValue.textValue());
			}
		}

		return valuesList;
	}

	/**
	 * Returns enumerated values for this parameter by serializing the validator field
	 * where enumerations are stored for enumeration parameter types. If the parameter
	 * is not a validator, this will return an empty ArrayNode.
	 *
	 * @return ArrayNode of JsonNode objects representing the key and label for each enumerated value.
	 * an empty ArrayNode if this is not an enumeration parameter.
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@Transient
	public ArrayNode getEnumeratedValues() throws SoftwareException
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode enumeratedValuesArray;
		if (!SoftwareParameterType.enumeration.equals(getType()))
		{
			return mapper.createArrayNode();
		}
		else if (!getEnumValues().isEmpty())
		{
			return mapper.valueToTree(getEnumValues());
		}
		else
		{
			try
			{
				if (StringUtils.isEmpty(getValidator())) {
					return mapper.createArrayNode();
				} else {
					List<SoftwareParameterEnumeratedValue> enums = new ArrayList<SoftwareParameterEnumeratedValue>();
					enumeratedValuesArray = mapper.readTree(getValidator());
					if (enumeratedValuesArray.isArray())
					{
						for(Iterator<JsonNode> iter = ((ArrayNode)enumeratedValuesArray).iterator(); iter.hasNext();) {
							JsonNode child = iter.next();
							if (child.isObject()) {
								String value = child.fieldNames().next();
								String label = child.findValue(value).textValue();
								enums.add(new SoftwareParameterEnumeratedValue(value, label, this));
							} else {
								enums.add(new SoftwareParameterEnumeratedValue(child.asText(), child.asText(), this));
							}
						}
						return mapper.valueToTree(enums);
					} else {
						return mapper.createArrayNode().add(getValidator());
					}
				}
			}
			catch (Exception e)
			{
				throw new SoftwareException("Invalid enumValue field value for parameter " +
						getKey() + ". Unable to parse saved enumerations for this parameter.", e);
			}
		}
	}

	/**
	 * @param validator
	 *            the validator to set
	 */
	public void setValidator(String validator)
	{
		if (StringUtils.length(validator) > 256) {
			throw new SoftwareException("'software.parameter.value.validator' must be less than 256 characters");
		}
		// verify the regex. We use Perl regex
		else if (!StringUtils.isEmpty(validator))
		{
			try {
				new Regex().compile(validator);
			} catch (Exception e) {
				throw new SoftwareException("'software.parameter.value.validator' is not a valid regular expression.", e);
			}
		}

		this.validator = validator;
	}

	/**
	 * @return the enumValues
	 */
//	@OneToMany(fetch = FetchType.LAZY,
//			   cascade = {CascadeType.REMOVE, CascadeType.ALL})
//	@Cascade({
//		org.hibernate.annotations.CascadeType.PERSIST,
//        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
////	@JoinColumn(name="softwareparameter_id")
//	@JoinTable(
//            name="softwareparameter_softwareparameterenums",
//            joinColumns = @JoinColumn( name="softwareparameter_id"),
//            inverseJoinColumns = @JoinColumn( name="softwareparameterenum_id")
//    )
//	@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.ALL})
//	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
//        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
//	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(fetch=FetchType.EAGER, cascade = {CascadeType.REMOVE, CascadeType.ALL}, mappedBy = "softwareParameter")
//	@JoinColumn(name="software_parameter_id")
    public List<SoftwareParameterEnumeratedValue> getEnumValues()
	{
		return enumValues;
	}

	/**
	 * @param enumValues the enumValues to set
	 */
	public void setEnumValues(List<SoftwareParameterEnumeratedValue> enumValues)
	{
		this.enumValues = enumValues;
//		this.enumValues.clear();
//		for(SoftwareParameterEnumeratedValue enumVal: enumValues) {
//			enumVal.setSoftwareParameter(this);
//			this.enumValues.add(enumVal);
//		}
	}

	/**
	 * Adds an enumerated value to the list of available enumerated values
	 * and sets the association with this {@link SoftwareParameter}
	 *
	 * @param enumValue
	 * @throws SoftwareException
	 */
	@Transient
	public void addEnumValue(SoftwareParameterEnumeratedValue enumValue) throws SoftwareException
	{
		if (enumValue == null) {
			throw new SoftwareException("Cannot add a null enumerated value");
		} else {
			enumValue.setSoftwareParameter(this);
			if (!this.enumValues.contains(enumValue)) {
				this.enumValues.add(enumValue);
			}
		}
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
	 * template once for each user-supplied parameter value or once
	 * per parameter.
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
	 * once for each user-supplied parameter value or once per parameter.
	 *
	 * @param repeatArgument
	 */
	public void setRepeatArgument(boolean repeatArgument) {
		this.repeatArgument = repeatArgument;
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
	 * Should this parameter value be wrapped in quotes before injecting into the wrapper template.
	 * If multiple values are given, each will be quoted independently and separated by a space.
	 *
	 * @return enquote True if the user-supplied parameter value should be wrapped in quotes before
	 * injecting the value into the wrapper template.
	 *
	 */
	@Column(name = "enquoted", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isEnquote()
	{
		return enquote;
	}

	/**
	 * True if the user-supplied parameter value should be wrapped in quotes before
	 * injecting the value into the wrapper template.
	 *
	 * @param enquote
	 */
	public void setEnquote(boolean enquote)
	{
		this.enquote = enquote;
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
	 * @param software
	 *            the software to set
	 */
	public void setSoftware(Software software)
	{
		this.software = software;
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
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
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
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
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

	public SoftwareParameter clone(Software newSoftwareReference)
	{
		SoftwareParameter parameter = new SoftwareParameter();
		parameter.setArgument(getArgument());
		parameter.setDefaultValue(getDefaultValue());
		parameter.setDescription(getDescription());
		parameter.setEnquote(isEnquote());
		parameter.setKey(getKey());
		parameter.setLabel(getLabel());
		parameter.setOntology(getOntology());
		parameter.setOrder(getOrder());
		parameter.setRepeatArgument(isRepeatArgument());
		parameter.setRequired(isRequired());
		parameter.setMinCardinality(minCardinality);
		parameter.setMaxCardinality(maxCardinality);
		parameter.setShowArgument(isShowArgument());
		parameter.setSoftware(newSoftwareReference);
		parameter.setType(getType());
		parameter.setValidator(getValidator());
		parameter.setVisible(isVisible());
		for(SoftwareParameterEnumeratedValue enumVal: getEnumValues()) {
			parameter.addEnumValue(enumVal.clone());
		}

		return parameter;
	}

	public boolean equals(Object o)
	{
		if (o instanceof SoftwareParameter)
		{
			return StringUtils.equalsIgnoreCase(getKey(), ( (SoftwareParameter) o ).getKey());
		}
		return false;
	}

	public int hashCode()
	{
		return getKey().hashCode();
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
				.key("visible").value(isVisible())
				.key("required").value(isRequired())
				.key("type").value(getType().name())
				.key("order").value(getOrder())
				.key("enquote").value(isEnquote());
				ArrayNode defaultValueArrayNode = getDefaultValueAsJsonArray();
				if (defaultValueArrayNode.size() == 0)
				{
					writer.key("default").value(null);
				}
				else if (defaultValueArrayNode.size() == 1) {
					if (getType().equals(SoftwareParameterType.bool) ||
							getType().equals(SoftwareParameterType.flag)) {
						writer.key("default").value(defaultValueArrayNode.get(0).asBoolean(false));
					}
					else if (getType().equals(SoftwareParameterType.number))
					{
						if (defaultValueArrayNode.get(0).isInt()) {
							writer.key("default").value(defaultValueArrayNode.get(0).asInt());
						} else {
							writer.key("default").value(defaultValueArrayNode.get(0).asDouble());
						}
					} else {
						writer.key("default").value(defaultValueArrayNode.get(0).textValue());
					}
				}
				else
				{
					writer.key("default").array();
					for(Iterator<JsonNode> iter = defaultValueArrayNode.iterator(); iter.hasNext();) {
						JsonNode child = iter.next();
						if (getType().equals(SoftwareParameterType.number))
						{
							if (defaultValueArrayNode.get(0).isInt()) {
								writer.value(child.asInt());
							} else {
								writer.value(child.asDouble());
							}
						}
						else
						{
							writer.value(child.asText());
						}
					}
					writer.endArray();
				}

				if (getType().equals(SoftwareParameterType.enumeration))
				{
					writer.key("enum_values").array();
					for(Iterator<JsonNode> iter = getEnumeratedValues().iterator(); iter.hasNext();) {
						// object with a single default value and label
						JsonNode child = iter.next();
						if (child.fieldNames().hasNext()) {
							String key = child.get("value").textValue();
							String val = child.get("label").textValue();
							writer.object().key(key).value(val).endObject();
						}
					}
					writer.endArray();
				}
				else
				{
					writer.key("validator").value(getValidator());
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
			.endObject()
		.endObject();
	}

	public static SoftwareParameter fromJSON(JSONObject json)
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

		SoftwareParameter parameter = new SoftwareParameter();
		if (jsonNode.has("id") && jsonNode.get("id").isTextual())
		{
			parameter.setKey(json.getString("id"));
		}
		else
		{
			throw new SoftwareException("No id attribute found for software parameter " + parameter.getKey() + " description");
		}

		// visible fields do not need a default value
		if (jsonNode.path("value").has("visible"))
		{
			if (ServiceUtils.isBooleanOrNumericBoolean(jsonNode.path("value").get("visible"))) {
				parameter.setVisible(jsonNode.path("value").get("visible").asBoolean());
			} else {
				throw new SoftwareException("Invalid visible value found for parameter " + parameter.getKey() +
						". If specified, visible must be a boolean value, 1, or 0.");
			}
		}
		else {
			parameter.setVisible(true);
		}

		if (jsonNode.path("value").has("required"))
		{
			if (ServiceUtils.isBooleanOrNumericBoolean(jsonNode.path("value").get("required"))) {
				parameter.setRequired(jsonNode.path("value").get("required").asBoolean());
			} else {
				throw new SoftwareException("Invalid required value found for parameter " + parameter.getKey() +
						". If specified, required must be a boolean value, 1, or 0.");
			}
		}
		else
		{
			parameter.setRequired(!parameter.isVisible());
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
								throw new SoftwareException("Invalid ontology value for parameter " + parameter.getKey() +
										". If specified, ontologies should be an array of valid string values");
							}
							else
							{
								if (StringUtils.isNotEmpty(child.asText()) &&
										!ontologyList.contains(ServiceUtils.enquote(child.asText()))) {
									ontologyList.add(ServiceUtils.enquote(child.asText()));
								}

							}
						}
						parameter.setOntology(StringUtils.join(ontologyList, ","));
					}
					else
					{
						throw new SoftwareException("ontology attribute for parameter " + parameter.getKey() + " is not a valid json array.");
					}
				}
				else {
					parameter.setOntology(null);
				}

				if (semantics.has("minCardinality") && !semantics.get("minCardinality").isNull())
				{
					if (semantics.get("minCardinality").isIntegralNumber()) {
						if (semantics.get("minCardinality").asInt() >= 0 ) {
							parameter.setMinCardinality(semantics.get("minCardinality").asInt());
						} else {
							throw new SoftwareException("Invalid minCardinality value found for parameter " + parameter.getKey() +
									". If specified, the minCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid minCardinality value found for parameter " + parameter.getKey() +
								". If specified, the minCardinality value must be an integer value greater than zero.");
					}
				}
				else
				{
					parameter.setMinCardinality(parameter.isRequired() || !parameter.isVisible() ? 1 : 0);
				}

				if (semantics.has("maxCardinality"))
				{
					if (semantics.get("maxCardinality").isIntegralNumber()) {
						int maxCard = semantics.get("maxCardinality").asInt();
						if (maxCard == -1 || maxCard > 0) {
							parameter.setMaxCardinality(maxCard);
						} else {
							throw new SoftwareException("Invalid maxCardinality value found for parameter " + parameter.getKey() +
									". If specified, the maxCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid maxCardinality value found for parameter " + parameter.getKey() +
								". If specified, the maxCardinality value must be an integer value greater than zero.");
					}
				}
				else
				{
					parameter.setMaxCardinality(1);
				}

				if (parameter.getMaxCardinality() < parameter.getMinCardinality() && parameter.getMaxCardinality() != -1) {
					throw new SoftwareException("Invalid maxCardinality value found for parameter " + parameter.getKey() +
							". If specified, the maxCardinality value must be greater than or equal to the "
							+ "minCardinality value.");
				}
				else if (parameter.getMaxCardinality() == 0) {
					throw new SoftwareException("Invalid maxCardinality value found for parameter " + parameter.getKey() +
							". If specified, the maxCardinality value must be greater than zero.");
				}
			}
			else if (!semantics.isNull())
			{
				throw new SoftwareException("Invalid semantics attribute value found for parameter " + parameter.getKey() + ". If specified, the semantics attribute value must be a JSON object.");
			}
			else
			{
				parameter.setOntology(null);
				parameter.setMaxCardinality(1);
				parameter.setMinCardinality(parameter.isRequired() || !parameter.isVisible() ? 1 : 0);
			}
		}
		else
		{
			parameter.setOntology(null);
			parameter.setMaxCardinality(1);
			parameter.setMinCardinality(parameter.isRequired() || !parameter.isVisible() ? 1 : 0);
		}


		if (jsonNode.has("value") && jsonNode.get("value").isObject())
		{
			JsonNode value = jsonNode.get("value");
			if (!value.isObject()) {
				throw new SoftwareException("Invalid value attribute value found for parameter "
						+ parameter.getKey() + ". If specified, the value attribute value must be a JSON object.");
			}

			if (value.has("type") && value.get("type").isTextual())
			{
				try {
					SoftwareParameterType type = SoftwareParameterType.valueOf(StringUtils.lowerCase(value.get("type").asText()));
					parameter.setType(type);
				} catch (IllegalArgumentException e) {
					throw new SoftwareException("Invalid type for software parameter " + parameter.getKey() + ". Valid types are [string, num, bool, enumeration]");
				}

				if (value.has("enquote") && !value.get("enquote").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(value.get("enquote"))) {
						parameter.setEnquote(value.get("enquote").asBoolean());
					} else {
						throw new SoftwareException("Invalid enquote value found for parameter " + parameter.getKey() +
								". If specified, the enquote must be a boolean value, 0, or 1.");
					}
				} else {
					parameter.setEnquote(false);
				}

				if (parameter.getType().equals(SoftwareParameterType.enumeration))
				{
					// enums must have a value list. Default is optional unless it's hidden.
					if (value.has("validator"))
					{
						throw new SoftwareException("Validator field found for parameter " + parameter.getKey() +
								" description. Validator field is not applicable for enumeration parameter types.");
					}

					if (value.has("enum_values") || value.has("enumValues"))
					{
						ArrayNode jsonArray = null;
						if (value.get("enum_values") != null)
						{
							if (!value.get("enum_values").isArray())
							{
								throw new SoftwareException("Invalid enumValue field value for parameter " +
										parameter.getKey() + ". enumValues should be an array of values or single attribute JSON objects.");
							}
							else {
								jsonArray = (ArrayNode)value.get("enum_values");
							}
						}
						else if (value.get("enumValues") != null)
						{
							if (!value.get("enumValues").isArray())
							{
								throw new SoftwareException("Invalid enumValue field value for parameter " +
										parameter.getKey() + ". enumValues should be an array of values or single attribute JSON objects.");
							}
							else {
								jsonArray = (ArrayNode)value.get("enumValues");
							}
						}

//						ArrayNode validatedArray = objectMapper.createArrayNode();
						JsonNode jsonEnum = null;
						List<SoftwareParameterEnumeratedValue> newEnumValues = new ArrayList<SoftwareParameterEnumeratedValue>();
						for(int i=0; i<jsonArray.size(); i++)
						{
							jsonEnum = jsonArray.get(i);

							if (jsonEnum.isObject())
							{
								if (jsonEnum.size() != 1) {
									throw new SoftwareException("Invalid enumvValues value found for parameter " + parameter.getKey());
								} else {
									String enumValue = jsonEnum.fieldNames().next();
									String enumLabel = jsonEnum.get(enumValue).asText();
									newEnumValues.add(new SoftwareParameterEnumeratedValue(enumValue, enumLabel, parameter));
//									validatedArray.add(jsonEnum);
								}
							}
							else if (jsonEnum.isTextual())
							{
								if (StringUtils.isEmpty(jsonEnum.asText())) {
									throw new SoftwareException("Invalid enumvValues value found for parameter " + parameter.getKey()
											+ ". Empty values may not be specifed in an enumerated array. If you need to specify an empty value,"
											+ " Specify it as the value of a JSON object with a valid key.");
								} else {
									newEnumValues.add(new SoftwareParameterEnumeratedValue(jsonEnum.asText(), jsonEnum.asText(), parameter));
//									validatedArray.addObject().put(jsonEnum.asText(), jsonEnum.asText());
								}
							}
							else {
								throw new SoftwareException("Invalid enumvValues value found for parameter " +
										parameter.getKey() + ". Each enum value must be a string or single-attribute json object.");
							}
						}

						parameter.setEnumValues(newEnumValues);

						// wipe the validator field since this parameter is an enumeration
						parameter.setValidator(null);
					}
					else
					{
						throw new SoftwareException("No enumvValues field found for parameter" + parameter.getKey() +
								". enumValues field is required for enumeration parameter types.");
					}

					if (value.has("default"))
					{
						ArrayNode defaultValueArray = objectMapper.createArrayNode();
						List<String> enumValues = parameter.getEnumeratedValuesAsList();
						if (value.get("default").isArray())
						{
							JsonNode val = null;
							for(int i=0; i<value.get("default").size(); i++)
							{
								val = value.get("default").get(i);
								if (!val.isTextual())
								{
									throw new SoftwareException("Invalid default value " + val.textValue() +
											" found for parameter " + parameter.getKey() +
											". Default values must be an single or array of primary types and match one or "
											+ "more values in the enumValues field.");
								}
								else
								{
									if (enumValues.contains(val.textValue()))
									{
										if(!Arrays.asList(ServiceUtils.getStringValuesFromJsonArray(defaultValueArray, false)).contains(value.asText()))
										{
											defaultValueArray.add(val.textValue());
										}
									}
									else
									{
										throw new SoftwareException("Invalid default value " + val.textValue() +
												" found for parameter " + parameter.getKey() +
												". The default value does not match any of the enumerated values." +
												" Default values should be one or more of the following: " +
												ServiceUtils.explode(",  ", enumValues));
									}
								}
							}
						}
						else if (value.get("default").isTextual())
						{
							if (enumValues.contains(value.get("default").textValue()))
							{
								if (!Arrays.asList(ServiceUtils.getStringValuesFromJsonArray(defaultValueArray, false)).contains(value.asText()))
								{
									defaultValueArray.add(value.get("default").textValue());
								}
							}
							else
							{
								throw new SoftwareException("Invalid default value " + value.get("default").textValue() +
										" found for parameter " + parameter.getKey() +
										". The default value does not match any of the enumerated values." +
										" Default values should be one or more of the following: " +
										ServiceUtils.explode(",  ", enumValues));
							}
						}
						else if (!value.get("default").isNull())
						{
							throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
									". Default values must be an single or array of primary types and match one or "
									+ "more values in the enumValues field.");
						}

						if (defaultValueArray.size() > 0) {
							parameter.setDefaultValue(defaultValueArray.toString());
						} else {
							// we validate the existence and size after this block
							parameter.setDefaultValue(null);
						}
					}
					else if (!parameter.isVisible())
					{
						// we validate the existence and size after this block
						parameter.setDefaultValue(null);
					}
				}
				else
				{
					// enumValues are not valid for types other than enumerations
					if (value.has("enumValues"))
					{
						throw new SoftwareException("enumValues field found for parameter " +
								parameter.getKey() + ". enumValues field is not applicable for " +
								"string, number, and bool parameter types.");
					}

					if (value.has("validator") && !value.get("validator").isNull())
					{
						if (value.get("validator").isTextual()) {
							parameter.setValidator(value.get("validator").asText());
						} else {
							throw new SoftwareException("Invalid validator found for parameter " + parameter.getKey() +
									". If provided, validator must be a string regular expression.");
						}
					}
					else
					{
						parameter.setValidator(null);
					}

					// wipe the enumValues since this parameter is no longer an enumeration
					parameter.enumValues.clear();

					// check for a default value that is a json array or primary type matching the type of the parameter
					if (value.has("default") && !value.get("default").isNull())
					{
						ArrayNode defaultValueArray = null;
						ArrayNode validatedDefaultValueArray = objectMapper.createArrayNode();

						// read in the parameter
						if (value.get("default").isArray()) {
							// it's an array, no nothing
							defaultValueArray = (ArrayNode)value.get("default");
						}
						else if (value.get("default").isValueNode()) {
							defaultValueArray = objectMapper.createArrayNode().add(value.get("default"));
						}
						else {
							throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
									". If specified, default must be a primary type.");
						}

						JsonNode child = null;
						for(Iterator<JsonNode> iter = defaultValueArray.iterator(); iter.hasNext();)
						{
							child = iter.next();
							if (parameter.getType().equals(SoftwareParameterType.bool) ||
									parameter.getType().equals(SoftwareParameterType.flag))
							{
								if (defaultValueArray.size() > 1 || !ServiceUtils.isBooleanOrNumericBoolean(child)) {
									throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
											". If specified, the default value for a boolean or flag parameter type must be a boolean value, 1, or 0.");
								}
								else {
									validatedDefaultValueArray.add(child.asBoolean() ? "true" : "false");
								}
							}
							else if (parameter.getType().equals(SoftwareParameterType.number))
							{
								if (child.isIntegralNumber())
								{
									if (ServiceUtils.doesValueMatchValidatorRegex(new Long(child.asLong(0)).toString(), parameter.getValidator()))
									{
										if (!Arrays.asList(ServiceUtils.getStringValuesFromJsonArray(validatedDefaultValueArray, false)).contains(child.asText()))
										{
											validatedDefaultValueArray.add(child.asLong(0));
										}
									}
									else
									{
										throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
												". Default value does not match the validator for this parameter.");
									}
								}
								else if (child.isNumber())
								{
									if (ServiceUtils.doesValueMatchValidatorRegex(new Double(child.asDouble(0)).toString(), parameter.getValidator()))
									{
										if(!Arrays.asList(ServiceUtils.getStringValuesFromJsonArray(validatedDefaultValueArray, false)).contains(child.asText()))
										{
											validatedDefaultValueArray.add(new Double(child.asDouble(0)));
										}
									}
									else
									{
										throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
												". Default value does not match the validator for this parameter.");
									}
								}
								else
								{
									throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
										". If specified, the default value for a number parameter type must be a numeric value.");
								}
							}
							else if (child.isTextual())
							{
								if (ServiceUtils.doesValueMatchValidatorRegex(child.asText(), parameter.getValidator()))
								{
									if (!Arrays.asList(ServiceUtils.getStringValuesFromJsonArray(validatedDefaultValueArray, false)).contains(child.asText())) {
										validatedDefaultValueArray.add(child.asText());
									}
								}
								else
								{
									throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
											". Default value does not match the validator for this parameter.");
								}
							}
							else if (!child.isNull())
							{
								throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
										". If specified, the default value for a number parameter type must be a numeric value.");
							}
						}

						parameter.setDefaultValue(validatedDefaultValueArray.toString());
					}
					else
					{
						// we validate the existence and size after this block
						parameter.setDefaultValue(null);
					}
				}
			}
			else
			{
				throw new SoftwareException("No type field found for parameter " + parameter.getKey() +
						". Parameters must have a type field with any of the following values: " +
						ServiceUtils.explode(", ",Arrays.asList(SoftwareParameterType.values())));
			}

			// check for an ordering value. If none is found, set to zero
			if (value.has("order") && !value.get("order").isNull())
			{
				JsonNode orderNode = value.get("order");
				if (orderNode.isIntegralNumber())
				{
					int order = orderNode.asInt();
					if (order >= 0) {
						parameter.setOrder(order);
					} else {
						throw new SoftwareException("Invalid order value found for parameter " + parameter.getKey() +
								". If specified, the order value must be an integer value greater than or equal to zero.");
					}
				}
				else
				{
					throw new SoftwareException("Invalid order value found for parameter " + parameter.getKey() +
							". If specified, the order value must be an integer value greater than or equal to zero.");
				}
			}
			else
			{
				parameter.setOrder(0);
			}

			// validate the combinations of cardinality, visible, required, and default value
			if (!parameter.isVisible())
			{
				// hidden parameters must have a default value
				if (parameter.getDefaultValueAsJsonArray().size() == 0) {
					throw new SoftwareException("No default field found for parameter " + parameter.getKey() +
							". default field is required for hidden parameters.");
				}

				// hidden parameters must have a cardinality greater than 0 to allow a default value
				if (parameter.getMinCardinality() == 0) {
					throw new SoftwareException("Invalid minCardinality value found for parameter " + parameter.getKey() +
							". If specified, the minCardinality must be greater than zero when the parameter is hidden.");
				}
			}

			// required parameters must have a cardinality greater than 0 to allow any value
			if (parameter.isRequired() && parameter.getMinCardinality() == 0) {
				throw new SoftwareException("Invalid minCardinality value found for parameter " + parameter.getKey() +
						". If specified, the minCardinality must be greater than zero when the parameter is required.");
			}

			// make sure the number of default honors the cardinality settings
			int defaultparameterCount = parameter.getDefaultValueAsJsonArray().size();
			if (defaultparameterCount > parameter.getMaxCardinality() && parameter.getMaxCardinality() != -1) {
				throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
						". If specified, the total number of default values must be less than the maxCardinality of this parameter.");
			}
			else if (defaultparameterCount < parameter.getMinCardinality()) {
				throw new SoftwareException("Invalid default value found for parameter " + parameter.getKey() +
						". If specified, the total number of default values must be greater than the minCardinality of this parameter.");
			}
		}
		else if (jsonNode.has("value") && !jsonNode.get("value").isNull())
		{
			throw new SoftwareException("Invalid values attribute found for software parameter " + parameter.getKey() +
					". If specified, the values attribute should be a JSON object.");
		}
		else
		{
			throw new SoftwareException("No values attribute found for parameter " + parameter.getKey() + ". No values attribute found in software parameter description");
		}

		if (jsonNode.has("details"))
		{
			JsonNode details = jsonNode.get("details");
			if (details.isObject())
			{
				if (details.has("label") && !details.get("label").isNull())
				{
					if (details.get("label").isTextual()) {
						parameter.setLabel(details.get("label").asText());
					} else {
						throw new SoftwareException("Invalid label value found for parameter " + parameter.getKey() +
								". If specified, the label must be a string value.");
					}
				} else {
					parameter.setLabel(null);
				}

				if (details.has("description") && !details.get("description").isNull())
				{
					if (details.get("description").isTextual()) {
						parameter.setDescription(details.get("description").asText());
					} else {
						throw new SoftwareException("Invalid description value found for parameter " + parameter.getKey() +
								". If specified, the description must be a string value.");
					}
				} else {
					parameter.setDescription(null);
				}

				if (details.has("showArgument") && !details.get("showArgument").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(details.get("showArgument"))) {
						parameter.setShowArgument(details.get("showArgument").asBoolean());
					} else {
						throw new SoftwareException("Invalid showArgument value found for parameter " + parameter.getKey() +
								". If specified, the showArgument must be a boolean value, 1, or 0.");
					}
				} else {
					parameter.setShowArgument(false);
				}

				if (details.has("argument") && !details.get("argument").isNull())
				{
					if (!details.get("argument").isTextual()) {
						throw new SoftwareException("Invalid argument value found for parameter " + parameter.getKey() +
								". If specified, the argument must be a string value.");

					} else if (details.get("argument").asText() == null && parameter.isShowArgument()) {
						throw new SoftwareException("Invalid argument value found for parameter " + parameter.getKey() +
								". argument cannot be empty or null when showArgument is true.");
					} else {
						parameter.setArgument(details.get("argument").asText());
					}
				}
				else if (parameter.isShowArgument())
				{
					throw new SoftwareException("Invalid argument value found for parameter " + parameter.getKey() +
							". A valid string value must be supplied when showArgument is true.");
				}
				else
				{
					parameter.setArgument(null);
				}

				if (details.has("repeatArgument") && !details.get("repeatArgument").isNull())
				{
					if (ServiceUtils.isBooleanOrNumericBoolean(details.get("repeatArgument"))) {
						parameter.setRepeatArgument(details.get("repeatArgument").asBoolean());
					} else {
						throw new SoftwareException("Invalid repeatArgument value found for parameter " + parameter.getKey() +
								". If specified, the repeatArgument must be a boolean value, 1, or 0.");
					}
				} else {
					parameter.setRepeatArgument(false);
				}
			}
			else if (!details.isNull()) {
				throw new SoftwareException("Invalid details value found for parameter " +
						parameter.getKey() + ". If specified, the details value must be a JSON object.");
			}
			else
			{
				parameter.setArgument(null);
				parameter.setShowArgument(false);
				parameter.setRepeatArgument(false);
				parameter.setLabel(null);
				parameter.setDescription(null);
			}
		}
		else
		{
			parameter.setArgument(null);
			parameter.setShowArgument(false);
			parameter.setRepeatArgument(false);
			parameter.setLabel(null);
			parameter.setDescription(null);
		}

		return parameter;
	}
	
	@Override
	public String toString() {
	    return String.format("%s - %s, %s, %s",
	    		getKey(),
	    		getType().name(),
	    		isVisible() ? "visible" : "hidden",
	    		isRequired() ? "required" : "optional");
	}

	@Override
	public int compareTo(SoftwareParameter o)
	{
//		return this.getOrder().compareTo(o.getOrder());
		if (this.getOrder().intValue() == o.getOrder().intValue())
		{
			if (this.getId() == o.getId()) {
				return this.getKey().compareTo(o.getKey());
			} else {
				return this.getId().compareTo(o.getId());
			}
		} else {
			return this.getOrder().compareTo(o.getOrder());
		}
	}

	@Override
	public int compare(SoftwareParameter a, SoftwareParameter b)
	{
		return a.compareTo(b);
	}
}
