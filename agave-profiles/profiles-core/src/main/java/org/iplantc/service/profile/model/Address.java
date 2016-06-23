/**
 * 
 */
package org.iplantc.service.profile.model;

import javax.persistence.Embeddable;

import org.iplantc.service.profile.exceptions.ProfileException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Holds Address info from OpenID Connect spec http://openid.net/specs/openid-connect-basic-1_0-22.html#address_claim
 * 
 * @author dooley
 *
 */
@Embeddable
public class Address 
{
	private String formatted; // The full mailing address, formatted for display or use with a mailing label. This field MAY contain newlines. This is the Primary Sub-Field for this field, for the purposes of sorting and filtering.
	private String streetAddress; // The full street address component, which may include house number, street name, PO BOX, and multi-line extended street address information. This field MAY contain newlines.
	private String locality; // The city or locality component.
	private String region; // The state, province, prefecture or region component.
	private String postalCode; // The zip code or postal code component.
	private String country; // The country name component.

	public Address(){}
	
	/**
	 * @return the formatted
	 */
	public String getFormatted()
	{
		return formatted;
	}


	/**
	 * @param formatted the formatted to set
	 */
	public void setFormatted(String formatted)
	{
		this.formatted = formatted;
	}


	/**
	 * @return the streetAddress
	 */
	public String getStreetAddress()
	{
		return streetAddress;
	}


	/**
	 * @param streetAddress the streetAddress to set
	 */
	public void setStreetAddress(String streetAddress)
	{
		this.streetAddress = streetAddress;
	}


	/**
	 * @return the locality
	 */
	public String getLocality()
	{
		return locality;
	}


	/**
	 * @param locality the locality to set
	 */
	public void setLocality(String locality)
	{
		this.locality = locality;
	}


	/**
	 * @return the region
	 */
	public String getRegion()
	{
		return region;
	}


	/**
	 * @param region the region to set
	 */
	public void setRegion(String region)
	{
		this.region = region;
	}


	/**
	 * @return the postalCode
	 */
	public String getPostalCode()
	{
		return postalCode;
	}


	/**
	 * @param postalCode the postalCode to set
	 */
	public void setPostalCode(String postalCode)
	{
		this.postalCode = postalCode;
	}


	/**
	 * @return the country
	 */
	public String getCountry()
	{
		return country;
	}


	/**
	 * @param country the country to set
	 */
	public void setCountry(String country)
	{
		this.country = country;
	}


	public Address fromJSON(JSONObject json) throws ProfileException {
		Address address = new Address();
		try
		{
			address.setFormatted(json.getString("formatted"));
			address.setStreetAddress(json.getString("street_address"));
			address.setLocality(json.getString("locality"));
			address.setRegion(json.getString("region"));
			address.setPostalCode(json.getString("postal_code"));
			address.setCountry(json.getString("country"));
			
			return address;
		}
		catch (Exception e) {
			throw new ProfileException("Error parsing address.", e);
		}
	}
	
	
	public String toJSON() throws ProfileException 
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{	
			js.object()
				.key("formatted").value(getFormatted())
				.key("street_address").value(getStreetAddress())
				.key("locality").value(getLocality())
				.key("region").value(getRegion())
				.key("postal_code").value(getPostalCode())
				.key("country").value(getCountry());
			output = js.toString();
		}
		catch (Exception e)
		{
			throw new ProfileException("Error producing address JSON output.", e);
		}

		return output;
	}
	public String toString() {
		return getFormatted();
	}

}


