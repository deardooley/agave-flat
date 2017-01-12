package org.iplantc.service.jobs.managers.launchers;

import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.testng.annotations.Test;

/**
 * Test template validation and remote submission to a SGE 
 * scheduler with the {@link HPCLauncher} class.
 */
@Test(groups={"job","launcher"})
public class SGELauncherTest extends AbstractJobLauncherTest
{
	@Override
	protected SchedulerType getExectionSystemSchedulerType() {
		return SchedulerType.SGE;
	}
}