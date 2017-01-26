package org.iplantc.service.jobs.managers.launchers;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

/**
 * This class is a TestNG factory class which generates and 
 * runs a matrix of tests to test validation and {@link JobLauncher} 
 * macro parsing in the job wrapper scripts.
 * .
 * @author dooley
 *
 */
public class JobLauncherMacroTestFactory
{
	@Factory
    public Object[] createInstances() {
    	List<Object> testCases = new ArrayList<Object>();

    	for (SchedulerType scheduler: SchedulerType.values()) {
    		if (SchedulerType.LOADLEVELER == scheduler || 
    				SchedulerType.UNKNOWN == scheduler || 
    				scheduler.name().startsWith("CUSTOM")) {
    			// don't have containers for them yet.
    			continue;
    		}
    		testCases.add(new JobLauncherMacroTest(scheduler));
    	}
    	
        return testCases.toArray();
    }
}