package org.teragrid.service.tgcdb.dto;

public abstract class ResourceDTO {

	protected String	name;
	protected String	resourceId;
	protected String	site;
	protected String	type;

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
	 * @return the resourceId
	 */
	public String getResourceId()
	{
		return resourceId;
	}

	/**
	 * @param resourceId
	 *            the resourceId to set
	 */
	public void setResourceId(String resourceId)
	{
		this.resourceId = resourceId;
	}

	/**
	 * @param site
	 *            the site to set
	 */
	public void setSite(String site)
	{
		this.site = site;
	}

	/**
	 * @return the site
	 */
	public String getSite()
	{
		return site;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

}
