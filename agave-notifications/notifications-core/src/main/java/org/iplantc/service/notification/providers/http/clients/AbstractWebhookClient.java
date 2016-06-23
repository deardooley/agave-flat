package org.iplantc.service.notification.providers.http.clients;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;

public abstract class AbstractWebhookClient implements WebhookClient {
	
	private static final Logger	log	= Logger.getLogger(AbstractWebhookClient.class);
	
	private NotificationAttempt attempt;

	public AbstractWebhookClient(NotificationAttempt attempt) {
		this.attempt = attempt;
	}
	
	/**
	 * Subclasses should extend this method to alter the http headers of the 
	 * notification client used when posting the callback url.
	 * 
	 * @param headers
	 * @return
	 * @throws NotificationException 
	 */
	public Map<String,String> getFilteredHeaders(Map<String,String> headers) 
	throws NotificationException 
	{
		return headers;
	}
    
	
	/**
	 * Subclasses should extend this method to alter the name of the 
	 * notification client used in error and response messages.
	 * 
	 * @return
	 */
	protected abstract String getSupportedCallbackProviderType();
	
	/**
	 * Subclasses should extend this method to altering the body of the 
	 * webhook response.
	 * 
	 * @param content
	 * @return the filtered content to be sent to the {@link NotificationAttempt#getCallbackUrl()}
	 * @throws NotificationException 
	 */
	protected String getFilteredContent(String content) throws NotificationException {
		return content;
	}
	
	/**
	 * Makes a HTTP POST request to {@link NotificationAttempt#getCallbackUrl()} with the 
	 * a {@code Content-Type: application/json} and body comprised of the 
	 * {@link NotificationAttempt#getContent()}. If the {@link NotificationAttempt#getCallbackUrl()}
	 * contains authorization informaiton, HTTP Basic auth is attempted with the
	 * given credentials.
	 * 
	 * @param attempt the attempt to make
	 * @return contains the http response code and interpreted message from the response. 
	 * @throws NotificationException
	 */
	@Override
	public NotificationAttemptResponse publish() 
	throws NotificationException
	{	
		long callstart = System.currentTimeMillis();
		NotificationAttemptResponse attemptResponse = new NotificationAttemptResponse();
		try 
		{
			URI escapedUri = URI.create(getFilteredCallbackUrl(attempt.getCallbackUrl()));
			
			CloseableHttpClient httpclient = null;
			if (escapedUri.getScheme().equalsIgnoreCase("https"))
			{
				SSLContext sslContext = new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustStrategy() {

		                    public boolean isTrusted(
		                            final X509Certificate[] chain, final String authType) throws CertificateException {
		                        return true;
		                    }
		                })
						.useTLS()
						.build();
			    
			    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
			    		sslContext,
			    		new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"},   
			    	    null,
			    		SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			    
			    httpclient = HttpClients.custom()
			    		.setSSLSocketFactory(sslsf)
			    		.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
			    		.build();
			}
			else
			{
				httpclient = HttpClients.custom()
						.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
			    		.build();
			}
			
			HttpPost httpPost = new HttpPost(escapedUri);
			CloseableHttpResponse response = null;
			RequestConfig config = RequestConfig.custom()
												.setConnectTimeout(20000)
												.setSocketTimeout(10000)
												.build();
			httpPost.setConfig(config);
			
			httpPost.setEntity(new StringEntity(getFilteredContent(attempt.getContent())));
			
			Map<String,String> headerMap = new HashMap<String,String>();
            
            headerMap.put("Content-Type", "application/json");
            headerMap.put("User-Agent", "Agave-Hookbot/"+ org.iplantc.service.common.Settings.getContainerId());
            headerMap.put("X-Agave-Delivery", attempt.getUuid());
            headerMap.put("X-Agave-Notification", attempt.getNotificationId());
            
            for (String key: getFilteredHeaders(headerMap).keySet()) {
            	httpPost.setHeader(key, headerMap.get(key));
            }
			
			callstart = System.currentTimeMillis();
			try
			{
				if (escapedUri.getUserInfo() != null) 
				{
					String userInfo = escapedUri.getUserInfo();
					String[] authTokens = userInfo.split(":");
					String username = authTokens[0];
					String password = authTokens.length > 1 ? authTokens[1] : "";
					
				    HttpHost targetHost = new HttpHost(escapedUri.getHost(), escapedUri.getPort(), escapedUri.getScheme());
				    CredentialsProvider credsProvider = new BasicCredentialsProvider();
				    credsProvider.setCredentials(
				            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				            new UsernamePasswordCredentials(username, password));
		
				    // Create AuthCache instance
				    AuthCache authCache = new BasicAuthCache();
				    // Generate BASIC scheme object and add it to the local auth cache
				    BasicScheme basicAuth = new BasicScheme();
				    authCache.put(targetHost, basicAuth);
		
				    // Add AuthCache to the execution context
				    HttpClientContext context = HttpClientContext.create();
				    context.setCredentialsProvider(credsProvider);
				    context.setAuthCache(authCache);
				    
				    response = httpclient.execute(targetHost, httpPost, context);
				}
				else
				{
					response = httpclient.execute(httpPost);
				}
				
				attemptResponse.setCode(response.getStatusLine().getStatusCode());
				
//				StringUtils.isEmpty(getCustomNotificationMessageContextData())				
				if (attemptResponse.getCode() >= 200 && attemptResponse.getCode() < 300) {
					attemptResponse.setMessage("200 ok");
					log.debug("[" + attempt.getUuid() + "] Successfully sent " + attempt.getEventName() + 
							getSupportedCallbackProviderType() + " notification  to " + attempt.getCallbackUrl());
				} else {
					InputStream in = null;
					byte[] bs = new byte[2048]; 
					try {
						in = response.getEntity().getContent();
						in.read(bs);
						attemptResponse.setMessage(new String(bs));
						log.error("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
								" " + getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
								". Server responded with: " + attemptResponse.getCode() + " - " + attemptResponse.getMessage());
						
					} catch (Exception e) {
						attemptResponse.setMessage("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
								" " + getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
								". Server responded with: " + attemptResponse.getCode() + " - " + response.getStatusLine().getReasonPhrase());
						log.error(attemptResponse.getMessage(), e);
					}
					finally {
						try {in.close();} catch (Exception e){}
					}
				}
			} 
			catch (ConnectTimeoutException e) {
				attemptResponse.setCode(408);
				attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
						getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
						". Remote call to " + escapedUri.toString() + " timed out after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.");
				throw new NotificationException(attemptResponse.getMessage(),e);
			} 
			catch (SSLException e) {
				attemptResponse.setCode(404);
				if (StringUtils.equalsIgnoreCase(escapedUri.getScheme(), "https")) {
					attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
							getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
							". Remote call to " + escapedUri.toString() + " failed due to the remote side not supporting SSL.");
				} else {
					attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
							getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
							". Remote call to " + escapedUri.toString() + " failed due a server side SSL failure.");
				}
				throw new NotificationException(attemptResponse.getMessage(),e);
			} 
			catch (Exception e) {
				attemptResponse.setCode(500);
				attemptResponse.setMessage("Failed to send " + attempt.getEventName() + " notification " + 
						getSupportedCallbackProviderType() + " to " + attempt.getCallbackUrl() +
						" due to internal server error.");
				throw new NotificationException(attemptResponse.getMessage(),e);
			} 
			finally {
		        try { response.close(); } catch (Exception e) {}
		    }	
		}
		catch (NotificationException e) {
			log.error("[" + attempt.getUuid() + "] " + attemptResponse.getMessage() + ". Remote call to " + attempt.getCallbackUrl() + " failed after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.", e);
			throw e;
		}
		catch(Exception e) {
			attemptResponse.setCode(500);
			attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
					" due to internal server error. Remote call failed after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.");
			log.error("[" + attempt.getUuid() + "] " + attemptResponse.getMessage(), e);
		}
		
		return attemptResponse;
	}

	/**
	 * Allows implementing classes to alter the callback url as needed.
	 * @param callbackUrl
	 * @return
	 * @throws NotificationException
	 */
	public String getFilteredCallbackUrl(String callbackUrl)
			throws NotificationException {
		return callbackUrl;
	}
}
