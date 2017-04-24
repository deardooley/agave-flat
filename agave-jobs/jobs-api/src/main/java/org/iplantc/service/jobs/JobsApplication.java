/**
 * 
 */
package org.iplantc.service.jobs;

import org.iplantc.service.common.representation.QuartzUtilityResource;
import org.iplantc.service.common.restlet.AgaveApplication;
import org.iplantc.service.jobs.resources.JobDocumentationResource;
import org.iplantc.service.jobs.resources.JobHistoryResource;
import org.iplantc.service.jobs.resources.JobListAttributeResource;
import org.iplantc.service.jobs.resources.JobManageResource;
import org.iplantc.service.jobs.resources.JobPermissionsResource;
import org.iplantc.service.jobs.resources.JobSearchResource;
import org.iplantc.service.jobs.resources.JobStatusResource;
import org.iplantc.service.jobs.resources.JobUpdateResource;
import org.iplantc.service.jobs.resources.JobsResource;
import org.iplantc.service.jobs.resources.OutputFileDownloadResource;
import org.iplantc.service.jobs.resources.OutputFileListingResource;
import org.iplantc.service.jobs.resources.QuartzResource;
import org.restlet.Router;

/**
 * @author dooley
 * 
 */
public class JobsApplication extends AgaveApplication 
{
	@Override
	protected void mapServiceEndpoints(Router router)
	{
		// Define the resource for the static usage page
        router.attach(getStandalonePrefix() + "/usage", JobDocumentationResource.class);
        router.attach(getStandalonePrefix() + "/usage/", JobDocumentationResource.class);
        router.attach(getStandalonePrefix() + "/workers/{quartzaction}", QuartzUtilityResource.class);
        secureEndpoint(router, "/quartz", QuartzResource.class);
        secureEndpoint(router, "/quartz/", QuartzResource.class);
        
        if (!Settings.SLAVE_MODE)
		{
			// add secure job resources
			
        	// submit(POST) job and get submit/delete/update form(GET)
			secureEndpoint(router, "", JobsResource.class); 
			secureEndpoint(router, "/", JobsResource.class); 
			
			// just return that job attribute
			secureEndpoint(router, "/search/{attribute}", JobListAttributeResource.class); 
			secureEndpoint(router, "/search/{attribute}/", JobListAttributeResource.class);
			
			secureEndpoint(router,"/search/{attribute1}/{value1}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}/{attribute6}/{value6}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}/{attribute6}/{value6}/",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}/{attribute6}/{value6}/{attribute7}/{value7}",JobSearchResource.class);
			secureEndpoint(router,"/search/{attribute1}/{value1}/{attribute2}/{value2}/{attribute3}/{value3}/{attribute4}/{value4}/{attribute5}/{value5}/{attribute6}/{value6}/{attribute7}/{value7}/",JobSearchResource.class);
			
			// individual job description(GET), X update(POST), and kill(DELETE)
			secureEndpoint(router, "/{jobid}", JobManageResource.class);  
			secureEndpoint(router, "/{jobid}/", JobManageResource.class); 
			
			// just job status
			secureEndpoint(router, "/{jobid}/status", JobStatusResource.class);  
			secureEndpoint(router, "/{jobid}/status/", JobStatusResource.class);
			
			// X list (GET) output listing of user job
			secureEndpoint(router, "/{jobid}/outputs/listings", OutputFileListingResource.class); 
			secureEndpoint(router, "/{jobid}/outputs/listings/", OutputFileListingResource.class);
			
			// X list (GET) output of user job
			secureEndpoint(router, "/{jobid}/outputs/media", OutputFileDownloadResource.class); 
			secureEndpoint(router, "/{jobid}/outputs/media/", OutputFileDownloadResource.class);
			
			secureEndpoint(router, "/{jobid}/history", JobHistoryResource.class); 
			secureEndpoint(router, "/{jobid}/history/", JobHistoryResource.class);
			
			// X list (GET) shared users for this job, add (POST) shared users to the job, delete (DELETE) users from this job
			secureEndpoint(router, "/{jobid}/pems", JobPermissionsResource.class); 
			secureEndpoint(router, "/{jobid}/pems/", JobPermissionsResource.class);
			
			secureEndpoint(router, "/{jobid}/pems/{user}", JobPermissionsResource.class); 
			secureEndpoint(router, "/{jobid}/pems/{user}/", JobPermissionsResource.class);
		}

		// disabled security on trigger resource
		router.attach(getStandalonePrefix() + "/trigger/job/{jobid}/localid/{localid}/token/{token}/status/{status}", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/trigger/job/{jobid}/localid/{localid}/token/{token}/status/{status}/", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/trigger/job/{jobid}/token/{token}/status/{status}", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/trigger/job/{jobid}/token/{token}/status/{status}/", JobUpdateResource.class); // convenience resource to update job status via get
		
		router.attach(getStandalonePrefix() + "/v2/trigger/job/{jobid}/localid/{localid}/token/{token}/status/{status}", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/v2/trigger/job/{jobid}/localid/{localid}/token/{token}/status/{status}/", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/v2/trigger/job/{jobid}/token/{token}/status/{status}", JobUpdateResource.class); // convenience resource to update job status via get
		router.attach(getStandalonePrefix() + "/v2/trigger/job/{jobid}/token/{token}/status/{status}/", JobUpdateResource.class); // convenience resource to update job status via get
	}
    
    @Override
	protected String getStandalonePrefix() {
		return !isStandaloneMode() ? "" : "/jobs";
	}
    
}