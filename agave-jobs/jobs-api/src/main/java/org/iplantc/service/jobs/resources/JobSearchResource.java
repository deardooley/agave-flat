/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.SearchableAgaveResource;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.dto.JobDTO;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.joda.time.DateTime;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to handle job listings for the authenticated user.
 * 
 * @author dooley
 * 
 */
public class JobSearchResource extends SearchableAgaveResource<JobSearchFilter> {
	
	private static final Logger	log	= Logger.getLogger(JobSearchResource.class);
	
	public static List<String> jobAttributes = new ArrayList<String>();
	
	static {
		for(Field field : Job.class.getFields()) {
			jobAttributes.add(field.getName());
		}
	}
	
	private Map<String, String> queryParameters = new HashMap<String, String>();

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobSearchResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);

		for (int i = 0; i < request.getAttributes().size(); i++)
		{
			// check that more attributes are supported
			if (!request.getAttributes().containsKey("attribute" + ( i + 1 )))
				break;

			String attribute = (String) request.getAttributes().get("attribute" + ( i + 1 ));
			if (!ServiceUtils.isValid(attribute)) {
				continue;
			}
			
			String value = (String) request.getAttributes().get("value" + ( i + 1 ));
			if (!ServiceUtils.isValid(value)) {
				continue;
			}
			
			// only add valid attributes, preserve their case for the sql query
			for(String attr: jobAttributes) {
				if (attr.toLowerCase().equals(attribute.toLowerCase())) {
					queryParameters.put(attr, value);
					break;
				}
			}
		}
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobSearch.name(), 
				getAuthenticatedUsername(), "", request.getClientInfo().getUpstreamAddress());
	}

	/**
	 * Returns a json array of jobs matching the key value pairs.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		try
		{
			List<JobDTO> jobs = JobDao.findMatching(getAuthenticatedUsername(), new JobSearchFilter().filterCriteria(queryParameters), offset, limit);
			
			ObjectMapper mapper = new ObjectMapper();
			if (hasJsonPathFilters()) {
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
		catch (Exception e)
		{
			log.error("Failed to perform search for jobs", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}

	@Override
	public JobSearchFilter getAgaveResourceSearchFilter() {
		return new JobSearchFilter();
	}

}
