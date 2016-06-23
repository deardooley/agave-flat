package org.iplantc.service.systems.model.enumerations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * @author dooley
 * 
 */
public enum ExecutionType
{
	ATMOSPHERE, HPC, CONDOR, CLI;
	
	@Override
	public String toString() {
		return name();
	}

	public List<ExecutionType> getCompatibleExecutionTypes()
	{
		List<ExecutionType> types = new ArrayList<ExecutionType>();
		if (this.equals(ATMOSPHERE)) {
			CollectionUtils.addAll(types, new ExecutionType[] { CLI });
		} else if (this.equals(HPC)) {
			CollectionUtils.addAll(types, new ExecutionType[] { HPC, CLI });
		} else if (this.equals(CONDOR)) {
			CollectionUtils.addAll(types, new ExecutionType[] { CONDOR });
		} else if (this.equals(CLI)) {
			CollectionUtils.addAll(types, new ExecutionType[] { CLI });
		}
		return types;
	}
}
