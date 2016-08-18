package org.iplantc.service.common.messaging;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.clients.BeanstalkClient;
import org.iplantc.service.common.messaging.clients.IronBeanstalkClient;
import org.iplantc.service.common.messaging.clients.IronMQClient;
import org.iplantc.service.common.messaging.clients.MessageQueueClient;
import org.iplantc.service.common.messaging.clients.RabbitMQClient;
import org.iplantc.service.common.messaging.model.enumerations.MessageQueueType;

public class MessageClientFactory {

	public static MessageQueueClient getMessageClient() throws MessagingException
	{
		if (StringUtils.equalsIgnoreCase(Settings.MESSAGING_SERVICE_PROVIDER, MessageQueueType.IRONMQ.name()))
		{
			return new IronMQClient();
		} 
		else if (StringUtils.equalsIgnoreCase(Settings.MESSAGING_SERVICE_PROVIDER, MessageQueueType.RABBITMQ.name()))
		{
			return new RabbitMQClient();
		}
		else if (StringUtils.equalsIgnoreCase(Settings.MESSAGING_SERVICE_PROVIDER, MessageQueueType.BEANSTALK.name()))
		{
			return new BeanstalkClient();
		}
		else if (StringUtils.equalsIgnoreCase(Settings.MESSAGING_SERVICE_PROVIDER, MessageQueueType.IRONBEANSTALK.name()))
		{
			return new IronBeanstalkClient();
		}
		else
		{
			throw new MessagingException("Unknown messaging service. Please specify one of the following: ironmq, rabbitmq, beanstalk");
		}
	}

}
