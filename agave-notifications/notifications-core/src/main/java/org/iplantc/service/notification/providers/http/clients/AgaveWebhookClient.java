package org.iplantc.service.notification.providers.http.clients;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
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
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;

public class AgaveWebhookClient extends AbstractWebhookClient {
	
	private static final Logger	log	= Logger.getLogger(AgaveWebhookClient.class);
	
	private NotificationAttempt attempt;

	public AgaveWebhookClient(NotificationAttempt attempt) {
		super(attempt);
	}
	
	/**
	 * Adds a valid JWT for the owner and tenant associated with the
	 * {@link NotificationAttempt} and adds it to the header for 
	 * authentication to the APIs.
	 * 
	 * @param headers
	 * @return
	 * @throws NotificationException 
	 */
	public Map<String,String> getFilteredHeaders(Map<String,String> headers) throws NotificationException {
		try {
			headers.put(JWTClient.getJwtHeaderKeyForTenant(attempt.getTenantId()), 
						JWTClient.createJwtForTenantUser(attempt.getOwner(),  attempt.getTenantId(),  false));
		} catch (TenantException e) {
			log.error("Failed to create a JWT to use when publishing an authenticated " + attempt.getEventName() + 
			" event webhhook notifiation to " + attempt.getUuid(), e);
		}
		return headers;
	}
    
	
	/**
	 * Subclasses should extend this method to alter the name of the 
	 * notification client used in error and response messages.
	 * 
	 * @return
	 */
	protected String getSupportedCallbackProviderType() {
		return NotificationCallbackProviderType.AGAVE.name().toLowerCase();
	}
	
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
						HttpEntity entity = response.getEntity();
						if (entity.getContentLength() > 0) {
							in = entity.getContent();
							in.read(bs);
							attemptResponse.setMessage(new String(bs));
						}
						else {
							attemptResponse.setMessage(response.getStatusLine().getReasonPhrase());
						}
						
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
				
				//HttpEntity entity = response.getEntity();
				//byte[] bis = StreamUtils.getBytes(entity.getContent());
				//System.out.println(new String(bis));
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
	throws NotificationException 
	{
		// use the tenant id to create the internal url we can can call with a jwt
		String tenantId = TenancyHelper.getCurrentTenantId();
		String internalUrl = "https://" + StringUtils.replace(tenantId, ".", "-") + ".api.prod.agaveapi.co/"; 
		
		// strip the version number as the backend does not refernece them.
		String filteredcallbackUrl = StringUtils.replace(callbackUrl, "/v2", "");
		// we'll swap out the scheme so we can split after the hostname and optional port.
		filteredcallbackUrl = StringUtils.replaceOnce(filteredcallbackUrl, "//", "");
		// now return the internal url and the path+fragment+query
		return internalUrl + StringUtils.substringAfter(filteredcallbackUrl, "/");
	}
}
