/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONStringer;

/**
 * @author dooley
 * 
 */
public class ComputeDTO extends ResourceDTO implements TgcdbDTO {

	private String			id = "";
	private String			tgcdbName = "";
	private String			status = "";
	private String			localUsername = "";
	private Load			load = new Load("default",0,0);
	// private Set<Service> services = new HashSet<Service>();
	private String			loginHost = "";
	private int				loginPort = 22;
	private String			gridftpHost = "";
	private int				gridftpPort = 2811;
	private String			gramEndpoint = "";
	private SchedulerType	schedulerType = SchedulerType.UNKNOWN;
	private String			workDir = "";
	private String			scratchDir = "";
	private String			chargeNumber = "";
	private BatchQueue		defaultQueue = new BatchQueue();
	private String			whitelist;
	private List<String> 	customDirectives = new ArrayList<String>();
	
	public ComputeDTO() {}

	// public ComputeDTO(TGSystem resource) {
	// super();
	// this.tgcdbName = resource.getTgcdbName();
	// this.resourceId = resource.getResourceId();
	// this.name = resource.getName();
	// this.localUsername = resource.getLocalUsername();
	// }

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public String getId()
	{
		return id;
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
	 * @return the localUsername
	 */
	public String getLocalUsername()
	{
		return localUsername;
	}

	/**
	 * @param localUsername
	 *            the localUsername to set
	 */
	public void setLocalUsername(String localUsername)
	{
		this.localUsername = localUsername;
	}

	/**
	 * @return the loads
	 */
	public Load getLoad()
	{
		return load;
	}

	/**
	 * @param loads
	 *            the loads to set
	 */
	public void setLoad(Load load)
	{
		this.load = load;
	}

	/**
	 * @param loginHostname
	 *            the loginHostname to set
	 */
	public void setLoginHost(String loginHost)
	{
		this.loginHost = loginHost;
	}

	/**
	 * @return the loginHostname
	 */
	public String getLoginHost()
	{
		return loginHost;
	}

	/**
	 * @param loginPort
	 *            the loginPort to set
	 */
	public void setLoginPort(int loginPort)
	{
		this.loginPort = loginPort;
	}

	/**
	 * @return the loginPort
	 */
	public int getLoginPort()
	{
		return loginPort;
	}

	/**
	 * @param gridftpHostname
	 *            the gridftpHostname to set
	 */
	public void setGridftpHost(String gridftpHost)
	{
		this.gridftpHost = gridftpHost;
	}

	/**
	 * @return the gridftpHostname
	 */
	public String getGridftpHost()
	{
		return gridftpHost;
	}

	// /**
	// * @return the services
	// */
	// public Set<Service> getServices() {
	// return services;
	// }
	//
	// /**
	// * @param services the services to set
	// */
	// public void setServices(Set<Service> services) {
	// this.services = services;
	// }

	/**
	 * @param gridftpPort
	 *            the gridftpPort to set
	 */
	public void setGridftpPort(int gridftpPort)
	{
		this.gridftpPort = gridftpPort;
	}

	/**
	 * @return the gridftpPort
	 */
	public int getGridftpPort()
	{
		return gridftpPort;
	}

	/**
	 * @param gramHostname
	 *            the gramEndpoint to set
	 */
	public void setGramEndpoint(String gramEndpoint)
	{
		this.gramEndpoint = gramEndpoint;
	}

	/**
	 * @return the gramHostname
	 */
	public String getGramEndpoint()
	{
		return gramEndpoint;
	}

	/**
	 * @param schedulerType
	 *            the schedulerType to set
	 */
	public void setSchedulerType(SchedulerType schedulerType)
	{
		this.schedulerType = schedulerType;
	}

	/**
	 * @return the schedulerType
	 */
	public SchedulerType getSchedulerType()
	{
		return schedulerType;
	}

	/**
	 * @return
	 */
	public String getTgcdbName()
	{
		return this.tgcdbName;
	}

	/**
	 * @param tgcdbName
	 *            the tgcdbName to set
	 */
	public void setTgcdbName(String tgcdbName)
	{
		this.tgcdbName = tgcdbName;
	}

	/**
	 * @return the resourceId
	 */
	public String getResourceId()
	{
		return resourceId;
	}

	/**
	 * @param workDir the workDir to set
	 */
	public void setWorkDir(String workDir)
	{
		this.workDir = workDir;
	}

	/**
	 * @return the workDir
	 */
	public String getWorkDir()
	{
		return workDir;
	}

	/**
	 * @param scratchDir the scratchDir to set
	 */
	public void setScratchDir(String scratchDir)
	{
		this.scratchDir = scratchDir;
	}

	/**
	 * @return the scratchDir
	 */
	public String getScratchDir()
	{
		return scratchDir;
	}

	/**
	 * @param chargeNumber the chargeNumber to set
	 */
	public void setChargeNumber(String chargeNumber)
	{
		this.chargeNumber = chargeNumber;
	}

	/**
	 * @return the chargeNumber
	 */
	public String getChargeNumber()
	{
		return chargeNumber;
	}

	/**
	 * @param defaultQueue the defaultQueue to set
	 */
	public void setDefaultQueue(BatchQueue defaultQueue)
	{
		this.defaultQueue = defaultQueue;
	}

	/**
	 * @return the defaultQueue
	 */
	public BatchQueue getDefaultQueue()
	{
		return defaultQueue;
	}

	/**
	 * @return the whitelist
	 */
	public String getWhitelist()
	{
		return whitelist;
	}

	/**
	 * @param whitelist the whitelist to set
	 */
	public void setWhitelist(String whitelist)
	{
		this.whitelist = whitelist;
	}

	public boolean equals(Object o)
	{
		if (o instanceof String)
		{
			return ( id.equals(o) );
		}
		else if (o instanceof ComputeDTO)
		{
			ComputeDTO s = (ComputeDTO) o;
			if (!id.equals(s.id))
				return false;
			if (!tgcdbName.equalsIgnoreCase(s.tgcdbName))
				return false;
			if (!resourceId.equalsIgnoreCase(s.resourceId))
				return false;
			if (!name.equalsIgnoreCase(s.name))
				return false;
			return true;
		}
		else
		{
			return false;
		}
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object()
				.key("site").value(this.getSite())
				.key("resource.id").value(this.resourceId)
				.key("tgcdb.name").value(this.tgcdbName)
				.key("resource.name").value(this.name)
				.key("name").value(this.name)
				.key("gsissh.host").value(this.loginHost)
				.key("gsissh.port").value(this.gridftpPort)
				.key("gridftp.host").value(this.gridftpHost)
				.key("gridftp.port").value(this.gridftpPort)
				.key("gram.host").value(this.gridftpHost)
				.key("scheduler.type").value(this.getSchedulerType())
				.key("status").value(this.status)
				.key("localUsername").value(this.localUsername)
				.key("type").value(this.getType())
					// .object().key("load").value(this.load).endObject()
					// .object().key("services").value(this.services).endObject()
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;

	}

	public String toString()
	{
		return resourceId;
	}

	public String toCsv()
	{
		return quote(getSite()) + "," + quote(resourceId) + ","
				+ quote(tgcdbName) + "," + quote(name) + "," + quote(loginHost)
				+ "," + quote(loginPort) + "," + quote(gridftpHost)
				+ quote(gridftpPort) + quote(gramEndpoint) + ","
				+ quote(getSchedulerType().toString()) + quote(status) + ","
				+ quote(localUsername) + "," + quote(getType());
		// + load.toCsv() + ",";
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	public String toHtml()
	{
		return "<tr>"
				+ formatColumn("<a href=\"http://info.teragrid.org/web-apps/html/ctss-resources-v1/SiteID/"
						+ getSite() + "\">" + getSite() + "</a>")
				+ formatColumn(resourceId) + formatColumn(tgcdbName)
				+ formatColumn(name) + formatColumn(getLoginHost())
				+ formatColumn(getLoginPort()) + formatColumn(getGridftpHost())
				+ formatColumn(getGridftpPort())
				+ formatColumn(getGramEndpoint())
				+ formatColumn(getSchedulerType()) + formatColumn(status)
				+ formatColumn(localUsername) + formatColumn(getType())
				+ "</tr>";// + load.toHtml();
		// formatColumn(usedAllocation) + formatColumn(remainingAllocation) +
		// formatColumn(allocResourceName) + formatColumn(piFirstName) +
		// formatColumn(piLastName) + formatColumn(acctState) +
		// formatColumn(projState) + "</tr>";
	}

	public String toHtml(String url)
	{
		String path = "";
		if (url.contains("username"))
		{
			String username = url.substring(url.indexOf("username"));
			username = username.substring(username.indexOf("/") + 1);
			username = username.substring(0, username.indexOf("/"));
			path = url.substring(0,
					( url.indexOf(username) + username.length() ));
		}
		else
		{
			path = url.substring(0, url.indexOf("profile-v1")
					+ "profile-v1".length());
		}
		return "<tr>"
				+ formatColumn("<a href=\"http://info.teragrid.org/web-apps/html/ctss-resources-v1/SiteID/"
						+ getSite() + "\">" + getSite() + "</a>")
				+ formatColumn("<a href=\"" + path + "/resource/resourceid/"
						+ resourceId + "\">" + resourceId + "</a>")
				+ formatColumn(tgcdbName)
				+ formatColumn(name)
				+ formatColumn(getLoginHost())
				+ formatColumn(getLoginPort())
				+ formatColumn(getGridftpHost())
				+ formatColumn(getGridftpPort())
				+ formatColumn(getGramEndpoint())
				+ formatColumn(getSchedulerType())
				+ formatColumn(status)
				+ formatColumn("<a href=\"" + path + "\">" + localUsername
						+ "</a>") + formatColumn(getType()) + "</tr>";
		// + load.toHtml();
		// formatColumn(usedAllocation) + formatColumn(remainingAllocation) +
		// formatColumn(allocResourceName) + formatColumn(piFirstName) +
		// formatColumn(piLastName) + formatColumn(acctState) +
		// formatColumn(projState) + "</tr>";
	}

	private String formatColumn(Object o)
	{
		return "<td>" + ( o == null ? o : o.toString() ) + "</td>";
	}

	public String toPerl()
	{
		// "id,requestId,project.title,,end.date," +
		// "base.allocation,used.allocation,remaining.allocation," +
		// "resource.name,pi.first.name,pi.last.name,account.state,project.state";
		String result = " {\n" + "   site => '" + getSite() + "',\n"
				+ "   resource.id => '" + name + "',\n" + "   tgcdb.name => '"
				+ name + "',\n" + "   resource.name => '" + name + "',\n"
				+ "   gsissh.host => '" + loginHost + "',\n"
				+ "   gsissh.port => '" + loginPort + "',\n"
				+ "   gridftp.host => '" + gridftpHost + "',\n"
				+ "   gridftp.port => '" + gridftpPort + "',\n"
				+ "   gram.endpoint => '" + gramEndpoint + "',\n"
				+ "   scheduler.type => '" + getSchedulerType().toString()
				+ "',\n" + "   status => '" + status + "',\n"
				+ "   local.username => '" + localUsername + "',\n"
				+ "   site.name => '" + getSite() + "',\n" + "   type => '"
				+ getType() + "'}\n";
		// load.toPerl();

		return result;
	}

	/**
	 * @param customDirectives the customDirectives to set
	 */
	public void setCustomDirectives(List<String> customDirectives)
	{
		this.customDirectives = customDirectives;
	}

	/**
	 * @return the customDirectives
	 */
	public List<String> getCustomDirectives()
	{
		return customDirectives;
	}	
}
