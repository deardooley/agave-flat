package org.teragrid.service.tgcdb.dto;

public class IM {

	public IMProvider	provider;
	public String		handle;

	public IM(IMProvider provider, String handle)
	{
		this.provider = provider;
		this.handle = handle;
	}

	public IMProvider getProvider()
	{
		return provider;
	}

	public void setProvider(IMProvider provider)
	{
		this.provider = provider;
	}

	public String getHandle()
	{
		return handle;
	}

	public void setHandle(String handle)
	{
		this.handle = handle;
	}

	public String toString()
	{
		return "\t\t\"im\" : {\n" + "\t\t\t\"provider : " + getProvider() + ","
				+ "\t\t\t\"handle : " + getHandle() + "\t\t}" + ",";
	}

}
