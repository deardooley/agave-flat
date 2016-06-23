package org.iplantc.service.jobs.resources;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.FileBean;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.DataLocator;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Class to handle get and post requests for
 * {@link org.iplantc.service.jobs.model.Job Jobs}
 * 
 * @author dooley
 * 
 */
public class OutputFileListingResource extends AbstractJobResource 
{
	private String				sJobId;
	private String				path;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public OutputFileListingResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		sJobId = (String) request.getAttributes().get("jobid");

		path = getFilePathFromURL();
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobsListOutputs.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
	}

	/**
	 * This method represents the HTTP GET action. Using the job id from the
	 * URL, the output file information for the specified job are retrieved from
	 * the submit machine and sent to the user as a {@link org.json.JSONArray
	 * JSONArray} of {@link org.json.JSONObject JSONObjects}. If the job id is
	 * invalid for any reason, a HTTP
	 * {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is
	 * sent.
	 * <p>
	 * The format for the file listing is:
	 * <p>
	 * [{<br>
	 * &nbsp "directory":false,<br>
	 * &nbsp "readable":true,<br>
	 * &nbsp "writable":true,<br>
	 * &nbsp "owner":"dooley",<br>
	 * &nbsp "length":688,<br>
	 * &nbsp "lastModified":"Mon Mar 01 15:20:40 CST 2010",<br>
	 * &nbsp "path":"/dooley/1/geospiza.traits.fel",<br>
	 * &nbsp "url":"https://foundation.iplantc.org/io-v1/dooley/archive/job-1928719834asdf/geospiza.traits.fel"
	 * ,<br>
	 * &nbsp "name":"geospiza.traits.fel",<br>
	 * &nbsp "parent":"file:/home/0004/iplant/contrast/dooley/1/"<br>
	 * } }]<br>
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{

		//Long jobId = null;

		if (StringUtils.isEmpty(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Job id cannot be empty");
		}
		

		Job job;
		try
		{
			job = JobDao.getByUuid(sJobId, true);

			if (job == null || !job.isVisible()) 
			{ 
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Not job found with job id " + sJobId); 
			}

			JobPermissionManager pemManager = new JobPermissionManager(job, username);
			if (!pemManager.canRead(username)) { throw new ResourceException(
					Status.CLIENT_ERROR_UNAUTHORIZED,
					"User does not have permission to view this resource."); }

			DataLocator locator = new DataLocator(job);

			List<FileBean> listing = locator.listOutputDirectory(path);
			
			StringBuilder builder = new StringBuilder();

			for (int i=offset; i< Math.min((limit+offset), listing.size()); i++)
			{
				FileBean bean = listing.get(i);
			
				if (bean.getName().equals(".") || bean.getName().equals("..")) {
					continue;
				} else {
					builder.append(",");
					builder.append(bean.toJSON());
				}
			}
			if (builder.length() > 0)
				builder.deleteCharAt(0);

			return new IplantSuccessRepresentation("[" + builder.toString()
					+ "]");

		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}

	}
	
	private String getFilePathFromURL()
	{
		String path = getRequest().getOriginalRef().toUri().getPath();
		
		path = path.substring(path.indexOf("listings")+8);
		
		path = "/" + path;
		
		path = path.replaceAll("\\.\\.", "").replaceAll("//", "/").replaceAll("~","");
		
		return path;
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
