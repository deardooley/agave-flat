package org.iplantc.service.jobs.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceUtils 
{
	private static final Logger log = Logger.getLogger(ServiceUtils.class);

	public static boolean isValid(String value)
	{
		return ( value != null ) && !value.equals("");
	}

	public static boolean isValid(File value)
	{
		return ( value != null ) && value.exists();
	}

	public static boolean isValid(Collection<?> value)
	{
		return ( value != null ) && ( !value.isEmpty() );
	}

	public static boolean isValid(Map<?,?> value)
	{
		return ( value != null ) && ( value.size() > 0 );
	}

	public static boolean isValid(Calendar value)
	{
		return value != null;
	}

	public static boolean isValid(Date value)
	{
		return value != null;
	}

	public static boolean isValid(Long value)
	{
		return value != null && value.intValue() >= 0;
	}

	public static boolean isValid(Integer value)
	{
		return value != null && value.intValue() >= 0;
	}
	
	/**
	 * Returns the current local IP address or an empty string in error case /
	 * when no network connection is up.
	 * <p>
	 * The current machine could have more than one local IP address so might
	 * prefer to use {@link #getAllLocalIPs() } or
	 * {@link #getAllLocalIPs(java.lang.String) }.
	 * <p>
	 * If you want just one IP, this is the right method and it tries to find
	 * out the most accurate (primary) IP address. It prefers addresses that
	 * have a meaningful dns name set for example.
	 * 
	 * @return Returns the current local IP address or an empty string in error
	 *         case.
	 * @since 0.1.0
	 */
	public static String getLocalIP()
	{
		String ipOnly = "";
		try
		{
			Enumeration<NetworkInterface> nifs = NetworkInterface
					.getNetworkInterfaces();
			if (nifs == null)
				return "";
			while (nifs.hasMoreElements())
			{
				NetworkInterface nif = nifs.nextElement();
				// We ignore subinterfaces - as not yet needed.

				if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual())
				{
					Enumeration<InetAddress> adrs = nif.getInetAddresses();
					while (adrs.hasMoreElements())
					{
						InetAddress adr = adrs.nextElement();
						if (adr != null
								&& !adr.isLoopbackAddress()
								&& ( nif.isPointToPoint() || !adr
										.isLinkLocalAddress() ))
						{
							String adrIP = adr.getHostAddress();
							String adrName;
							if (nif.isPointToPoint()) // Performance issues getting hostname for mobile internet sticks
								adrName = adrIP;
							else
								adrName = adr.getCanonicalHostName();

							if (!adrName.equals(adrIP))
								return adrIP;
							else
								ipOnly = adrIP;
						}
					}
				}
			}
			if (ipOnly.length() == 0)
				log.warn("No IP address available");
			return ipOnly;
		}
		catch (SocketException ex)
		{
			log.warn("No IP address available", ex);
			return "";
		}
	}

	/**
	 * Returns local ip in the simplest manner.
	 * 
	 * @return
	 * @deprecated
	 */
	public static String getIP()
	{
		InetAddress ip;
		try
		{
			ip = InetAddress.getLocalHost();
			return ip.getHostAddress();
		}
		catch (UnknownHostException e)
		{
			return getLocalIP();
		}
	}
	
	public static void main(String[] args) {
		System.out.println(ServiceUtils.getIP() + "\n");
		System.out.println(ServiceUtils.getLocalIP());
	}
	
	/**
	 * Trims leading zeros from string
	 * 
	 * @param token
	 * @return
	 */
	public static String trimLeadingZeros(String token) {
		while (StringUtils.startsWith(token, "0")) {
			token = StringUtils.removeStart(token, "0");
		}
		return token;
	}
	
	/**
	 * Wraps a string in properly escaped quotation marks.
	 * @param s
	 * @return
	 */
	public static String enquote(String s) {
		if (StringUtils.isEmpty(s)) {
			s = "";
		} 
		return "\""+s+"\"";
	}
	
	/**
	 * Checks whether a string is a valid JSON array of value nodes.
	 * This is used primarily when checking for job input arrays to enquote.
	 * 
	 * @param sJson String to check
	 * @return True if a json array with only value nodes
	 */
	public static boolean isJsonArrayOfStrings(final String sJson) 
	{
		try 
		{
			final JsonNode json = new ObjectMapper().readTree(sJson);
			if (json.isArray())
			{
				for(Iterator<JsonNode> iter = json.elements(); iter.hasNext();) 
				{
					JsonNode child = iter.next();
					if (!child.isValueNode()) {
						return false;
					}
				}
			} 
			else
			{
	    	  return false;
			}
	      
			return true;
		} 
		catch (Exception jpe) {
			return false;
		}
	}
	
	/**
	 * Returns the values from a JSON array of primary types as a String array.
	 * 
	 * @param sJson 
	 * @return String array of values from the JSON array. Any primary types will get converted here.
	 * @throws JsonParseException
	 * @throws IOException
	 */
	public static String[] getStringValuesFromJsonArray(final String sJson, final boolean enquoteValues)
	throws JsonParseException, IOException
	{
		try 
		{
			final JsonNode json = new ObjectMapper().readTree(sJson);
			if (json.isArray())
			{
				String[] arrayValues = new String[json.size()];
				for(int i=0; i<json.size(); i++) 
				{
					JsonNode child = json.get(i);
					if (child.isValueNode()) {
						if (enquoteValues) {
							arrayValues[i] = ServiceUtils.enquote(child.asText());
						} else {
							arrayValues[i] = child.asText();
						}
					}
				}
				return arrayValues;
			} 
			else
			{
	    	  throw new JsonParseException("Value is not a valid JSON array of primary values.", new JsonLocation(sJson, sJson.length(), 1, 1));
			}
		} 
		catch (JsonParseException e) {
			throw e;
		}
		catch (JsonProcessingException e) {
			throw e;
		}
	}
}
