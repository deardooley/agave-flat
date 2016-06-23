package org.iplantc.service.notification.model.enumerations;

import java.net.URI;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.uri.AgaveUriUtil;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.util.ServiceUtils;

/**
 * A {@link Notification} can be sent to destinations accessible through multiple
 * protocols. The supported protocols are given here. We also include a static 
 * resolution method to determine {@link NotificationCallbackProviderType} for a
 * given URL.
 *  
 * @author dooley
 *
 */
public enum NotificationCallbackProviderType {
	EMAIL, WEBHOOK, SMS, REALTIME, SLACK, AGAVE, NONE;
	
	public static NotificationCallbackProviderType getInstanceForUri(String callbackUrl) 
	throws BadCallbackException
	{
		if (StringUtils.isEmpty(callbackUrl)) {
			throw new BadCallbackException("callback cannot be null.");
		
		// SMS is detected by phone number regex match.
		// uncomment to enable sms notifications via twilio
		} else if (callbackUrl.replaceAll("[^\\d]", "").length() == callbackUrl.length()) {
			if (ServiceUtils.isValidPhoneNumber(callbackUrl)) {
				return SMS;
			} else {
				throw new BadCallbackException("Invalid phone number.");
			}
		
		// forward message based on destination. Email is detected by URL regex
		} else if (!(StringUtils.startsWithIgnoreCase(callbackUrl, "http://") || 
				StringUtils.startsWithIgnoreCase(callbackUrl, "https://") || 
				StringUtils.startsWithIgnoreCase(callbackUrl, "agave://"))) {
			if (ServiceUtils.isValidEmailAddress(callbackUrl)) {
				return EMAIL;
			} else {
				throw new BadCallbackException("Invalid email address.");
			}
			
		// realtime push messages are detected based on a prefix match of <pre>{@link Tenant#baseUrl()}/realtime</pre>
		} else if (ServiceUtils.isValidRealtimeChannel(callbackUrl, callbackUrl)) {
		    return REALTIME;
		
		// is it a slack webhook url?
	    } else if (StringUtils.startsWithIgnoreCase(callbackUrl, "https://hooks.slack.com")){
			return SLACK;
		
		// otherwise we assume it's a standard webhook. check for context and known integrations
		// checand forward accordingly.
		} else {
			try {
				URI callbackURI = URI.create(callbackUrl.replaceAll("\\$", "%24").replaceAll("\\{", "%7B").replaceAll("\\}", "%7B"));
				// avoid loopback attacks by filtering out reserve hostnames
				if (callbackURI.getHost().contains("localhost") || 
						callbackURI.getHost().contains("local") ||
						callbackURI.getHost().startsWith("127.") ||
						callbackURI.getHost().startsWith("255.") || 
						callbackURI.getHost().startsWith("172.") || 
						callbackURI.getHost().startsWith("192.") || 
						StringUtils.equals(callbackURI.getHost(), Settings.getLocalHostname()) ||
						Settings.getIpAddressesFromNetInterface().contains(callbackURI.getHost())) {
					throw new BadCallbackException("Invalid callback url.");
				}
				// if it's a known agave URI, we will use a custom http client to inject auth
				// on the attempt owner's behalf.
				else if (AgaveUriUtil.isInternalURI(callbackURI) && 
						!StringUtils.equalsIgnoreCase(callbackURI.getScheme(), "agave")){
					return AGAVE;
				} 
				// else it's a standard http(s) url
				else if (StringUtils.equalsIgnoreCase(callbackURI.getScheme(), "http") || 
						StringUtils.equalsIgnoreCase(callbackURI.getScheme(), "https")){
					return WEBHOOK;
				}
				else {
					throw new BadCallbackException("Invalid callback url.");
				}
			}
			catch (BadCallbackException e) {
				throw e;
			}
			catch (Exception e) {
				throw new BadCallbackException("Invalid callback url.", e);
			}
		}
	}
}
