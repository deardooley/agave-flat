package org.iplantc.service.apps.model;

/**
 * @author dooley
 * 
 */
public class Compiler {
	private String	name;
	private String	version;

	public Compiler(){}

	public Compiler(String name, String version)
	{
		this.name = name;
		this.version = version;
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
	 * @return the version
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version)
	{
		this.version = version;
	}

}
