package org.iplantc.service.io.manager;

import java.util.List;

import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.restlet.data.Range;

public class AgaveFileUploadProcessor extends AbstractFileUploadProcessor{
	public String owner;
	public String internalUsername;
	public String systemId;
	public RemoteSystem system;
	public String username;
	public String path;
	public List<Range> ranges;
	public SystemManager sysManager;
	public RemoteDataClient remoteDataClient;

	public AgaveFileUploadProcessor(List<Range> ranges, SystemManager sysManager) {
		this.ranges = ranges;
		this.sysManager = sysManager;
	}
	
	
}