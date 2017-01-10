/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.SearchableAgaveResource;
import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Class to handle job listings for the authenticated user.
 * 
 * @author dooley
 * 
 */
public class JobListAttributeResource extends SearchableAgaveResource<JobSearchFilter> 
{
	private static final Logger	log	= Logger.getLogger(JobListAttributeResource.class);
	
	public static List<String> jobAttributes = new ArrayList<String>();
	
	static {
		for(Field field : Job.class.getFields()) {
			jobAttributes.add(field.getName());
		}
	}
	
	private String	attribute = null;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobListAttributeResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);

		String userAttr = (String) request.getAttributes().get("attribute");
		if (ServiceUtils.isValid(userAttr)) {
			// only add valid attributes, preserve their case for the sql query
			for(String attr: jobAttributes) {
				if (attr.toLowerCase().equals(userAttr.toLowerCase())) {
					attribute = attr;
					break;
				}
			}
		}
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobAttributeList.name(), 
				getAuthenticatedUsername(), "", request.getClientInfo().getUpstreamAddress());
	}

	/**
	 * Returns a json array of jobs matching the key value pairs.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		if (attribute == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Unknown job attribute");
		}
		
		try
		{
			//String json = "";
			
			JSONArray json = new JSONArray();
			List<Job> jobs = JobDao.getByUsername(getAuthenticatedUsername(), offset, limit, getSortOrder(AgaveResourceResultOrdering.ASC), getSortOrderSearchTerm());
			for (Job job: jobs) 
			{
				JSONObject jsonJob = new JSONObject();
				jsonJob.put("id", job.getUuid());
				jsonJob.put(attribute, job.getValueForAttributeName(attribute));
				jsonJob.put("_links",
						new JSONObject().put("self", 
								new JSONObject().put("href",
										TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())));
				json.put(jsonJob);
			}
			return new IplantSuccessRepresentation(json.toString());
		}
		catch (Exception e)
		{
			log.error("Failed to search jobs by attribute " + attribute + " for user " + getAuthenticatedUsername(), e);
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
