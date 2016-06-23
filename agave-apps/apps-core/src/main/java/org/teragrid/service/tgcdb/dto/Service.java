/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

/**
 * @author dooley
 * 
 */
public class Service implements TgcdbDTO {
	public static final String	UP		= "up";
	public static final String	DOWN	= "down";
	public static final String	UNKNOWN	= "unknown";

	private String				name;
	private String				endpoint;
	private String				status;

	public Service()
	{}

	public Service(String name, String endpoint, String status)
	{
		super();
		this.name = name;
		this.endpoint = endpoint;
		this.status = status;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the endpoint
	 */
	public String getEndpoint()
	{
		return endpoint;
	}

	/**
	 * @param endpoint
	 *            the endpoint to set
	 */
	public void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}

	/**
	 * @return the status
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	public Service toDto()
	{
		return new Service(name, endpoint, status);
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("service").object().key("name").value(this.name)
					.endObject().object().key("endpoint").value(this.endpoint)
					.endObject().object().key("status").value(this.status)
					.endObject().endObject();
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
		return quote(name) + "," + quote(endpoint) + "," + quote(status);
	}

	private String quote(Object o)
	{
		return "\"" + o.toString() + "\"";
	}

	public String toHtml()
	{
		return formatColumn(name) + formatColumn(endpoint)
				+ formatColumn(status);
	}

	private String formatColumn(Object o)
	{
		return "<td>" + o.toString() + "</td>";
	}

	public String toPerl()
	{
		return " {\n" + "   service.name => '" + name + "',\n"
				+ "   service.endpoint => '" + endpoint + "',\n"
				+ "   service.status => '" + status + "'},\n";
	}

}
