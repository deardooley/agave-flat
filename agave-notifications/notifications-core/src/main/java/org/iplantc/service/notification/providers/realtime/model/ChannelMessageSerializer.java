/**
 * 
 */
package org.iplantc.service.notification.providers.realtime.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author dooley
 *
 */
public class ChannelMessageSerializer extends JsonSerializer<ChannelMessage> {

	@Override
	public void serialize(ChannelMessage channelMessage, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		String channel = channelMessage.getChannel();
        jgen.writeStringField("channel", channel);
        
        
        if (channelMessage.getMessageType().getContentField() == null) {
        	jgen.writeObjectField(channelMessage.getMessageType().getContentKey(), channelMessage.getContent());
		} else {
			jgen.writeFieldName(channelMessage.getMessageType().getContentKey());
			jgen.writeStartObject();
			jgen.writeStringField(channelMessage.getMessageType().getContentField(),
					new ObjectMapper().writeValueAsString(channelMessage.getContent()));
			jgen.writeEndObject();
		}
		jgen.writeEndObject();
	}

}
