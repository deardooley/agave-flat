/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

/**
 * @author dooley
 * 
 */
public class DN implements TgcdbDTO {
	String	dn;

	public DN()
	{}

	// public DN(Dns dns) {
	// this.dn = dns.getId().getDn();
	// }

	public String getDn()
	{
		return dn;
	}

	public void setDn(String dn)
	{
		this.dn = dn;
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("dn").object().key("dn").value(this.dn).endObject()
					.endObject();

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
		// TODO Auto-generated method stub
		return null;
	}

	public String toHtml()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String toPerl()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
