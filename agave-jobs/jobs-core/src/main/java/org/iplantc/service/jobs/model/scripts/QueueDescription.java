package org.iplantc.service.jobs.model.scripts;

import org.json.JSONObject;

class QueueDescription {
	String name;
	int memoryTotal;
	int coresPerNode;
	int[] wayness;

	public QueueDescription()
	{}

	public QueueDescription(String name, int memoryTotal, int coresPerNode, int[] wayness)
	{
		this.name = name;
		this.memoryTotal = memoryTotal;
		this.coresPerNode = coresPerNode;
		this.wayness = wayness;
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
	 * @return the memoryTotal
	 */
	public int getMemoryTotal()
	{
		return memoryTotal;
	}

	/**
	 * @param memoryTotal
	 *            the memoryTotal to set
	 */
	public void setMemoryTotal(int memoryTotal)
	{
		this.memoryTotal = memoryTotal;
	}

	/**
	 * @return the coresPerNode
	 */
	public int getCoresPerNode()
	{
		return coresPerNode;
	}

	/**
	 * @param coresPerNode
	 *            the coresPerNode to set
	 */
	public void setCoresPerNode(int coresPerNode)
	{
		this.coresPerNode = coresPerNode;
	}

	/**
	 * @return the wayness
	 */
	public int[] getWayness()
	{
		return wayness;
	}

	/**
	 * @param wayness
	 *            the wayness to set
	 */
	public void setWayness(int[] wayness)
	{
		this.wayness = wayness;
	}

	public String toString()
	{
		return new JSONObject(this).toString();
	}

}