package org.iplantc.service.apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class SoftwareTestCase extends TestCase {
	protected String	TEST_SESSION_DATA_FILE	= "test.dat";

	public SoftwareTestCase()
	{
		super();
	}

	public SoftwareTestCase(String name)
	{
		super(name);
	}

	protected void setProperty(String name, String value) throws IOException
	{

		Properties props = getProperties();
		props.put(name, value);
		saveProperties(props);
	}

	protected String getProperty(String name) throws IOException
	{
		return (String) getProperties().get(name);
	}

	protected void saveProperties(Properties props) throws IOException
	{
		File propsFile = new File(TEST_SESSION_DATA_FILE);
		if (!propsFile.exists())
		{
			propsFile.createNewFile();
		}

		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(propsFile);
			props.store(out, "iPlant junit test properties file");
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	protected Properties getProperties() throws IOException
	{
		File propsFile = new File(TEST_SESSION_DATA_FILE);
		if (!propsFile.exists())
		{
			propsFile.createNewFile();
		}

		Properties props = new Properties();
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(propsFile);
			props.load(in);
			return props;
		}
		finally
		{
			try
			{
				in.close();
			}
			catch (IOException e)
			{
			}
		}

	}

}
