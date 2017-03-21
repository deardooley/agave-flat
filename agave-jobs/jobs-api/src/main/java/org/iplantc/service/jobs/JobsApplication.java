/**
 * 
 */
package org.iplantc.service.jobs;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.restlet.AgaveApplication;
import org.iplantc.service.jobs.phases.schedulers.ArchivingScheduler;
import org.iplantc.service.jobs.phases.schedulers.MonitoringScheduler;
import org.iplantc.service.jobs.phases.schedulers.StagingScheduler;
import org.iplantc.service.jobs.phases.schedulers.SubmittingScheduler;
import org.iplantc.service.jobs.resources.JobClaimsResource;
import org.iplantc.service.jobs.resources.JobDocumentationResource;
import org.iplantc.service.jobs.resources.JobHistoryResource;
import org.iplantc.service.jobs.resources.JobInterruptsResource;
import org.iplantc.service.jobs.resources.JobLeaseResource;
import org.iplantc.service.jobs.resources.JobListAttributeResource;
import org.iplantc.service.jobs.resources.JobManageResource;
import org.iplantc.service.jobs.resources.JobPermissionsResource;
import org.iplantc.service.jobs.resources.JobPublishedResource;
import org.iplantc.service.jobs.resources.JobQueueResource;
import org.iplantc.service.jobs.resources.JobSearchResource;
import org.iplantc.service.jobs.resources.JobStatusResource;
import org.iplantc.service.jobs.resources.JobUpdateResource;
import org.iplantc.service.jobs.resources.JobsResource;
import org.iplantc.service.jobs.resources.OutputFileDownloadResource;
import org.iplantc.service.jobs.resources.OutputFileListingResource;
import org.restlet.Component;
import org.restlet.Router;
import org.restlet.service.TaskService;

/**
 * @author dooley
 * 
 */
public class JobsApplication extends AgaveApplication implements ServletContextListener 
{
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobsApplication.class);
    
    // Static links to each of the schedulers that we start.  These links are used
    // to help shutdown gracefully.  For example, if shutdown is invoked from the 
    // command line using "catalina.sh stop", then we can use a servlet context
    // listener (see web.xml) to detect when tomcat is shutting us down.  Once
    // detected, we can interrupt all core threads.
    //
    // These fields are static because a new instance of this class is dynamically 
    // created by the servlet engine when a listener method is invoked.  If the no-arg 
    // constructor of this class ever needs to do real work, then a separate listener 
    // class should be created to avoid duplicating that work.
    private static StagingScheduler    _stagingScheduler;
    private static SubmittingScheduler _submittingScheduler;
    private static MonitoringScheduler _monitoringScheduler;
    private static ArchivingScheduler  _archivingScheduler;
    
	@Override
	protected void mapServiceEndpoints(Router router)
	{
		// Define the resource for the static usage page
        router.attach(getStandalonePrefix() + "/usage", JobDocumentationResource.class);
        router.attach(getStandalonePrefix() + "/usage/", JobDocumentationResource.class);
        
        // Job queue api.
        secureEndpoint(router, "/admin/queues", JobQueueResource.class); 
        secureEndpoint(router, "/admin/queues/{queuename}", JobQueueResource.class); 
        
        // Job api that provides r/w access to internal job tracking and management data.
        secureEndpoint(router, "/admin/claims", JobClaimsResource.class);
        secureEndpoint(router, "/admin/claims/job/{jobid}", JobClaimsResource.class);
        secureEndpoint(router, "/admin/claims/worker/{workerid}", JobClaimsResource.class);
        secureEndpoint(router, "/admin/claims/container/{containerid}", JobClaimsResource.class);
        secureEndpoint(router, "/admin/claims/scheduler/{schedulerid}", JobClaimsResource.class);
        secureEndpoint(router, "/admin/published", JobPublishedResource.class);
        secureEndpoint(router, "/admin/published/{phase}", JobPublishedResource.class);
        secureEndpoint(router, "/admin/published/{phase}/{jobid}", JobPublishedResource.class);
        secureEndpoint(router, "/admin/lease", JobLeaseResource.class);
        secureEndpoint(router, "/admin/interrupts", JobInterruptsResource.class);
        secureEndpoint(router, "/admin/interrupts/{jobid}", JobInterruptsResource.class);
        
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
    
    @Override
    public synchronized void start() throws Exception {
        if (isStopped()) {
            super.start();
            
            // Use the restlet taskservice to spawn the job schedulers.
            TaskService taskService = getTaskService();
            
            // Spawn each scheduler.
            _stagingScheduler = new StagingScheduler();
            try {taskService.execute(_stagingScheduler);}
                catch (Exception e)
                {
                    String msg = "Unable to start StagingScheduler.";
                    _log.error(msg, e);
                    throw e;
                }
            _submittingScheduler = new SubmittingScheduler();
            try {taskService.execute(_submittingScheduler);}
                catch (Exception e)
                {
                    String msg = "Unable to start SubmittingScheduler.";
                    _log.error(msg, e);
                    throw e;
                }
            _monitoringScheduler = new MonitoringScheduler();
            try {taskService.execute(_monitoringScheduler);}
                catch (Exception e)
                {
                    String msg = "Unable to start MonitoringScheduler.";
                    _log.error(msg, e);
                    throw e;
                }
            _archivingScheduler = new ArchivingScheduler();
            try {taskService.execute(_archivingScheduler);}
                catch (Exception e)
                {
                    String msg = "Unable to start ArchivingScheduler.";
                    _log.error(msg, e);
                    throw e;
                }
        }
    }
    
    public static void main(String[] args) throws Exception 
	{	
		JndiSetup.init();
		
		// Create a new Component.
        Component component = new Component();

        // Attach the AppsApplication
        JobsApplication application = new JobsApplication();
        application.setStandaloneMode(true);
        component.getDefaultHost().attach(application);
        
        launchServer(component);
    }

    /* ********************************************************************** */
    /*                      ServletContextListener Methods                    */
    /* ********************************************************************** */
	@Override
	public void contextInitialized(ServletContextEvent sce) {}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
		// Announce our demise.
		String msg = "JobApplication listener received a contextDestroyed event.";
		if (sce != null) msg += " Shutting down context " + 
		                        sce.getServletContext().getServletContextName() + ".";
		_log.info(msg);
		
		// Interrupt the main scheduler threads.
		if (_stagingScheduler != null)    _stagingScheduler.interruptScheduler();
		if (_submittingScheduler != null) _submittingScheduler.interruptScheduler();
		if (_monitoringScheduler != null) _monitoringScheduler.interruptScheduler();
		if (_archivingScheduler != null)  _archivingScheduler.interruptScheduler();
		
		// Give the scheduler threads a chances to die.  We use the same timeout
		// value as used in AbstractPhaseScheduler.interruptThreads().
		_log.info("** JobApplication.contextDestroyed() sleeping for " + Settings.JOB_THREAD_DEATH_MS + " milliseconds.");
		try {Thread.sleep(Settings.JOB_THREAD_DEATH_MS);}
			catch (InterruptedException e) {}
		_log.info("** JobApplication.contextDestroyed() continuing shutdown process.");
	}
}