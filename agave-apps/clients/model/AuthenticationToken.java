package org.iplantc.service.clients.model;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.codec.digest.DigestUtils;
import org.iplantc.service.auth.exceptions.AuthenticationTokenException;
import org.json.JSONStringer;

public class AuthenticationToken {
	  
	private Long 		id;
	private String 		username;			// api user for whom this token is valid
	private String		createdBy;			// api user who created this token
	private String		internalUsername;	// username of internal user creaded by the api user username
	private String 		token;				// nonce
	private String		ipAddress;			// ip address calling the service
	private Date		lastRenewal;		// last time the token was extended
	private int			remainingUses = -1;	// number of times the token can be used, -1 is infinity
	private Date		created;			// when was this token created
	private Date		expirationDate;		// when does this token expire, default 2 hours
	
	public AuthenticationToken() {
		created = new Date();
		lastRenewal = created;
		Calendar cal = Calendar.getInstance();
		cal.setTime(created);
		cal.add(Calendar.HOUR, 2);
		expirationDate = cal.getTime();
	}
	
	public AuthenticationToken(String createdBy) {
		this(createdBy, createdBy, "127.0.0.1", -1, null);
	}
	public AuthenticationToken(String createdBy,
			String ipAddress, int remainingUses)
	{
		this(createdBy, createdBy, ipAddress, remainingUses, null);
	}
	
	public AuthenticationToken(String createdBy,
			String ipAddress, Date expirationDate)
	{
		this(createdBy, createdBy, ipAddress, -1, expirationDate);
	}
	
	public AuthenticationToken(String username, String createdBy,
			String ipAddress, int remainingUses,
			Date expirationDate)
	{
		this();
		this.username = username;
		this.createdBy = createdBy;
		this.ipAddress = ipAddress;
		this.remainingUses = remainingUses;
		if (expirationDate != null) {
			this.expirationDate = expirationDate;
		}
		token = createNonce(username);
	}
	
	/**
	 * @return the id
	 */
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
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}

	/**
	 * @return the token
	 */
	public String getToken()
	{
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token)
	{
		this.token = token;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}
	
	/**
	 * @return the internalUsername
	 */
	public String getInternalUsername()
	{
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername)
	{
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress()
	{
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the lastRenewal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLastRenewal()
	{
		return lastRenewal;
	}

	/**
	 * @param lastRenewal the lastRenewal to set
	 */
	public void setLastRenewal(Date lastRenewal)
	{
		this.lastRenewal = lastRenewal;
	}

	/**
	 * @return the remainingUses
	 */
	public int getRemainingUses()
	{
		return remainingUses;
	}

	/**
	 * @param remainingUses the remainingUses to set
	 */
	public void setRemainingUses(int remainingUses)
	{
		this.remainingUses = remainingUses;
	}

	/**
	 * @return the created
	 */
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
	 * @return the expirationDate
	 */
	public Date getExpirationDate()
	{
		return expirationDate;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate)
	{
		this.expirationDate = expirationDate;
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
				+ ( ( createdBy == null ) ? 0 : createdBy.hashCode() );
		result = prime
				* result
				+ ( ( internalUsername == null ) ? 0 : internalUsername
						.hashCode() );
		result = prime * result + ( ( token == null ) ? 0 : token.hashCode() );
		result = prime * result
				+ ( ( username == null ) ? 0 : username.hashCode() );
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
		AuthenticationToken other = (AuthenticationToken) obj;
		if (createdBy == null)
		{
			if (other.createdBy != null)
				return false;
		}
		else if (!createdBy.equals(other.createdBy))
			return false;
		if (internalUsername == null)
		{
			if (other.internalUsername != null)
				return false;
		}
		else if (!internalUsername.equals(other.internalUsername))
			return false;
		if (token == null)
		{
			if (other.token != null)
				return false;
		}
		else if (!token.equals(other.token))
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "AuthenticationToken [username=" + username + ", createdBy="
				+ createdBy + ", internalUsername=" + internalUsername
				+ ", token=" + token + ", remainingUses=" + remainingUses
				+ ", expirationDate=" + expirationDate + "]";
	}
	
	public String toJSON() throws AuthenticationTokenException {
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{	js.object()
				.key("created").value(this.created.getTime())
				.key("creator").value(this.createdBy)
				.key("expires").value(this.expirationDate.getTime())
				.key("remaining_uses").value(this.remainingUses == -1 ? "unlimited" : this.remainingUses)
				.key("renewed").value(this.lastRenewal.getTime())
				.key("token").value(this.token)
				.key("username").value(this.username)
				.key("internal_username").value(this.internalUsername)
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			throw new AuthenticationTokenException("Error producing JSON output.", e);
		}

		return output;
	}

	/**
	 * Creates a nonce for use as the token by generating an md5 hash of the 
	 * salt, current timestamp, and a random number.
	 * 
	 * @param salt
	 * @return md5 hash of the adjusted salt
	 */
	private String createNonce(String salt) {
		String digestMessage = salt + System.currentTimeMillis() + new Random().nextInt();
		return DigestUtils.md5Hex(digestMessage);
	}
}
