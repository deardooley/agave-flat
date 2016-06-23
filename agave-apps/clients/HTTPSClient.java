package org.iplantc.service.clients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import com.thoughtworks.xstream.core.util.Base64Encoder;

public class HTTPSClient {
	protected String			url;
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

	public HTTPSClient(String url, Map<String, String> headers)
	{
		this(url, null, null, headers);
	}

	public HTTPSClient(String url, String username, String password)
	{
		this(url, username, password, new HashMap<String, String>());
	}

	public HTTPSClient(String url, String username, String password,
			Map<String, String> headers)
	{
		this.url = url;
		this.username = username;
		this.password = password;
		this.headers = headers;

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

	// This function is called periodically, the important thing
	// to note here is that there is no special code that needs to
	// be added to deal with a "HTTPS" URL. All of the trust
	// management, verification, is handled by the HttpsURLConnection.
	public String getText() throws Exception
	{
		return getText(url);
	}

	public String getText(String endpoint) throws Exception
	{
		String content = "";
		try
		{
			URLConnection urlCon = ( new URL(endpoint) ).openConnection();
			
			Base64Encoder encoder = new Base64Encoder();
			String userpass = username + ":" + password;
			String encoded = encoder.encode(userpass.getBytes());
			urlCon.setRequestProperty("Authorization", "Basic " + encoded);
			if (!headers.isEmpty())
			{
				for (String key : headers.keySet())
				{
					urlCon.setRequestProperty(key, headers.get(key));
				}
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(urlCon
					.getInputStream()));
			String line;
			while ( ( line = in.readLine() ) != null)
			{
				content += line + "\n";
			}
		}
		catch (MalformedURLException mue)
		{
			throw mue;
		}
		catch (IOException ioe)
		{
			throw ioe;
		}
		catch (Exception e)
		{
			throw e;
		}

		return content;
	}

	public static void main(String[] args)
	{

		HTTPSClient httpsTest = new HTTPSClient(
				"https://info.teragrid.org:8444/web-apps/csv/profile-v1/", "",
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

	public Map<String, List<String>> getHeaderResponse() throws Exception
	{
		try
		{
			URLConnection urlCon = ( new URL(url) ).openConnection();
			HttpsURLConnection httpsUrlCon = (HttpsURLConnection) urlCon;

			Base64Encoder encoder = new Base64Encoder();
			String userpass = username + ":" + password;
			String encoded = encoder.encode(userpass.getBytes());
			httpsUrlCon.setRequestProperty("Authorization", "Basic " + encoded);
			if (!headers.isEmpty())
			{
				for (String key : headers.keySet())
				{
					httpsUrlCon.setRequestProperty(key, headers.get(key));
				}
			}

			return httpsUrlCon.getHeaderFields();

		}
		catch (MalformedURLException mue)
		{
			throw mue;
		}
		catch (IOException ioe)
		{
			throw ioe;
		}
		catch (Exception e)
		{
			throw e;
		}
	}

}
