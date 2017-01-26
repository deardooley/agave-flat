/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.SearchableAgaveResource;
import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.search.JobEventSearchFilter;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferSummary;
import org.joda.time.DateTime;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles search and listings for {@link JobEvent} resources.
 * 
 * @author dooley
 * 
 */
public class JobHistoryResource extends SearchableAgaveResource<JobEventSearchFilter>
{
	private String				sJobId;
	private Job					job;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobHistoryResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.sJobId = (String) request.getAttributes().get("jobid");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/**
	 * Returns a collection of JobEvent objects representing the history
	 * of events for this job.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobsGetHistory.name(), 
				getAuthenticatedUsername(), "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Job id cannot be empty");
		}

		try
		{
			job = JobDao.getByUuid(sJobId, true);
			if (job == null || !job.isVisible()) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						"No job found with job id " + sJobId);
			}
			else if (new JobPermissionManager(job, getAuthenticatedUsername()).canRead(getAuthenticatedUsername()))
			{
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode history = mapper.createArrayNode();
				
				Map<SearchTerm, Object> queryParameters = getQueryParameters();

				List<JobEvent> events = null;
				if (queryParameters.isEmpty()) {
					events = JobEventDao.getByJobId(job.getId(), limit, offset, getSortOrder(AgaveResourceResultOrdering.ASC), getSortOrderSearchTerm());
				}
				else {
					events = JobEventDao.findMatching(job.getId(), queryParameters, offset, limit, getSortOrder(AgaveResourceResultOrdering.ASC), getSortOrderSearchTerm());
				}
				
				for(JobEvent event: events)
				{
            		ObjectNode jsonEvent = mapper.createObjectNode();
            		
//            		ObjectNode jsonLinks = mapper.createObjectNode();
//            		jsonLinks.putObject("self")
//                        .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid() + "/events/" + event.getUUid());
//            		jsonLinks.putObject("job")
//            		    .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid());
//            
    				if (event.getTransferTask() != null) 
					{
//    				    ObjectNode jsonLinks = mapper.createObjectNode();
//    				    jsonLinks.putObject("transferTask")
//                            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TRANSFER_SERVICE) + event.getTransferTask().getUuid());
//            
						ObjectNode jsonTransferTask = mapper.createObjectNode();
						
						try {
							TransferSummary summary = TransferTaskDao.getTransferSummary(event.getTransferTask());
							
							jsonTransferTask
								.put("uuid", event.getTransferTask().getUuid())
							    .put("source", event.getTransferTask().getSource())
								.put("totalActiveTransfers",  summary.getTotalActiveTransfers())
								.put("totalFiles", summary.getTotalTransfers())
								.put("totalBytesTransferred", summary.getTotalTransferredBytes().longValue())
								.put("totalBytes", summary.getTotalBytes().longValue())
								.put("averageRate", summary.getAverageTransferRate());
							
							jsonEvent.set("progress", jsonTransferTask);
						} 
						catch (TransferException e) {
							jsonEvent.put("progress", (String) null);
						}
					}
					jsonEvent
						.put("status", event.getStatus())
						.put("created", new DateTime(event.getCreated()).toString())
						.put("createdBy", event.getCreatedBy())
	        			.put("description", event.getDescription());
//					    .put("_links", jsonLinks);
					
					history.add(jsonEvent);
				}
				return new IplantSuccessRepresentation(history.toString());
			}
			else
			{
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				return new IplantErrorRepresentation(
						"User does not have permission to view this job history");
			}
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (JobException e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid job id "
					+ e.getMessage());
		}
		catch (Exception e)
		{
			// can't set a stopped job back to running. Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}

	}

	@Override public boolean allowDelete() { return false; }
	@Override public boolean allowGet() { return true; }
	@Override public boolean allowPut() { return false; }
	@Override public boolean allowPost() { return false; }

	@Override
	public JobEventSearchFilter getAgaveResourceSearchFilter() {
		return new JobEventSearchFilter();
	}
}
