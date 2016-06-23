package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

public class Address implements TgcdbDTO {
	private String	street1;
	private String	street2;
	private String	city;
	private String	state;
	private String	zipcode;
	private String	country;

	public Address()
	{}

	public String getStreet1()
	{
		return street1;
	}

	public void setStreet1(String street1)
	{
		this.street1 = street1;
	}

	public String getStreet2()
	{
		return street2;
	}

	public void setStreet2(String street2)
	{
		this.street2 = street2;
	}

	public String getCity()
	{
		return city;
	}

	public void setCity(String city)
	{
		this.city = city;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getZipcode()
	{
		return zipcode;
	}

	public void setZipcode(String zip)
	{
		this.zipcode = zip;
	}

	public String getCountry()
	{
		return country;
	}

	public void setCountry(String country)
	{
		this.country = country;
	}

	public String toString()
	{
		return "\t\t\"address\" : {\n" + "\t\t\t\"street1 : " + getStreet1()
				+ "," + "\t\t\t\"street2 : " + getStreet2() + ","
				+ "\t\t\t\"city : " + getCity() + "," + "\t\t\t\"state : "
				+ getState() + "," + "\t\t\t\"zip : " + getZipcode() + "\t\t}"
				+ ",";
	}

	public Address clone()
	{
		Address address = new Address();
		address.setStreet1(street1);
		address.setStreet2(street2);
		address.setCity(city);
		address.setState(state);
		address.setZipcode(zipcode);
		address.setCountry(country);

		return address;
	}

	public Address toDto()
	{
		return this.clone();
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("address").object().key("street1").value(
					this.street1).endObject().object().key("street2").value(
					this.street2).endObject().object().key("city").value(
					this.city).endObject().object().key("state").value(
					this.state).endObject().object().key("zipcode").value(
					this.zipcode).endObject().object().key("country").value(
					this.country).endObject().endObject();

			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;

	}

	public String toCsv()
	{
		return quote(street1) + "," + quote(street2) + "," + quote(city) + ","
				+ quote(state) + "," + quote(zipcode) + "," + quote(country);
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	public String toHtml()
	{
		return formatColumn(street1) + formatColumn(street2)
				+ formatColumn(city) + formatColumn(state)
				+ formatColumn(zipcode) + formatColumn(country);
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	public String toPerl()
	{
		return "   address.street1 => '" + street1 + "',\n"
				+ "   address.street2 => '" + street2 + "',\n"
				+ "   address.city => '" + city + "',\n"
				+ "   address.state => '" + state + "',\n"
				+ "   address.zip => '" + zipcode + "',\n"
				+ "   address.country => '" + country + "',\n";
	}
}
