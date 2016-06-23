/**
 * 
 */
package org.iplantc.service.io.manager.actions;

import java.io.IOException;

import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * Abstract class for all {@link LogicalFileAction} implementations.
 * This class provides a centralized place for setting up the {@link ActionContext}
 * and applying custom validation rules.
 * 
 * @author dooley
 *
 */
public abstract class AbstractAction implements LogicalFileAction {

	protected ActionContext data;
	protected LogicalFile logicalFile;
	protected SystemManager sysManager;
	protected RemoteDataClient remoteDataClient;

	public AbstractAction(ActionContext context) 
	{
		this.data = context;
	}
	/* (non-Javadoc)
	 * @see org.iplantc.service.io.manager.actions.RemoteFileItemAction#doAction()
	 */
	@Override
	public LogicalFile doAction() throws RemoteDataException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	

}
