package org.teragrid.service.tgcdb.dto;

public class BatchQueue {

	private String 			name;
	private int				maxJobs = Integer.MAX_VALUE;
	private int				maxMemory = Integer.MAX_VALUE;
	
	public BatchQueue() {}
	
	public BatchQueue(String name, int maxJobs, int maxMemory) 
	{
		this.name = name;
		this.maxJobs = maxJobs;
		this.maxMemory = maxMemory;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the maxJobs
	 */
	public int getMaxJobs()
	{
		return maxJobs;
	}

	/**
	 * @param maxJobs the maxJobs to set
	 */
	public void setMaxJobs(int maxJobs)
	{
		this.maxJobs = maxJobs;
	}

	/**
	 * @return the maxMemory
	 */
	public int getMaxMemory()
	{
		return maxMemory;
	}

	/**
	 * @param maxMemory the maxMemory to set
	 */
	public void setMaxMemory(int maxMemory)
	{
		this.maxMemory = maxMemory;
	}
	
	public String toString() {
		return this.name;
	}

}
