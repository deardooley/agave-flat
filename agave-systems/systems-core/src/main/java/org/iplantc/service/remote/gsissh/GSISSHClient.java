 /**
 * 
 */
package org.iplantc.service.remote.gsissh;

import java.io.IOException;

import org.apache.airavata.gsi.ssh.api.CommandExecutor;
import org.apache.airavata.gsi.ssh.api.CommandInfo;
import org.apache.airavata.gsi.ssh.api.SSHApiException;
import org.apache.airavata.gsi.ssh.api.ServerInfo;
import org.apache.airavata.gsi.ssh.config.ConfigReader;
import org.apache.airavata.gsi.ssh.impl.RawCommandInfo;
import org.apache.airavata.gsi.ssh.impl.StandardOutReader;
import org.apache.airavata.gsi.ssh.impl.authentication.PropertyAuthenticationInfo;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.globus.common.CoGProperties;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.auth.MyProxyClient;
import org.iplantc.service.common.auth.TrustedCALocation;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;

import com.jcraft.jsch.JSch;

/**
 * Implements an SSH client with methods to connect to a remote server and
 * perform all necessary SSH functions such as SCP, SFTP, executing commands,
 * starting the users shell and perform port forwarding.
 * 
 * @author dooley
 * 
 */
public class GSISSHClient implements RemoteSubmissionClient 
{	
	private static final Logger			log	= Logger.getLogger(GSISSHClient.class);
	static {
		JSch.setConfig("StrictHostKeyChecking",  "no");
	}
	private String						hostname;
	private int							port = 22;
	private GSSCredential				credential;

	/**
	 * 
	 */
	public GSISSHClient(String hostname, GSSCredential credential)
	{
		this.hostname = hostname;
		this.credential = credential;
	}
	
	public GSISSHClient(String hostname, int port, GSSCredential credential)
	{
		this(hostname, credential);
		this.port = port;
	}

	public String runCommand(String command) throws Exception
	{
		log.debug("Forking command " + command + " on " + hostname + ":" + port);
		
		PropertyAuthenticationInfo authenticationInfo = new PropertyAuthenticationInfo(credential, CoGProperties.getDefault().getCaCertLocations());
        // Create command
        CommandInfo commandInfo = new RawCommandInfo(command);

        // Server info
        ServerInfo serverInfo = new ServerInfo("", hostname, port);
        
        // Output
        StandardOutReader commandOutput = new StandardOutReader();

        // Execute command
        try {
			CommandExecutor.executeCommand(commandInfo, serverInfo, authenticationInfo, commandOutput, new ConfigReader());
		} catch (SSHApiException e) {
			throw new RemoteExecutionException("Failed to execute command " + command + " on " + hostname + ":" + port, e);
		} catch (IOException e) {
			throw new RemoteExecutionException("Failed to execute command " + command + " on " + hostname + ":" + port, e);
		}
        
        return commandOutput.getStdOutputString();
	}

	public void close() {}

	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

		// Set up a simple configuration that logs on the console.
		log.addAppender(new ConsoleAppender());
		log.setLevel(Level.DEBUG);
		String response = "";
		GSISSHClient client = null;
		try
		{
			System.out.println("Retrieving credential...");
			
			GSSCredential proxy = MyProxyClient.getCredential("myproxy.teragrid.org", 7512, "username","passphrase", null);
			System.out.println("Credential retrieved valid for "
					+ proxy.getRemainingLifetime() + " seconds");

			System.out.println("Connecting to " + "stampede...");
			client = new GSISSHClient("stampede.tacc.utexas.edu",2222, proxy);
			System.out.println("Running command");

			response = client.runCommand("/bin/date");//"source ~/.bashrc; cd /home1/02818/reddy/dxing/job-0001391405382179-b0b0b0bb0b-0001-007-meme_test/meme_4.9.1; chmod +x meme_test.ipcexe; sbatch meme_test.ipcexe");

			System.out.println(response);
			log.debug(response);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean canAuthentication()
	{
		log.debug("Verifying authentication to " + hostname + ":" + port);
		
		PropertyAuthenticationInfo authenticationInfo = new PropertyAuthenticationInfo(credential, CoGProperties.getDefault().getCaCertLocations());
        
        // Create command
        CommandInfo commandInfo = new RawCommandInfo("whoami");

        // Server info
        ServerInfo serverInfo = new ServerInfo("", hostname, port);

        // Output
        StandardOutReader commandOutput = new StandardOutReader();

        // Execute command
        try {
			CommandExecutor.executeCommand(commandInfo, serverInfo, authenticationInfo, commandOutput, new ConfigReader());
		} catch (SSHApiException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
        
        return true;
	}

}
