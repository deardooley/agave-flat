package org.iplantc.service.realtime.resource.impl;

import java.util.logging.Level;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.arn.AgaveResourceName;
import org.iplantc.service.common.arn.AgaveResourceNameBuilder;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.realtime.dao.RealtimeChannelDao;
import org.iplantc.service.realtime.managers.RealtimeChannelEventProcessor;
import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelEventType;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelProtocolType;
import org.iplantc.service.realtime.representation.WebsocketSubscription;
import org.iplantc.service.realtime.resource.RealtimeCollection;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class RealtimeCollectionImpl extends AbstractServerResource implements RealtimeCollection {
	
	private static final Logger log = Logger.getLogger(RealtimeCollectionImpl.class);
	
	// Define allowed roles for the method "get".
    private static final String[] get1AllowedGroups = new String[] { "anyone" };
    // Define denied roles for the method "get".
    private static final String[] get1DeniedGroups = new String[] {};

    public Response represent() throws Exception {
    	
    	RealtimeChannel realtimeChannel = null;
        
        checkGroups(get1AllowedGroups, get1DeniedGroups);

        try {

            // Query parameters
        	String[] arnFilters = getQuery().getValuesArray("filter");
        	
        	realtimeChannel = new RealtimeChannel();
        	
        	for (String arnString: arnFilters) {
        		AgaveResourceName arn = AgaveResourceNameBuilder.getInstance(Reference.decode(arnString)).build();
        		realtimeChannel.addObservableEvent(arn.toString());
        	}
        	
        	realtimeChannel.setOwner(getClientInfo().getUser().getIdentifier());
        	
        	realtimeChannel.setProtocol(getClientProtocolFromRequestHeaders());
        	
        	setResponseHeadersForClientProtocol(realtimeChannel.getProtocol());
        	
        	RealtimeChannelDao dao = new RealtimeChannelDao();
        	dao.persist(realtimeChannel);
        	
        	log.info(String.format("New realtime channel created %s/%s/%s",
    				TenancyHelper.getCurrentTenantId(),
    				getClientInfo().getUser().getIdentifier(),
    				realtimeChannel.getUuid()));
        	
        	// process the event and send notifications to queue
        	RealtimeChannelEventProcessor.process(realtimeChannel, 
					RealtimeChannelEventType.CREATED, 
					getClientInfo().getUser().getIdentifier());
            
        } 
        catch (Exception ex) {
            // In a real code, customize handling for each type of exception
            getLogger().log(Level.WARNING, "Error when executing the method", ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex.getMessage(), ex);
        }
        
        return Response.ok(new WebsocketSubscription(realtimeChannel).toString()).build();
    }

	private void setResponseHeadersForClientProtocol(RealtimeChannelProtocolType clientProtocol) {
		
		if (clientProtocol == RealtimeChannelProtocolType.WEBSOCKET) {
    		
    		getResponse().getHeaders().add("Sec-WebSocket-Extensions", "grip; message-prefix=\"\"");
        	getResponse().getHeaders().add("Grip-Hold","stream");
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

	/**
	 * Negotiates the client protocol requested based on the request headers.
	 * 
	 * @return the protocol supported.
	 */
	protected RealtimeChannelProtocolType getClientProtocolFromRequestHeaders() {
		
		// websocket over http contains teller headers
    	if (StringUtils.equalsIgnoreCase(getRequest().getHeaders().getFirstValue("Sec-WebSocket-Extensions"), "grip") && 
    			StringUtils.equalsIgnoreCase(getRequest().getHeaders().getFirstValue("Content-Type"), "application/websocket-events")) {
    		
    		return RealtimeChannelProtocolType.WEBSOCKET;
    		
    	} 
    	// if they requested a content type of text/event-stream, they want the firehose over http, so 
    	// we establish a streaming connection
    	else if (StringUtils.equalsIgnoreCase(getRequest().getHeaders().getFirstValue("Content-Type"), "text/event-stream")){
    		
    		return RealtimeChannelProtocolType.HTTP_STREAM;
    		
    	} 
    	// if any other request comes in, we respond with the standard response of application/json and the 
    	// content response. these connections will be open for 55 seconds
    	else { //if (StringUtils.equalsIgnoreCase(getRequest().getHeaders().getFirstValue("Content-Type"), "application/json")) {
    		
    		return RealtimeChannelProtocolType.HTTP_LONGPOLL;
    		
    	}
	}

//    // Define allowed roles for the method "post".
//    private static final String[] post2AllowedGroups = new String[] { "anyone" };
//    // Define denied roles for the method "post".
//    private static final String[] post2DeniedGroups = new String[] {};
//
//    public org.iplantc.service.tags.representation.Tag add(org.iplantc.service.tags.representation.Tag bean)
//            throws Exception {
//        org.iplantc.service.tags.representation.Tag result = null;
//        checkGroups(post2AllowedGroups, post2DeniedGroups);
//
//        if (bean == null) {
//            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
//        }
//
//        try {
//
//            // Query parameters
//
//            result = new org.iplantc.service.tags.representation.Tag();
//
//            // Initialize here your bean
//        } catch (Exception ex) {
//            // In a real code, customize handling for each type of exception
//            getLogger().log(Level.WARNING, "Error when executing the method", ex);
//            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex.getMessage(), ex);
//        }
//
//        return result;
//    }

}
