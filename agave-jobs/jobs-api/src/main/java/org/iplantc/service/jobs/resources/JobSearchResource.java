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
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.search.JobSearchFilter;
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
public class JobSearchResource extends AbstractJobResource {
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

		this.username = getAuthenticatedUsername();
		
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
				username, "", request.getClientInfo().getUpstreamAddress());
	}

	/**
	 * Returns a json array of jobs matching the key value pairs.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		try
		{
			String json = "";
			
			List<Job> jobs = JobDao.findMatching(username, new JobSearchFilter().filterCriteria(queryParameters));
			for (int i=offset; i< Math.min((limit+offset), jobs.size()); i++)
			{
				json += "," + jobs.get(i).toJSON();
			}
			if (json.startsWith(","))
				json = json.substring(1);
			return new IplantSuccessRepresentation("[" + json + "]");
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

}
