package org.iplantc.service.jobs.dao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class JobSearchTest  extends AbstractDaoTest {

	@BeforeClass
	@Override
	public void beforeClass() throws Exception
	{
		HibernateUtil.getConfiguration().getProperties().setProperty("hibernate.show_sql", "false");
		super.beforeClass();
		SoftwareDao.persist(software);
	}
	
	@AfterClass
	@Override
	public void afterClass() throws Exception
	{
		super.afterClass();
	}
	
	@BeforeMethod
	public void setUp() throws Exception {
		initSystems();
        initSoftware();
        clearJobs();
	}
	
	@AfterMethod
	public void tearDown() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
	}
	
	@DataProvider(name="searchJobsByDerivedRunTimeProvider")
    protected Object[][] searchJobsByDerivedRunTimeProvider() throws Exception
    {
        List<Object[]> searchCriteria = new ArrayList<Object[]>();
        
        for (JobStatusType status: JobStatusType.values()) 
        {
            Job job = createJob(status);
            searchCriteria.add(new Object[]{ status, "runtime.le", true, "Searching by less than or equal to  known exact runtime should not fail" });
            
            if (job.getStartTime() == null) 
            {  
                searchCriteria.add(new Object[]{ status, "runtime", false, "Searching by known exact runtime on a job that has not started should not fail" });
                searchCriteria.add(new Object[]{ status, "runtime.eq", false, "Searching by known exact runtime on a job that has not started should not fail" });
                searchCriteria.add(new Object[]{ status, "runtime.le", true, "Searching by less than or equal to known exact runtime on a job that has not started should succeed" });
                searchCriteria.add(new Object[]{ status, "runtime.lt", true, "Searching less than known exact runtime on a job that has not started should succeed" });
                searchCriteria.add(new Object[]{ status, "runtime.gt", false, "Searching runtime strictly greater than than actual amount on a job that has not started should fail" });
                searchCriteria.add(new Object[]{ status, "runtime.in", false, "Searching runtime with exact value in range on a job that has not started should not fail" });
                searchCriteria.add(new Object[]{ status, "runtime.ge", false, "Searching by greater than or equal to known exact runtime on a job that has not started should not fail" });
            }
            else 
            {  
                searchCriteria.add(new Object[]{ status, "runtime", false, "Searching by old runtime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "runtime.eq", false, "Searching by old runtime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "runtime.ge", false, "Searching by greater than or equal to old runtime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "runtime.lt", true, "Searching by less than old runtime of unfinished job should succeed" });
                searchCriteria.add(new Object[]{ status, "runtime.gt", false, "Searching by value greater than or equal to old runtime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "runtime.in", false, "Searching by range with values greater than or equal to old runtime of unfinished job should fail" });
            }
        };
        
        return searchCriteria.toArray(new Object[][] {});
    }
	
	@Test(dataProvider="searchJobsByDerivedRunTimeProvider", enabled=false)
    public void searchJobsByDerivedRunTime(JobStatusType status, String searchField, boolean shouldSucceed, String message) throws Exception
    {   
        Job job = createJob(status);
        JobDao.persist(job);
        Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
        
        long endTime = (job.getEndTime() == null ? new Date().getTime() : job.getEndTime().getTime());
        
        Integer searchValue = Math.round((endTime - job.getStartTime().getTime()) / 1000);
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("walltime", String.valueOf(searchValue));
        
        List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertEquals(searchJobs == null, shouldSucceed, message);
        if (shouldSucceed) {
            Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by " + searchField);
            Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job.");
        }
        
    }
	
	@DataProvider(name="searchJobsByDerivedWallTimeProvider")
	protected Object[][] searchJobsByDerivedTimeProvider() throws Exception
    {
	    List<Object[]> searchCriteria = new ArrayList<Object[]>();
        
	    for (JobStatusType status: JobStatusType.values()) 
	    {   
	        searchCriteria.add(new Object[]{ status, "walltime.le", true, "Searching by less than or equal to  known exact walltime should not fail" });
	        
	        if (JobStatusType.isFinished(status)) 
	        {  
	            searchCriteria.add(new Object[]{ status, "walltime", true, "Searching by known exact walltime should not fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.eq", true, "Searching by known exact walltime should not fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.le", true, "Searching by less than or equal to  known exact walltime should not fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.lt", false, "Searching walltime less than actual amount should fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.gt", false, "Searching walltime strictly greater than than actual amount should fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.in", true, "Searching walltime with exact value in range should not fail" });
	            searchCriteria.add(new Object[]{ status, "walltime.ge", true, "Searching by greater than or equal to known exact walltime should not fail" });
	        }
	        else 
	        {  
                searchCriteria.add(new Object[]{ status, "walltime", false, "Searching by old walltime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "walltime.eq", false, "Searching by old walltime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "walltime.ge", false, "Searching by greater than or equal to old walltime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "walltime.lt", true, "Searching by less than old walltime of unfinished job should succeed" });
                searchCriteria.add(new Object[]{ status, "walltime.gt", false, "Searching by value greater than or equal to old walltime of unfinished job should fail" });
                searchCriteria.add(new Object[]{ status, "walltime.in", false, "Searching by range with values greater than or equal to old walltime of unfinished job should fail" });
            }
        };
        
        return searchCriteria.toArray(new Object[][] {});
    }
	
	@Test(dataProvider="searchJobsByDerivedWallTimeProvider")
	public void searchJobsByDerivedWallTime(JobStatusType status, String searchField, boolean shouldSucceed, String message) throws Exception
    {   
	    Job job = createJob(status);
        JobDao.persist(job);
        Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
        
        long endTime = (job.getEndTime() == null ? new Date().getTime() : job.getEndTime().getTime());
        Integer searchValue = Math.round((endTime - job.getCreated().getTime()) / 1000);
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("walltime", String.valueOf(searchValue));
        
        List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertEquals(searchJobs == null, shouldSucceed, message);
        if (shouldSucceed) {
            Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by " + searchField);
            Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job.");
        }
	    
    }
	
	@DataProvider(name="searchJobsProvider")
	protected Object[][] searchJobsProvider() throws Exception
	{
		Job job = createJob(JobStatusType.FINISHED);
		return new Object[][] {
			{ "appid", job.getSoftwareName() },
			{ "archive", job.isArchiveOutput() },
			{ "archivepath", job.getArchivePath() },
			{ "archivesystem", job.getArchiveSystem().getSystemId() },
			{ "batchqueue", job.getBatchQueue() },
			{ "executionsystem", job.getSystem() },
			{ "inputs", job.getInputs() },
//			{ "localId", job.getLocalJobId() },
			{ "maxruntime", job.getMaxRunTime() },
			{ "memorypernode", job.getMemoryPerNode() },
			{ "name", job.getName() },
			{ "nodecount", job.getNodeCount() },
			{ "outputpath", job.getOutputPath() },
			{ "owner", job.getOwner() },
			{ "parameters", job.getParameters() },
			{ "processorspernode", job.getProcessorsPerNode() },
			{ "retries", job.getRetries() },
			{ "visible", job.isVisible() },
		};
	}
	
	@Test(dataProvider="searchJobsProvider")
	public void findMatching(String attribute, Object value) throws Exception
	{
		Job job = createJob(JobStatusType.FINISHED);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(attribute, String.valueOf(value));
		
		List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
		Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs.");
		Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by " + attribute);
		Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job.");
	}
	
	@Test(dataProvider="searchJobsProvider", dependsOnMethods={"findMatching"} )
    public void findMatchingTime(String attribute, Object value) throws Exception
    {
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	    
        Job job = createJob(JobStatusType.FINISHED);
        JobDao.persist(job);
        Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("starttime", formatter.format(job.getStartTime()));
        
        List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs matching starttime.");
        Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by starttime");
        Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job when searching by starttime.");
        
        map.clear();
        map.put("created", formatter.format(job.getCreated()));
        
        searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs matching created.");
        Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by created");
        Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job when searching by created.");
        
        
        map.clear();
        map.put("endtime", formatter.format(job.getEndTime()));
        
        searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs matching endtime.");
        Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by endtime");
        Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job when searching by endtime.");
        
        
        map.clear();
        map.put("submittime", formatter.format(job.getSubmitTime()));
        
        searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs matching submittime.");
        Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by submittime");
        Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job when searching by submittime.");
    }
	
	@Test(dependsOnMethods={"findMatchingTime"}, dataProvider="searchJobsProvider")
	public void findMatchingCaseInsensitive(String attribute, Object value) throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(attribute.toUpperCase(), String.valueOf(value));
		
		List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
		Assert.assertNotNull(searchJobs, "findMatching failed to find any jobs.");
		Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by " + attribute);
		Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job.");
	}
	
	@DataProvider
	protected Object[][] dateSearchExpressionTestProvider() 
	{
	    List<Object[]> testCases = new ArrayList<Object[]>();
	    String[] timeFormats = new String[] { "Ka", "KKa", "K a", "KK a", "K:mm a", "KK:mm a", "Kmm a", "KKmm a", "H:mm", "HH:mm", "HH:mm:ss"};
	    String[] sqlDateFormats = new String[] { "YYYY-MM-dd", "YYYY-MM"};
	    String[] relativeDateFormats = new String[] { "yesterday", "today", "-1 day", "-1 month", "-1 year", "+1 day", "+1 month"};
	    String[] calendarDateFormats = new String[] {"MMMM d", "MMM d", "YYYY-MM-dd", "MMMM d, Y", 
	                                                "MMM d, Y", "MMMM d, Y", "MMM d, Y",
	                                                "M/d/Y", "M/dd/Y", "MM/dd/Y", "M/d/YYYY", "M/dd/YYYY", "MM/dd/YYYY"};
	    
	    DateTime dateTime = new DateTime().minusMonths(1);
	    for(String field: new String[] {"created.before"}) {   
	        
	        // milliseconds since epoch
	        testCases.add(new Object[] {field, "" + dateTime.getMillis(), true});
            
	        // ISO 8601
	        testCases.add(new Object[] {field, dateTime.toString(), true});
	        
	        // SQL date and time format
	        for (String date: sqlDateFormats) {
	            for (String time: timeFormats) {
                    testCases.add(new Object[] {field, dateTime.toString(date + " " + time), date.contains("dd") && !time.contains("Kmm")});
                }
	            testCases.add(new Object[] {field, dateTime.toString(date), true});
	        }
	        
	        // Relative date formats
	        for (String date: relativeDateFormats) {
	            for (String time: timeFormats) {
                    testCases.add(new Object[] {field, date + " " + dateTime.toString(time), !(!(date.contains("month") || date.contains("year") || date.contains(" day")) && time.contains("Kmm"))});
                }
	            testCases.add(new Object[] {field, date, true});
	        }
	        
	        
	        for (String date: calendarDateFormats) {
	            for (String time: timeFormats) {
	                testCases.add(new Object[] {field, dateTime.toString(date + " " + time), !(date.contains("d") && time.contains("Kmm"))});
	            }
	            testCases.add(new Object[] {field, dateTime.toString(date), true});
	        }
	            
	    }
	    
	    return testCases.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="dateSearchExpressionTestProvider", dependsOnMethods={"findMatchingCaseInsensitive"}, enabled=true)
	public void dateSearchExpressionTest(String attribute, String dateFormattedString, boolean shouldSucceed) throws Exception
//	@Test
//	public void dateSearchExpressionTest() throws Exception
    {
//		String attribute = "created.before";
//		String dateFormattedString = "yesterday 10:43PM";
//		boolean shouldSucceed = true;
		
	    Job job = createJob(JobStatusType.ARCHIVING);
	    job.setCreated(new DateTime().minusYears(5).toDate());
        JobDao.persist(job);
        Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
        
        Map<String, String> map = new HashMap<String, String>();
        map.put(attribute, dateFormattedString);
        
        try
        {
            List<Job> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
            Assert.assertNotEquals(searchJobs == null, shouldSucceed, "Searching by date string of the format " 
                        + dateFormattedString + " should " + (shouldSucceed ? "" : "not ") + "succeed");
            if (shouldSucceed) {
                Assert.assertEquals(searchJobs.size(), 1, "findMatching returned the wrong number of jobs for search by " 
                        + attribute + "=" + dateFormattedString);
                Assert.assertTrue(searchJobs.contains(job), "findMatching did not return the saved job.");
            }
        }
        catch (Exception e) {
            if (shouldSucceed) {
                Assert.fail("Searching by date string of the format " 
                    + dateFormattedString + " should " + (shouldSucceed ? "" : "not ") + "succeed",e);
            }
        }
    }
	
}
