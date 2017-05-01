package org.iplantc.service.jobs.scheduling;

import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Test(groups={"integration"})
public class JobSchedulingFairDistributionTest extends AbstractJobSchedulingTest 
{
	@DataProvider
	public Object[][] getRandomUserForNextQueuedJobOfStatusProvider()
	throws Exception
	{
		int TOTAL_TEST_SETS = 10;
		
		return new Object[][] {
//				{ TOTAL_TEST_SETS, JobStatusType.PENDING },
				{ TOTAL_TEST_SETS, JobStatusType.STAGED },
		};
	}
	
	/**
	 * Tests a fair selection of users for pending jobs when users all have
	 * even number of pending jobs.
	 *  
	 * @throws Exception
	 */
	@Test(dataProvider="getRandomUserForNextQueuedJobOfStatusProvider", enabled=true)
	public void getRandomUserForNextQueuedJobOfStatus(int TOTAL_TEST_SETS, JobStatusType status) 
	throws Exception
	{
		int TOTAL_TEST_CASES = TOTAL_TEST_SETS * TEST_USER_COUNT;
		
		TreeMap<String,List<String>> jobDistribution = loadRandomDistributionOfJobsForSingleApp(TOTAL_TEST_CASES, status);
		
		TreeMap<String, Integer> jobSelection = new TreeMap<String, Integer>();
		for (String username: testUsernames) {
			jobSelection.put(username, 0);
		}
		
		// make the actual selection to verify uniform distribution
		for (int i=0;i<TOTAL_TEST_SETS*TEST_USER_COUNT; i++) {
			String username = JobDao.getRandomUserForNextQueuedJobOfStatus(status, null, null, null);			 
			jobSelection.put(username, jobSelection.get(username).intValue() + 1);
		}
		
		double[] doubles = new double[jobDistribution.size()];
		double mean = 0;
		int i = 0;
		for(List<String> assignedJobs: jobDistribution.values()) {
		    doubles[i] = assignedJobs.size();
		    mean += doubles[i];
		    i++;
		}
		mean = mean / jobDistribution.size();
		
		StandardDeviation stdev = new StandardDeviation();
		double sd = stdev.evaluate(doubles, TOTAL_TEST_SETS);
		NormalDistribution nd = new NormalDistribution(mean, sd);
		
		ObjectMapper mapper = new ObjectMapper();
		System.out.println("Job assignment and distribution:");
		System.out.println("\tstandard deviation: " + sd);
		System.out.println("\tmean: " + mean);
		for(String username: jobSelection.keySet()) {
			JsonNode json = mapper.createObjectNode()
				.put("username", username)
				.put("created", jobDistribution.get(username).size())
				.put("assigned", jobSelection.get(username))
				.put("probability", nd.cumulativeProbability((double)jobSelection.get(username)));
			System.out.println(json.toString());
		}
			
		for(String username: jobSelection.keySet()) {
			int assigned = jobSelection.get(username);
			int created = jobDistribution.get(username).size();
			Assert.assertTrue((assigned <= mean + sd) && (assigned >= mean - sd), 
					"Job distribution for " + username + " was more than 1 standard deviation "
					+ "off of the expected value. Actual: " + created + ", Expected: " + assigned + " +- " + sd);
		}
	}
	
	/**
     * Tests a fair selection of users for pending jobs when users all have
     * even number of pending jobs.
     *  
     * @throws Exception
     */
    @Test(dataProvider="getRandomUserForNextQueuedJobOfStatusProvider", enabled=false)
    public void getRandomUserForNextQueuedJobOfStatusSelectsAcrossSystems(int TOTAL_TEST_SETS, JobStatusType status) 
    throws Exception
    {
        int TOTAL_TEST_CASES = TOTAL_TEST_SETS * TEST_USER_COUNT;
        
        TreeMap<String,List<String>> jobDistribution = loadRandomDistributionOfJobsAcrossMultipleSystems(TOTAL_TEST_CASES, status);
        
        TreeMap<String, Integer> jobSelection = new TreeMap<String, Integer>();
        for (String username: testUsernames) {
            jobSelection.put(username, 0);
        }
        
        // make the actual selection to verify uniform distribution
        for (int i=0;i<TOTAL_TEST_SETS*TEST_USER_COUNT; i++) {
            String username = JobDao.getRandomUserForNextQueuedJobOfStatus(status, null, null, null);             
            jobSelection.put(username, jobSelection.get(username).intValue() + 1);
        }
        
        double[] doubles = new double[jobDistribution.size()];
        double mean = 0;
        int i = 0;
        for(List<String> assignedJobs: jobDistribution.values()) {
            doubles[i] = assignedJobs.size();
            mean += doubles[i];
            i++;
        }
        mean = mean / jobDistribution.size();
        
        StandardDeviation stdev = new StandardDeviation();
        double sd = stdev.evaluate(doubles, TOTAL_TEST_SETS);
        NormalDistribution nd = new NormalDistribution(mean, sd);
        
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Job assignment and distribution:");
        System.out.println("\tstandard deviation: " + sd);
        System.out.println("\tmean: " + mean);
        for(String username: jobSelection.keySet()) {
            JsonNode json = mapper.createObjectNode()
                .put("username", username)
                .put("created", jobDistribution.get(username).size())
                .put("assigned", jobSelection.get(username))
                .put("probability", nd.cumulativeProbability((double)jobSelection.get(username)));
            System.out.println(json.toString());
        }
            
        for (String username: jobSelection.keySet()) {
            int assigned = jobSelection.get(username);
            int created = jobDistribution.get(username).size();
            Assert.assertTrue((assigned <= mean + sd) && (assigned >= mean - sd), 
                    "Job distribution for " + username + " was more than 1 standard deviation "
                    + "off of the expected value. Actual: " + created + ", Expected: " + assigned + " +- " + sd);
        }
    }
	
//	@Test(enabled=false)
//	public void testRandomSelectionOfUserTest() throws Exception
//	{
//		
//		Hashtable<String, Integer> vals = new Hashtable<String, Integer>();
//		for (int i=0;i<1000;i++) {
//			String username = JobDao.getRandomUserForNextQueuedJobOfStatus(JobStatusType.PENDING, null);
//			
//			vals.put(username, vals.containsKey(username) ? vals.get(username) + 1 : 1);
//		}
//		
//		for(String username: vals.keySet()) {
//			System.out.println(username + ": " + vals.get(username));
//		}
//		System.out.println("Selected " + vals.size() + " unique users.");
//	}
//	
//	@Test(enabled=false)
//	public void testRandomSelectionOfJobTest() throws Exception
//	{
//		
//		TreeMap<String, Long> users = new TreeMap<String, Long>();
//		TreeMap<String, Long> jobs = new TreeMap<String, Long>();
//		for (int i =0; i<10000; i++) {
//			Job job = JobDao.getNextQueuedJob(JobStatusType.PENDING, null);
//			
//			users.put(job.getOwner(), users.containsKey(job.getOwner()) ? users.get(job.getOwner()) + 1 : 1);
//			jobs.put(job.getOwner() + "-" + job.getUuid(), jobs.containsKey(job.getOwner() + "-" + job.getUuid()) ? jobs.get(job.getOwner() + "-" + job.getUuid()) + 1 : 1);
//		}
//		System.out.println("Selected " + users.size() + " unique users.");
//		for (String key: users.descendingKeySet()) {
//			System.out.println(key + ": " + users.get(key));
//		}
//		
//		System.out.println("Selected " + jobs.size() + " unique jobs.");
//		for (String key: jobs.descendingKeySet()) {
//			System.out.println(key + ": " + jobs.get(key));
//		}
//		
//	}
}
