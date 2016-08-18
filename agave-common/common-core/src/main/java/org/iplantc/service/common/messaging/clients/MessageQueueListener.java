package org.iplantc.service.common.messaging.clients;

import org.iplantc.service.common.exceptions.MessageProcessingException;

public interface MessageQueueListener {
	
	public void processMessage(String message) throws MessageProcessingException;
	
	public void stop();
}
