package org.iplantc.service.clients.beans;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/* 
 * Bean to hold permission information for an arbitrary entity
 */
public class IplantPermission {

	public static final String	PUBLIC_USER			= "everyone";

	public static final int		READ_WRITE_EXECUTE	= 7;
	public static final int		READ_WRITE			= 6;
	public static final int		READ_EXECUTE		= 5;
	public static final int		READ				= 4;
	public static final int		WRITE_EXECUTE		= 3;
	public static final int		WRITE				= 2;
	public static final int		EXECUTE				= 1;

	private Long				id;
	private String				username;
	private boolean				read;
	private boolean				write;
	private boolean				execute;
	private boolean				recursive			= false;
	private Date				created				= new Date();

	public IplantPermission()
	{}

	public IplantPermission(String username, int permission)
	{
		this.username = username;
		setPermission(permission);
	}

	public IplantPermission(String shareUsername, boolean grantRead,
			boolean grantWrite, boolean grantExecute)
	{
		this.username = shareUsername;
		this.read = grantRead;
		this.write = grantWrite;
		this.execute = grantExecute;
		this.recursive = false;
	}

	public IplantPermission(JSONObject jsonPem) throws JSONException
	{
		this.username = jsonPem.getString("username");
		this.read = jsonPem.getBoolean("read");
		this.write = jsonPem.getBoolean("write");
		this.execute = jsonPem.getBoolean("execute");
		this.recursive = jsonPem.getBoolean("recursive");
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
	 * @return the id
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	public int getPermission()
	{
		return ( isRead() ? 4 : 0 ) + ( isWrite() ? 2 : 0 )
				+ ( isExecute() ? 1 : 0 );
	}

	public void setPermission(int permission)
	{
		switch (permission) {
		case 1:
			this.read = false;
			this.write = false;
			this.execute = true;
			break;
		case 2:
			this.read = false;
			this.write = true;
			this.execute = false;
			break;
		case 3:
			this.read = false;
			this.write = true;
			this.execute = true;
			break;
		case 4:
			this.read = true;
			this.write = false;
			this.execute = false;
			break;
		case 5:
			this.read = true;
			this.write = false;
			this.execute = true;
			break;
		case 6:
			this.read = true;
			this.write = true;
			this.execute = false;
			break;
		case 7:
			this.read = true;
			this.write = true;
			this.execute = true;
			break;
		default:
			this.read = false;
			this.write = false;
			this.execute = false;
			break;
		}
	}

	public void makePublic()
	{
		this.username = PUBLIC_USER;
		this.read = true;
		this.execute = true;
	}

	public boolean isRead()
	{
		return read;
	}

	public void setRead(boolean read)
	{
		this.read = read;
	}

	public boolean isWrite()
	{
		return write;
	}

	public void setWrite(boolean write)
	{
		this.write = write;
	}

	public boolean isExecute()
	{
		return execute;
	}

	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}

	/**
	 * @param recursive
	 *            the recursive to set
	 */
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive()
	{
		return recursive;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
	 * @return the created
	 */
	public Date getCreated()
	{
		return created;
	}

	public String toString()
	{
		if (read && write && execute)
		{
			return "all";
		}
		else
		{
			return ( ( isRead() ? "read " : "" ) + ( isWrite() ? "write " : "" ) + ( isExecute() ? "execute"
					: "" ) ).trim();
		}
	}

	public boolean equals(Object o)
	{
		if (o instanceof IplantPermission)
		{
			IplantPermission sp = (IplantPermission) o;
			return ( sp.getUsername().equals(username) && sp.getPermission() == getPermission() );
		}

		return false;
	}
}
