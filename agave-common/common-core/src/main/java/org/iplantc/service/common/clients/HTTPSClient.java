package org.iplantc.service.common.clients;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.log4j.lf5.util.StreamUtils;

import com.thoughtworks.xstream.core.util.Base64Encoder;

public class HTTPSClient {
	private String				url;
	private String				username;
	private String				password;
	private Map<String, String>	headers;

	// Create an anonymous class to trust all certificates.
	// This is bad style, you should create a separate class.
	private X509TrustManager	xtm	= new X509TrustManager() {

										public void checkClientTrusted(
												X509Certificate[] chain,
												String authType)
										{}

										public void checkServerTrusted(
												X509Certificate[] chain,
												String authType)
										{}

										public X509Certificate[] getAcceptedIssuers()
										{
											return null;
										}
									};

	// Create an class to trust all hosts
	private HostnameVerifier	hnv	= new HostnameVerifier() {
										public boolean verify(String hostname,
												SSLSession session)
										{
											return true;
										}
									};

	// In this function we configure our system with a less stringent
	// hostname verifier and X509 trust manager. This code is
	// executed once, and calls the static methods of HttpsURLConnection
									
	public HTTPSClient(String url)
	{
		this(url, null, null, null);
	}
	
	public HTTPSClient(String url, Map<String, String> headers)
	{
		this(url, null, null, headers);
	}

	public HTTPSClient(String url, String username, String password)
	{
		this(url, username, password, new HashMap<String, String>());
	}

	public HTTPSClient(String sUrl, String username, String password,
			Map<String, String> headers)
	{
		this.url = sUrl;
		this.username = username;
		this.password = password;
		this.headers = headers == null ? new HashMap<String, String>() : headers;
		
		// only set up ssl if the auth service protocol calls for it
		if (url.toLowerCase().startsWith("https"))
		{
			// Initialize the TLS SSLContext with
			// our TrustManager
			SSLContext sslContext = null;
			try
			{
				sslContext = SSLContext.getInstance("TLS");
				X509TrustManager[] xtmArray = new X509TrustManager[] { xtm };
				sslContext.init(null, xtmArray, new java.security.SecureRandom());
			}
			catch (GeneralSecurityException gse)
			{
				// Print out some error message and deal with this exception
			}
	
			// Set the default SocketFactory and HostnameVerifier
			// for javax.net.ssl.HttpsURLConnection
			if (sslContext != null)
			{
				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext
						.getSocketFactory());
			}
	
			HttpsURLConnection.setDefaultHostnameVerifier(hnv);
		}
	}

	// This function is called periodically, the important thing
	// to note here is that there is no special code that needs to
	// be added to deal with a "HTTPS" URL. All of the trust
	// management, verification, is handled by the HttpsURLConnection.
	public String getText() throws Exception
	{
		String content = "";
		URLConnection urlCon = null;
		InputStream in = null;
		ByteArrayOutputStream out = null;
		
		try
		{
			urlCon = ( new URL(url) ).openConnection();
			
			if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
				Base64Encoder encoder = new Base64Encoder();
				String userpass = username + ":" + password;
				String encoding = encoder.encode(userpass.getBytes());
				encoding = encoding.replaceAll("\n", "");
				urlCon.setRequestProperty("Authorization", "Basic " + encoding);
			}
			
			if (!headers.isEmpty())
			{
				for (String key : headers.keySet())
				{
					urlCon.setRequestProperty(key, headers.get(key));
				}
			}
			
			in = urlCon.getInputStream();
			out = new ByteArrayOutputStream();
			StreamUtils.copy(in, out);
			content = new String(out.toByteArray());
		}
		catch (MalformedURLException mue)
		{
			throw mue;
		}
		catch (FileNotFoundException e) {
			return "{\n\"users\": []\n}";
		}
		catch (IOException ioe)
		{
			throw ioe;
		}
		catch (Exception e)
		{
			throw e;
		}
		finally {
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
		}

		return content;
	}
	
	public static void doGet(URL url, File downloadFile) throws IOException, URISyntaxException
	{	
		InputStream in = null;
		FileOutputStream out = null;
		try 
		{
			URI uri = url.toURI();
			HttpGet httpGet = new HttpGet(uri);
		    
			CloseableHttpResponse response = doRequest(uri, httpGet);
			
			StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
	    		out = new FileOutputStream(downloadFile);
	    		response.getEntity().writeTo(out);
	    	} else if (statusLine.getStatusCode() == 404) {
	    		throw new FileNotFoundException("File or folder ");
	    	} else if (statusLine.getStatusCode() == 401 || statusLine.getStatusCode() == 403) {
	    		throw new IOException("Failed to get " + uri.toString() + " due to insufficient privileges.");
	    	} else {
	    		throw new IOException(statusLine.getReasonPhrase());
	    	}
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) 
		{
			throw new IOException("Unable to establish secure connection with remote server", e);
		}
		finally {
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
		}
	}
	
	public static String doGet(URL url) throws IOException, URISyntaxException
	{	
		InputStream in = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try 
		{
			URI uri = url.toURI();
			HttpGet httpGet = new HttpGet(uri);
		    
			CloseableHttpResponse response = doRequest(uri, httpGet);
			
			StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
	    		HttpEntity entity = response.getEntity();
	    		entity.writeTo(out);
	    		
	    		
	    		return out.toString();
	    	} else if (statusLine.getStatusCode() == 404) {
	    		throw new FileNotFoundException("File or folder ");
	    	} else if (statusLine.getStatusCode() == 401 || statusLine.getStatusCode() == 403) {
	    		throw new IOException("Failed to get " + uri.toString() + " due to insufficient privileges.");
	    	} else {
	    		throw new IOException(statusLine.getReasonPhrase());
	    	}
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) 
		{
			throw new IOException("Unable to establish secure connection with remote server", e);
		}
		finally {
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Constructs HTTP client with "trust everything" ssl support to make a HTTP request.
	 * @param escapedUri
	 * @param httpUriRequest
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException 
	 */
	private static CloseableHttpResponse doRequest(URI escapedUri, HttpUriRequest httpUriRequest)
	throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		CloseableHttpClient httpclient = initClient(escapedUri);
		
		String username = null;
		String password = null;
		
		if (!StringUtils.isEmpty(escapedUri.getUserInfo())) {
            String[] tokens = escapedUri.getUserInfo().split(":");
            if (tokens.length >= 1) {
                username = tokens[0];
                password = tokens[1];
            }
        }
		
		if (!StringUtils.isEmpty(username)) 
		{
			String nullSafePassword = StringUtils.isEmpty(password) ? "" : password;
			
		    HttpHost targetHost = new HttpHost(escapedUri.getHost(), escapedUri.getPort(), escapedUri.getScheme());
		    CredentialsProvider credsProvider = new BasicCredentialsProvider();
		    credsProvider.setCredentials(
		            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
		            new UsernamePasswordCredentials(username, nullSafePassword));

		    // Create AuthCache instance
		    AuthCache authCache = new BasicAuthCache();
		    // Generate BASIC scheme object and add it to the local auth cache
		    BasicScheme basicAuth = new BasicScheme();
		    authCache.put(targetHost, basicAuth);

		    // Add AuthCache to the execution context
		    HttpClientContext context = HttpClientContext.create();
		    context.setCredentialsProvider(credsProvider);
		    context.setAuthCache(authCache);
		    
		    return httpclient.execute(targetHost, httpUriRequest, context);
		}
		else
		{
			return httpclient.execute(httpUriRequest);
		}
	}
	
	/**
	 * Creats a "trust everyone" HTTP client.
	 * @param escapedUri
	 * @return http client ready to be called with a HttpUriRequest
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 */
	private static CloseableHttpClient initClient(URI escapedUri) 
	throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
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
		
		return httpclient;
	}

	public static void main(String[] args)
	{

		HTTPSClient httpsTest = new HTTPSClient(
				"https://iplant-vm.tacc.utexas.edu/auth-v1/", "",
				"");
		try
		{
			System.out.println(httpsTest.getText());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
