/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;


/**
 * @author dooley
 *
 */
public abstract class AbstractEntityEventCollection<T extends AgaveEntityEvent,V extends Enum<?>> extends AbstractEntityEventResource<T,V> {

	private static final Logger log = Logger.getLogger(AbstractEntityEventCollection.class);
    
    /**
     * 
     */
    public AbstractEntityEventCollection() {}
    
    public Response getEntityEvents(String entityId) {
        
    	logUsage(AgaveLogServiceClient.ActivityKeys.MonitorHistoryList);
        
        try 
        {
            getEntityFromPathValue(entityId);
            
            List<T> events = getEntityEventDao().getEntityEventByEntityUuid(entityId, getLimit(), getOffset());
            
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode history = mapper.createArrayNode();
            for(T event: events) {
                history.add(mapper.valueToTree(event));
            }
            
            return Response.ok(new AgaveSuccessRepresentation(history.toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (EntityEventPersistenceException e) {
        	log.error("Failed to query history for resource " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"Failed to retrieve resource history. If this persists, "
            				+ "please contact your system administrator", e);
        }
        catch (Exception e) {
        	log.error("Failed to query history for resource " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"Failed to retrieve resource history. If this persists, "
                    		+ "please contact your system administrator", e);
        }
    }
    
//    /**
//     * Parses url query looking for a search string
//     * @return
//     */
//    protected Map<SearchTerm, Object> getQueryParameters() 
//    {
//        Request currentRequest = Request.getCurrent();
//        Reference ref = currentRequest.getOriginalRef();
//        Form form = ref.getQueryAsForm();
//        if (form != null && !form.isEmpty()) {
//            return new MonitorFilter().filterCriteria(form.getValuesMap());
//        } else {
//            return new HashMap<SearchTerm, Object>();
//        }
//    }

}
