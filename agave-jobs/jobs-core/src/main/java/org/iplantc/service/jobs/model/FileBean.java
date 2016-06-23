package org.iplantc.service.jobs.model;

import java.io.File;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;

import org.iplantc.service.apps.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

public class FileBean {
	private String	name		= "";		// name of file
	private String	path		= "";		// absolute path of file
	private String	owner		= "";
	private long	length		= 0;		// file size in bytes
	private Date	lastModified;			// last time the file was touched on
	private String systemId;
	private String jobId;
	private boolean	directory	= false;	// is this a directory
	private String	parent		= null;	// uri of the parent folder
	private String	url			= "";
	private boolean	readable	= false;
	private boolean	writable	= false;
	private boolean executable	= false;
	
	public FileBean()
	{}

	public FileBean(String username, File file)
	{
		this.name = file.getName();
		this.path = file.getPath();
		this.owner = username;
		this.length = file.length();
		this.lastModified = new Date();
		this.directory = file.isDirectory();
		this.parent = file.getParentFile().toURI().toString();
		this.readable = file.canRead();
		this.writable = file.canWrite();
		this.executable = file.canExecute();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
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
	 * @return the owner
	 */
	public String getOwner()
	{
		return owner;
	}

	public long getLength()
	{
		return length;
	}

	public void setLength(long length)
	{
		this.length = length;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public boolean isDirectory()
	{
		return directory;
	}

	public void setDirectory(boolean directory)
	{
		this.directory = directory;
	}

	public String getParent()
	{
		return parent;
	}

	public void setParent(String parent)
	{
		this.parent = parent;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public String getUrl()
	{
		return url;
	}

	public void setReadable(boolean readable)
	{
		this.readable = readable;
	}

	public boolean isReadable()
	{
		return readable;
	}

	public void setWritable(boolean writable)
	{
		this.writable = writable;
	}

	public boolean isWritable()
	{
		return writable;
	}

	public boolean isExecutable() {
		return executable;
	}

	public void setExecutable(boolean executable) {
		this.executable = executable;
	}
	
	public PermissionType getPermissionType() {
		PermissionType permissionType = PermissionType.NONE;
		if (isReadable()) permissionType = permissionType.add(PermissionType.READ);
		if (isWritable()) permissionType = permissionType.add(PermissionType.WRITE);
		if (isExecutable()) permissionType = permissionType.add(PermissionType.EXECUTE);
		
		return permissionType;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String toJSON() throws JSONException
	{
		JSONWriter writer = new JSONStringer();

		writer.object()
			.key("name").value(name)
			.key("path").value(path)
			.key("lastModified").value(new DateTime(lastModified).toString())
			.key("length").value(length)
//			.key("owner").value(owner)
			.key("permission").value(getPermissionType().name());
		if (isDirectory()) {
			writer.key("mimeType").value("text/directory");
		} else {
			File f = new File(new File(path).getName());
			String mimetype = new MimetypesFileTypeMap().getContentType(f);
			writer.key("mimeType").value(mimetype);
		}
		writer.key("format").value(directory ? "folder" : "unknown")
			.key("type").value(directory? LogicalFile.DIRECTORY : LogicalFile.FILE)
			
//			.key("parent").value(parent)
			.key("_links").object()
				.key("self").object()
					.key("href").value(TenancyHelper.resolveURLToCurrentTenant(url))
				.endObject()
				.key("system").object()
					.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId())
				.endObject()
				.key("parent").object()
					.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + getJobId())
				.endObject()
			.endObject()
		.endObject();

		return writer.toString();

	}

	public String toString()
	{
		String lineItem = "";

		try
		{
			lineItem = ( ( isDirectory() ) ? "d " : "- " ) + "\t";
			lineItem += ( ( isReadable() ) ? "r " : "- " )
					+ ( ( isWritable() ) ? "w " : "- " ) + "\t";
			lineItem += getOwner() + "\t";
			lineItem += getLastModified() + "\t";
			lineItem += getLength() + "\t";
			lineItem += getName();
		}
		catch (Exception e)
		{

			e.printStackTrace();
		}

		return lineItem;
	}

	// /**
	// * Give similar output to the unix command ls -l
	// *
	// * @return listing a unix formatted directory listing.
	// */
	// public String list() {
	//        
	// String listing = "";
	//        
	// if (directory) {
	//
	// listing = "total " + children.size() + "\n" +
	// toString() + "\n";
	//            
	// for (FileBean child: children) {
	//            	
	// listing += child.toString() + "\n";
	// }
	//            
	// } else {
	//            
	// listing = this.toString();
	// }
	//        
	// return listing;
	// }

	public boolean equals(Object o)
	{
		if (o instanceof FileBean)
		{
			return ( ( (FileBean) o ).name.equals(name)
					&& ( (FileBean) o ).path.equals(path)
					&& ( ( (FileBean) o ).length == length ) && ( ( (FileBean) o ).directory == directory ) );
		}
		else
		{
			return false;
		}
	}

}
