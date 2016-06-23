/**
 * 
 */
package org.iplantc.service.common.clients;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.log4j.lf5.util.StreamUtils;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.auth.TrustedCALocation;
import org.iplantc.service.common.exceptions.MyProxyGatewayException;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author dooley
 *
 */
public class MyProxyGatewayClient 
{
	private static final Logger log = Logger.getLogger(MyProxyGatewayClient.class);
	
	private String serviceUrl;
	
	public MyProxyGatewayClient(String serviceUrl)
	{
		this.serviceUrl = serviceUrl;
	}
	
	public GSSCredential getCredential(String username, String tenantId, TrustedCALocation trustedCALocation) 
	throws MyProxyGatewayException
	{
		try 
		{
			URL url = new URL(serviceUrl);
			URI escapedUri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), "?trustroots=true", url.getRef());
			
			CloseableHttpClient httpclient = null;
			if (escapedUri.getScheme().equalsIgnoreCase("https"))
			{
				SSLContextBuilder builder = new SSLContextBuilder();
			    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			    
			    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
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
			httpPost.addHeader(JWTClient.getJwtHeaderKeyForTenant(tenantId), 
					JWTClient.createJwtForTenantUser(username, tenantId, false));
		    int statusCode = -1;
		    
			CloseableHttpResponse response = httpclient.execute(httpPost);
			ByteArrayInputStream is = null;
			try 
			{
				HttpEntity entity = response.getEntity();
				byte[] bis = StreamUtils.getBytes(entity.getContent());
				String content = new String(bis);
				JsonNode json = new ObjectMapper().readTree(content);
				
				statusCode = response.getStatusLine().getStatusCode();
				
				// verify the response was successful
				if (statusCode >= 200 && statusCode < 300) 
				{
					if (json.has("message")) {
						throw new MyProxyGatewayException(json.get("message").asText());
					} else {
						throw new MyProxyGatewayException(response.getStatusLine().toString());
					}
				}
				// process the 
				else
				{
					JsonNode result = json.get("result");
					if (result == null || !result.has("credential"))
					{
						throw new MyProxyGatewayException("No credential found in response from " + serviceUrl);
						
					}
					else
					{
						is = new ByteArrayInputStream(result.get("credential").asText().getBytes());
						X509Credential globusCred = new X509Credential(is);
						GSSCredential cred = new GlobusGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT);
						
						File trustrootPath = new File(trustedCALocation.getCaPath());
						try
						{
							if (!trustrootPath.exists()) {
								if (!trustrootPath.mkdirs()) {
									throw new IOException("Unable to create trustroot path on local file system.");
								}
							}
							
							if (result.has("trustroots"))
							{
								ArrayNode trustroots = (ArrayNode)result.get("trustroots");
								JsonNode trustroot = null;
								for(Iterator<JsonNode> iter = trustroots.iterator(); iter.hasNext(); trustroot = iter.next())
								{
									if (trustroot.has("filename") && trustroot.has("content")) {
										File cacertFile = new File(trustrootPath, trustroot.get("filename").asText());
										try {
											FileUtils.write(cacertFile, trustroot.get("content").textValue(), "utf-8");
										} catch (IOException e) {
											throw new IOException("Unable to write cacert to " + cacertFile.getAbsolutePath() + " on local file system.");
										}
									}
								}
								
							}
						}
						catch (IOException e) {
							log.error("Unable to create trustroot path on local file system.");
						}
						catch (Exception e) {
							log.error("Unable to save trustroots from " + serviceUrl + " to local file system.", e);
						}
						
						return cred;
					}
				}
			} 
			catch (MyProxyGatewayException e) { 
				throw e;
			} 
			catch (Exception e) {
				throw new MyProxyGatewayException("Call to " + serviceUrl + " failed", e);
		    } 
			finally {
		        response.close();
		        try { is.close(); } catch (Exception e) {}
		    }
		} 
		catch (MyProxyGatewayException e) {
			throw e;
		} 
		catch(Exception e) {
			throw new MyProxyGatewayException("Failed to retrieve credential from  " + serviceUrl, e);
		}
	}
}
