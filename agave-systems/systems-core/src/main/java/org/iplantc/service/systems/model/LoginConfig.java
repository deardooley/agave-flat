/**
 * 
 */
package org.iplantc.service.systems.model;

import java.io.IOException;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains the information needed to connect to login to a remote
 * system.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "loginconfigs")
@PrimaryKeyJoinColumn(name="id")
public class LoginConfig extends RemoteConfig implements LastUpdatable  
{
	private LoginProtocolType 		protocol;
	
	public LoginConfig() {}
	
	public LoginConfig(String host, int port, LoginProtocolType protocol, AuthConfig authConfig)
	throws SystemArgumentException 
	{
		setHost(host);
		setPort(port);
		addAuthConfig(authConfig);
		setProtocol(protocol);
	}
	
	/**
	 * @return the protocol
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "protocol", nullable = false, length = 16)
	public LoginProtocolType getProtocol()
	{
		return protocol;
	}
	/**
	 * @param type the protocol to set
	 */
	public void setProtocol(LoginProtocolType type)
	{
		this.protocol = type;
	}
	
	public static LoginConfig fromJSON(JSONObject jsonConfig) throws SystemArgumentException
	{
		return fromJSON(jsonConfig, null);
	}
	
	public static LoginConfig fromJSON(JSONObject jsonConfig, LoginConfig config) throws SystemArgumentException
	{
		if (config  == null) {
			config = new LoginConfig();
		}
		
		try {
			
			if (ServiceUtils.isNonEmptyString(jsonConfig, "host")) {
				config.setHost(jsonConfig.getString("host"));
			} else {
				throw new SystemArgumentException("Please specify a valid string " +
						"value for the 'login.host' field.");
			}
			
			if (jsonConfig.has("port") && !jsonConfig.isNull("port") )
			{
				try 
				{
					int portNumber = jsonConfig.getInt("port");
					if (portNumber > 0) {
						config.setPort(portNumber);
					} else {
						throw new SystemArgumentException("Invalid 'login.port' value. " +
								"If provided, please specify a positive integer value for the port number.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'login.port' value. " +
							"If provided, please specify a positive integer value for the port number.");
				}
			}
			else if (jsonConfig.has("protocol") && 
					!StringUtils.equalsIgnoreCase(jsonConfig.getString("protocol"), LoginProtocolType.LOCAL.name()))
			{
				throw new SystemArgumentException("Invalid 'login.port' value. " +
						"If provided, please specify a positive integer value for the port number.");
			}
			else
			{
				config.setPort(null);
			}
			
			if (jsonConfig.has("protocol") && !StringUtils.isEmpty(jsonConfig.getString("protocol"))) {
				try {
					LoginProtocolType protocolType = LoginProtocolType.valueOf(jsonConfig.getString("protocol").toUpperCase());
					config.setProtocol(protocolType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'login.protocol' value. Please specify one of: " + 
							ServiceUtils.explode(",",Arrays.asList(LoginProtocolType.values())));
				}
				
			} else {
				throw new SystemArgumentException("No 'login.protocol' value specified. " +
						"Please specify one of: " + LoginProtocolType.values());
			}
			
			if (jsonConfig.has("proxy") && !jsonConfig.isNull("proxy"))
			{
				if (config.getProtocol().equals(LoginProtocolType.SSH)) 
				{
					JSONObject jsonProxy = jsonConfig.getJSONObject("proxy");
					
					if (jsonProxy == null) {
						throw new SystemArgumentException("Invalid 'login.proxy' value. Please specify a " +
								"JSON object representing a valid 'login.proxy' configuration.");
					}
					else
					{
						ProxyServer proxyServer = ProxyServer.fromJSON(jsonProxy, config.getProxyServer());
						proxyServer.setRemoteConfig(config);
						config.setProxyServer(proxyServer);
					}
				}
				else
				{
					throw new SystemArgumentException("Proxy servers are not supported when using " + 
							config.getProtocol().name() +
							". To specify a proxy server you must change 'login.protocol' to 'SSH'.");
				}
			}
			else
			{
				config.setProxyServer(null);
			}
			
			if (jsonConfig.has("auth"))
			{
				JSONObject jsonAuth = jsonConfig.getJSONObject("auth");
				
				if (jsonAuth == null) {
					if (config.getDefaultAuthConfig() == null) {
						throw new SystemArgumentException("Invalid 'login.auth' value. Please specify a " +
								"JSON object representing a valid 'login.auth' configuration.");
					} else {
						// reusing the existing auth config.
					}
				}
				else
				{
//					// delete old default auth config
//					AuthConfig defaultAuthConfig = config.getDefaultAuthConfig();
//					if (defaultAuthConfig != null) {
//						config.getAuthConfigs().remove(defaultAuthConfig);
//					}
					
					// add new one
					AuthConfig defaultAuthConfig = config.getDefaultAuthConfig();
					AuthConfig authConfig = AuthConfig.fromJSON(jsonAuth, defaultAuthConfig);
					authConfig.setSystemDefault(true);
					authConfig.setRemoteConfig(config);
					if (defaultAuthConfig == null) {
						config.addAuthConfig(authConfig);
					}
					//TODO: should we verify here that any existing auth configs are still valid after this update?
				}
			} 
			else if (config.getDefaultAuthConfig() == null
					&& !config.getProtocol().equals(LoginProtocolType.LOCAL)){ 
				throw new SystemArgumentException("No 'login.auth' value specified. Please specify a " +
							"JSON object representing a valid 'login.auth' configuration.");
			}
		}
		catch (JSONException e) {
			throw new SystemArgumentException("Failed to parse 'login' object", e);
		}
		catch (Exception e) {
			throw new SystemArgumentException("Failed to parse 'login' object: " + e.getMessage(), e);
		}
		
		return config;
	}

	public LoginConfig clone() 
	{
		LoginConfig config = new LoginConfig();
		config.host = host;
		config.port = port;
		
//		if (!authConfigs.isEmpty())
//		{
//			for(AuthConfig ac: this.authConfigs) 
//			{
//				config.getAuthConfigs().add(ac.clone());
//			}
//		}
		
		config.protocol = protocol;
		
		if (proxyServer != null)
			config.proxyServer = proxyServer.clone();
		
		return config;
	}
	

	@Override
	public boolean testConnection() throws IOException
	{
		throw new NotImplementedException();
	}
	
	@Override
	@Transient
	public String getType()
	{
		return "login";
	}
}
