package org.iplantc.service.common.discovery.providers.sql;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DiscoverableServiceCapabilityTest
{
	@Test(dataProvider = "dp")
	public void f(Integer n, String s)
	{
	}

	@DataProvider
	public Object[][] discoverableServiceCapabilityProvider()
	{
		String[] tokens = { "", "*", "foo", "bar", "alph.a" };
		String[] delimiters = { "^" };

//		List<Object[]> testCases = new ArrayList<Object[]>();
//		StringUtils.join(s, "/");
//		List<String[]> expectedStrings = List<String[]>();
//		expectedStrings.add(new String[] {"*"});
//		for (int i=0;i<6; i++) 
//		{
//			for (String token : tokens)
//			{ 
//				
//			String capabilityString = "";
//			for (int j=0; j<=i; j++) 
//			{
//				capabilityString += "/" + tokens[j];
//			}
//			
//			
//			
//		}
//		
//			testCases.add(new Object[]{ )
//		}
		return new Object[][] { new Object[] { "*/*/*/*/*/*" } };
		
	}

	@Test(dataProvider = "discoverableServiceCapabilityProvider")
	public void DiscoverableServiceCapability(String capabilityString, String expectedCapability, String message)
	{

		DiscoverableServiceCapability capability = new DiscoverableServiceCapability(capabilityString);

		Assert.assertEquals(capability.toString(), expectedCapability, message);
	}

	@Test
	public void allows()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void invalidates()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void negates()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void setActivityType()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void setApiName()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void supports()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void testToString()
	{
		throw new RuntimeException("Test not implemented");
	}
}
