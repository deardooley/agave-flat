package org.iplantc.service.tags;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.model.Tag;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.surftools.BeanstalkClientImpl.ClientImpl;

public class AbstractTagTest {

	protected ObjectMapper mapper = new ObjectMapper();
	protected NotificationDao notificationDao = new NotificationDao();
	protected TagDao dao = new TagDao();
	protected TestDataHelper dataHelper;
	protected Tenant defaultTenant;
	protected TenantDao tenantDao;
	
	protected Tag createTag() throws TagException, JSONException, IOException
	{
		ObjectNode json = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG);
		json.put("owner", TestDataHelper.TEST_USER);
		((ArrayNode)json.get("associationIds")).removeAll().add(defaultTenant.getUuid());
		
		return Tag.fromJSON(json);
	}
	
	
	protected void clearNotifications() throws TagException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();
			session.createQuery("delete Notification").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new TagException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	protected void clearTags() throws TagException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
			session.createQuery("delete TagEvent").executeUpdate();
			session.createQuery("delete TagPermission").executeUpdate();
			session.createQuery("delete Tag").executeUpdate();
			session.createQuery("delete TaggedResource").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new TagException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	protected void clearTenants() throws TagException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
			session.createQuery("delete Tenant").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new TagException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		dataHelper = TestDataHelper.getInstance();
		
		HibernateUtil.getConfiguration();
		
		clearTags();
		clearNotifications();
		clearTenants();
		
		initTenant();
	}
	
	protected void initTenant() throws TenantException {
		defaultTenant = new Tenant("agave.dev", "http://docker.example.com", "foo@example.com", "Dev User");
		tenantDao = new TenantDao();
		tenantDao.persist(defaultTenant);
		TenancyHelper.setCurrentTenantId(defaultTenant.getTenantCode());
		TenancyHelper.setCurrentEndUser(TestDataHelper.TEST_USER);
	}	

	@AfterClass
	protected void afterClass() throws TagException {
		
		clearTags();
		clearNotifications();
		clearQueues();
		clearTenants();
	}
	
	/**
	 * Flushes the messaging tube of any and all existing jobs.
	 * @param queueName
	 */
	@AfterMethod
	protected void clearQueues() 
	{
		ClientImpl client = null;
	
		// drain the message queue
		client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
				Settings.MESSAGING_SERVICE_PORT);
		
		for (String tube: client.listTubes())
		{
			try {
				client.watch(tube);
				client.useTube(tube);
				client.kick(Integer.MAX_VALUE);
				
				com.surftools.BeanstalkClient.Job beanstalkJob = null;
				do {
					try {
						beanstalkJob = client.peekReady();
						if (beanstalkJob != null)
							client.delete(beanstalkJob.getJobId());
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} while (beanstalkJob != null);
				do {
					try {
						beanstalkJob = client.peekBuried();
						if (beanstalkJob != null)
							client.delete(beanstalkJob.getJobId());
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} while (beanstalkJob != null);
				do {
					try {
						beanstalkJob = client.peekDelayed();
						
						if (beanstalkJob != null)
							client.delete(beanstalkJob.getJobId());
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} while (beanstalkJob != null);
				
			} catch (Throwable e) {
				e.printStackTrace();
			}
			finally {
				try { client.ignore(tube); } catch (Throwable e) {}
				
			}
		}
		try { client.close(); } catch (Throwable e) {}
		client = null;
	}
	
	public int getMessageQueueSize(String queue)
	{
		ClientImpl client = null;
		int size = 0;
		// drain the message queue
		client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
				Settings.MESSAGING_SERVICE_PORT);
		try 
		{
			client.watch(queue);
			client.useTube(queue);
			client.kick(Integer.MAX_VALUE);
			
			Map<String, String> stats = client.statsTube(queue);
			String totalJobs = stats.get("current-jobs-ready");
			if (NumberUtils.isNumber(totalJobs)) {
				size = NumberUtils.toInt(totalJobs);
			} 
		} 
		catch (Throwable e) {
			Assert.fail("Failed to retrieve message queue size", e);
		}
		finally {
			try { client.ignore(queue); } catch (Throwable e) {}
			try { client.close(); } catch (Throwable e) {}
			client = null;
		}
		
		return size;
	}
	
	protected boolean isWebhookSent(String callback) throws Exception
	{
		return true;
//		File webhookLogFile = new File("/tmp/postbin.out");
//		if (!webhookLogFile.exists()) {
//			return false;
//		}
//		
//		String webhookParameters = FileUtils.readFileToString(webhookLogFile);
//		if (StringUtils.isEmpty(webhookParameters)) {
//			return false;
//		}
//		
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode json = mapper.readTree(webhookParameters);
//		
//		URI uri = new URI(callback);
//		
//		//if ()
	}
}