/**
 * 
 */
package org.iplantc.service.remote;


/**
 * Defines the interface required of all classes supporting
 * remote job submission. This essentially consists of being able
 * to connect to a remote system and start a job.
 * 
 * @author dooley
 *
 */
public interface RemoteSubmissionClient {

	public String runCommand(String command) throws Exception;

	public void close();
	
	public boolean canAuthentication();
}
