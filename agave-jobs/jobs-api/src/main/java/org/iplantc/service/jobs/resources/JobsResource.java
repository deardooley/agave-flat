/**
 *
 */
package org.iplantc.service.jobs.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.managers.JobRequestProcessor;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.dto.JobDTO;
import org.iplantc.service.jobs.model.dto.JobDTOSummaryFilter;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.joda.time.DateTime;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * The JobResource object enables HTTP GET and POST actions on contrast jobs.
 * This resource is primarily used for submitting jobs and viewing a sample HTML
 * job submission form.
 *
 * @author dooley
 *
 */
public class JobsResource extends AbstractJobResource {
	private static final Logger	log	= Logger.getLogger(JobsResource.class);

	private String internalUsername;

//	private List<String> jobAttributes = new ArrayList<String>();
//
//	static {
//		for(Field field : Job.class.getFields()) {
//			jobAttributes.add(field.getName());
//		}
//	}
//
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobsResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();

		internalUsername = (String) context.getAttributes().get("internalUsername");

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/**
	 * This method represents the HTTP GET action. A list of jobs is retrieved
	 * from the service database and serialized to a {@link org.json.JSONArray
	 * JSONArray} of {@link org.json.JSONObject JSONObject}. On error, a HTTP
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(),
				AgaveLogServiceClient.ActivityKeys.JobsList.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		try
		{
			Map<SearchTerm, Object> queryParameters = getQueryParameters();

			if (queryParameters.isEmpty()) {
				List<Job> jobs = JobDao.getByUsername(username, offset, limit);
				if (hasJsonPathFilters()) {
					ObjectMapper mapper = new ObjectMapper();
					ArrayNode json = mapper.createArrayNode();
				
					for(Job job: jobs)
					{
						json.add(mapper.readTree(job.toJSON()));
					}
					
					return new IplantSuccessRepresentation(json.toString());
				}
				else {
					JSONWriter writer = new JSONStringer();
					writer.array();
		
					for(Job job: jobs)
					{
		//				Job job = jobs.get(i);
						writer.object()
							.key("id").value(job.getUuid())
							.key("name").value(job.getName())
							.key("owner").value(job.getOwner())
							.key("executionSystem").value(job.getSystem())
							.key("appId").value(job.getSoftwareName())
							.key("created").value(new DateTime(job.getCreated()).toString())
							.key("status").value(job.getStatus())
							.key("startTime").value(job.getStartTime() == null ? null : new DateTime(job.getStartTime()).toString())
							.key("endTime").value(job.getEndTime() == null ? null : new DateTime(job.getEndTime()).toString())
							.key("_links").object()
					        	.key("self").object()
					        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())
						        .endObject()
						        .key("archiveData").object()
				        			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(job.getArchiveUrl()))
						        .endObject()
					       .endObject()
				        .endObject();
					}
		
					writer.endArray();
		
					return new IplantSuccessRepresentation(writer.toString());
				}
			} 
			else {
				List<JobDTO> jobs = JobDao.findMatching(username, queryParameters, offset, limit);
				ObjectMapper mapper = new ObjectMapper();
				if (hasJsonPathFilters()) {
//					return new IplantSuccessRepresentation(mapper.valueToTree(jobs));
					return new IplantSuccessRepresentation(mapper.writeValueAsString(jobs));
				}
				else {
					JSONWriter writer = new JSONStringer();
					writer.array();
					
					for(JobDTO job: jobs)
					{
		//				Job job = jobs.get(i);
						writer.object()
							.key("id").value(job.getUuid())
							.key("name").value(job.getName())
							.key("owner").value(job.getOwner())
							.key("executionSystem").value(job.getExecution_system())
							.key("appId").value(job.getSoftware_name())
							.key("created").value(new DateTime(job.getCreated()).toString())
							.key("status").value(job.getStatus())
							.key("startTime").value(job.getStart_time() == null ? null : new DateTime(job.getStart_time()).toString())
							.key("endTime").value(job.getEnd_time() == null ? null : new DateTime(job.getEnd_time()).toString())
							.key("_links").object()
					        	.key("self").object()
					        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())
						        .endObject()
						        .key("archiveData").object()
				        			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(job.getArchiveUrl()))
						        .endObject()
					       .endObject()
				        .endObject();
					}
		
					writer.endArray();
					return new IplantSuccessRepresentation(writer.toString());
				}
			}	
		}
		catch (HibernateException e) {
				log.error("Failed to fetch job listings from db.", e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						return new IplantErrorRepresentation("Unable to fetch job records.");
		}
		catch (Exception e)
		{
			log.error("Failed to fetch job listings from db.", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
	}

	/**
	 * This method represents the HTTP POST action. Posting a job submission
	 * form to this service will submit a contrast job on behalf of the
	 * authenticated user. While this method does not return a value internally,
	 * a {@link org.json.JSONObject JSONObject} representation of the
	 * successfully submitted job is written to the output stream. If the job
	 * fails due to an internal error, a
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} HTTP code will
	 * be sent. If the job fails due to a user error, a link
	 * {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} HTTP code
	 * will be sent.
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(),
				AgaveLogServiceClient.ActivityKeys.JobsSubmit.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		try
		{
			JsonNode json = super.getPostedEntityAsObjectNode(true);
//			Job job = JobManager.processJob(json, username, internalUsername);
			JobRequestProcessor processor = new JobRequestProcessor(username, internalUsername);
			Job job = processor.processJob(json);
			
			// append any bundled notifications to the hypermedia response 
			getResponse().setStatus(Status.SUCCESS_CREATED);
			getResponse().setEntity(new IplantSuccessRepresentation(
					job.toJsonWithNotifications(
							processor.getNotificationProcessor().getNotifications())));
		}
		catch (JobProcessingException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.valueOf(e.getStatus()));
		}
    	catch (ResourceException e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
			log.error("Job submission failed for user " + username, e);
		}
		catch (Exception e) {
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to submit job: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			log.error("Job submission failed for user " + username, e);
		}
	}

	@Override
	public boolean allowDelete()
	{
		return false;
	}

	@Override
	public boolean allowGet()
	{
		return true;
	}

	@Override
	public boolean allowPost()
	{
		return true;
	}

	@Override
	public boolean allowPut()
	{
		return false;
	}

	/**
	 * Parses url query looking for a search string
	 * @return
	 */
	private Map<SearchTerm, Object> getQueryParameters()
	{
		Form form = getRequest().getOriginalRef().getQueryAsForm();
		if (form != null && !form.isEmpty()) {
			return new JobSearchFilter().filterCriteria(form.getValuesMap());
		} else {
			return new HashMap<SearchTerm, Object>();
		}
	}


}
