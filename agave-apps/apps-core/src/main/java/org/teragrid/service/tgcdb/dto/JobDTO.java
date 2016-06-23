/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import java.util.Date;

import org.json.JSONStringer;
import org.teragrid.service.util.TGUtil;

/**
 * @author dooley
 * 
 */
public class JobDTO implements TgcdbDTO {

	private Integer	id;
	private String	name;
	private float	wallTime;
	private int		charge;
	private int		nodeCount;
	private int		processorCount;
	private String	queue;
	private Date	startTime;
	private Date	endTime;
	private String	localJobId;
	private String	resourceID;
	private String	projectNumber;

	public JobDTO()
	{}

	/**
	 * @return the id
	 */
	public Integer getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Integer id)
	{
		this.id = id;
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
	 * @return the wallTime
	 */
	public float getWallTime()
	{
		return wallTime;
	}

	/**
	 * @param wallTime
	 *            the wallTime to set
	 */
	public void setWallTime(float wallTime)
	{
		this.wallTime = wallTime;
	}

	/**
	 * @return the charge
	 */
	public int getCharge()
	{
		return charge;
	}

	/**
	 * @param charge
	 *            the charge to set
	 */
	public void setCharge(int charge)
	{
		this.charge = charge;
	}

	/**
	 * @return the nodeCount
	 */
	public int getNodeCount()
	{
		return nodeCount;
	}

	/**
	 * @param nodeCount
	 *            the nodeCount to set
	 */
	public void setNodeCount(int nodeCount)
	{
		this.nodeCount = nodeCount;
	}

	/**
	 * @return the processorCount
	 */
	public int getProcessorCount()
	{
		return processorCount;
	}

	/**
	 * @param processorCount
	 *            the processorCount to set
	 */
	public void setProcessorCount(int processorCount)
	{
		this.processorCount = processorCount;
	}

	/**
	 * @return the queue
	 */
	public String getQueue()
	{
		return queue;
	}

	/**
	 * @param queue
	 *            the queue to set
	 */
	public void setQueue(String queue)
	{
		this.queue = queue;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime()
	{
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public Date getEndTime()
	{
		return endTime;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}

	/**
	 * @return the localJobId
	 */
	public String getLocalJobId()
	{
		return localJobId;
	}

	/**
	 * @param localJobId
	 *            the localJobId to set
	 */
	public void setLocalJobId(String localJobId)
	{
		this.localJobId = localJobId;
	}

	/**
	 * @return the system
	 */
	public String getResourceID()
	{
		return resourceID;
	}

	/**
	 * @param system
	 *            the system to set
	 */
	public void setResourceID(String resourceID)
	{
		this.resourceID = resourceID;
	}

	/**
	 * @return the projetct
	 */
	public String getProjectNumber()
	{
		return projectNumber;
	}

	/**
	 * @param projetct
	 *            the projetct to set
	 */
	public void setProjectNumber(String projectNumber)
	{
		this.projectNumber = projectNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teragrid.service.tgcdb.dto.TgcdbDTO#toCsv()
	 */
	public String toCsv()
	{
		return quote(id) + "," + quote(name) + "," + quote(wallTime) + ","
				+ quote(charge) + "," + quote(nodeCount) + ","
				+ quote(processorCount) + "," + quote(queue) + ","
				+ quote(startTime) + "," + quote(TGUtil.formatUTC(endTime))
				+ "," + quote(localJobId) + "," + quote(resourceID) + ","
				+ quote(projectNumber);
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teragrid.service.tgcdb.dto.TgcdbDTO#toHtml()
	 */
	public String toHtml()
	{
		return "<tr>" + formatColumn(id) + formatColumn(name)
				+ formatColumn(wallTime) + formatColumn(charge)
				+ formatColumn(nodeCount) + formatColumn(processorCount)
				+ formatColumn(queue)
				+ formatColumn(TGUtil.formatUTC(startTime))
				+ formatColumn(TGUtil.formatUTC(endTime))
				+ formatColumn(localJobId) + formatColumn(resourceID)
				+ formatColumn(projectNumber) + "</tr>";
	}

	public String toHtml(String url)
	{

		String path = "";
		if (url.contains("username"))
		{
			String username = url.substring(url.indexOf("username"));
			username = username.substring(username.indexOf("/") + 1);
			username = username.substring(0, username.indexOf("/"));
			path = url.substring(0,
					( url.indexOf(username) + username.length() ));
		}
		else
		{
			path = url.substring(0, url.indexOf("profile-v1")
					+ "profile-v1".length());
		}

		return "<tr>"
				+ formatColumn(id)
				+ formatColumn(name)
				+ formatColumn(wallTime)
				+ formatColumn(charge)
				+ formatColumn(nodeCount)
				+ formatColumn(processorCount)
				+ formatColumn(queue)
				+ formatColumn(TGUtil.formatUTC(startTime))
				+ formatColumn(TGUtil.formatUTC(endTime))
				+ formatColumn(localJobId)
				+ formatColumn("<a href=\"" + path + "/resource/resourceid/"
						+ resourceID + "\">" + resourceID + "</a>")
				+ formatColumn("<a href=\"" + path + "/project/project_number/"
						+ projectNumber + "\">" + projectNumber + "</a>")
				+ "</tr>";
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teragrid.service.tgcdb.dto.TgcdbDTO#toPerl()
	 */
	public String toPerl()
	{
		return " {\n" + "   id => '" + id + "',\n" + "   name => '" + name
				+ "',\n" + "   wall.time => '" + wallTime + "',\n"
				+ "   charge => '" + charge + "',\n" + "   node.count => '"
				+ nodeCount + "',\n" + "   proc.count => '" + processorCount
				+ "',\n" + "   queue.name => '" + queue + "',\n"
				+ "   start.time => '" + TGUtil.formatUTC(startTime) + "',\n"
				+ "   end.time => '" + TGUtil.formatUTC(endTime) + "',\n"
				+ "   local.id => '" + localJobId + "',\n"
				+ "   ResourceID => '" + resourceID + "',\n"
				+ "   project_number => '" + projectNumber + "'}";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.json.JSONString#toJSONString()
	 */
	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("allocation").object().key("id").value(this.id)
					.endObject().object().key("name").value(this.name)
					.endObject().object().key("wall.time").value(this.wallTime)
					.endObject().object().key("charge").value(this.charge)
					.endObject().object().key("node.count").value(
							this.nodeCount).endObject().object().key(
							"proc.count").value(this.processorCount)
					.endObject().object().key("queue.name").value(this.queue)
					.endObject().object().key("start.time").value(
							TGUtil.formatUTC(startTime)).endObject().object()
					.key("end.time").value(TGUtil.formatUTC(endTime))
					.endObject().object().key("local.id")
					.value(this.localJobId).endObject().object().key(
							"ResourceID").value(this.resourceID).endObject()
					.object().key("project_number").value(this.projectNumber)
					.endObject().endObject();

			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;
	}

}
