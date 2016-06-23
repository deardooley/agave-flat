/**
 * 
 */
package org.iplantc.service.systems.model;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.common.auth.MyProxyClient;
import org.iplantc.service.common.auth.TrustedCALocation;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.CredentialServerProtocolType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "credentialservers")
public class CredentialServer implements LastUpdatable {

	public static final CredentialServer IPLANT_MYPROXY = 
			new CredentialServer(	"Agave MyProxy Server", 
										Settings.IPLANT_MYPROXY_SERVER, 
										Settings.IPLANT_MYPROXY_PORT, 
										CredentialServerProtocolType.MYPROXY);

	private Long					id;
	private String					name;
	private String 					endpoint;
	private int						port;
	private CredentialServerProtocolType 		protocol;
	private Date 					lastUpdated = new Date();
	private Date 					created = new Date();
	
	public CredentialServer() {}
	
	public CredentialServer(String name, String endpoint, int port, CredentialServerProtocolType authProtocol) 
	{
		setName(name);
		setEndpoint(endpoint);
		setPort(port);
		setProtocol(authProtocol);
	}
	
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
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}
	
	/**
	 * @return the endpoint
	 */
	@Column(name = "endpoint", nullable = false, length = 255)
	public String getEndpoint()
	{
		return endpoint;
	}

	/**
	 * @param endpoint the endpoint to set
	 * @throws SystemException 
	 */
	public void setEndpoint(String endpoint)
	{
		if (StringUtils.length(endpoint) > 255) {
			throw new SystemException("'auth.server.endpoint' must be less than 255 characters.");
		}
		
		this.endpoint = endpoint;
	}
	
	/**
	 * @return the name
	 */
	@Column(name = "`name`", nullable = false, length = 64)
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
		if (StringUtils.length(name) > 256) {
			throw new SystemException("'auth.server.name' must be less than 64 characters.");
		}
		
		if (!StringUtils.isEmpty(name) && !name.equals(name.replaceAll( "[^0-9a-zA-Z\\.\\-_ ]" , ""))) {
			throw new SystemException("'auth.server.name' may only contain alphanumeric characters, spaces, periods, and dashes.");
		}
		
		this.name = name;
	}

	/**
	 * @return the port
	 */
	@Column(name = "`port`")
	public Integer getPort()
	{
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(Integer port)
	{
		this.port = port;
	}

	/**
	 * @return the authProtocol
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "protocol", nullable = false, length = 16)
	public CredentialServerProtocolType getProtocol()
	{
		return protocol;
	}

	/**
	 * @param authProtocol the authProtocol to set
	 */
	public void setProtocol(CredentialServerProtocolType authProtocol)
	{
		this.protocol = authProtocol;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ( ( endpoint == null ) ? 0 : endpoint.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result
				+ ( ( protocol == null ) ? 0 : protocol.hashCode() );
		return result;
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
		CredentialServer other = (CredentialServer) obj;
		if (endpoint == null)
		{
			if (other.endpoint != null)
				return false;
		}
		else if (!endpoint.equals(other.endpoint))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (protocol != other.protocol)
			return false;
		return true;
	}

	public String toString()
	{
		return this.name;
	}

	/**
	 * Retrieves a new credential using the the current protocol and supplied
	 * username and password.
	 * 
	 * @param username
	 * @param password
	 * @return Object a credential. Generally this will be a GSSCredential until we support other protocols.
	 * @throws NotYetImplementedException if the protocol is KERBEROSE or OAUTH2.
	 * @throws Exception
	 */
	public Object retrieveCredential(String username, char[] password) throws Exception
	{
		switch (protocol) 
		{
			case MYPROXY:
				File tempDir = new File(System.getProperty("java.io.tmpdir")
						+ File.separator + DigestUtils.md5Hex(getEndpoint()));
				
				TrustedCALocation trustedCALocation = new TrustedCALocation(tempDir.getAbsolutePath());
				return MyProxyClient.getCredential(endpoint, port, username, new String(password), trustedCALocation.getCaPath());
			case KERBEROSE:
				throw new NotYetImplementedException("Remote KERBEROSE support is not yet implemented");
			case OAUTH2:
				throw new NotYetImplementedException("Remote OAUTH2 support is not yet implemented");
			default:
				return null;
		}
	}

	/**
	 * Serializes this CredentialServer into a JSON string.
	 * 
	 * @return String json representation of this CredentialServer
	 */
	public String toJSON()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object()
				.key("name").value(this.name)
				.key("endpoint").value(this.endpoint)
				.key("port").value(this.port)
				.key("protocol").value(getProtocol().name())
				//.key("lastModified").value(new DateTime(this.lastUpdated).toString())
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing AuthenticationSystem JSON output.");
		}

		return output;
	}
	
	/**
	 * Deserializes a json object into a CredentialServer. The resulting object is
	 * validated before sending back.
	 * 
	 * @param jsonSystem
	 * @return
	 * @throws SystemException
	 */
	public static CredentialServer fromJSON(JSONObject jsonSystem) throws SystemException
	{
		return fromJSON(jsonSystem, null);
	}
	
	/**
	 * Deserializes a json object into a CredentialServer. If the provided CredentialServer is
	 * not null, it is updated with the values from the json object. The resulting object is
	 * validated before sending back.
	 * 
	 * @param jsonSystem
	 * @param credentialServer
	 * @return
	 * @throws SystemException
	 */
	public static CredentialServer fromJSON(JSONObject jsonSystem, CredentialServer credentialServer) throws SystemException
	{
		if (credentialServer == null) {
			credentialServer = new CredentialServer();
		}
		
		try {
			if (ServiceUtils.isNonEmptyString(jsonSystem, "name")) {
				credentialServer.setName(jsonSystem.getString("name"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'auth.server.name' field.");
			}
			
			if (ServiceUtils.isNonEmptyString(jsonSystem, "endpoint")) {
				credentialServer.setEndpoint(jsonSystem.getString("endpoint"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'auth.server.endpoint' field.");
			}
			
			if (jsonSystem.has("port") && !jsonSystem.isNull("port"))
			{
				try 
				{
					int portNumber = jsonSystem.getInt("port");
					if (portNumber > 0) {
						credentialServer.setPort(portNumber);
					} else {
						throw new SystemArgumentException("Invalid 'auth.server.port' value. " +
								"If provided, please specify a positive integer value for the port number.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'auth.server.port' value " + jsonSystem.get("port") + ". " +
							"If provided, please specify a positive integer value for the port number.");
				}
			}
			else
			{
				credentialServer.setPort(-1);
			}
			
			if (jsonSystem.has("protocol") && !StringUtils.isEmpty(jsonSystem.getString("protocol"))) {
				try {
					CredentialServerProtocolType protocolType = CredentialServerProtocolType.valueOf(jsonSystem.getString("protocol").toUpperCase());
					credentialServer.setProtocol(protocolType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'auth.server.protocol' value " + 
							jsonSystem.getString("protocol") + ". Please specify one of: " +
							"'" + ServiceUtils.explode("', '", Arrays.asList(CredentialServerProtocolType.values())) + "'");
				}
				
			} else {
				throw new SystemArgumentException("No 'auth.server.protocol' value specified. " +
						"Please specify one of: '" + ServiceUtils.explode("', '", Arrays.asList(CredentialServerProtocolType.values())) + "'");
			}
			
		}
		catch (JSONException e) {
			throw new SystemException("Failed to parse ExecutionSystem.", e);
		}
		catch (SystemArgumentException e) {
			throw new SystemException("Failed to parse ExecutionSystem. " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new SystemException("Failed to parse ExecutionSystem: " + e.getMessage(), e);
		}
		
		return credentialServer;
	}

	/* 
	 * Creates a shallow clone of this CredentialServer.
	 */
	public CredentialServer clone()
	{
		CredentialServer system = new CredentialServer();
		system.name = name;
		system.endpoint = endpoint;
		system.port = port;
		system.protocol = protocol;
		return system;
	}

}
