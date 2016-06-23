/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import org.json.JSONStringer;

/**
 * @author dooley
 * 
 */
public class BandwidthMeasurement implements TgcdbDTO {
	private String	fromResourceID;
	private String	fromResourceName;
	private String	fromPath;
	private String	toResourceID;
	private String	toResourceName;
	private String	toPath;
	private String	lastMeasured;
	private Double	megabytespersecond;

	public BandwidthMeasurement()
	{}

	public BandwidthMeasurement(String fromResourceID, String fromResourceName,
			String fromPath, String toResourceID, String toResourceName,
			String toPath, String lastMeasured, Double megabytespersecond)
	{
		super();
		this.fromResourceID = fromResourceID;
		this.fromResourceName = fromResourceName;
		this.fromPath = fromPath;
		this.toResourceID = toResourceID;
		this.toResourceName = toResourceName;
		this.toPath = toPath;
		this.lastMeasured = lastMeasured;
		this.megabytespersecond = megabytespersecond;
	}

	/**
	 * @return the fromResourceID
	 */
	public String getFromResourceID()
	{
		return fromResourceID;
	}

	/**
	 * @param fromResourceID
	 *            the fromResourceID to set
	 */
	public void setFromResourceID(String fromResourceID)
	{
		this.fromResourceID = fromResourceID;
	}

	/**
	 * @return the fromResourceName
	 */
	public String getFromResourceName()
	{
		return fromResourceName;
	}

	/**
	 * @param fromResourceName
	 *            the fromResourceName to set
	 */
	public void setFromResourceName(String fromResourceName)
	{
		this.fromResourceName = fromResourceName;
	}

	/**
	 * @return the fromPath
	 */
	public String getFromPath()
	{
		return fromPath;
	}

	/**
	 * @param fromPath
	 *            the fromPath to set
	 */
	public void setFromPath(String fromPath)
	{
		this.fromPath = fromPath;
	}

	/**
	 * @return the toResourceID
	 */
	public String getToResourceID()
	{
		return toResourceID;
	}

	/**
	 * @param toResourceID
	 *            the toResourceID to set
	 */
	public void setToResourceID(String toResourceID)
	{
		this.toResourceID = toResourceID;
	}

	/**
	 * @return the toResourceName
	 */
	public String getToResourceName()
	{
		return toResourceName;
	}

	/**
	 * @param toResourceName
	 *            the toResourceName to set
	 */
	public void setToResourceName(String toResourceName)
	{
		this.toResourceName = toResourceName;
	}

	/**
	 * @return the toPath
	 */
	public String getToPath()
	{
		return toPath;
	}

	/**
	 * @param toPath
	 *            the toPath to set
	 */
	public void setToPath(String toPath)
	{
		this.toPath = toPath;
	}

	/**
	 * @return the lastMeasured
	 */
	public String getLastMeasured()
	{
		return lastMeasured;
	}

	/**
	 * @param lastMeasured
	 *            the lastMeasured to set
	 */
	public void setLastMeasured(String lastMeasured)
	{
		this.lastMeasured = lastMeasured;
	}

	/**
	 * @return the megabytespersecond
	 */
	public Double getMegabytespersecond()
	{
		return megabytespersecond;
	}

	/**
	 * @param megabytespersecond
	 *            the megabytespersecond to set
	 */
	public void setMegabytespersecond(Double megabytespersecond)
	{
		this.megabytespersecond = megabytespersecond;
	}

	public boolean equals(Object o)
	{
		if (o instanceof String)
		{
			return fromResourceID.equals(o);
		}
		else if (o instanceof BandwidthMeasurement)
		{
			return ( fromResourceID
					.equals( ( (BandwidthMeasurement) o ).fromResourceID) && toResourceID
					.equals( ( (BandwidthMeasurement) o ).toResourceID) );
		}
		else if (o instanceof ComputeDTO) { return toResourceID
				.equals( ( (ComputeDTO) o ).getId()); }
		return false;
	}

	public String toCsv()
	{
		return quote(fromResourceID) + "," + quote(fromResourceName) + ","
				+ quote(fromPath) + "," + quote(toResourceID) + ","
				+ quote(toResourceName) + "," + quote(toPath) + ","
				+ quote(lastMeasured) + "," + quote(megabytespersecond);
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	public String toHtml()
	{
		return "<tr>"
				+ formatColumn("<a href=\"http://info.teragrid.org/web-apps/html/ctss-resources-v1/ResourceID/"
						+ fromResourceID + "\">" + fromResourceID + "</a>")
				+ formatColumn(fromResourceName)
				+ formatColumn(fromPath)
				+ formatColumn("<a href=\"http://info.teragrid.org/web-apps/html/ctss-resources-v1/ResourceID/"
						+ toResourceID + "\">" + toResourceID + "</a>")
				+ formatColumn(toResourceName) + formatColumn(toPath)
				+ formatColumn(lastMeasured) + formatColumn(megabytespersecond)
				+ "</tr>";
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	public String toPerl()
	{
		return " {\n" + "   fromResourceID => '" + fromResourceID + "',\n"
				+ "   fromResourceName => '" + fromResourceName + "',\n"
				+ "   fromPath => '" + fromPath + "',\n"
				+ "   toResourceID => '" + toResourceID + "',\n"
				+ "   toResourceName => '" + toResourceName + "',\n"
				+ "   toPath => '" + toPath + "',\n" + "   lastMeasured => '"
				+ lastMeasured + "',\n" + "   megabytespersecond => '"
				+ megabytespersecond + "'},\n";

	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("measurement").object().key("fromResourceID")
					.value(fromResourceID).endObject().object().key(
							"fromResourceName").value(fromResourceName)
					.endObject().object().key("toResourceID").value(
							toResourceID).endObject().object().key(
							"toResourceName").value(toResourceName).endObject()
					.object().key("toPath").value(toPath).endObject().object()
					.key("lastMeasured").value(lastMeasured).endObject()
					.object().key("megabytespersecond").value(
							megabytespersecond).endObject().endObject();

			output = js.toString();

		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;
	}

	public String toString()
	{
		return toCsv();
	}

}
