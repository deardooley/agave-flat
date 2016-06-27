/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.search.SystemSearchFilter;
import org.iplantc.service.systems.search.SystemSearchResult;
import org.iplantc.service.systems.util.ServiceUtils;
import org.joda.time.DateTime;
import org.restlet.Context;
import org.restlet.data.Form;
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
 * @author dooley
 * 
 */
public abstract class AbstractSystemListResource extends AgaveResource 
{
	private static final Logger	log	= Logger.getLogger(AbstractSystemListResource.class);

	protected List systems = new ArrayList();
	
	protected String username;
	protected RemoteSystemType systemType;
	protected boolean defaultsOnly = false;
	protected boolean publicOnly = false;
	protected boolean privateOnly = false;
	
	public AbstractSystemListResource(Context context, Request request, Response response) 
	{
		super(context, request, response);
		
		Form form = request.getOriginalRef().getQueryAsForm();
		if (!StringUtils.isEmpty(form.getFirstValue("type"))) 
		{
			String type = form.getFirstValue("type");
			try {
				systemType = RemoteSystemType.valueOf(type.toUpperCase());
			} catch (Exception e) {
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				response.setEntity(new IplantErrorRepresentation(
						"Invalid system type. Valid values are: " + 
						ServiceUtils.explode(", ", Arrays.asList(RemoteSystemType.values()))));
			}
		}
		
		if (!StringUtils.isEmpty(form.getFirstValue("default"))) 
		{
			defaultsOnly = Boolean.valueOf(form.getFirstValue("default"));
		}
		
		if (!StringUtils.isEmpty(form.getFirstValue("publicOnly"))) 
		{
			publicOnly = Boolean.valueOf(form.getFirstValue("publicOnly"));
		}
		
		if (!StringUtils.isEmpty(form.getFirstValue("privateOnly"))) 
		{
			privateOnly = Boolean.valueOf(form.getFirstValue("privateOnly"));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode jsonArray = mapper.createArrayNode();
		
		try
		{
			SystemManager manager = new SystemManager();
			
			RemoteSystem defaultStorageSystem = manager.getUserDefaultStorageSystem(username);
			String defaultStorageSystemId = defaultStorageSystem == null ? null : defaultStorageSystem.getSystemId();
			RemoteSystem defaultExecutionSystem = manager.getUserDefaultExecutionSystem(username);
			String defaultExecutionSystemId = defaultExecutionSystem == null ? null : defaultExecutionSystem.getSystemId();
			
			// if we have full path filters, we should include the entire Sofware
        	// object so all the fields will be present for filtering.
        	if (hasJsonPathFilters()) {
        		for (RemoteSystem system: (List<RemoteSystem>)systems) {
        			boolean isDefault = false;
					if (system.getType() == RemoteSystemType.EXECUTION) {
					    isDefault = StringUtils.equals(system.getSystemId(), defaultExecutionSystemId);
					} else if (system.getType() == RemoteSystemType.STORAGE) {
					    isDefault = StringUtils.equals(system.getSystemId(), defaultStorageSystemId);
					}
					ObjectNode json = (ObjectNode)mapper.readTree(system.toJSON());
        			json.put("default", isDefault);
        			jsonArray.add(json);
        		}
        	} 
        	// return summary response
        	else {
				for (SystemSearchResult system: (List<SystemSearchResult>)systems)
				{
					String desc = StringUtils.substring(system.getDescription(), 0, 255);
					if (StringUtils.length(system.getDescription()) > 255) {
						desc = desc.trim() + "...";
					}
					
					boolean isDefault = false;
					if (system.getType() == RemoteSystemType.EXECUTION) {
					    isDefault = StringUtils.equals(system.getSystemId(), defaultExecutionSystemId);
					} else if (system.getType() == RemoteSystemType.STORAGE) {
					    isDefault = StringUtils.equals(system.getSystemId(), defaultStorageSystemId);
					}
					
					ObjectNode json = mapper.createObjectNode()
						.put("id", system.getSystemId())
						.put("name", system.getName())
						.put("type", system.getType().name())
						.put("description", desc)
						.put("status", system.getStatus().name())
						.put("public", system.isPubliclyAvailable())
						.put("lastUpdated", new DateTime(system.getLastUpdated()).toString())
						.put("default", isDefault);
					json.putObject("_links")
					    .putObject("self")
				        		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + system.getSystemId());
					
					jsonArray.add(json);
				}
        	}
        	
			return new IplantSuccessRepresentation(jsonArray.toString());
		}
		catch (Exception e)
		{
			log.error("Failed to list systems", e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
	}
	
	/**
     * Parses url query looking for a search string
     * @return
     */
    protected Map<SearchTerm, Object> getQueryParameters() 
    {
        Form form = getRequest().getOriginalRef().getQueryAsForm();
        if (form != null && !form.isEmpty()) {
            return new SystemSearchFilter().filterCriteria(form.getValuesMap());
        } else {
            return new HashMap<SearchTerm, Object>();
        }
    }
}
