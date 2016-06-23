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

@Entity
@Table(name = "software_outputs")
public class SoftwareOutput implements SoftwareAttribute<SoftwareOutput>
{
	private Long		id;
	private String		key;					// Mandatory unique name of the
												// parameter
	private String		fileTypes;				// The fileTypes supported by
												// /data/transforms/list.
	private String		label;					// Optional. If not specified,
												// label defaults to string
												// specified by 'id'
	private String		description;			// Optional extended description
												// of this output
	private String		ontology;				// Mandatory xs entity type
	private int			minCardinality = 0;		// Optional min occurances of
												// this output pattern in the
												// resulting job output dir
	private int			maxCardinality = -1;		// Optional max occurances of
												// this output pattern in the
												// resulting job output dir. -1
												// implies no limit
	private int			order = 0;		// Optional max occurances of
	private String		validator;				// Optional regex validating
												// expression
	private String		defaultValue;
	private Software	software;
	private Date		lastUpdated;
	private Date		created;

	public SoftwareOutput(){
		this.lastUpdated = new Date();
		this.created = new Date();
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
	@Column(name = "outputKey", nullable = false, length = 64)
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
		if (!ServiceUtils.isValid(key)) { 
			throw new SoftwareException("No key specified");
		} else if (key.length() > 64) {
			throw new SoftwareException("'software.output.id' must be less than 64 characters");
		}
		
//		if (!key.equals(key.replaceAll( "[^0-9a-zA-Z\\.\\-\\_\\(\\)]" , ""))) {
//			throw new SoftwareException("'software.output.id' may only contain alphanumeric characters, periods, and dashes.");
//		}
		
		this.key = key;
	}

	/**
	 * @return the fileType
	 */
	@Column(name = "fileTypes", nullable = true, length = 128)
	public String getFileTypes()
	{
		return fileTypes;
	}

	/**
	 * @param fileType
	 *            the fileType to set
	 */
	public void setFileTypes(String fileTypes)
	{
		if (!StringUtils.isEmpty(fileTypes) && fileTypes.length() > 128) {
			throw new SoftwareException("'software.output.semantics.fileTypes' must be less than 128 characters");
		}
		
		this.fileTypes = fileTypes;
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
		if (!StringUtils.isEmpty(label) && label.length() > 128) {
			throw new SoftwareException("'software.output.details.label' must be less than 128 characters");
		}
		
		this.label = label;
	}

	/**
	 * @return the description
	 */
	@Column(name = "description", nullable = false, length = 1024)
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
		if (!StringUtils.isEmpty(description) && description.length() > 1024) {
			throw new SoftwareException("'software.output.details.description' must be less than 1024 characters");
		}
		
		this.description = description;
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
	public String getOntology()
	{
		return ontology;
	}

	/**
	 * @param ontology
	 *            the ontology to set
	 */
	public void setOntology(String ontology)
	{
		if (StringUtils.isNotEmpty(ontology) && ontology.length() > 255) {
			throw new SoftwareException("'software.output.semantics.ontology' must be less than 255 characters");
		}
		
		this.ontology = ontology;
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
	 * @return the validator
	 */
	@Column(name = "pattern", nullable = true, length = 255)
	public String getValidator()
	{
		return validator;
	}

	/**
	 * @param validator
	 *            the validator to set
	 */
	public void setValidator(String validator)
	{
		if (StringUtils.length(validator) > 255) {
			throw new SoftwareException("'software.output.value.validator' must be less than 255 characters");
		} 
		// verify the regex. We use Perl regex
		else if (!StringUtils.isEmpty(validator))
		{
			try {
				new Regex().compile(validator);
			} catch (Exception e) {
				throw new SoftwareException("'software.output.value.validator' is not a valid regular expression.", e);
			}
		}
		
		this.validator = validator;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue)
	{
		if (!StringUtils.isEmpty(defaultValue) && defaultValue.length() > 255) {
			throw new SoftwareException("'software.output.value.default' must be less than 255 characters");
		}
		
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the defaultValue
	 */
	@Column(name = "default_value", nullable = false, length = 255)
	public String getDefaultValue()
	{
		return defaultValue;
	}
	
	/**
	 * Returns an ArrayNode containing the default values for this SoftwareOutput.
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
	
	public SoftwareOutput clone(Software software)
	{
		SoftwareOutput output = new SoftwareOutput();
		output.setDefaultValue(getDefaultValue());
		output.setDescription(getDescription());
		output.setFileTypes(getFileTypes());
		output.setKey(getKey());
		output.setLabel(getLabel());
		output.setLastUpdated(new Date());
		output.setMaxCardinality(getMaxCardinality());
		output.setMinCardinality(getMinCardinality());
		output.setOntology(getOntology());
		output.setOrder(getOrder());
		output.setSoftware(software);
		output.setValidator(getValidator());
		
		return output;
	}

	public boolean equals(Object o)
	{
		if (o instanceof SoftwareOutput) 
		{ 
			return ( (SoftwareOutput) o ).key.equalsIgnoreCase(key); 
		}
		return false;
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
				.key("order").value(getOrder());
			ArrayNode defaultValueArrayNode = getDefaultValueAsJsonArray();
			if (defaultValueArrayNode.size() == 0) {
				writer.key("default").value(null);
			} else if (getMinCardinality() < 2  || (getMaxCardinality() < 2 && getMaxCardinality() != -1)) {
				writer.key("default").value(defaultValueArrayNode.get(0).textValue());
			} else {
				writer.key("default").array();
				for (int i=0; i<defaultValueArrayNode.size(); i++) {	
					writer.value(defaultValueArrayNode.get(i).textValue());
				}
				writer.endArray();
			}
			writer.endObject()
			.key("details").object()
				.key("label").value(label)
				.key("description").value(getDescription())
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
					if (!StringUtils.isEmpty(getFileTypes())) {
						for(String term: ServiceUtils.implode(",",getFileTypes())) {
							writer.value(term);
						}
					}
					writer.endArray()
				.endObject()
			.endObject();
	}
	
	public static SoftwareOutput fromJSON(JSONObject json)
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
		
		SoftwareOutput output = new SoftwareOutput();
		if (jsonNode.has("id") && jsonNode.get("id").isTextual())
		{
			output.setKey(json.getString("id"));
		}
		else
		{
			throw new SoftwareException("No id attribute found in software output description");
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
						output.setValidator(validator);
					} else {
						throw new SoftwareException("Invalid validator found for output " + output.getKey() + 
								". If provided, validator must be a string regular expression.");
					}
				}
				else {
					output.setValidator(null);
				}
			
				if (value.has("default") && !value.get("default").isNull())
				{	
					JsonNode defaultOutputValue = value.get("default");
					
					if (defaultOutputValue.isNull()) {
						output.setDefaultValue(null);
					}
					else if (defaultOutputValue.isArray()) 
					{
						if (ServiceUtils.isJsonArrayOfStrings(defaultOutputValue.toString())) 
						{
							JsonNode child = null;
							ArrayNode prunedDefaultOutputValues = objectMapper.createArrayNode();
							for (Iterator<JsonNode> iter = defaultOutputValue.iterator(); iter.hasNext();) 
							{
								child = iter.next();
								if (!child.isNull() && StringUtils.isNotEmpty(child.asText())) {
									prunedDefaultOutputValues.add(child.asText());
								}
							}
							output.setDefaultValue(prunedDefaultOutputValues.toString());
						}
						else {
							throw new SoftwareException("Invalid default value found for output " + output.getKey() + 
									". If specified, default must be string or array of strings.");
						}
					}
					else if (defaultOutputValue.isTextual()) 
					{
						ArrayNode prunedDefaultOutputValues = objectMapper.createArrayNode();
						prunedDefaultOutputValues.add(defaultOutputValue.asText());
						output.setDefaultValue(prunedDefaultOutputValues.toString());
					}
					else 
					{
						throw new SoftwareException("Invalid default value found for output " + output.getKey() + 
								". If specified, default must be string or array of strings.");
					}
				}
				else 
				{
					output.setDefaultValue(null);
				}
				
				// check for an ordering value. If none is found, set to zero
				if (value.has("order") && !value.get("order").isNull())
				{
					JsonNode orderNode = value.get("order");
					if (orderNode.isIntegralNumber()) 
					{
						int order = orderNode.asInt();
						if (order >= 0) {
							output.setOrder(order);
						} else {
							throw new SoftwareException("Invalid order value found for output " + output.getKey() + 
									". If specified, the order value must be an integer value greater than or equal to zero.");
						}
					} 
					else 
					{
						throw new SoftwareException("Invalid order value found for output " + output.getKey() + 
								". If specified, the order value must be an integer value greater than or equal to zero.");
					}
				}
				else
				{
					output.setOrder(0);
				}
			}
			else if (!value.isNull())
			{
				throw new SoftwareException("Invalid values attribute found for software output " + output.getKey() + 
						". If specified, the values attribute should be a JSON object.");
			}
			else 
			{
				output.setOrder(0);
				output.setDefaultValue(null);
				output.setValidator(null);
			}
		}
		else
		{
			output.setOrder(0);
			output.setDefaultValue(null);
			output.setValidator(null);
		}
		
		if (jsonNode.has("details"))
		{
			JsonNode details = jsonNode.path("details");
			if (details.isObject()) 
			{	
				if (details.has("label") && !details.get("label").isNull()) 
				{
					if (details.get("label").isTextual()) {
						output.setLabel(details.get("label").asText());
					} else {
						throw new SoftwareException("Invalid label value found for output " + output.getKey() + 
								". If specified, the label must be a string value.");
					}
				} else {
					output.setLabel(null);
				}
				
				if (details.has("description") && !details.get("description").isNull()) 
				{
					if (details.get("description").isTextual()) {
						output.setDescription(details.get("description").asText());
					} else {
						throw new SoftwareException("Invalid description value found for output " + output.getKey() + 
								". If specified, the description must be a string value.");
					}
				} else {
					output.setDescription(null);
				}
			}
			else if (!details.isNull()) 
			{
				throw new SoftwareException("Invalid details value found for output " + output.getKey() + 
						". If specified, the details value must be a JSON object.");
			}
			else 
			{
				output.setLabel(null);
				output.setDescription(null);
			}
		}
		else 
		{
			output.setLabel(null);
			output.setDescription(null);
		}
		
		if (jsonNode.has("semantics"))
		{
			JsonNode semantics = jsonNode.path("semantics");
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
								throw new SoftwareException("Invalid ontology value for output " + output.getKey() + 
										". If specified, ontologies should be an array of valid string values");
							} else {
								if (StringUtils.isNotEmpty(child.asText()) && 
										!ontologyList.contains(ServiceUtils.enquote(child.asText()))) {
									ontologyList.add(ServiceUtils.enquote(child.asText()));
								}
							}
						}
						output.setOntology(StringUtils.join(ontologyList, ","));
					} else {
						throw new SoftwareException("ontology attribute for output " + output.getKey() + 
								" is not a valid json array.");
					}	
				}
				else
				{
					output.setOntology(null);
				}
				
				if (semantics.has("minCardinality") && !semantics.get("minCardinality").isNull())
				{
					if (semantics.get("minCardinality").isIntegralNumber()) {
						if (semantics.get("minCardinality").asInt() >= 0 ) {
							output.setMinCardinality(semantics.get("minCardinality").asInt());
						} else {
							throw new SoftwareException("Invalid minCardinality value found for output " + output.getKey() + 
									". If specified, the minCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid minCardinality value found for output " + output.getKey() + 
								". If specified, the minCardinality value must be an integer value greater than zero.");
					}
				}
				else 
				{
					output.setMinCardinality(0);
				}
				
				if (semantics.has("maxCardinality"))
				{
					if (semantics.get("maxCardinality").isIntegralNumber()) {
						int maxCard = semantics.get("maxCardinality").asInt();
						if (maxCard == -1 || maxCard > 0) {
							output.setMaxCardinality(maxCard);
						} else {
							throw new SoftwareException("Invalid maxCardinality value found for output " + output.getKey() + 
									". If specified, the maxCardinality value must be an integer value greater than zero.");
						}
					} else {
						throw new SoftwareException("Invalid maxCardinality value found for output " + output.getKey() + 
								". If specified, the maxCardinality value must be an integer value greater than zero.");
					}
				}
				else 
				{
					output.setMaxCardinality(1);
				}
				
				if (output.getMaxCardinality() < output.getMinCardinality() && output.getMaxCardinality() != -1) {
					throw new SoftwareException("Invalid maxCardinality value found for output " + output.getKey() + 
							". If specified, the maxCardinality value must be greater than or equal to the "
							+ "minCardinality value.");
				} 
				else if (output.getMaxCardinality() == 0) {
					throw new SoftwareException("Invalid maxCardinality value found for output " + output.getKey() + 
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
								throw new SoftwareException("Invalid fileTypes value found for output " + output.getKey() + 
										". If specified, the fileTypes value must be an array of names of valid file types.");
							}
							else
							{
								if (!fileTypesList.contains(child.asText())) {
									fileTypesList.add(child.asText());
								}
							}
						}
						output.setFileTypes(StringUtils.join(fileTypesList, ","));
					} 
					else 
					{
						throw new SoftwareException("Invalid fileTypes value found for output " + output.getKey() + 
								". If specified, the fileTypes value must be an array of names of valid file types.");
					}
				}
				else 
				{
					output.setFileTypes(null);
				}
			}
			else if (!semantics.isNull())
			{
				throw new SoftwareException("Invalid semantics attribute value found for output " + 
						output.getKey() + ". If specified, the semantics attribute value must be a JSON object.");
			}
			else 
			{
				output.setFileTypes(null);
				output.setMaxCardinality(-1);
				output.setMinCardinality(0);
				output.setOntology(null);
			}
		}
		else
		{
			output.setFileTypes(null);
			output.setMaxCardinality(-1);
			output.setMinCardinality(0);
			output.setOntology(null);
		}
		
		return output;
	}

	@Override
	public int compareTo(SoftwareOutput o)
	{
		return this.getOrder().compareTo(o.getOrder());
	}
	
	@Override
	public int compare(SoftwareOutput a, SoftwareOutput b)
	{
		return a.compareTo(b);		
	}
}
