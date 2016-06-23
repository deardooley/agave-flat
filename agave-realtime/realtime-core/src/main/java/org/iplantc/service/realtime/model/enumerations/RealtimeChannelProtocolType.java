package org.iplantc.service.realtime.model.enumerations;

/**
 * All possible events occuring on a {@link RealtimeChannel} object within
 * the platform. These correspond to notification events thrown throughout
 * the domain logic.
 * 
 * @author dooley
 *
 */
public enum RealtimeChannelProtocolType {
    
	/**
	 * Connect via websocket
	 */
	WEBSOCKET,
    
	/**
	 * Connect via long polling
	 */
	HTTP_LONGPOLL,
    
	/**
	 * Streaming HTTP connections.
	 */
	HTTP_STREAM;
    

//    private String description;
//    
//    private RealtimeChannelProtocolType(String description) {
//        this.description = description;
//    }
//    
//    /**
//     * Gets the description of this event.
//     * @return
//     */
//    public String getDescription() {
//        return this.description;
//    }
}
