package org.iplantc.service.jobs.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.dto.JobDTO;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JobDTOTest extends AbstractDaoTest {

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
	
	@Test
    public void toJSONTest() throws Exception
    {   
        Job job = createJob(JobStatusType.RUNNING);
        JobDao.persist(job);
        
        Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("id.eq", job.getUuid());
        
        List<JobDTO> searchJobs = JobDao.findMatching(job.getOwner(), new JobSearchFilter().filterCriteria(map));
        
        Assert.assertEquals(searchJobs.size(), 1, "No job returned after persisting. DTO serialization test will fail.");
        Assert.assertEquals(searchJobs.get(0).getUuid(), job.getUuid(), "findMatching did not return the saved job when searching by id");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode modelObject = (ObjectNode)mapper.readTree(job.toJSON());
        ObjectNode dtoObject = (ObjectNode)searchJobs.get(0).toJSON();
        for (Iterator<String> iter = modelObject.fieldNames(); iter.hasNext();) {
        	String fieldName = iter.next();
        	Assert.assertEquals(dtoObject.get(fieldName).toString(), modelObject.get(fieldName).toString(), fieldName + " in the serialized JobDTO should be equal to the same named field in the original serialized Job.");
        }
        Assert.assertEquals(searchJobs.get(0).toJSON().toString(), job.toJSON(), "findMatching did not return the saved job when searching by id");
        
    }
}