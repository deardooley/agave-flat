/**
 * 
 */
package org.iplantc.service.clients;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.clients.exceptions.APIClientException;
import org.iplantc.service.clients.model.AuthenticationToken;

/**
 * Abstract parent class for all API Service clients
 * @author dooley
 *
 */
public abstract class AbstractService {
	
	private static Logger log = Logger.getLogger(AbstractService.class);
	
	protected String baseUrl;
	
	/**
	 * Perform an authenticated GET on the endpoint returning a structured
	 * APIResponse.
	 * 
	 * @param endpoint URL to which to post the form.
	 * @param token AuthenticationToken used for the request
	 * @return
	 * @throws APIClientException
	 */
	public APIResponse get(String endpoint, AuthenticationToken token)
	throws APIClientException {
		
		if (StringUtils.isEmpty(endpoint)) {
			throw new APIClientException("No endpoint provided");
		}
		
		if (token == null) {
			throw new APIClientException("No authenticatino token provided");
		}
		
		HttpClient httpClient = null;
        try {
        	httpClient = getAuthenticatedClient(endpoint, token.getUsername(), token.getToken());
            HttpGet put = new HttpGet(endpoint);
            
            HttpResponse response = httpClient.execute(put);

            try {
                log.debug(response.getStatusLine());
                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, "UTF-8");
                EntityUtils.consume(entity);
                return new APIResponse(body);
            } finally {
                put.releaseConnection();
            }
        } 
        catch (APIClientException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new APIClientException("Failed to perform GET on " + endpoint, e);
        } 
        finally {
            try { httpClient.getConnectionManager().shutdown(); } catch (Exception e) {}
        }
    }
	
	/**
	 * Perform a put operation. The form should be inlucded as a list of NameValuePair
	 * <code> 
	 * List <NameValuePair> nvps = new ArrayList <NameValuePair>();
	 * nvps.add(new BasicNameValuePair("action", "publish"));
	 * nvps.add(new BasicNameValuePair("name", "publicapp-1.1"));
	 * </code>
	 * @param endpoint URL to invoke
	 * @param token	Authentication token to use to authenticate.
	 * @param nvps form to PUT to the service. Leave null of not used.
	 * @throws IOException
	 * @throws APIClientException 
	 */
	public APIResponse put(String endpoint, AuthenticationToken token, List <NameValuePair> nvps)
    throws APIClientException {
		
		if (StringUtils.isEmpty(endpoint)) {
			throw new APIClientException("No endpoint provided");
		}
		
		if (token == null) {
			throw new APIClientException("No authenticatino token provided");
		}
		
		HttpClient httpClient = null;
        try {
        	httpClient = getAuthenticatedClient(endpoint, token.getUsername(), token.getToken());
            HttpPut put = new HttpPut(endpoint);
            put.setEntity(new UrlEncodedFormEntity(nvps));
            
            HttpResponse response = httpClient.execute(put);

            try {
                log.debug(response.getStatusLine());
                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, "UTF-8");
                EntityUtils.consume(entity);
                return new APIResponse(body);
            } finally {
                put.releaseConnection();
            }
        } 
        catch (APIClientException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new APIClientException("Failed to perform PUT on " + endpoint, e);
        } 
        finally {
            try { httpClient.getConnectionManager().shutdown(); } catch (Exception e) {}
        }
    }
	
	/**
	 * @param endpoint URL to which to post the form.
	 * @param token AuthenticationToken used for the request
	 * @param nvps List of form variables.
	 * @return
	 * @throws APIClientException
	 */
	public APIResponse post(String endpoint, AuthenticationToken token, List <NameValuePair> nvps)
    throws APIClientException {
		
		if (StringUtils.isEmpty(endpoint)) {
			throw new APIClientException("No endpoint provided");
		}
		
		if (token == null) {
			throw new APIClientException("No authenticatino token provided");
		}
		
		HttpClient httpClient = null;
        try {
        	httpClient = getAuthenticatedClient(endpoint, token.getUsername(), token.getToken());
            HttpPost post = new HttpPost(endpoint);
            post.setEntity(new UrlEncodedFormEntity(nvps));
            
            HttpResponse response = httpClient.execute(post);

            try {
                log.debug(response.getStatusLine());
                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, "UTF-8");
                EntityUtils.consume(entity);
                return new APIResponse(body);
            } finally {
                post.releaseConnection();
            }
        } 
        catch (APIClientException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new APIClientException("Failed to perform PUT on " + endpoint, e);
        } 
        finally {
            try { httpClient.getConnectionManager().shutdown(); } catch (Exception e) {}
        }
    }
	
	/**
	 * Post a multipart form with file to the api.
	 * 
	 * @param endpoint URL to which to post the form.
	 * @param token AuthenticationToken used for the request
	 * @param nvps List of form variables.
	 * @param file File object to upload
	 * @return
	 * @throws APIClientException
	 */
	public APIResponse post(String endpoint, AuthenticationToken token, List <NameValuePair> nvps, File file)
    throws APIClientException {
		
		if (StringUtils.isEmpty(endpoint)) {
			throw new APIClientException("No endpoint provided");
		}
		
		if (token == null) {
			throw new APIClientException("No authenticatino token provided");
		}
		
		if (file == null) {
			throw new APIClientException("No file provided");
		} else if (!file.exists()) {
			throw new APIClientException("File does not exist.");
		}
		
		try {
        	Executor executor = Executor.newInstance()
        	        .auth(new HttpHost(Settings.SERVICE_BASE_URL), token.getUsername(), token.getToken());
        	
        	String body = executor.execute(Request.Post(endpoint)
		            .version(HttpVersion.HTTP_1_1)
		            .useExpectContinue()
		            .bodyFile(file, ContentType.WILDCARD)
		            .bodyForm(nvps, Charset.defaultCharset()))
		            .returnContent().asString();
        	
        	return new APIResponse(body);
        } 
        catch (APIClientException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new APIClientException("Failed to perform POST multipart file & form to " + endpoint, e);
        } 
    }
	
	/**
	 * Performs an authenticated delete on the given endpoint using the given token.
	 * 
	 * @param endpoint URL on which to peform the delete
	 * @param token AuthenticationToken used for the request
	 * @return
	 * @throws APIClientException
	 */
	public APIResponse delete(String endpoint, AuthenticationToken token)
	throws APIClientException {
		
		if (StringUtils.isEmpty(endpoint)) {
			throw new APIClientException("No endpoint provided");
		}
		
		if (token == null) {
			throw new APIClientException("No authentication token provided");
		}
		
		HttpClient httpClient = null;
        try {
        	httpClient = getAuthenticatedClient(baseUrl + token.getToken(), token.getUsername(), token.getToken());
            HttpDelete delete = new HttpDelete(baseUrl + token.getToken());
            
            HttpResponse response = httpClient.execute(delete);

            try {
                log.debug(response.getStatusLine());
                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, "UTF-8");
                EntityUtils.consume(entity);
                return new APIResponse(body);
            } finally {
                delete.releaseConnection();
            }
        } 
        catch (APIClientException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new APIClientException("Failed to perform DELETE on " + baseUrl + token.getToken(), e);
        } 
        finally {
            try { httpClient.getConnectionManager().shutdown(); } catch (Exception e) {}
        }
    }	
	
	/**
	 * Verifies the validity of an existing tokey by checking the expiration date.
	 * 
	 * @param token AuthenticationToken
	 * @return true if the token expires in the future. false otherwise.
	 */
	public static boolean isTokenValid(AuthenticationToken token)
	{
		if (token == null) {
			return false;
		} else {
			return token.getExpirationDate().after(new Date()) && token.getRemainingUses() > 0;
		}
	}
	
	protected boolean isEmpty(String val) {
		return (val == null || val.isEmpty());
	}
	
	/**
	 * Returns a pre-authenticated client. Client must be closed manually.
	 * 
	 * @param endpoint
	 * @param apiUsername
	 * @param apiPassword
	 * @return
	 * @throws Exception
	 */
	private HttpClient getAuthenticatedClient(String endpoint, String apiUsername, String apiPassword) 
	throws Exception
	{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream instream = Settings.class.getClassLoader().getResourceAsStream(Settings.KEYSTORE_LOCATION);
        try {
            trustStore.load(instream, "changeit".toCharArray());
        } finally {
            try { instream.close(); } catch (Exception ignore) {}
        }

        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        Scheme sch = new Scheme("https", 443, socketFactory);
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
        httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(Settings.SERVICE_AUTH_REALM, Settings.SERVICE_BASE_PORT),
                    new UsernamePasswordCredentials(apiUsername, apiPassword));
        
        return httpClient;
	}
}
