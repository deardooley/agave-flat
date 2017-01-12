package org.iplantc.service.jobs.managers.launchers;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;

/**
 * This class is a TestNG factory class which generates and 
 * runs a matrix of tests to test validation and remote job 
 * submission of all {@link JobLauncher} classes.
 * .
 * @author dooley
 *
 */
public class JobLauncherTestFactory extends AbstractJobLauncherTest
{
    private SchedulerType schedulerType;
    private StorageProtocolType storageProtocol;
 
//    @Factory(dataProvider = "jobSubmissionSchedulerMatrixGenerator")
    public JobLauncherTestFactory(SchedulerType schedulerType, StorageProtocolType storageProtocol) {
        this.schedulerType = schedulerType;
        this.storageProtocol = storageProtocol;
    }
 
//    @DataProvider(parallel=false)
    public static Object[][] jobSubmissionSchedulerMatrixGenerator() {
    	List<Object[]> testCases = new ArrayList<Object[]>();

//    	for (SchedulerType scheduler: SchedulerType.values()) {
//    		if (SchedulerType.LOADLEVELER == scheduler || 
//    				SchedulerType.UNKNOWN == scheduler || 
//    				scheduler.name().startsWith("CUSTOM")) {
//    			// don't have containers for them yet.
//    			continue;
//    		}
//    		testCases.add(new Object[] { scheduler, StorageProtocolType.SFTP });
//    	}
        return testCases.toArray(new Object[][]{});
    }
    
	@Override
	public SchedulerType getExectionSystemSchedulerType() {
		return schedulerType;
	}
	
	@Override
	public StorageProtocolType getStorageSystemProtocolType() {
		return storageProtocol;
	}
}