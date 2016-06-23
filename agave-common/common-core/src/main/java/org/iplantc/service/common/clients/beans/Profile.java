/**
 * 
 */
package org.iplantc.service.common.clients.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 * 
 */
public class Profile implements TgcdbDTO {

	protected int		id;
	protected String	username;
	protected String	firstName;
	protected String	middleName;
	protected String	lastName;
	protected String	organization;
	protected String	department;
	protected String	position;
	protected String	email;
	protected Address	address;
	protected String	homePhoneNumber;
	protected String	homePhoneExtension;
	protected String	busPhoneNumber;
	protected String	busPhoneExtension;
	protected String	faxNumber;

	public Profile()
	{}

	// public Profile(UserInfo user) {
	// id = user.getId();
	// username = user.getUsername();
	// firstName = user.getFirstName();
	// middleName = user.getMiddleName();
	// lastName = user.getLastName();
	// organization = user.getOrganization();
	// department = user.getDepartment();
	// position = user.getPosition();
	// email = user.getEmail();
	// if (user.getAddress() != null) {
	// address = user.getAddress().clone();
	// } else {
	// address = new Address();
	// }
	// homePhoneNumber = user.getHomePhoneNumber();
	// homePhoneExtension = user.getHomePhoneExtension();
	// busPhoneNumber = user.getBusPhoneNumber();
	// busPhoneExtension = user.getBusPhoneExtension();
	// faxNumber = user.getFaxNumber();
	// }

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getMiddleName()
	{
		return middleName;
	}

	public void setMiddleName(String middleName)
	{
		this.middleName = middleName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getOrganization()
	{
		return organization;
	}

	public void setOrganization(String organization)
	{
		this.organization = organization;
	}

	public String getDepartment()
	{
		return department;
	}

	public void setDepartment(String department)
	{
		this.department = department;
	}

	public String getPosition()
	{
		return position;
	}

	public void setPosition(String position)
	{
		this.position = position;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public Address getAddress()
	{
		return address;
	}

	public void setAddress(Address address)
	{
		this.address = address;
	}

	public String getHomePhoneNumber()
	{
		return homePhoneNumber;
	}

	public void setHomePhoneNumber(String homePhoneNumber)
	{
		this.homePhoneNumber = homePhoneNumber;
	}

	public String getHomePhoneExtension()
	{
		return homePhoneExtension;
	}

	public void setHomePhoneExtension(String homePhoneExtension)
	{
		this.homePhoneExtension = homePhoneExtension;
	}

	public String getBusPhoneNumber()
	{
		return busPhoneNumber;
	}

	public void setBusPhoneNumber(String busPhoneNumber)
	{
		this.busPhoneNumber = busPhoneNumber;
	}

	public String getBusPhoneExtension()
	{
		return busPhoneExtension;
	}

	public void setBusPhoneExtension(String busPhoneExtension)
	{
		this.busPhoneExtension = busPhoneExtension;
	}

	public String getFaxNumber()
	{
		return faxNumber;
	}

	public void setFaxNumber(String faxNumber)
	{
		this.faxNumber = faxNumber;
	}

	public String toString()
	{
		return quote(id) + "," + quote(username) + "," + quote(firstName) + ","
				+ quote(middleName) + "," + quote(lastName) + ","
				+ quote(email) + "," + quote(address) + ","
				+ quote(busPhoneNumber) + "," + quote(busPhoneExtension) + ","
				+ quote(faxNumber) + "," + quote(organization) + ","
				+ quote(department) + "," + quote(position);
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	public String toCsv()
	{
		return quote(id) + "," + quote(username) + "," + quote(firstName) + ","
				+ quote(middleName) + "," + quote(lastName) + ","
				+ quote(email) + "," + address.toCsv() + ","
				+ quote(busPhoneNumber) + "," + quote(busPhoneExtension) + ","
				+ quote(faxNumber) + "," + quote(organization) + ","
				+ quote(department) + "," + quote(position);
	}

	public String toHtml()
	{
		return "<tr>" + formatColumn(id) + formatColumn(username)
				+ formatColumn(firstName) + formatColumn(middleName)
				+ formatColumn(lastName) + formatColumn(email)
				+ address.toHtml() + formatColumn(busPhoneNumber)
				+ formatColumn(busPhoneExtension) + formatColumn(faxNumber)
				+ formatColumn(organization) + formatColumn(department)
				+ formatColumn(position) + "</tr>";
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	public String toJSONString()
	{
		try {
			ObjectMapper mapper = new ObjectMapper();
		
			return mapper.writeValueAsString(this); 
		}
		catch (JsonProcessingException e)
		{
			System.out.println("Error producing JSON output.");
			return "{}";
		}
	}

	public String toPerl()
	{
		// "id,username,first.name,middle.name,last.name,email," +
		// "address.street1,address.street2,address.city,address.state,address.zip,address.country,"
		// +
		// "business.phone,business.phone.ext,business.fax,organization,department,position"
		return " {\n" + "   id => '" + id + "',\n" + "   username => '"
				+ username + "',\n" + "   first.name => '" + firstName + "',\n"
				+ "   middle.name => '" + middleName + "',\n"
				+ "   last.name => '" + lastName + "',\n" + "   email => '"
				+ email + "',\n" + address.toPerl() + "   business.phone => '"
				+ busPhoneNumber + "',\n" + "   business.phone.ext => '"
				+ busPhoneExtension + "',\n" + "   business.fax => '"
				+ faxNumber + "',\n" + "   organization => '" + organization
				+ "',\n" + "   department => '" + department + "',\n"
				+ "   position => '" + position + "'}";
	}

}
