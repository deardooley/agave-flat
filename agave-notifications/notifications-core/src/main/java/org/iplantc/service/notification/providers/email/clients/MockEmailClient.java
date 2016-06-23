/**
 * 
 */
package org.iplantc.service.notification.providers.email.clients;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.providers.email.EmailClient;

/**
 * Mock {@link EmailClient} to swallow notifications.
 * 
 * @author dooley
 *
 */
public class MockEmailClient implements EmailClient {
	
	protected Map<String, String> customHeaders = new HashMap<String, String>();
    
    /* (non-Javadoc)
     * @see org.iplantc.service.notification.email.EmailClient#send(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody)
    throws NotificationException 
    {   
        // swallow notifications and do nothing
    }

	@Override
	public void setCustomHeaders(Map<String, String> headers) {
		this.customHeaders = headers;
	}

	@Override
	public Map<String, String> getCustomHeaders() {
		return this.customHeaders;
	}

}
