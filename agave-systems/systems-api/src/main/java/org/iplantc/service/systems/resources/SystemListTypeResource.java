/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.search.SystemSearchFilter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * @author dooley
 * 
 */
public class SystemListTypeResource extends AbstractSystemListResource {
    
    private static final Logger log = Logger.getLogger(SystemListTypeResource.class);
    
	private SystemDao dao = null;
	private String systemType;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SystemListTypeResource(Context context, Request request,
			Response response)
	{

		super(context, request, response);

		dao = new SystemDao();
		
		this.systemType = (String) request.getAttributes().get("type");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemsListType.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
	}
	
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		try 
		{
			Map<String, String> queryParameters = new HashMap<String, String>();
	        queryParameters.put("type.eq", this.systemType);
	        SystemSearchFilter filter = new SystemSearchFilter();
	        Map<SearchTerm, Object> searchCriteria = filter.filterCriteria(queryParameters);
	        
	        systems = dao.findMatching(getAuthenticatedUsername(), searchCriteria, limit, offset, hasJsonPathFilters());
		
		} 
		catch (IllegalArgumentException e) {
		    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
		} 
		catch (Throwable e) {
		    log.error(e);
		    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
		            "Unable to complete system search due to unexpected error", e);
		}
		
		return super.represent(variant);
	}
}
