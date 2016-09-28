package org.iplantc.service.apps.dao;

import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareOutput;
import org.iplantc.service.apps.search.SoftwareSearchFilter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class SoftwareSearchTest  extends AbstractDaoTest {

	@BeforeClass
	@Override
	public void beforeClass() throws Exception
	{
		HibernateUtil.getConfiguration().getProperties().setProperty("hibernate.show_sql", "true");
		super.beforeClass();
//		SoftwareOutput output = new SoftwareOutput();
//		output.setKey("default");
//		output.setDefaultValue("foo.out");
//		software.addOutput(output);
	}
	
	@AfterClass
	@Override
	public void afterClass() throws Exception
	{
		super.afterClass();
	}
	
	@BeforeMethod
	public void setUp() throws Exception {
//		initSystems();
		clearSoftware();
        this.software = createSoftware();
	}
	
	@AfterMethod
	public void tearDown() throws Exception {
		clearSoftware();
//		clearSystems();
	}
	
	protected Software createSoftware() throws JSONException, IOException {
	    JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
        software = Software.fromJSON(json, TEST_OWNER);
        software.setExecutionSystem(privateExecutionSystem);
        software.setOwner(TEST_OWNER);
        software.setVersion("1.0.1");
        software.setChecksum("abc12345");
//        SoftwareOutput output = new SoftwareOutput();
//        output.setKey("default");
//        output.setDefaultValue("foo.out");
//        software.addOutput(output);
        return software;
	}
	
	@DataProvider(name="softwaresProvider")
	public Object[][] softwaresProvider() throws Exception
	{
	    Software software = createSoftware();
	    
		return new Object[][] {
//	        { "available", software.isAvailable() },
//	        { "checkpointable", software.isCheckpointable() },
//	        { "checksum", software.getChecksum() },
////	        { "created", software.getCreated() },
//	        { "defaultMaxRunTime", software.getDefaultMaxRunTime() },
//	        { "defaultMemoryPerNode", software.getDefaultMemoryPerNode() },
//	        { "defaultNodes", software.getDefaultNodes() },
//	        { "defaultProcessorsPerNode", software.getDefaultProcessorsPerNode() },
//	        { "defaultQueue", software.getDefaultQueue() },
//	        { "deploymentPath", software.getDeploymentPath() },
//	        { "executionSystem", software.getExecutionSystem().getSystemId() },
//	        { "executionType", software.getExecutionType() },
//	        { "helpURI", software.getHelpURI() },
//	        { "icon", software.getIcon() },
//	        { "id", software.getUniqueName() },
//	        { "inputs.id", software.getInputs().get(0).getKey() },
//	        { "label", software.getLabel() },
////	        { "lastUpdated", software.getLastUpdated() },
//	        { "longDescription", software.getLongDescription() },
//	        { "modules", software.getModulesAsList().get(0) },
//	        { "name", software.getName() },
	        { "ontology.like", software.getOntologyAsList().get(0) },
//	        { "outputs.id", software.getOutputs().get(0).getKey() },
//	        { "owner", software.getOwner() },
//	        { "parallelism", software.getParallelism() },
//	        { "parameters.id", software.getParameters().get(0).getKey() },
//	        { "public", software.isPubliclyAvailable() },
//	        { "publicOnly", true },
//	        { "privateOnly", false },
//	        { "revision", software.getRevisionCount() },
//	        { "shortDescription", software.getShortDescription() },
//	        { "storageSystem", software.getStorageSystem().getSystemId() },
////	        { "tags", software.getTagsAsList().get(0) },
//	        { "templatePath", software.getExecutablePath() },
//            { "testPath", software.getTestPath() },
//	        { "uuid", software.getUuid() },
//	        { "version", software.getVersion() },
		};
	}
	
	@Test(dataProvider="softwaresProvider")
	public void findMatching(String attribute, Object value) throws Exception
	{
		Software software = createSoftware();
		if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
		    software.setPubliclyAvailable(false);
		} else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            software.setPubliclyAvailable(true);
        } 
		SoftwareDao.persist(software);
		Assert.assertNotNull(software.getId(), "Failed to generate a software ID.");
		
		Map<String, String> map = new HashMap<String, String>();
		if (StringUtils.equals(attribute, "uuid")) {
		    map.put(attribute, software.getUuid());
		}
		else if (StringUtils.equals(attribute, "ontology")) {
		    map.put(attribute, "*"+value+"*");
		} else {
		    map.put(attribute, String.valueOf(value));
		}
		
		List<Software> softwares = SoftwareDao.findMatching(software.getOwner(), new SoftwareSearchFilter().filterCriteria(map), false);
		Assert.assertNotNull(softwares, "findMatching failed to find any software.");
		Assert.assertEquals(softwares.size(), 1, "findMatching returned the wrong number of software for search by " + attribute);
		Assert.assertTrue(softwares.contains(software), "findMatching did not return the saved software.");
	}
	
	@Test(dataProvider="softwaresProvider", dependsOnMethods={"findMatching"} )
    public void findMatchingTime(String attribute, Object value) throws Exception
    {
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	    
        Software software = createSoftware();
        if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
            software.setPubliclyAvailable(false);
        } else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            software.setPubliclyAvailable(true);
        } 
        SoftwareDao.persist(software);
        Assert.assertNotNull(software.getId(), "Failed to generate a software ID.");
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("lastupdated", formatter.format(software.getLastUpdated()));
        
        List<Software> softwares = SoftwareDao.findMatching(software.getOwner(), new SoftwareSearchFilter().filterCriteria(map), false);
        Assert.assertNotNull(softwares, "findMatching failed to find any softwares matching starttime.");
        Assert.assertEquals(softwares.size(), 1, "findMatching returned the wrong number of software records for search by starttime");
        Assert.assertTrue(softwares.contains(software), "findMatching did not return the saved software when searching by starttime.");
        
        map.clear();
        map.put("created", formatter.format(software.getCreated()));
        
        softwares = SoftwareDao.findMatching(software.getOwner(), new SoftwareSearchFilter().filterCriteria(map), false);
        Assert.assertNotNull(softwares, "findMatching failed to find any softwares matching created.");
        Assert.assertEquals(softwares.size(), 1, "findMatching returned the wrong number of software records for search by created");
        Assert.assertTrue(softwares.contains(software), "findMatching did not return the saved software when searching by created.");
    }
	
	@Test(dependsOnMethods={"findMatchingTime"}, dataProvider="softwaresProvider")
	public void findMatchingCaseInsensitive(String attribute, Object value) throws Exception
	{
		Software software = createSoftware();
		if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
            software.setPubliclyAvailable(false);
        } else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            software.setPubliclyAvailable(true);
        } 
		SoftwareDao.persist(software);
		Assert.assertNotNull(software.getId(), "Failed to generate a software ID.");
		
		Map<String, String> map = new HashMap<String, String>();
		if (StringUtils.equals(attribute, "uuid")) {
            map.put(attribute, software.getUuid());
        } else {
            map.put(attribute, String.valueOf(value));
        }
		
		List<Software> softwares = SoftwareDao.findMatching(software.getOwner(), new SoftwareSearchFilter().filterCriteria(map), false);
		Assert.assertNotNull(softwares, "findMatching failed to find any softwares.");
		Assert.assertEquals(softwares.size(), 1, "findMatching returned the wrong number of software records for search by " + attribute);
		Assert.assertTrue(softwares.contains(software), "findMatching did not return the saved software.");
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
    {
	    Software software = createSoftware();
	    if (StringUtils.equalsIgnoreCase(attribute, "privateonly")) {
            software.setPubliclyAvailable(false);
        } else if (StringUtils.equalsIgnoreCase(attribute, "publiconly")) {
            software.setPubliclyAvailable(true);
        } 
	    software.setCreated(new DateTime().minusYears(5).toDate());
        SoftwareDao.persist(software);
        Assert.assertNotNull(software.getId(), "Failed to generate a software ID.");
        
        Map<String, String> map = new HashMap<String, String>();
        map.put(attribute, dateFormattedString);
        
        try
        {
            List<Software> softwares = SoftwareDao.findMatching(software.getOwner(), new SoftwareSearchFilter().filterCriteria(map), false);
            Assert.assertNotEquals(softwares == null, shouldSucceed, "Searching by date string of the format " 
                        + dateFormattedString + " should " + (shouldSucceed ? "" : "not ") + "succeed");
            if (shouldSucceed) {
                Assert.assertEquals(softwares.size(), 1, "findMatching returned the wrong number of software records for search by " 
                        + attribute + "=" + dateFormattedString);
                Assert.assertTrue(softwares.contains(software), "findMatching did not return the saved software.");
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
