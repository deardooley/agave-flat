/*Copyright (c) 2004,University of Illinois at Urbana-Champaign.  All rights reserved.
 * 
 * Created on May 10, 2007
 * 
 * Developed by: CCT, Center for Computation and Technology, 
 * 				NCSA, University of Illinois at Urbana-Champaign
 * 				OSC, Ohio Supercomputing Center
 * 				TACC, Texas Advanced Computing Center
 * 				UKy, University of Kentucky
 * 
 * https://www.gridchem.org/
 * 
 * Permission is hereby granted, free of charge, to any person 
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal with the Software without 
 * restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom 
 * the Software is furnished to do so, subject to the following conditions:
 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimers.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimers in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the names of Chemistry and Computational Biology Group , NCSA, 
 *    University of Illinois at Urbana-Champaign, nor the names of its contributors 
 *    may be used to endorse or promote products derived from this Software without 
 *    specific prior written permission.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  
 * IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS WITH THE SOFTWARE.
*/

package org.iplantc.service.notification.util;

import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.providers.email.EmailClient;
import org.iplantc.service.notification.providers.email.EmailClientFactory;

/**
 * Simple email class using the JavaMail API to send an email in both
 * HTML and plain text format.
 * 
 * @author Rion Dooley < dooley [at] tacc [dot] utexas [dot] edu >
 */
public class EmailMessage {
    
    public static Logger log = Logger.getLogger(EmailMessage.class.getName());
    
    /**
     * Synchronously sends a multipart email in both html and plaintext format 
     * using an {@link EmailClient} determined by the service settings. Supports
     * addition of custom headers using the conventions of the email service
     * provider.
     * 
     * @param recipientName Full name of recipient (ex. John Smith)
     * @param recipientAddress email address of recipient
     * @param subject of the email
     * @param body of the email in plain text format.
     * @param custom headers to add to the email
     * @throws NotificationException
     */
    public static void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody, Map<String, String> customHeaders) 
    throws NotificationException 
    {
        EmailClient client = EmailClientFactory.getInstance(Settings.EMAIL_PROVIDER);
        client.send(recipientName, recipientAddress, subject, body, htmlBody);
    }
    
    /**
     * Synchronously sends a multipart email in both html and plaintext format 
     * using an {@link EmailClient} determined by the service settings.
     * 
     * @param recipientName Full name of recipient (ex. John Smith)
     * @param recipientAddress email address of recipient
     * @param subject of the email
     * @param body of the email in plain text format.
     * @throws NotificationException
     */
    public static void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody) 
    throws NotificationException 
    {
        EmailClient client = EmailClientFactory.getInstance(Settings.EMAIL_PROVIDER);
        client.send(recipientName, recipientAddress, subject, body, htmlBody);
    }
}