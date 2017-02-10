package org.iplantc.service.uuid.resource.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.uuid.exceptions.UUIDResolutionException;
import org.restlet.Request;
import org.restlet.data.Header;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class AbstractUuidResource extends AbstractAgaveResource {

	
	private static final Logger log = Logger
			.getLogger(AbstractUuidResource.class);
	
	/**
     *
     */
	public AbstractUuidResource() {
	}

	/**
	 * Creates a {@link AgaveUUID} object for the uuid in the URL or throws an
	 * exception that can be re-thrown from the route method.
	 * 
	 * @param uuid
	 * @return AgaveUUID object referenced in the path
	 * @throws AgaveUUID
	 * @throws ResourceException
	 */
	protected AgaveUUID getAgaveUUIDInPath(String uuid) throws UUIDException {
		
		AgaveUUID agaveUuid = null;
		if (StringUtils.isEmpty(uuid)) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No uuid provided");
		} else {

			try {
				agaveUuid = new AgaveUUID(uuid);
			} catch (UUIDException e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No resource found matching " + uuid, e);
			} catch (Throwable e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Unable to resolve uuid " + uuid, e);
			}
		}

		return agaveUuid;
	}

	/**
	 * Convenience class to log usage info per request
	 * 
	 * @param action
	 */
	protected void logUsage(AgaveLogServiceClient.ActivityKeys activityKey) {
		AgaveLogServiceClient.log(getServiceKey().name(), activityKey.name(),
				getAuthenticatedUsername(), "", org.restlet.Request
						.getCurrent().getClientInfo().getUpstreamAddress());
	}

	protected ServiceKeys getServiceKey() {
		return AgaveLogServiceClient.ServiceKeys.UUID02;
	}
	
	
	
	/** Try to ping a URL. Return true only if successful. */
	final class ResourceResolutionTask implements Callable<JsonNode> {
		private final JsonNode json;
		private final String url;
		private final String filter;
		private final Map<String,String> headerMap;
		
		ResourceResolutionTask(String url, String filter, Map<String,String> headerMap) {
			this.url = url;
			this.json = null;
			this.filter = filter;
			this.headerMap = headerMap;
		}
		
		ResourceResolutionTask(JsonNode json, String filter, Map<String,String> headerMap) {
			this.url = null;
			this.json = json;
			this.filter = filter;
			this.headerMap = headerMap;
		}

		/** Access a URL, and see if you get a healthy response. */
		@Override
		public JsonNode call() throws Exception {
			if (json == null) {
				try {
					return fetchResource(this.url, this.filter, this.headerMap);
				}
				catch (UUIDResolutionException e) {
					log.error(e);
	    			
	    			return new ObjectMapper().createObjectNode()
							.put("status", "error")
							.put("message", e.getMessage());
				}
			}
			else {
				return json;
			}
		}		
	}
	
	/**
	 * 
	 * @author dooley
	 *
	 */
	private static final class ApiResponse {
		String url;
		Boolean success;
		Long timing;
		JsonNode response;

		@Override
		public String toString() {
			return response == null ? "{}" : response.toString();
		}
	}
	
	/**
	 * Fetches the resource at the given URL using a HTTP Get and pass through
	 * auth headers.
	 * 
	 * @param resourceUrl the url to the resource requested
	 * @param filter the url filter to use on the remote request
	 * @param headers the auth and client headers used to request the resource
	 * @return JsonNode representation of the naked response
	 * @throws UUIDResolutionException
	 */
	protected JsonNode fetchResource(String resourceUrl, String filter, Map<String,String> headerMap) throws UUIDResolutionException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode attemptResponse = null;
		try 
		{
			String filteredResourceUrl = resourceUrl;
			if (StringUtils.isNotEmpty(filter)) {
				if (StringUtils.contains(filteredResourceUrl, "?")) {
					filteredResourceUrl += "&filter="+URLEncoder.encode(StringUtils.stripToEmpty(filter));
				}
				else {
					filteredResourceUrl += "?filter="+URLEncoder.encode(StringUtils.stripToEmpty(filter));
				}
			}
			
			URI escapedUri = URI.create(filteredResourceUrl);
			
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
			
//			HttpHead httpGet = new HttpHead(escapedUri);
			HttpGet httpGet = new HttpGet(escapedUri);
			CloseableHttpResponse response = null;
			RequestConfig config = RequestConfig.custom()
												.setConnectTimeout(30000)
												.setSocketTimeout(30000)
												.build();
			httpGet.setConfig(config);
			
			// add the auth and client headers provided to this method
			for (String headerName: headerMap.keySet()) {
				httpGet.addHeader(headerName, headerMap.get(headerName));
	        }
	        
			long callstart = System.currentTimeMillis();
			
			try
			{
				response = httpclient.execute(httpGet);
				
				InputStream in = null;
				
				try {
					int statusCode = response.getStatusLine().getStatusCode();
					HttpEntity entity = response.getEntity();
					
					long contentLength = entity.getContentLength();
					
					if (contentLength > 0 || contentLength == -1) {
						in = entity.getContent();
						attemptResponse = mapper.readTree(in);
						// if successful, just return the result from the response
						if (statusCode >= 200 && statusCode <= 300) {
							if (attemptResponse.hasNonNull("result")) {
								// calls to the files api will return an array. the first (and only 
								// since we appended limit=1 to files api queries) object is
								// always the one we're interested in.
								if (attemptResponse.get("result").isArray()) {
									return ((ArrayNode)attemptResponse.get("result")).get(0);
								}
								else {
									return attemptResponse.get("result");
								}
							} else {
								return mapper.createObjectNode();
							}
						}
						// if failed, just return the error message and status from teh response.
						else {
							if (attemptResponse.hasNonNull("version")) 
								((ObjectNode)attemptResponse).remove("version");
							if (attemptResponse.hasNonNull("result")) 
								((ObjectNode)attemptResponse).remove("result");
							
							return attemptResponse;
						}
					}
					// if no content is returned, return an empty object.
					else {
						
						return mapper.createObjectNode();
					}
				}
				catch (Exception e) {
					int statusCode = 0;
					String message = null;
					if (response != null) {
						StatusLine statusLine = response.getStatusLine();
						if (statusLine != null) {
							statusCode = statusLine.getStatusCode();
							message = statusLine.getReasonPhrase();
						}
					}
					
					throw new UUIDResolutionException("Failed to resolve " + resourceUrl +
							". Server responded with: " + statusCode + " - " + message, e);
				}
				finally {
					try {in.close();} catch (Exception e){}
				}
			} 
			catch (ConnectTimeoutException e) {
				throw new UUIDResolutionException("Failed to resolve " + resourceUrl + 
						". Remote call to " + escapedUri.toString() + " timed out after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.", e);
			} 
			catch (SSLException e) {
				if (StringUtils.equalsIgnoreCase(escapedUri.getScheme(), "https")) {
					throw new UUIDResolutionException("Failed to resolve " + resourceUrl +
							". Remote call to " + escapedUri.toString() + " failed due to the remote side not supporting SSL.", e);
				} else {
					throw new UUIDResolutionException("Failed to resolve " + resourceUrl + 
							". Remote call to " + escapedUri.toString() + " failed due a server side SSL failure.");
				}
			}
			finally {
		        try { response.close(); } catch (Exception e) {}
		    }	
		}
		catch (UUIDResolutionException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new UUIDResolutionException("Failed to resolve " + resourceUrl + 
					" due to internal server error.");
		} 
	}
	
	/**
	 * Creates a {@link Map} of header names and values used in the requests
	 * to resolve resource representations.
	 * 
	 * @return
	 * @throws TenantException
	 */
	protected Map<String,String> getRequestAuthCredentials() throws TenantException {
		Map<String,String> headerMap = new HashMap<String,String>();
        
		// these will uniquely identify the requesting client
		headerMap.put("Content-Type", "application/json");
		headerMap.put("User-Agent", "Agave-UUID-API/"+ org.iplantc.service.common.Settings.getContainerId());
        
		// we add the jwt header for backend (dev) access
        String jwtHeaderKey = JWTClient.getJwtHeaderKeyForTenant(TenancyHelper.getCurrentTenantId());
        Series<Header> headers = Request.getCurrent().getHeaders();
        
        Header jwtHeader = headers.getFirst(jwtHeaderKey);
        if (jwtHeader != null) {
        	headerMap.put(jwtHeaderKey, jwtHeader.getValue());
        }
        
        // we add the frontend bearer token if present.
        Header authHeader = headers.getFirst("authorization");
        if (authHeader != null) {
        	headerMap.put("Authorization", authHeader.getValue());
        }
        
        return headerMap;
	}

}
