/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

/**
 * @author dooley
 * 
 */
public class Collaborator extends Profile {

	private String	projectId;

	public Collaborator()
	{}

	/**
	 * @param user
	 */
	// public Collaborator(UserInfo user, String projectId) {
	// super(user);
	// this.projectId = projectId;
	// }

	/**
	 * @param chargeNumber
	 *            the chargeNumber to set
	 */
	public void setChargeNumber(String chargeNumber)
	{
		this.projectId = chargeNumber;
	}

	/**
	 * @return the chargeNumber
	 */
	public String getChargeNumber()
	{
		return projectId;
	}

	public String toString()
	{
		return quote(id) + "," + quote(username) + "," + quote(firstName) + ","
				+ quote(middleName) + "," + quote(lastName) + ","
				+ quote(email) + "," + quote(address) + ","
				+ quote(busPhoneNumber) + "," + quote(busPhoneExtension) + ","
				+ quote(faxNumber) + "," + quote(organization) + ","
				+ quote(department) + "," + quote(position) + ","
				+ quote(projectId) + ",";
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
				+ quote(department) + "," + quote(position) + ","
				+ quote(projectId);
	}

	public String toHtml()
	{
		return "<tr>" + formatColumn(id) + formatColumn(username)
				+ formatColumn(firstName) + formatColumn(middleName)
				+ formatColumn(lastName) + formatColumn(email)
				+ address.toHtml() + formatColumn(busPhoneNumber)
				+ formatColumn(busPhoneExtension) + formatColumn(faxNumber)
				+ formatColumn(organization) + formatColumn(department)
				+ formatColumn(position) + formatColumn(projectId) + "</tr>";
	}

	public String toHtml(String url)
	{
		String path = url.substring(0, url.indexOf("profile-v1")
				+ "profile-v1".length());

		return "<tr>"
				+ formatColumn(id)
				+
				// formatColumn("<a href=\"" + path + "/username/" + username +
				// "\">" + username + "</a>") +
				formatColumn(username)
				+ formatColumn(firstName)
				+ formatColumn(middleName)
				+ formatColumn(lastName)
				+ formatColumn(email)
				+ address.toHtml()
				+ formatColumn(busPhoneNumber)
				+ formatColumn(busPhoneExtension)
				+ formatColumn(faxNumber)
				+ formatColumn(organization)
				+ formatColumn(department)
				+ formatColumn(position)
				+ formatColumn("<a href=\"" + path + "/username/" + username
						+ "/project/project_number/" + projectId + "\">"
						+ projectId + "</a>") + "</tr>";
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("dn").object().key("id").value(this.id).endObject()
					.object().key("username").value(this.username).endObject()
					.object().key("firstName").value(this.firstName)
					.endObject().object().key("middleName").value(
							this.middleName).endObject().object().key(
							"lastName").value(this.lastName).endObject()
					.object().key("email").value(this.email).endObject()
					.object().key("address").value(this.address).endObject()
					.object().key("homePhoneNumber")
					.value(this.homePhoneNumber).endObject().object().key(
							"homePhoneExtension")
					.value(this.homePhoneExtension).endObject().object().key(
							"busPhoneNumber").value(this.busPhoneNumber)
					.endObject().object().key("busPhoneExtension").value(
							this.busPhoneExtension).endObject().object().key(
							"faxNumber").value(this.faxNumber).endObject()
					.object().key("organization").value(this.organization)
					.endObject().object().key("department").value(
							this.department).endObject().object().key(
							"position").value(this.position).endObject()
					.object().key("projectId").value(this.projectId)
					.endObject().endObject();

			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;
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
				+ "   position => '" + position + "',\n" + "   project.id => '"
				+ projectId + "'}";
	}

}
