/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

import java.math.BigDecimal;
import java.util.Date;

import org.json.JSONStringer;
import org.teragrid.service.util.TGUtil;

/**
 * @author dooley
 * 
 */
public class Allocation implements TgcdbDTO {

	private String		projectId;
	private String		projectTitle;
	private Integer		allocationId;
	private Date		startDate;
	private Date		endDate;
	private BigDecimal	baseAllocation;
	private BigDecimal	remainingAllocation;
	private String		allocResourceName;
	private String		projState;
	private String		piLastName;
	private String		piFirstName;
	private Integer		personId;
	private String		firstName;
	private String		lastName;
	private Boolean		isPi;
	private BigDecimal	usedAllocation;
	private String		acctState;

	public Allocation()
	{}

	// public Allocation(Acctv acctv) {
	// projectId = acctv.getChargeNumber();
	// projectTitle = acctv.getProjectTitle();
	// allocationId = acctv.getAllocationId();
	// startDate = acctv.getStartDate();
	// endDate = acctv.getEndDate();
	// baseAllocation = acctv.getBaseAllocation();
	// remainingAllocation = acctv.getRemainingAllocation();
	// allocResourceName = acctv.getAllocResourceName();
	// projState = acctv.getProjState();
	// piLastName = acctv.getPiLastName();
	// piFirstName = acctv.getPiFirstName();
	// personId = acctv.getPersonId();
	// firstName = acctv.getFirstName();
	// lastName = acctv.getLastName();
	// usedAllocation = acctv.getUsedAllocation();
	// acctState = acctv.getAcctState();
	// isPi = acctv.getIsPi();
	// }

	public String getProjectId()
	{
		return projectId;
	}

	public void setProjectId(String projectId)
	{
		this.projectId = projectId;
	}

	public String getProjectTitle()
	{
		return projectTitle;
	}

	public void setProjectTitle(String projectTitle)
	{
		this.projectTitle = projectTitle;
	}

	public Integer getAllocationId()
	{
		return allocationId;
	}

	public void setAllocationId(Integer allocationId)
	{
		this.allocationId = allocationId;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}

	public BigDecimal getBaseAllocation()
	{
		return baseAllocation;
	}

	public void setBaseAllocation(BigDecimal baseAllocation)
	{
		this.baseAllocation = baseAllocation;
	}

	public BigDecimal getRemainingAllocation()
	{
		return remainingAllocation;
	}

	public void setRemainingAllocation(BigDecimal remainingAllocation)
	{
		this.remainingAllocation = remainingAllocation;
	}

	public String getAllocResourceName()
	{
		return allocResourceName;
	}

	public void setAllocResourceName(String allocResourceName)
	{
		this.allocResourceName = allocResourceName;
	}

	public String getProjState()
	{
		return projState;
	}

	public void setProjState(String projState)
	{
		this.projState = projState;
	}

	public String getPiLastName()
	{
		return piLastName;
	}

	public void setPiLastName(String piLastName)
	{
		this.piLastName = piLastName;
	}

	public String getPiFirstName()
	{
		return piFirstName;
	}

	public void setPiFirstName(String piFirstName)
	{
		this.piFirstName = piFirstName;
	}

	public Integer getPersonId()
	{
		return personId;
	}

	public void setPersonId(Integer personId)
	{
		this.personId = personId;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public Boolean getIsPi()
	{
		return isPi;
	}

	public void setIsPi(Boolean isPi)
	{
		this.isPi = isPi;
	}

	public BigDecimal getUsedAllocation()
	{
		return usedAllocation;
	}

	public void setUsedAllocation(BigDecimal usedAllocation)
	{
		this.usedAllocation = usedAllocation;
	}

	public String getAcctState()
	{
		return acctState;
	}

	public void setAcctState(String acctState)
	{
		this.acctState = acctState;
	}

	public String toJSONString()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object().key("allocation").object().key("id").value(
					this.allocationId).endObject().object().key("project.id")
					.value(this.projectId).endObject().object().key(
							"project.title").value(this.projectTitle)
					.endObject().object().key("start.date").value(
							TGUtil.formatUTC(startDate)).endObject().object()
					.key("end.date").value(TGUtil.formatUTC(endDate))
					.endObject().object().key("base.allocation").value(
							this.baseAllocation).endObject().object().key(
							"used.allocation").value(this.usedAllocation)
					.endObject().object().key("remaining.allocation").value(
							this.remainingAllocation).endObject().object().key(
							"resource.name").value(this.allocResourceName)
					.endObject().object().key("pi.first.name").value(
							this.piFirstName).endObject().object().key(
							"pi.last.name").value(this.piLastName).endObject()
					.object().key("account.state").value(this.acctState)
					.endObject().object().key("project.state").value(
							this.projState).endObject().endObject();

			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;

	}

	public String toCsv()
	{
		return quote(allocationId) + "," + quote(projectId) + ","
				+ quote(projectTitle) + ","
				+ quote(TGUtil.formatUTC(startDate)) + ","
				+ quote(TGUtil.formatUTC(endDate)) + ","
				+ quote(baseAllocation) + "," + quote(usedAllocation) + ","
				+ quote(remainingAllocation) + "," + quote(allocResourceName)
				+ "," + quote(piFirstName) + "," + quote(piLastName) + ","
				+ quote(acctState) + "," + quote(projState);
	}

	private String quote(Object o)
	{
		return "\"" + ( o == null ? o : o.toString() ) + "\"";
	}

	public String toHtml()
	{
		return "<tr>" + formatColumn(allocationId) + formatColumn(projectId)
				+ formatColumn(projectTitle)
				+ formatColumn(TGUtil.formatUTC(startDate))
				+ formatColumn(TGUtil.formatUTC(endDate))
				+ formatColumn(baseAllocation) + formatColumn(usedAllocation)
				+ formatColumn(remainingAllocation)
				+ formatColumn(allocResourceName) + formatColumn(piFirstName)
				+ formatColumn(piLastName) + formatColumn(acctState)
				+ formatColumn(projState) + "</tr>";
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

		String resourceLink = null;
		// ComputeDTO system =
		// ResourceCache.getResourceByTgcdbName(allocResourceName);
		// if (system == null) {
		resourceLink = allocResourceName;
		// } else {
		// resourceLink = "<a href=\"" + path + "/resource/resourceid/" +
		// system.getResourceId() + "\">" + system.getResourceId() + "</a>";
		// }
		return "<tr>"
				+ formatColumn(allocationId)
				+ formatColumn("<a href=\"" + path + "/project/project_number/"
						+ projectId + "\">" + projectId + "</a>")
				+ formatColumn(projectTitle)
				+ formatColumn(TGUtil.formatUTC(startDate))
				+ formatColumn(TGUtil.formatUTC(endDate))
				+ formatColumn(baseAllocation) + formatColumn(usedAllocation)
				+ formatColumn(remainingAllocation)
				+ formatColumn(resourceLink) + formatColumn(piFirstName)
				+ formatColumn(piLastName) + formatColumn(acctState)
				+ formatColumn(projState) + "</tr>";
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
		return " {\n" + "   id => '" + allocationId + "',\n"
				+ "   project.id => '" + projectId + "',\n"
				+ "   project.title => '" + projectTitle + "',\n"
				+ "   start.date => '" + TGUtil.formatUTC(startDate) + "',\n"
				+ "   end.date => '" + TGUtil.formatUTC(endDate) + "',\n"
				+ "   base.allocation => '" + baseAllocation + "',\n"
				+ "   used.allocation => '" + usedAllocation + "',\n"
				+ "   remaining.allocation => '" + remainingAllocation + "',\n"
				+ "   resource.name => '" + allocResourceName + "',\n"
				+ "   pi.first.name => '" + piFirstName + "',\n"
				+ "   pi.last.name => '" + piLastName + "',\n"
				+ "   account.state => '" + acctState + "',\n"
				+ "   project.state => '" + projState + "'}";
	}

}
