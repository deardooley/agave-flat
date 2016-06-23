/**
 * 
 */
package org.iplantc.service.clients.beans;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * @author dooley
 * 
 */
public class IplantIOFile {

	private String					name;
	private String					uuid;
	private String					owner;
	private Date					lastUpdated;
	private String					source;
	private String					path;
	private String					status;
	private String					nativeFormat	= "raw";
	private int						size;
	private String					type;
	private List<IplantPermission>	permissions;
	private Date					created			= new Date();

	/**
	 * 
	 */
	public IplantIOFile()
	{}

	public IplantIOFile(JSONObject jsonFile) throws JSONException
	{
		this.name = jsonFile.getString("name");
		this.uuid = jsonFile.getString("uuid");
		this.owner = jsonFile.getString("owner");
		this.lastUpdated = new Date(jsonFile.getLong("lastUpdated"));
		this.source = jsonFile.getString("source");
		this.path = jsonFile.getString("path");
		this.status = jsonFile.getString("status");
		this.nativeFormat = jsonFile.getString("nativeFormat");
		this.size = jsonFile.getInt("size");
		this.type = jsonFile.getString("type");
		this.created = new Date(jsonFile.getLong("created"));
		JSONArray jsonPems = jsonFile.getJSONArray("permissions");
		for (int i = 0; i < jsonPems.length(); i++)
		{
			IplantPermission pem = new IplantPermission(jsonPems
					.getJSONObject(i));
			this.permissions.add(pem);
		}
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param uuid
	 *            the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * @return the owner
	 */
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the source
	 */
	public String getSource()
	{
		return source;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	public void setSource(String source)
	{
		this.source = source;
	}

	/**
	 * @return the path
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(String path)
	{
		this.path = path;
	}

	/**
	 * @return the status
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	/**
	 * @return the nativeFormat
	 */
	public String getNativeFormat()
	{
		return nativeFormat;
	}

	/**
	 * @param nativeFormat
	 *            the nativeFormat to set
	 */
	public void setNativeFormat(String nativeFormat)
	{
		this.nativeFormat = nativeFormat;
	}

	/**
	 * @return the permissions
	 */
	public List<IplantPermission> getPermissions()
	{
		return permissions;
	}

	/**
	 * @param permissions
	 *            the permissions to set
	 */
	public void setPermissions(List<IplantPermission> permissions)
	{
		this.permissions = permissions;
	}

	/**
	 * @return the created
	 */
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	public String toJSON() throws JSONException
	{
		JSONWriter writer = new JSONStringer();
		writer.object().key("name").value(name).key("owner").value(owner).key(
				"source").value(source).key("path").value(path).key("status")
				.value(status).key("nativeFormat").value(nativeFormat).key(
						"size").value(size).key("type").value(type).key(
						"created").value(created.getTime()).key("lastUpdated")
				.value(lastUpdated.getTime());

		for (IplantPermission pem : permissions)
		{
			writer.object().key("username").value(pem.getUsername()).key(
					"permission").value(pem.getPermission()).endObject();
		}

		return writer.toString();
	}

}
