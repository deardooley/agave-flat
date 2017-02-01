package org.iplantc.service.common.clients;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Simple client library to reqeustb.in
 * @author dooley
 *
 */
public class RequestBin
{
	private static final Logger log = Logger.getLogger(RequestBin.class);
	private static final String BASE_URL = "https://requestbin.agaveapi.co/api/v1/bins";
	private static final String PUBLIC_URL = "https://requestbin.agaveapi.co";
	private ObjectMapper mapper = new ObjectMapper();
	
	private String binId;
	
	private RequestBin() {}
	    
	public static RequestBin getInstance() throws IOException {
	    RequestBin bin = new RequestBin();
	    bin.init();
	    return bin;
	}
	
	public void init() throws IOException 
	{
		HttpPost post = new HttpPost(BASE_URL);
		JsonNode json = doRequest(post);
		
		setBinId(json.get("name").asText());
		log.debug("Created request bin " + toString());
	}
	
//	/**
//	 * Deletes an existing request bin
//	 * @throws IOException
//	 * @deprecated no longer supported
//	 */
//	public void delete() throws IOException {
//		if (StringUtils.isEmpty(binId)) {
//			throw new IOException("No request bin has been created.");
//		}
//		else 
//		{
//			log.debug("Deleting request bin " + toString());
//			HttpDelete delete = new HttpDelete(BASE_URL + getBinId());
//			doRequest(delete);
//		}
//	}
	
	/**
	 * Returns an ArrayNode of JSON objects represeting the contents of the bin.
	 * Format of reponse is something like this:
	 * {  
	 *	"path":"/rrlfn3rr",
	 *	"content_type":"",
	 *	"content_length":0,
	 *	"time":1409174158.138781,
	 *	"form_data":{  
	 *
	 *	},
	 *	"body":"",
	 *	"id":"s908zy",
	 *	"method":"POST",
	 *	"query_string":{  
	 *		"message":[  
	 *			"Job completed. Skipping archiving at user request."
	 *		],
	 *		"jobid":[  
	 *			"0001409168603266-5056a550b8-0001-007"
	 *		],
	 *		"status":[  
	 *			"FINISHED"
	 *		]
	 *	},
	 *	"headers":{  
	 *		"Connect-Time":"1",
	 *		"Accept-Encoding":"gzip,deflate",
	 *		"Via":"1.1 vegur",
	 *		"User-Agent":"Apache-HttpClient/4.3.4 (java 1.5)",
	 *		"Content-Length":"0",
	 *		"X-Request-Id":"d909a0ae-32ac-43f3-8644-d066b137a63f",
	 *		"Connection":"close",
	 *		"Total-Route-Time":"0",
	 *		"Host":"requestb.in"
	 *	},
	 *	"remote_addr":"129.114.60.167"
	 *}
	 * @return
	 * @throws IOException
	 */
	public ArrayNode getRequests() throws IOException
	{
		if (StringUtils.isEmpty(binId)) {
			throw new IOException("No request bin has been created.");
		}
		else
		{
			log.debug("Listing contents of request bin " + toString());
			HttpGet get = new HttpGet(BASE_URL + getBinId());
			JsonNode json = doRequest(get);
			if (json != null && json.isArray()) {
				return (ArrayNode)json;
			} 
			else
			{
				throw new IOException("Failed to retrieve bin contents");
			}
		}
	}
	
	/**
	 * Does this bin exist
	 * @return true if a 200 response
	 * @throws IOException
	 */
	public boolean exists() throws IOException {
		if (StringUtils.isEmpty(binId)) {
			throw new IOException("No request bin has been initialized.");
		}
		else
		{
			try {
				log.debug("Checking status of request bin " + toString());
				HttpGet get = new HttpGet(BASE_URL + getBinId());
				JsonNode json = doRequest(get);
				return (json != null && !json.isNull());
			} catch (IOException e) {
				return false;
			}
		}
	}
	
	/**
	 * @return
	 */
	public String getBinId()
	{
		return binId;
	}

	/**
	 * @param binId
	 */
	public void setBinId(String binId)
	{
		this.binId = binId;
	}
	
	/**
	 * Makes the HTTP request, checks response, and returns JSON object response.
	 * @param httpUriRequest
	 * @return JsonNode
	 * @throws IOException
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyManagementException 
	 */
	@SuppressWarnings("deprecation")
	private JsonNode doRequest(HttpUriRequest httpUriRequest)
	throws IOException
	{
		DefaultHttpClient httpClient = null;
		try {
			TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
					return true;
				}
			};
		    SSLSocketFactory sf = new SSLSocketFactory(
		      acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		    SchemeRegistry registry = new SchemeRegistry();
		    registry.register(new Scheme("https", 443, sf));
		    ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
		    
		    httpClient = new DefaultHttpClient(ccm);
		    
		    HttpResponse response = httpClient.execute(httpUriRequest);
		    
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 300) 
			{
				JsonNode json = mapper.readTree(response.getEntity().getContent());
				
				log.debug("Response from request bin api: " + json.toString());
				
				return json;
			} 
			else 
			{
				throw new IOException("Error response received from Request Bin API at " + toString() + ": " + statusCode + 
						" - " + response.getStatusLine().getReasonPhrase());
			}
		}
		catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IOException("Request failed to requestbin.", e);
		}
		finally {
			try {httpClient.close(); } catch (Exception e){}
		}
		
	}
	
	public String toString() {
		return PUBLIC_URL + "/" + getBinId();
	}
}
