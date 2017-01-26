package org.iplantc.service.jobs.managers.launchers;

import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.testng.annotations.Test;

/**
 * Test template validation and remote fork job submission 
 * with the {@link CLILauncher} class.
 */
@Test(groups={"job","launcher"})
public class CLILauncherTest extends AbstractJobLauncherTest
{
	@Override
	protected SchedulerType getExectionSystemSchedulerType() {
		return SchedulerType.FORK;
	}
}
