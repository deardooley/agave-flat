package org.iplantc.service.io.manager.actions;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.systems.model.RemoteSystem;
import org.restlet.data.Range;

/**
 * The ActionContext holds information about the context of a 
 * particular {@link LogicalFileAction} invocation. An instance
 * of this class is passed into the constructor of each {@link LogicalFileAction}. 
 * 
 * @author dooley
 *
 */
public class ActionContext {
	private static final Logger log = Logger.getLogger(ActionContext.class);
	
	protected String owner;
	protected String internalUsername;
	protected String systemId;
	protected RemoteSystem system;
	protected String currentUser;
	protected String path;
	protected List<Range> ranges;
	protected File tempFile;
	
	public ActionContext(String path, String currentUser, String systemId) 
	{
		setPath(path);
		setCurrentUser(currentUser);
		setSystemId(systemId);
	}

	/**
	 * @return the owner
	 */
	public synchronized String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public synchronized void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the internalUsername
	 */
	public synchronized String getInternalUsername() {
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public synchronized void setInternalUsername(String internalUsername) {
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the systemId
	 */
	public synchronized String getSystemId() {
		return systemId;
	}

	/**
	 * @param systemId the systemId to set
	 */
	public synchronized void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	/**
	 * @return the system
	 */
	public synchronized RemoteSystem getSystem() {
		return system;
	}

	/**
	 * @param system the system to set
	 */
	public synchronized void setSystem(RemoteSystem system) {
		this.system = system;
	}

	/**
	 * @return the currentUser
	 */
	public synchronized String getCurrentUser() {
		return currentUser;
	}

	/**
	 * @param currentUser the currentUser to set
	 */
	public synchronized void setCurrentUser(String currentUser) {
		this.currentUser = currentUser;
	}

	/**
	 * @return the path
	 */
	public synchronized String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public synchronized void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the ranges
	 */
	public synchronized List<Range> getRanges() {
		return ranges;
	}

	/**
	 * @param ranges the ranges to set
	 */
	public synchronized void setRanges(List<Range> ranges) {
		this.ranges = ranges;
	}

	/**
	 * @return the tempFile
	 */
	public synchronized File getTempFile() {
		return tempFile;
	}

	/**
	 * @param tempFile the tempFile to set
	 */
	public synchronized void setTempFile(File tempFile) {
		this.tempFile = tempFile;
	}
	
}
