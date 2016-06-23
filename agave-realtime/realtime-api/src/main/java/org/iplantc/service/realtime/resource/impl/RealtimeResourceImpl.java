package org.iplantc.service.realtime.resource.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.realtime.dao.RealtimeChannelDao;
import org.iplantc.service.realtime.managers.RealtimeChannelEventProcessor;
import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelEventType;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelProtocolType;
import org.iplantc.service.realtime.representation.WebsocketSubscription;
import org.iplantc.service.realtime.resource.RealtimeResource;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class RealtimeResourceImpl extends AbstractServerResource implements RealtimeResource 
{
	private static final Logger log = Logger.getLogger(RealtimeResourceImpl.class);
	
	// Define allowed roles for the method "get".
	private static final String[] get3AllowedGroups = new String[] { "user" };
	// Define denied roles for the method "get".
	private static final String[] get3DeniedGroups = new String[] {};

	public javax.ws.rs.core.Response represent() throws Exception {
		checkGroups(get3AllowedGroups, get3DeniedGroups);
		RealtimeChannel realtimeChannel = null;
		try {

			// Path variables
			String channelId = Reference.decode(getAttribute("channelId"));
			
			RealtimeChannelDao dao = new RealtimeChannelDao();
			realtimeChannel = dao.findByUuidWithinSessionTenant(channelId);
			
			if (realtimeChannel == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"Unknown channel id");
			}
			else {
				setResponseHeadersForClientProtocol(realtimeChannel.getProtocol());
	        	
	        	log.info(String.format("New realtime channel created %s/%s/%s",
	    				TenancyHelper.getCurrentTenantId(),
	    				getClientInfo().getUser().getIdentifier(),
	    				realtimeChannel.getUuid()));
	        	
	        	// process the event and send notifications to queue
	        	RealtimeChannelEventProcessor.process(realtimeChannel, 
						RealtimeChannelEventType.JOINED, 
						getClientInfo().getUser().getIdentifier());
			}
		} 
		catch (Exception ex) {
			// In a real code, customize handling for each type of exception
			getLogger().log(Level.WARNING, "Error when executing the method",
					ex);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					ex.getMessage(), ex);
		}

		return Response.ok(new WebsocketSubscription(realtimeChannel).toString()).build();
	}

	// Define allowed roles for the method "delete".
	private static final String[] delete5AllowedGroups = new String[] { "user" };
	// Define denied roles for the method "delete".
	private static final String[] delete5DeniedGroups = new String[] {};

	public void remove() throws Exception {
		checkGroups(delete5AllowedGroups, delete5DeniedGroups);

		try {
			// Path variables
			String channelId = Reference.decode(getAttribute("channelId"));
			RealtimeChannelDao dao = new RealtimeChannelDao();
			RealtimeChannel realtimeChannel = dao.findByUuidWithinSessionTenant(channelId);
			
			if (realtimeChannel == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"Unknown channel id");
			}
			else {
				realtimeChannel.setAvailable(false);
				realtimeChannel.setLastUpdated(new Timestamp(new Date().getTime()));
				
				dao.persist(realtimeChannel);
				
				RealtimeChannelEventProcessor.process(realtimeChannel, 
						RealtimeChannelEventType.DELETED, 
						getClientInfo().getUser().getIdentifier());
				
				getResponse().setEntity(new AgaveSuccessRepresentation());
			}
		} 
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception ex) {
			// In a real code, customize handling for each type of exception
			getLogger().log(Level.WARNING, "Error when executing the method",
					ex);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					ex.getMessage(), ex);
		}
	}
	
	private void setResponseHeadersForClientProtocol(RealtimeChannelProtocolType clientProtocol) {
		
		if (clientProtocol == RealtimeChannelProtocolType.WEBSOCKET) {
    		
    		getResponse().getHeaders().add("Sec-WebSocket-Extensions", "grip; message-prefix=\"\"");
        	getResponse().getHeaders().add("Content-Type", "application/websocket-events");

        	
    	} 
    	
    	// streaming responses will be negotiated by the proxy. here we just
    	// need to respond with the correct headers 
    	else if (clientProtocol == RealtimeChannelProtocolType.HTTP_STREAM) {
    		
    		getResponse().getHeaders().add("Grip-Hold","stream");
        	getResponse().getHeaders().add("Content-Type", "application/json");
    	}
    	
		// we default to long-polling when nothing else is specified
    	else {
        	
    		// add response for long-polling connections
        	getResponse().getHeaders().add("Grip-Hold","response");
        	
        	// tell the proxy to timeout long-polling connections after 55 seconds
        	getResponse().getHeaders().add("Grip-Timeout", "55");
    	}
	}

}
