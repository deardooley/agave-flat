package org.iplantc.service.transfer.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author dooley
 *
 */
public class HTTP implements RemoteDataClient {

	private URI uri;
	private String host;
	private String username;
	private String password;
	private int port;
	private boolean useSSL;
	private String homeDir, rootDir;
	private Hashtable<String, Long> lengthCache = new Hashtable<String,Long>();
	
    protected static final int MAX_BUFFER_SIZE = 1048576;

//    static {
//    	try { 
//			
//			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){ 
//                    public boolean verify(String hostname, SSLSession session) { 
//                            return true; 
//                    }}); 
//            SSLContext context = SSLContext.getInstance("TLS"); 
//            context.init(null, new X509TrustManager[]{new X509TrustManager(){ 
//                    public void checkClientTrusted(X509Certificate[] chain, 
//                                    String authType) throws CertificateException {} 
//                    public void checkServerTrusted(X509Certificate[] chain, 
//                                    String authType) throws CertificateException {} 
//                    public X509Certificate[] getAcceptedIssuers() { 
//                            return new X509Certificate[0]; 
//                    }}}, new SecureRandom()); 
//            HttpsURLConnection.setDefaultSSLSocketFactory( 
//                            context.getSocketFactory()); 
//	    } catch (Exception e) { // should never happen 
//	            e.printStackTrace(); 
//	    }
//    }
	public HTTP() {}
	
	public HTTP(URI uri)
	{
		this.host = uri.getHost();
		this.useSSL = "https".equals(uri.getScheme());
		this.port = uri.getPort();

		if (!StringUtils.isEmpty(uri.getUserInfo())) {
            String[] tokens = uri.getUserInfo().split(":");
            if (tokens.length >= 1) {
                username = tokens[0];
                password = tokens[1];
            }
        }
	}
	
	public HTTP(String host, int port, String username, String password, boolean useSSL)
	{
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.useSSL = useSSL;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getHomeDir()
	 */
	@Override
	public String getHomeDir() {
		return this.homeDir;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getRootDir()
	 */
	@Override
	public String getRootDir() {
		return this.rootDir;
	}	
	
	/* 
	 * This is not supported for http/s
	 */
	@Override
	public void updateSystemRoots(String rootDir, String homeDir)
	{
		this.rootDir = rootDir;
		this.homeDir = homeDir;
	}
	
	/**
	 * Sets the length for a given path. We generally don't reuse this 
	 * class for multiple transfers and directory copies are not supported,
	 * so this is relatively safe.
	 * 
	 * @param remotepath
	 * @param length
	 */
	public void setLength(String remotepath, Long length) {
		if (lengthCache.containsKey(remotepath)) {
			lengthCache.remove(remotepath);
		}
		
		lengthCache.put(remotepath, length);
	}

    @Override
    public int getMaxBufferSize() {
        return MAX_BUFFER_SIZE;
    }

	@Override
	public List<RemoteFilePermission> getAllPermissionsWithUserFirst(
			String path, String username) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public List<RemoteFilePermission> getAllPermissions(String path)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public PermissionType getPermissionForUser(String username, String path)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public boolean hasReadPermission(String path, String username)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public boolean hasWritePermission(String path, String username)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public boolean hasExecutePermission(String path, String username)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void setPermissionForUser(String username, String path,
			PermissionType type, boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void setOwnerPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void setReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void removeReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void setWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void removeWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void setExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void removeExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public void clearPermissions(String username, String path, boolean recursive)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public String getPermissions(String path) throws RemoteDataException
	{
		throw new NotImplementedException("Remote file permissions not supported by HTTP.");
	}

	@Override
	public boolean mkdir(String dir) throws IOException, RemoteDataException
	{
		throw new NotImplementedException("Remote directory creation not supported by HTTP.");
	}

	@Override
	public boolean mkdirs(String dir) throws IOException, RemoteDataException
	{
		throw new NotImplementedException("Remote directory creation not supported by HTTP.");
	}
	
	@Override
    public boolean mkdirs(String remotepath, String authorizedUsername) 
    throws IOException, RemoteDataException 
    {
	    throw new NotImplementedException("Remote directory creation not supported by HTTP.");
    }

	@Override
	public void authenticate() throws RemoteDataException, IOException
	{
		// nothing to do here
	}

	@Override
	public HTTPInputStream getInputStream(String path, boolean passive)
			throws IOException, RemoteDataException
	{
		
		return new HTTPInputStream(this, path, passive);
	}
	
//	/**
//	 * Returns the input stream from a HTTP GET on the path. Auth and ssl are honored.
//	 * 
//	 * @param path
//	 * @return input stream from http get response
//	 * @throws IOException
//	 * @throws RemoteDataException
//	 */
//	protected InputStream getRawInputStream(String path)
//	throws IOException, RemoteDataException
//	{	
//		try 
//		{
//			CloseableHttpResponse response = doGet(path);
//			
//		    StatusLine statusLine = response.getStatusLine();
//	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
//	    		return response.getEntity().getContent();
//	    	} else {
//	    		throw new IOException(statusLine.getReasonPhrase());
//	    	}
//		}
//		catch (IOException e) {
//			throw e;
//		}
//		catch (Exception e) {
//			throw new RemoteDataException("Failed to establish output stream to " + path, e);
//		}
//	}

	@Override
	public RemoteOutputStream<HTTP> getOutputStream(String path, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		throw new NotImplementedException("Remote data creation not supported by HTTP.");
	}

	@Override
	public List<RemoteFileInfo> ls(String path) throws IOException,
			RemoteDataException
	{
		throw new NotImplementedException("Remote directory listing not supported by HTTP.");
	}

	@Override
	public void get(String remotedir, String localdir) throws IOException,
			RemoteDataException
	{
		get(remotedir, localdir, null);
	}

	@Override
	public void get(String remotePath, String localdir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		InputStream in = null;
		OutputStream out = null;
		BufferedOutputStream bout = null;
		CloseableHttpResponse response = null;
		try 
		{
			File localFile = new File(localdir);
				
			// verify local path and explicity resolve target path
			if (!localFile.exists()) {
				if (!localFile.getParentFile().exists()) {
					throw new FileNotFoundException("No such file or directory");
				} 
			} 
			// if not a directory, overwrite local file
			else if (!localFile.isDirectory()) {
				
			}
			// if a directory, resolve full path
			else {
				localFile = new File(localFile,  FilenameUtils.getName(remotePath));
			}
			
			response = doGet(remotePath);
			
		    StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
	    		in = response.getEntity().getContent();
	    	} else if (statusLine.getStatusCode() == 404) {
	    		throw new FileNotFoundException("File or folder ");
	    	} else if (statusLine.getStatusCode() == 401 || statusLine.getStatusCode() == 403) {
	    		throw new RemoteDataException("Failed to get " + remotePath + " due to insufficient privileges.");
	    	} else {
	    		throw new IOException(statusLine.getReasonPhrase());
	    	}
			
	    	out = new FileOutputStream(localFile);
			bout = new BufferedOutputStream(out);
			int length = 0;
			int callbackCount = 0;
			long bytesSoFar = 0;
			byte[] b = new byte[8192];
			
			if (listener != null) listener.started(0, remotePath);
			
			while (( length = in.read(b)) != -1) {
				bytesSoFar += length;
				out.write(b);
				callbackCount++;
				if (listener != null && callbackCount == 4)
				{
					callbackCount = 0;
					listener.progressed(bytesSoFar);
				}
			}
			
			if (listener != null) listener.completed();
			// we could do a size check here...meah
		}
		catch (IOException e) 
		{
			if (listener != null) listener.failed();
			throw e;
		}
		catch (RemoteDataException e) 
		{
			if (listener != null) listener.failed();
			throw e;
		}
		catch (Exception e) 
		{
			if (listener != null) listener.failed();
			String url = null;
			try {
				url = getUriForPath(remotePath).toString();
			} catch (Exception e1) {
				url = remotePath;
			}
			throw new RemoteDataException("Transfer failed from " + url, e);
		}
		finally 
		{
			try { in.close(); } catch(Exception e) {}
			try { out.close(); } catch(Exception e) {}
			try { bout.close(); } catch(Exception e) {}
			try { response.close(); } catch (Exception e) {}
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
	private CloseableHttpClient initClient(URI escapedUri) 
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
	private CloseableHttpResponse doRequest(URI escapedUri, HttpUriRequest httpUriRequest)
	throws IOException, RemoteDataException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		CloseableHttpClient httpclient = initClient(escapedUri);
		
		if (!StringUtils.isEmpty(username)) 
		{
			String nullSafePassword = StringUtils.isEmpty(this.password) ? "" : this.password;
			
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
	 * Makes HTTP POST on path with given form parameters and no headers other than optional auth
	 * @param path
	 * @param mappedFormParameters
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPost(String path, Map<String, String> mappedFormParameters)
	throws IOException, RemoteDataException
	{
		return doPost(path, mappedFormParameters, null);
	}
	
	/**
	 * Makes HTTP POST on path with given form parameters and  headers.
	 * @param path
	 * @param mappedFormParameters
	 * @param mappedHeaders
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPost(String path, Map<String, String> mappedFormParameters, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpPost httpPost = new HttpPost(escapedUri);
			
		    if (mappedFormParameters != null && !mappedFormParameters.isEmpty()) 
		    {
		    	List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		    	for (String key: mappedFormParameters.keySet()) {
		    		nvps.add(new BasicNameValuePair(key, mappedFormParameters.get(key)));
		    	}
		    	
		    	httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		    }
		    
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpPost.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
		    
			return doRequest(escapedUri, httpPost);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to establish output stream to " + path, e);
		}
	}
	
	/**
	 * Makes multipart/form post to path with given form params and files. Only headers are optional auth.
	 * @param path
	 * @param mappedFormParameters
	 * @param localFiles
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPostMultipart(String path, Map<String, String> mappedFormParameters, File[] localFiles)
	throws IOException, RemoteDataException
	{
		return doPostMultipart(path, mappedFormParameters, localFiles, null);
	}
	
	/**
	 * Makes multipart/form post to path with given form params, headers, and files.
	 * @param path
	 * @param mappedFormParameters
	 * @param localFiles
	 * @param mappedHeaders
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	@SuppressWarnings("deprecation")
	public CloseableHttpResponse doPostMultipart(String path, Map<String, String> mappedFormParameters, File[] localFiles, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpPost httpPost = new HttpPost(escapedUri);
			
			// add files
			MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
			
			for (File file: localFiles) {
				if (file == null) {
					throw new FileNotFoundException("Null file specified in array of files");
				} else if (file.isDirectory()) {
					throw new IOException("Directory upload is not supported.");
				} else {
					multipartBuilder.addPart("fileToUpload", new FileBody(file));
				}
			}
			
			// add forms params
		    if (mappedFormParameters != null && !mappedFormParameters.isEmpty()) 
		    {
		    	for (String key: mappedFormParameters.keySet()) {
		    		multipartBuilder.addPart(key, new StringBody(mappedFormParameters.get(key)));
		    	}
		    }
		    
		    httpPost.setEntity(multipartBuilder.build());
		    
		    // add headers
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpPost.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
		    
		    // make request
			return doRequest(escapedUri, httpPost);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to post multipart form to " + path, e);
		}
	}
	
	/**
	 * Makes HTTP POST with serialized JSON and content type of application/json and no headers other
	 * than optional auth.
	 * @param path
	 * @param json
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPostJson(String path, JsonNode json)
	throws IOException, RemoteDataException
	{
		return doPostJson(path, json, null);
	}
	
	/**
	 * Makes HTTP POST with serialized JSON and content type of application/json and given headers. 
	 * @param path
	 * @param json
	 * @param mappedHeaders
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPostJson(String path, JsonNode json, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpPost httpPost = new HttpPost(escapedUri);
			
			httpPost.setEntity(new StringEntity(
				    json == null ? "" : json.toString(),
				    "application/json"));
			
			// add headers
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpPost.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
		    
		    // make request
			return doRequest(escapedUri, httpPost);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to post multipart form to " + path, e);
		}
	}

	/**
	 * Makes HTTP PUT on path with given form parameters and no headers other than optional auth
	 * @param path
	 * @param mappedFormParameters
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPut(String path, Map<String, String> mappedFormParameters)
	throws IOException, RemoteDataException
	{
		return doPut(path, mappedFormParameters, null);
	}
	
	/**
	 * Makes HTTP PUT on path with given form parameters and  headers.
	 * @param path
	 * @param mappedFormParameters
	 * @param mappedHeaders
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doPut(String path, Map<String, String> mappedFormParameters, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpPut httpPut = new HttpPut(escapedUri);
			
		    if (mappedFormParameters != null && !mappedFormParameters.isEmpty()) 
		    {
		    	List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		    	for (String key: mappedFormParameters.keySet()) {
		    		nvps.add(new BasicNameValuePair(key, mappedFormParameters.get(key)));
		    	}
		    	
		    	httpPut.setEntity(new UrlEncodedFormEntity(nvps));
		    }
		    
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpPut.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
		    
			return doRequest(escapedUri, httpPut);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to establish output stream to " + path, e);
		}
	}
	
	/**
	 * Makes HTTP get on a remote path. No headers other than auth if needed.
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doGet(String path)
	throws IOException, RemoteDataException
	{
		return doGet(path, null);
	}
	
	/**
	 * Makes HTTP get on a remote path using given headers.
	 * @param path
	 * @param mappedHeaders
	 * @return
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doGet(String path, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(path)) {
			throw new IOException("No path to the remote file provided.");
		}
		
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpGet httpGet = new HttpGet(escapedUri);
		    
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpGet.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
			
			return doRequest(escapedUri, httpGet);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to establish output stream to " + path, e);
		}
	}
	
	/**
	 * Makes HTTP delete on path with no headers other than optional auth.
	 * @param path
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doDelete(String path)
	throws IOException, RemoteDataException
	{
		return doDelete(path, null);
	}
	
	/**
	 * Makes HTTP delete on path with supplied headers.
	 * @param path
	 * @return raw http response object with entity in tact
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	public CloseableHttpResponse doDelete(String path, Map<String, String> mappedHeaders)
	throws IOException, RemoteDataException
	{
		try 
		{
			URI escapedUri = getUriForPath(path);
			
			HttpDelete httpDelete = new HttpDelete(escapedUri);
		    
		    if (mappedHeaders != null && !mappedHeaders.isEmpty()) 
		    {
		    	for (String key: mappedHeaders.keySet()) {
		    		httpDelete.addHeader(key, mappedHeaders.get(key));
		    	}
		    }
			
			return doRequest(escapedUri, httpDelete);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to establish output stream to " + path, e);
		}
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
     */
    @Override
    public void append(String localpath, String remotepath) throws IOException,
    RemoteDataException
    {
        append(localpath, remotepath, null);
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
     */
    @Override
    public void append(String localpath, String remotepath, RemoteTransferListener listener)  
    throws IOException, RemoteDataException
    {
        throw new NotImplementedException("Remote file upload not supported by HTTP.");
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#put(java.lang.String, java.lang.String)
	 */
	@Override
	public void put(String localdir, String remotedir) throws IOException,
			RemoteDataException
	{
	    put(localdir, remotedir, null);
	}

	/**
	 * Performs a file upload to the remote path.
	 * @param localdir local path
	 * @param remotedir remtoe path
	 * @param listener remotetransfer listener
	 */
	@Override
	public void put(String localdir, String remotedir,
			RemoteTransferListener listener) throws IOException,
			RemoteDataException
	{
		
		throw new NotImplementedException("Remote file upload not supported by HTTP.");
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#syncToRemote(java.lang.String, java.lang.String, org.iplantc.service.transfer.RemoteTransferListener)
	 */
	@Override
	public void syncToRemote(String absolutePath, String parent, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		put(absolutePath, parent, listener);
	}


	@Override
	public boolean isDirectory(String path) throws IOException,
			RemoteDataException
	{
		return false;
		//throw new NotImplementedException("Remote file info not supported by HTTP.");
	}

	@Override
	public boolean isFile(String path) throws IOException, RemoteDataException
	{
		return true;
		//throw new NotImplementedException("Remote file info not supported by HTTP.");
	}

	@Override
	public long length(String remotepath) throws IOException,
			RemoteDataException
	{
		// return from the cache first
		if (lengthCache.containsKey(remotepath)) {
			return lengthCache.get(remotepath);
		}
		
		// attempt options request to get file size
		if (StringUtils.isEmpty(remotepath)) {
			throw new IOException("No path to the remote file provided.");
		}
		
		// otherwise fall back on a HEAD request, which will fail for 
		// many CDN and cloud providers.
		try 
		{
			URI escapedUri = getUriForPath(remotepath);
			
			HttpOptions httpOptions = new HttpOptions(escapedUri);
		    
			CloseableHttpResponse response = doRequest(escapedUri, httpOptions);
			
			StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) 
	    	{
	    		Header header = response.getFirstHeader("Content-Length");
				
				if (header != null) {
					Long length = NumberUtils.toLong(header.getValue(), -1);
					lengthCache.put(remotepath, length);
					return length;
				} else {
					return -1;
				}
	    	} else if (statusLine.getStatusCode() == 404) {
	    		throw new FileNotFoundException("File or folder not found");
	    	} else if (statusLine.getStatusCode() == 401 || statusLine.getStatusCode() == 403) {
	    		return -1;
//	    		throw new RemoteDataException("Failed to get " + remotepath + " due to insufficient privileges.");
	    	} else {
	    		return -1;
//	    		throw new IOException(statusLine.getReasonPhrase());
	    	}
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to fetch length of " + remotepath, e);
		}
	}

	@Override
	public String checksum(String remotepath) throws IOException,
			RemoteDataException, NotImplementedException
	{
		throw new NotImplementedException();
		
	}

	@Override
	public void doRename(String oldpath, String newpath) throws IOException,
			RemoteDataException
	{
		throw new NotImplementedException();
		
	}

	@Override
	public void copy(String remotedir, String localdir) throws IOException,
			RemoteDataException
	{
		throw new NotImplementedException();
		
	}

	@Override
	public void copy(String remotedir, String localdir,
			RemoteTransferListener listener) throws IOException,
			RemoteDataException
	{
		throw new NotImplementedException("Remote copies not supported by HTTP");
	}

	private URI _getUriForPath(String path) throws IOException, RemoteDataException
	{
		URI escapedUri;
		try
		{
			URI url = getUriForPath(path);
			escapedUri = new URI(url.getScheme(), 
									url.getUserInfo(), 
									url.getHost(), 
									url.getPort(), 
									url.getPath(), 
									url.getQuery(),
									null);
			return escapedUri;
		}
		catch (URISyntaxException e)
		{
			throw new FileNotFoundException("Unable to parse path into a URI");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getUriForPath(java.lang.String)
	 */
	@Override
	public URI getUriForPath(String path) 
	throws IOException, RemoteDataException
	{
		StringBuilder builder = new StringBuilder();
		builder.append(useSSL ? "https://" : "http://");
		builder.append(host);
		if ((port == 443 && useSSL) || (port == 80 && !useSSL)) {
			// no port needed
		} else if (port == -1 && useSSL){
			builder.append(":443");
		} else if (port == -1 && !useSSL) {
			builder.append(":80");
		} else {
			builder.append(":" + port);
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		builder.append(path.replaceAll(" ", "%20"));
		try {
			return new URI(builder.toString());
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		
	}

	@Override
	public void delete(String path) throws IOException, RemoteDataException
	{
		throw new NotImplementedException("Remote delete not supported by HTTP.");
	}

	@Override
	public boolean isThirdPartyTransferSupported()
	{
		return false;
	}

	@Override
	public void disconnect() {}

	@Override
	public boolean doesExist(String string) throws IOException,
			RemoteDataException
	{
		throw new NotImplementedException();
	}

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return false;
	}

	@Override
	public String resolvePath(String path) throws FileNotFoundException
	{
		return path;
	}

	@Override
	public RemoteFileInfo getFileInfo(String path) throws RemoteDataException,
			IOException
	{
		throw new NotImplementedException();
	}

	@Override
	public String getUsername()
	{
		return username;
	}

	@Override
	public String getHost()
	{
		return host;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HTTP other = (HTTP) obj;
		if (host == null)
		{
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
			return false;
		if (password == null)
		{
			if (other.password != null)
				return false;
		}
		else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (uri == null)
		{
			if (other.uri != null)
				return false;
		}
		else if (!uri.equals(other.uri))
			return false;
		if (useSSL != other.useSSL)
			return false;
		if (username == null)
		{
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		return true;
	}	
}
