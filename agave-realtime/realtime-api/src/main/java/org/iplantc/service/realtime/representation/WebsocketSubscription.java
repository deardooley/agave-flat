package org.iplantc.service.realtime.representation;

import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelProtocolType;

public class WebsocketSubscription {
	
	RealtimeChannel realtimeChannel;
	
	public WebsocketSubscription(RealtimeChannel realtimeChannel) {
		this.realtimeChannel = realtimeChannel;
	}
	
	@Override
	public String toString() {
		if (realtimeChannel.getProtocol() == RealtimeChannelProtocolType.WEBSOCKET) {
			return "TEXT 2F\\\r\\\n\n" + 
				"c:{\"type\": \"subscribe\", \""+ realtimeChannel.getName() +"\": \"\"}\\\r\\\n";
		} 
		else if (realtimeChannel.getProtocol() == RealtimeChannelProtocolType.HTTP_STREAM) {
			return "";
		}
		// otherwise it's a long polling response which will only be sent if no 
		// other data is pushed to the channel. We default to an empty json object;
		else {
			return realtimeChannel.toJSON();
		}
	}

}
