package org.iplantc.service.metadata.util;

import com.thoughtworks.xstream.core.util.Base64Encoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class HTTPSClient {
	private String				url;
	private String				username;
	private String				password;
	private Map<String, String>	headers;

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
	}

	// This function is called periodically, the important thing
	// to note here is that there is no special code that needs to
	// be added to deal with a "HTTPS" URL. All of the trust
	// management, verification, is handled by the HttpsURLConnection.
	public String getText() throws Exception
	{
		String content = "";
		try
		{
			URLConnection urlCon = ( new URL(url) ).openConnection();
			
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
				"https://info.teragrid.org:8444/web-apps/json/profile-v1/", "",
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
