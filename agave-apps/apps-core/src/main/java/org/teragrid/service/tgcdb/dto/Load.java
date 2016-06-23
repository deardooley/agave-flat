package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

public class Load implements TgcdbDTO {
	private String	name;
	private int		total;
	private int		available;

	public Load()
	{}

	public Load(String name, int total, int available)
	{
		super();
		this.name = name;
		this.total = total;
		this.available = available;
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
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the total
	 */
	public int getTotal()
	{
		return total;
	}

	/**
	 * @param total
	 *            the total to set
	 */
	public void setTotal(int total)
	{
		this.total = total;
	}

	/**
	 * @return the available
	 */
	public int getAvailable()
	{
		return available;
	}

	/**
	 * @param available
	 *            the available to set
	 */
	public void setAvailable(int available)
	{
		this.available = available;
	}

	public Load toDto()
	{
		return new Load(name, total, available);
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("load").object().key("name").value(this.name)
					.endObject().object().key("total").value(this.total)
					.endObject().object().key("available")
					.value(this.available).endObject().endObject();
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
		return quote(name) + "," + quote(total) + "," + quote(available);
	}

	private String quote(Object o)
	{
		return "\"" + o.toString() + "\"";
	}

	public String toHtml()
	{
		return formatColumn(name) + formatColumn(total)
				+ formatColumn(available);
	}

	private String formatColumn(Object o)
	{
		return "<td>" + o.toString() + "</td>";
	}

	public String toPerl()
	{
		return "   load.name => '" + name + "',\n" + "   load.total => '"
				+ total + "',\n" + "   load.available => '" + available + "'\n";
	}

}
