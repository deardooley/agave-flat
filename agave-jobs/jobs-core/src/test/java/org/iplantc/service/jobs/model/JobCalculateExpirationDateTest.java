package org.iplantc.service.jobs.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.iplantc.service.jobs.dao.JobDao;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import com.jcabi.immutable.Array;

public class JobCalculateExpirationDateTest
{
	@BeforeMethod
	public void beforeMethod()
	{
		
	}

	@AfterMethod
	public void afterMethod()
	{
	}

	

	@BeforeClass
	public void beforeClass()
	{
		System.setProperty("user.timezone", "America/Chicago");
	}

	@AfterClass
	public void afterClass()
	{
		
	}
	
	public void _calculateExpirationDateTest(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		Job job = new Job();
		job.setCreated(createdAt);
		job.setStartTime(startedAt);
		job.setEndTime(endedAt);
		job.setMaxRunTime(maxRunTime);
		
		Assert.assertEquals(job.calculateExpirationDate(), expectedExpirationDate.toDate(), message);	
	}
	
	@DataProvider
	public Object[][] returnsEndTimeIfNotNullProvider()
	{
		Date now = new Date();
		
		return new Object[][] { 
				{ null, null, now, null, new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ null, now, now, null, new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ now, null, now, null, new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ null, null, now, "24:00:00", new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ now, null, now, "24:00:00", new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ null, now, now, "24:00:00", new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
				{ now, now, now, "24:00:00", new DateTime(now), "calculateExpirationDate should be the job end time if it exists." },
		};
	}
	
	@Test(dataProvider = "returnsEndTimeIfNotNullProvider")
	public void returnsEndTimeIfNotNull(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		_calculateExpirationDateTest(createdAt, startedAt, endedAt, maxRunTime, expectedExpirationDate, message);	
	}
	
	@DataProvider
	public Object[][] usesStartTimeIfNotNullAndEndTimeNullProvider()
	{
		Date now = new Date();
		
		return new Object[][] { 
				{ null, now, null, null, new DateTime(now).plusHours(1000), "calculateExpirationDate should be 1000 hours after start if no maxRunTime given." },
				{ now, now, null, null, new DateTime(now).plusHours(1000), "calculateExpirationDate should be 1000 hours after start if no maxRunTime given." },
		};
	}
	
	@Test(dataProvider = "usesStartTimeIfNotNullAndEndTimeNullProvider", dependsOnMethods={"returnsEndTimeIfNotNull"})
	public void usesStartTimeIfNotNullAndEndTimeNull(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		_calculateExpirationDateTest(createdAt, startedAt, endedAt, maxRunTime, expectedExpirationDate, message);	
	}
	
	@DataProvider
	public Object[][] calculateExpirationDateTestProvider()
	{
		Date now = new Date();
		List<Object[]> testDataCases = new ArrayList<Object[]>();
		
		for (int i=0; i<=100; i+=10) 
		{
			String hours = i < 10 ? "0" + i : String.valueOf(i);
			for (int j=0; j<=100; j+=10) 
			{
				String mins = j < 10 ? "0" + j : String.valueOf(j);
				for (int k=0; k<=100; k+=10) 
				{
					String secs = k < 10 ? "0" + k : String.valueOf(k);
					String stime = hours + ":" + mins + ":" + secs;
					DateTime expectedTime = new DateTime(now).plusHours(i)
															 .plusMinutes(j)
															 .plusSeconds(k)
															 .plusDays(1);
					testDataCases.add(new Object[]{ null, now, null, stime, expectedTime, 
							String.format("calculateExpirationDate should be 24 hours after "
										+ "the job start time plus %s hours, %s minutes, "
										+ "and %s seconds when maxRunTime is %s and created is null",
										  hours, mins, secs, stime) });
					testDataCases.add(new Object[]{ now, now, null, stime, expectedTime, 
							String.format("calculateExpirationDate should be 24 hours after "
										+ "the job start time plus %s hours, %s minutes, "
										+ "and %s seconds when maxRunTime is %s",
										  hours, mins, secs, stime) });
					
				}
			}
		}
		return testDataCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "calculateExpirationDateTestProvider", dependsOnMethods={"usesStartTimeIfNotNullAndEndTimeNull"})
	public void calculateExpirationDateTest(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		_calculateExpirationDateTest(createdAt, startedAt, endedAt, maxRunTime, expectedExpirationDate, message);	
	}
	
	@DataProvider
	public Object[][] leadingZerosIgnoredTestProvider()
	{
		Date now = new Date();
		List<Object[]> testDataCases = new ArrayList<Object[]>();
		
		for (int i=0; i<10; i++) 
		{
			for (int j=0; j<10; j++) 
			{
				for (int k=0; k<10; k++) 
				{
					String stime = String.valueOf(i) + ":" + String.valueOf(j) + ":" + String.valueOf(k);
					DateTime expectedTime = new DateTime(now).plusHours(i)
															 .plusMinutes(j)
															 .plusSeconds(k)
															 .plusDays(1);
					testDataCases.add(new Object[]{ null, now, null, stime, expectedTime, 
							String.format("calculateExpirationDate should be 24 hours after the "
										+ "job start time plus %s hours, %s minutes, "
										+ "and %s seconds when maxRunTime is %s, "
										+ "no leading zeros are used, and created is null", 
										 i, j, k, stime) });
					testDataCases.add(new Object[]{ null, now, null, stime, expectedTime, 
							String.format("calculateExpirationDate should be 24 hours after the "
									+ "job start time plus %s hours, %s minutes, "
									+ "and %s seconds when maxRunTime is %s "
									+ "and no leading zeros are used", 
									 i, j, k, stime) });
					
				}
			}
		}
		return testDataCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "leadingZerosIgnoredTestProvider", dependsOnMethods={"calculateExpirationDateTest"})
	public void leadingZerosIgnoredTest(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		_calculateExpirationDateTest(createdAt, startedAt, endedAt, maxRunTime, expectedExpirationDate, message);	
	}
	
	@DataProvider
	public Object[][] usesFourteenDaysAfterCreatedTimeIfStartTimeAndEndTimeNullProvider()
	{
		Date created = new Date();
		List<Object[]> testDataCases = new ArrayList<Object[]>();
		
		for (int i=0; i<=100; i+=10) 
		{
			String hours = i < 10 ? "0" + i : String.valueOf(i);
			for (int j=0; j<=100; j+=10) 
			{
				String mins = j < 10 ? "0" + j : String.valueOf(j);
				for (int k=0; k<=100; k+=10) 
				{
					String secs = k < 10 ? "0" + k : String.valueOf(k);
					String stime = hours + ":" + mins + ":" + secs;
					DateTime expectedTime = new DateTime(created).plusDays(14)
															 .plusHours(i)
															 .plusMinutes(j)
															 .plusSeconds(k)
															 .plusDays(1);
					testDataCases.add(new Object[]{ created, null, null, stime, expectedTime, 
							String.format("calculateExpirationDate should be 24 hours after the "
										+ "job created time plus 14 days, %s hours, %s minutes, "
										+ "and %s seconds when maxRunTime is %s and start and "
										+ "end times are null",
										  hours, mins, secs, stime) });
					
					
				}
			}
		}
		return testDataCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "usesFourteenDaysAfterCreatedTimeIfStartTimeAndEndTimeNullProvider", dependsOnMethods={"leadingZerosIgnoredTest"})
	public void usesFourteenDaysAfterCreatedTimeIfStartTimeAndEndTimeNull(Date createdAt, Date startedAt, Date endedAt, String maxRunTime, DateTime expectedExpirationDate, String message)
	throws Exception
	{
		_calculateExpirationDateTest(createdAt, startedAt, endedAt, maxRunTime, expectedExpirationDate, message);	
	}
	
	
}
