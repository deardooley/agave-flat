/**
 * 
 */
package org.iplantc.service.io.resources;

import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.joda.time.DateTime;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The QuartzResource serves as an endpoint to check the activity of worker
 * tasks within a specific api. 
 * 
 * @author dooley
 * 
 */
public class QuartzResource extends ServerResource {
	
	private static final Logger	log	= Logger.getLogger(QuartzResource.class);
	
	/**
	 * This method represents the HTTP GET action. A list of jobs is retrieved
	 * from the service database and serialized to a {@link org.json.JSONArray
	 * JSONArray} of {@link org.json.JSONObject JSONObject}. On error, a HTTP
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 */
	@Get
	public String represent() throws ResourceException
	{		
		if (AuthorizationHelper.isTenantAdmin(Request.getCurrent().getClientInfo().getUser().getIdentifier()))
		{
			try
			{
				Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
				
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode json = mapper.createObjectNode()
						.put("id", sched.getSchedulerInstanceId())
						.put("name", sched.getSchedulerName());
						
				ObjectNode jsonJobs = mapper.createObjectNode();
				
				Map<String, Trigger> triggerMap = new HashMap<String, Trigger>();
				
				ArrayNode allJobs = jsonJobs.putArray("available");
				List<JobExecutionContext> currentJobs = sched.getCurrentlyExecutingJobs();
				for(String group: sched.getJobGroupNames()) {
					GroupMatcher<JobKey> groupMatcher = groupEquals(group);
				    for(JobKey jobKey : sched.getJobKeys(groupMatcher)) {
				    	JobDetail jobDetail = sched.getJobDetail(jobKey);
				    	ObjectNode jsonJob = mapper.createObjectNode()
				    			.put("key", jobDetail.getKey().toString())
				    			.put("description", jobDetail.getDescription())
				    			.put("allowsConcurrentExecution", jobDetail.isConcurrentExectionDisallowed())
				    			.put("type", jobDetail.getClass().getSimpleName());
				    	allJobs.add(jsonJob);
				    }
				}
				
				ArrayNode activeJobs = jsonJobs.putArray("active");
				for (JobExecutionContext job: currentJobs) {
					ObjectNode jsonJob = mapper.createObjectNode()
						.put("key", job.getJobDetail().getKey().toString())
						.put("isActive", job.getJobRunTime() == -1)
						.put("type", job.getJobDetail().getClass().getSimpleName())
						.put("description", job.getJobDetail().getDescription())
						.put("scheduledAt", new DateTime(job.getScheduledFireTime()).toString())
						.put("firedAt", new DateTime(job.getFireTime()).toString())
						.put("nextFireAt", new DateTime(job.getNextFireTime()).toString())
						.put("previousFiredAt", new DateTime(job.getPreviousFireTime()).toString())
						.put("refireCount", job.getRefireCount())
						.put("isRecovering", job.isRecovering())
						.put("lastFireDuration", job.getJobRunTime())
						.put("allowsConcurrentExecution", job.getJobDetail().isConcurrentExectionDisallowed())
						.put("result", job.getResult() == null ? null : job.getResult().toString())
						.put("triggerKey", job.getTrigger().getKey().toString());
					
					if (!triggerMap.containsKey(job.getTrigger().getKey().toString())) {
						triggerMap.put(job.getTrigger().getKey().toString(), job.getTrigger());
					}
					
					activeJobs.add(jsonJob);
				}
				
				json.put("jobs", jsonJobs);
				
				ArrayNode triggers = json.putArray("triggers");
				for(String group: sched.getTriggerGroupNames()) {
				    // enumerate each trigger in group
					GroupMatcher<TriggerKey> groupMatcher = groupEquals(group);
				    for(TriggerKey triggerKey : sched.getTriggerKeys(groupMatcher)) {
				    	Trigger trigger = sched.getTrigger(triggerKey);
				    	ObjectNode jsonTrigger = mapper.createObjectNode()
				    			.put("key",  triggerKey.toString())
								.put("description", trigger.getDescription())
								.put("nextFireAt", new DateTime(trigger.getNextFireTime()).toString())
								.put("previousFiredAt", new DateTime(trigger.getPreviousFireTime()).toString())
								.put("finalFireAt", new DateTime(trigger.getEndTime()).toString());
				    	
						triggers.add(jsonTrigger);
				    }
				}
				
				return json.toString();
			}
			catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			}
		}
		else {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"User does not have permission to view this resource");
		}
	}
}
