package org.iplantc.service.systems.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Class to hold information about proxy servers needed to access actual 
 * remote systems. This is currently only applicable to SSH and SFTP systems.
 * @author dooley
 *
 */
@Entity
@Table(name = "proxyservers")
public class ProxyServer {

	private Long id;
	private String name;
	private String host;
	private int port = 22;
	private RemoteConfig remoteConfig;
	
	public ProxyServer(String host, int port) 
	{
		this(null, host, port);
	}
	
	public ProxyServer(String name, String host, int port) 
	{
		setName(name);
		setPort(port);
		setHost(host);
	}

	public ProxyServer() {}
	
	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the host
	 */
	@Column(name = "`host`", nullable = false, length = 256)
	public String getHost()
	{
		return host;
	}

	/**
	 * @param host the host to set
	 * @throws SystemException 
	 */
	public void setHost(String host)
	{
		if (!StringUtils.isEmpty(host) && host.length() > 256) {
			throw new SystemException("'system.config.proxy.host' must be less than 256 characters.");
		}
		
		this.host = host;
	}

	/**
	 * @return the port
	 */
	@Column(name = "`port`")
	public int getPort()
	{
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port)
	{
		this.port = port;
	}
	
	/**
	 * @return the name
	 */
	@Column(name = "name", nullable = true, length = 64)
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 * @throws SystemException 
	 */
	public void setName(String name)
	{
		if (!StringUtils.isEmpty(name) && name.length() > 64) {
			throw new SystemException("'system.config.proxy.name' must be less than 64 characters.");
		}
		
		this.name = name;
	}
	
	/**
	 * @return the remoteConfig
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remote_config_id")
	public RemoteConfig getRemoteConfig()
	{
		return remoteConfig;
	}

	/**
	 * @param remoteConfig the remoteConfig to set
	 */
	public void setRemoteConfig(RemoteConfig remoteConfig)
	{
		this.remoteConfig = remoteConfig;
	}

	/**
	 * Serializes this ProxyServer into a JSON string.
	 * 
	 * @return String json representation of this ProxyServer
	 */
	public String toJSON()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object()
				.key("name").value(this.name)
				.key("host").value(this.host)
				.key("port").value(this.port)
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing ProxyServer JSON output.");
		}

		return output;
	}
	
	/**
	 * Deserializes a json object into a ProxyServer. The resulting object is
	 * validated before sending back.
	 * 
	 * @param jsonSystem
	 * @return ProxyServer
	 * @throws SystemException
	 */
	public static ProxyServer fromJSON(JSONObject jsonSystem) throws SystemException
	{
		return fromJSON(jsonSystem, null);
	}
	
	/**
	 * Deserializes a json object into a ProxyServer. If the provided ProxyServer is
	 * not null, it is updated with the values from the json object. The resulting object is
	 * validated before sending back.
	 * 
	 * @param jsonSystem
	 * @param proxyServer
	 * @return ProxyServer
	 * @throws SystemException
	 */
	public static ProxyServer fromJSON(JSONObject jsonSystem, ProxyServer proxyServer) throws SystemException
	{
		if (proxyServer == null) {
			proxyServer = new ProxyServer();
		}
		
		try {
			if (ServiceUtils.isNonEmptyString(jsonSystem, "name")) {
				proxyServer.setName(jsonSystem.getString("name"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'storage.proxy.name' field.");
			}
			
			if (ServiceUtils.isNonEmptyString(jsonSystem, "host")) {
				proxyServer.setHost(jsonSystem.getString("host"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'storage.proxy.host' field.");
			}
			
			if (jsonSystem.has("port") && !jsonSystem.isNull("port"))
			{
				try 
				{
					int portNumber = jsonSystem.getInt("port");
					if (portNumber > 0) {
						proxyServer.setPort(portNumber);
					} else {
						throw new SystemArgumentException("Invalid 'storage.proxy.port' value. " +
								"If provided, please specify a positive integer value for the port number.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'storage.proxy.port' value " + jsonSystem.get("port") + ". " +
							"If provided, please specify a positive integer value for the port number.");
				}
			}
		}
		catch (JSONException e) {
			throw new SystemException("Failed to parse proxy server description.", e);
		}
		catch (SystemArgumentException e) {
			throw new SystemException("Failed to parse proxy server description. " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new SystemException("Failed to parse proxy server description. " + e.getMessage(), e);
		}
		
		return proxyServer;
	}

	/* 
	 * Creates a shallow clone of this CredentialServer.
	 */
	public ProxyServer clone()
	{
		ProxyServer proxyServer = new ProxyServer();
		proxyServer.name = getName();
		proxyServer.host = getHost();
		proxyServer.port = getPort();
		return proxyServer;
	}
	
}
