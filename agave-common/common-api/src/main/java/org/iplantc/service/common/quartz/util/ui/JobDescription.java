package org.iplantc.service.common.quartz.util.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.quartz.JobExecutionContext;

/**
 * Created by silviu
 * Date: 08/03/14 / 12:29664
 * <p>
 *     describes a job
 * </p>
 * <br/>
 *
 * @author Silviu Ilie
 */
public class JobDescription {

    private String name;
    private String groupName;
    private String description;
    private String className;
    private Boolean paused;
    private List<SimpleCronExpression> cronExpressions = new ArrayList<SimpleCronExpression>(1);
    private List<ActiveJobDescription> activeJobDescriptions = new ArrayList<ActiveJobDescription>();
    
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobDescription addCronExpression(SimpleCronExpression cronExpression) {
        this.getCronExpressions().add(cronExpression);
        return this;
    }

    public List<SimpleCronExpression> getCronExpressions() {
        return cronExpressions;
    }

    public void setCronExpressions(List<SimpleCronExpression> cronExpressions) {
        this.cronExpressions = cronExpressions;
    }

    /**
	 * @return the activeJobs
	 */
	public List<ActiveJobDescription> getActiveJobDescriptions()
	{
		return this.activeJobDescriptions;
	}

	/**
	 * @param activeJobs the activeJobs to set
	 */
	public void setActiveJobDescriptions(List<ActiveJobDescription> activeJobDescriptions)
	{
		this.activeJobDescriptions = activeJobDescriptions;
	}
	
	public void addActiveJobDescription(ActiveJobDescription jec) 
	{
		this.activeJobDescriptions.add(jec);
	}

	public Boolean getPaused() {
        return paused;
    }

    public void setPaused(Boolean paused) {
        this.paused = paused;
    }
}
