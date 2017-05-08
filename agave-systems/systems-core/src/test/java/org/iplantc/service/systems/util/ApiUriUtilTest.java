package org.iplantc.service.systems.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class ApiUriUtilTest
{
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/storage/";

	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_OWNER_SHARED = "shareduser";

	RemoteSystem publicSystem = null;
	RemoteSystem privateSystem = null;

	@BeforeMethod
	public void beforeMethod()
	{}

	@AfterMethod
	public void afterMethod()
	{}

	@BeforeClass
	public void beforeClass() throws IOException, SystemArgumentException, JSONException
	{
		SystemDao systemDao = new SystemDao();
		String sJson = FileUtils.readFileToString(new File(STORAGE_SYSTEM_TEMPLATE_DIR + "irods.example.com.json"));
		publicSystem = StorageSystem.fromJSON(new JSONObject(sJson));
		publicSystem.setOwner(SYSTEM_OWNER);
		publicSystem.setGlobalDefault(true);
		publicSystem.setPubliclyAvailable(true);
		systemDao.persist(publicSystem);

		sJson = FileUtils.readFileToString(new File(STORAGE_SYSTEM_TEMPLATE_DIR + "irods.example.com.json"));
		privateSystem = StorageSystem.fromJSON(new JSONObject(sJson));
		privateSystem.setSystemId("my." + privateSystem.getSystemId());
		privateSystem.setOwner(SYSTEM_OWNER);
		privateSystem.addUserUsingAsDefault(SYSTEM_OWNER);
		systemDao.persist(privateSystem);
		
//		SystemRoleManager roleManager = new SystemRoleManager();
//		roleManager.setRole(username, type);
	}

	@AfterClass
	public void afterClass()
	{
		SystemDao systemDao = new SystemDao();
		for (RemoteSystem system: systemDao.getAll()) {
			systemDao.remove(system);
		}
	}

	@DataProvider(name="getPathProvider")
	private Object[][] getPathProvider()
	{
	    String jobUuid = new AgaveUUID(UUIDType.JOB).toString();
	    
		return new Object[][] {
			{	"/this/is/an/absolute/path", "/this/is/an/absolute/path", false, "Failed to find path from absolute path" },
			{   "this/is/a/relative/path", "this/is/a/relative/path", false, "Failed to find path from relative path" },
			{   "http://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "http uri should throw exception" },
			{   "http://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "http uri should throw exception" },
			{   "agave://example.com/this/is/a/relative/path", "this/is/a/relative/path", false, "Failed to find path from relative agave uri" },
			{   "agave://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", false, "Failed to find path from absolute agave uri" },
			{   "ftp://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "ftp uri should throw exception" },
			{   "ftp://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "ftp uri should throw exception" },
			{   "sftp://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "sftp uri should throw exception" },
			{   "sftp://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "sftp uri should throw exception" },
			{   "https://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "https uri should throw exception" },
			{   "https://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "https uri should throw exception" },
			{   "gridftp://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "gridftp uri should throw exception" },
			{   "gridftp://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "gridftp uri should throw exception" },
			{   "irods://example.com/this/is/a/relative/path", "this/is/a/relative/path", true, "irods uri should throw exception" },
			{   "irods://example.com//this/is/an/absolute/path", "/this/is/an/absolute/path", true, "irods uri should throw exception" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", "this/is/a/relative/path", false, "Failed to relative path from files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + privateSystem.getSystemId() + "//this/is/an/absolute/path", "/this/is/an/absolute/path", false, "Failed to find absolute path from files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/this/is/a/relative/path", "this/is/a/relative/path", false, "Failed to find relative path from default files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media//this/is/an/absolute/path", "/this/is/an/absolute/path", false, "Failed to find absolute path from default files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", null, true, "Bad internal api uri should throw exception" },
			{   Settings.IPLANT_IO_SERVICE + "system/" + privateSystem.getSystemId() + "/this/is/a/relative/path", null, true, "Bad internal api uri should throw exception" },
			
			{   Settings.IPLANT_JOB_SERVICE + "//outputs/media/this/is/a/relative/path", null, true, "Missing job uuid should throw exception" },
            {   Settings.IPLANT_JOB_SERVICE + "/this/is/a/relative/path", null, true, "Invalid job uuid should throw exception" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "//this/is/a/relative/path", null, true, "Invalid job url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/this/is/a/relative/path", null, true, "Invalid job url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings/", null, true, "relative job output root dir listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings//", null, true, "absolute job output root dir listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings/this/is/a/relative/path", null, true, "relative job listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings//this/is/an/absolute/path", null, true, "absolute job listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/pems/", null, true, "Job permission listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/pems/testuser", null, true, "Job user permission url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/history", null, true, "Job history listing url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/history/1234234523451234-1234123-0001-028", null, true, "Job history event url should throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/", "", false, "Job output root folder should not throw exception" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//", "/", false, "Job output root folder should not throw exception" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path", "this/is/a/relative/path", false, "Job output relative path should not throw exception" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path", "/this/is/an/absolute/path", false, "Job output absolute path should not throw exception" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/?foo=bar", "", false, "Job output root folder with query argument should not show up in path" },
			{   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//?foo=bar", "/", false, "Job output root folder with query argument should not show up in path" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path?foo=bar", "this/is/a/relative/path", false, "Job output relative path should not show up in path" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path/?foo=bar", "this/is/a/relative/path/", false, "Job output relative path should not show up in path" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path?foo=bar", "/this/is/an/absolute/path", false, "Job output absolute path should not show up in path" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path/?foo=bar", "/this/is/an/absolute/path/", false, "Job output absolute path should not show up in path" },
            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/fastx_out/WT_rep1_1-fx2143.fastq", "fastx_out/WT_rep1_1-fx2143.fastq", false, "relative path with underscore and dashes should show up in path" },
            {   "https://agave.iplantc.org/jobs/v2/1633830433121627675-e0bd34dffff8de6-0001-007/outputs/media/fastx_out/WT_rep1_1-fx2143.fastq", "fastx_out/WT_rep1_1-fx2143.fastq", false, "relative path with underscore and dashes should show up in path" },
            
            
		};
	}

	@Test(dataProvider="getPathProvider")
	public void getPath(String sUri, String expectedPath, boolean shouldThrowException, String message)
	throws IOException, URISyntaxException
	{
		URI uri = new URI(sUri);
		try {
			String discoveredPath = ApiUriUtil.getPath(uri);
			Assert.assertEquals(discoveredPath, expectedPath, message);
			Assert.assertFalse(shouldThrowException, message);
		} catch (Exception e) {
			Assert.assertTrue(shouldThrowException, message);
		}
	}

	@DataProvider(name="getRemoteSystemProvider")
	private Object[][] getRemoteSystemProvider()
	{
	    String jobUuid = new AgaveUUID(UUIDType.JOB).toString();
        
        return new Object[][] {
			{   "http://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative http uri" },
			{   "http://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute http uri" },
			{   "ftp://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative ftp uri" },
			{   "ftp://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute ftp uri" },
			{   "sftp://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative sftp uri" },
			{   "sftp://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute sftp uri" },
			{   "https://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative https uri" },
			{   "https://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute https uri" },
			{   "gridftp://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative gridftp uri" },
			{   "gridftp://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute gridftp uri" },
			{   "irods://example.com/this/is/a/relative/path", SYSTEM_OWNER, null, true, "Failed to find path from relative irods uri" },
			{   "irods://example.com//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Failed to find path from absolute irods uri" },

			{	"/this/is/an/absolute/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find owner default system given an absolute path" },
			{   "this/is/a/relative/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find owner default system given a relative path" },
			{	"/this/is/an/absolute/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public default system given an absolute path" },
			{   "this/is/a/relative/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public default system given a relative path" },

			{   "agave://" + publicSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER, publicSystem.getSystemId(), false, "Failed to find public system from agave uri" },
			{   "agave://" + privateSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find owner private system from agave uri" },
			{   "agave://" + publicSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public system from agave uri" },
			{   "agave://" + privateSystem.getSystemId() + "//this/is/an/absolute/path", SYSTEM_OWNER_SHARED, null, true, "Agave URI with system the user does not have access to should throw exception" },
			{   "agave://" + StringUtils.reverse(publicSystem.getSystemId()) + "//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Valid system returned when given an invalid system id" },
			{   "agave://" + StringUtils.reverse(publicSystem.getSystemId()) + "//this/is/an/absolute/path", SYSTEM_OWNER_SHARED, null, true, "Valid system returned when given an invalid system id" },
			{   "agave:////this/is/an/absolute/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find default storage sytem from agave uri with empty host" },
			{   "agave:///this/is/an/absolute/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find default storage sytem from agave uri with empty host" },
			{   "agave:////this/is/an/absolute/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public system from agave uri with empty host" },
			{   "agave:///this/is/an/absolute/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public system from agave uri with empty host" },

			{   Settings.IPLANT_IO_SERVICE + "media/system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER, publicSystem.getSystemId(), false, "Failed to find public system from files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + privateSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER, privateSystem.getSystemId(), false, "Failed to find owner private system from files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", SYSTEM_OWNER_SHARED, publicSystem.getSystemId(), false, "Failed to find public system from files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + privateSystem.getSystemId() + "//this/is/an/absolute/path", SYSTEM_OWNER_SHARED, null, true, "Valid system returned for user without access to the files service uri" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + StringUtils.reverse(publicSystem.getSystemId()) + "//this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Invalid system should throw a SystemException" },
			{   Settings.IPLANT_IO_SERVICE + "media/system/" + StringUtils.reverse(publicSystem.getSystemId()) + "//this/is/an/absolute/path", SYSTEM_OWNER_SHARED, null, true, "Invalid system should throw a SystemException" },

			{   Settings.IPLANT_IO_SERVICE + "media/system///this/is/an/absolute/path", SYSTEM_OWNER, null, true, "Custom internal file service uri with an empty system id should throw an exception" },
			{   Settings.IPLANT_IO_SERVICE + "media/system//this/is/an/absolute/path", SYSTEM_OWNER_SHARED, null, true, "Custom internal file service uri with an empty system id should throw an exception" },
			
		};
	}

	@Test(dataProvider="getRemoteSystemProvider")
	public void getRemoteSystem(String sUri, String owner, String expectedSystemId, boolean shouldThrowException, String message)
	throws IOException, URISyntaxException
	{
		URI uri = new URI(sUri);
		try
		{
			RemoteSystem discoveredSystem = ApiUriUtil.getRemoteSystem(owner, uri);

			if (expectedSystemId != null) {
				Assert.assertNotNull(discoveredSystem, "No system returned");
				Assert.assertEquals(discoveredSystem.getSystemId(), expectedSystemId, message);
			}
			Assert.assertFalse(shouldThrowException, message);
		} catch(Exception e) {
			Assert.assertTrue(shouldThrowException, message);
		}
	}

	@DataProvider(name="isInternalURIProvider")
	private Object[][] isInternalURIProvider()
	{
	    String jobUuid = new AgaveUUID(UUIDType.JOB).toString();
        
		return new Object[][] {
				{	"/this/is/an/absolute/path", true, "Absolute path should be recognized as an internal uri" },
				{   "this/is/a/relative/path", true, "Relative path should be recognized as an internal uri" },
				{   "http://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "http://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "ftp://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "ftp://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "sftp://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "sftp://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "https://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "https://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "gridftp://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "gridftp://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "irods://example.com/this/is/a/relative/path", false, "IRODS uri should not be identified as an internal uri" },
				{   "irods://example.com//this/is/an/absolute/path", false, "IRODS uri should not be identified as an internal uri" },

				{   "agave://example.com/this/is/a/relative/path", true, "Failed to recognize agave uri regardless of validity of the system id" },
				{   "agave://example.com//this/is/an/absolute/path", true, "Failed to recognize agave uri regardless of validity of the system id" },
				{   "agave:///this/is/a/relative/path", true, "Failed to recognize agave uri with empty system id" },
				{   "agave:////this/is/an/absolute/path", true, "Failed to recognize agave uri with empty system id" },
				{   "agave://" + publicSystem.getSystemId() + "//this/is/an/absolute/path", true, "Failed to recognize agave uri with valid public system id" },
				{   "agave://" + publicSystem.getSystemId() + "/this/is/a/relative/path", true, "Failed to recognize agave uri with valid public system id" },
				{   "agave://" + privateSystem.getSystemId() + "//this/is/an/absolute/path", true, "Failed to recognize agave uri with valid private system id" },
				{   "agave://" + privateSystem.getSystemId() + "/this/is/a/relative/path", true, "Failed to recognize agave uri with valid private system id" },


				{   Settings.IPLANT_IO_SERVICE + "system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", false, "Files API url without the media/ token in their path should fail regardless of whether a public system is explicitly given" },
				{   Settings.IPLANT_IO_SERVICE + "system/" + privateSystem.getSystemId() + "/this/is/a/relative/path", false, "Files API url without the media/ token in their path should fail regardless of whether a private system is explicitly given" },
				{   Settings.IPLANT_IO_SERVICE + "system//this/is/a/relative/path", false, "Files API url without the media/ token in their path should fail regardless of whether they have an empty system in relative files service uri" },
				{   Settings.IPLANT_IO_SERVICE + "system///this/is/an/absolute/path", false, "Files API url without the media/ token in their path should fail regardless of whether they have an empty system in absolute files service uri" },
				{   Settings.IPLANT_IO_SERVICE + "this/is/a/relative/path", false, "Files API url without the media/ token in their path should fail regardless of whether they use the default relative path structure in files service uri" },
				{   Settings.IPLANT_IO_SERVICE + "/this/is/an/absolute/path", false, "Files API url without the media/ token in their path should fail regardless of whether they use the default absolute path structure in files service uri" },

				{   Settings.IPLANT_IO_SERVICE + "media/system/" + publicSystem.getSystemId() + "/this/is/a/relative/path", true, "Files API url using explicity system ids and a relative path should resolve as internal URL" },
				{   Settings.IPLANT_IO_SERVICE + "media/system/" + privateSystem.getSystemId() + "//this/is/an/absolute/path", true, "Files API url using explicity system ids and an absolute path should resolve as internal URL" },
				{   Settings.IPLANT_IO_SERVICE + "media/system//this/is/a/relative/path", false, "Files API url with an empty system id in their path should fail regardless of whether they have an empty system in relative files service uri" },
				{   Settings.IPLANT_IO_SERVICE + "media/system///this/is/an/absolute/path", false, "Files API url with an empty system id in their path should fail regardless of whether they have an empty system in absolute files service uri" },
				{   Settings.IPLANT_IO_SERVICE + "media/this/is/a/relative/path", true, "Files API url using the default system and default relative path structure in files service uri should resolve as internal" },
				{   Settings.IPLANT_IO_SERVICE + "media//this/is/an/absolute/path", true, "Files API url using the default system and default absolute path structure in files service uri should resolve as internal" },
				
				{   Settings.IPLANT_JOB_SERVICE + "//outputs/media/this/is/a/relative/path", false, "Missing job uuid should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + "/this/is/a/relative/path", false, "Invalid job uuid should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "//this/is/a/relative/path", false, "Invalid job url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/this/is/a/relative/path", false, "Invalid job url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings/", false, "relative job output root dir listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings//", false, "absolute job output root dir listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings/this/is/a/relative/path", false, "relative job listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/listings//this/is/an/absolute/path", false, "absolute job listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/pems/", false, "Job permission listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/pems/testuser", false, "Job user permission url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/history", false, "Job history listing url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/history/1234234523451234-1234123-0001-028", false, "Job history event url should throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/", true, "Job output root folder should not throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//", true, "Job output root folder should not throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path", true, "Job output relative path should not throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path", true, "Job output absolute path should not throw exception" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/?foo=bar", true, "Job output root folder with query argument should not show up in path" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//?foo=bar", true, "Job output root folder with query argument should not show up in path" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path?foo=bar", true, "Job output relative path should not show up in path" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media/this/is/a/relative/path/?foo=bar", true, "Job output relative path should not show up in path" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path?foo=bar", true, "Job output absolute path should not show up in path" },
	            {   Settings.IPLANT_JOB_SERVICE + jobUuid + "/outputs/media//this/is/an/absolute/path/?foo=bar", true, "Job output absolute path should not show up in path" },
		};
	}

	@Test(dataProvider="isInternalURIProvider")
	public void isInternalURI(String sUri, boolean expected, String message)
	throws URISyntaxException
	{
		URI uri = new URI(sUri);
		Assert.assertEquals(ApiUriUtil.isInternalURI(uri), expected, message);
	}
}
