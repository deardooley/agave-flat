/**
 * 
 */
package org.iplantc.service.apps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class to hold individual enumerated values for {@link #softwareParameter}
 *  
 * @author dooley
 *
 */
@Entity
@Table(name = "softwareparameterenums")
public class SoftwareParameterEnumeratedValue implements Comparable<SoftwareParameterEnumeratedValue>
{
	private Long id;
	private String value;
	private String label;
	private SoftwareParameter softwareParameter;
	
	public SoftwareParameterEnumeratedValue() {}
	
	public SoftwareParameterEnumeratedValue(String value, String label,
			SoftwareParameter parameter)
	{
		setValue(value);
		setLabel(label);
		setSoftwareParameter(parameter);
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
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}
	
	/**
	 * @return the value
	 */
	@Column(name = "value", length = 128, nullable = false)
	public String getValue()
	{
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value)
	{
		this.value = value;
	}
	/**
	 * @return the label
	 */
	@Column(name = "label", length = 255, nullable = false)
	public String getLabel()
	{
		return label;
	}
	
	/**
	 * @param label the label to set
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public String toString() 
	{
		return getValue() + ": " + getLabel();
	}
	
	/**
	 * Returns a shallow copy of this object without the id and association.
	 * @see java.lang.Object#clone()
	 */
	public SoftwareParameterEnumeratedValue clone() {
		return new SoftwareParameterEnumeratedValue(getValue(), getLabel(), null);
	}

	@Override
	public int compareTo(SoftwareParameterEnumeratedValue o)
	{
		if (StringUtils.equals(this.getValue(), o.getValue())) {
			return this.getLabel().compareTo(o.getLabel());
		} else {
			return this.getValue().compareTo(o.getValue());
		}
	}
	
	/**
	 * @return the softwareParameter
	 */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
    public SoftwareParameter getSoftwareParameter()
	{
		return softwareParameter;
	}
	
	/**
	 * @param softwareParameter the softwareParameter to set
	 */
	public void setSoftwareParameter(SoftwareParameter softwareParameter)
	{
		this.softwareParameter = softwareParameter;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime
				* result
				+ ((softwareParameter == null) ? 0 : softwareParameter
						.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SoftwareParameterEnumeratedValue other = (SoftwareParameterEnumeratedValue) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (softwareParameter == null) {
			if (other.softwareParameter != null)
				return false;
		} else if (!softwareParameter.equals(other.softwareParameter))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
