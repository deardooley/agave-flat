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

	/**
	 * Run a command on a remote host. Authentication is handled prior
	 * to this command being run.
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String runCommand(String command) throws Exception;

	/**
	 * Explicitly force the connection to the remote host to close.
	 * All exceptions are swallowed in this operation.
	 */
	public void close();
	
	/**
	 * Check whether authentication is valid on the remote host.
	 * @return true if auth succeeds, false otherwise.
	 */
	public boolean canAuthentication();
	
	/**
	 * Get the hostname of the remote system
	 * @return
	 */
	public String getHost();
	
	/**
	 * Get the port on which the remote system is listening
	 * @return
	 */
	public int getPort();
}
