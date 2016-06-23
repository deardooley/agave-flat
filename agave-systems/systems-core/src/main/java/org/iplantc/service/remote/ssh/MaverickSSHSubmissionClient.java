package org.iplantc.service.remote.ssh;

/* HEADER */
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

import com.maverick.ssh.PasswordAuthentication;
import com.maverick.ssh.PublicKeyAuthentication;
import com.maverick.ssh.Shell;
import com.maverick.ssh.ShellProcess;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.SshSession;
import com.maverick.ssh.components.SshKeyPair;
import com.maverick.ssh1.Ssh1Client;
import com.maverick.ssh2.Ssh2Client;
import com.maverick.ssh2.Ssh2Context;
import com.sshtools.net.ForwardingClient;
import com.sshtools.net.SocketWrapper;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;


/**
 * Client to execute commands on remote systems via SSH. Tunneling is supported
 * when a proxyHost and proxyPort are specified. The authentication to the proxy
 * server is assumed to be the same as the target hostname.
 * 
 * @author Rion Dooley <dooley@tacc.utexas.edu>
 */
public class MaverickSSHSubmissionClient implements RemoteSubmissionClient 
{
	private static final Logger log = Logger.getLogger(MaverickSSHSubmissionClient.class);
	
	private SshClient ssh;
	private Ssh2Client ssh2 = null;
	private SshSession session;
	private SshConnector con;
	private SshClient forwardedConnection = null;
	private SshAuthentication auth;
	private String cipher = Ssh2Context.CIPHER_ARCFOUR;
	private String mac = "hmac-sha1";
	private Socket transport = null;
	
	protected String hostname;
	protected int port;
	protected String username;
	protected String password;
	protected String proxyHost;
	protected int proxyPort;
	protected String publicKey;
	protected String privateKey;
	
	public MaverickSSHSubmissionClient(String host, int port, String username,
			String password)
	{
		this.hostname = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	public MaverickSSHSubmissionClient(String host, int port, String username,
			String password, String proxyHost, int proxyPort)
	{
		this.hostname = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	
	public MaverickSSHSubmissionClient(String host, int port, String username,
			String password, String proxyHost, int proxyPort, String publicKey, String privateKey)
	{
		this.hostname = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}
	
	private boolean authenticate() throws RemoteExecutionException 
	{
		com.maverick.ssh.LicenseManager.addLicense(
		        "----BEGIN 3SP LICENSE----\r\n"
                + "Product : J2SSH Maverick\r\n"
                + "Licensee: UT Austin\r\n"
                + "Comments: Uncategorised Project\r\n"
                + "Type    : Protocol License\r\n"
                + "Created : 17-Jul-2013\r\n"
                + "\r\n"
                + "37872036ADA42FDBA600F3CF9CCEF4C860D05C25E5DBBB3C\r\n"
                + "C6D2DDE3753D77E0B59ACF0D4BB95AEBB47533DC0480346B\r\n"
                + "0C533BA21F8F45D6B29B66DE266EF9EFCF062C48EBE72E0A\r\n"
                + "7110EE0CECDA317DE5BE4B099B6F47C28E610EBD30DEB0BE\r\n"
                + "7A4E9163CEB49C718C799848514835C959CC92AA00051613\r\n"
                + "E65F250C3E3442306B39A1257BC4A74BED6D4475FB30A94C\r\n"
                + "----END 3SP LICENSE----\r\n");
		
		try
		{
			/**
			 * Create an SshConnector instance
			 */
			con = SshConnector.createInstance();
			((Ssh2Context)con.getContext(2)).setPreferredKeyExchange(Ssh2Context.KEX_DIFFIE_HELLMAN_GROUP14_SHA1);
//			((Ssh2Context)con.getContext(2)).setPreferredCipherCS(cipher);
//	        ((Ssh2Context)con.getContext(2)).setPreferredCipherSC(cipher);
//	        ((Ssh2Context)con.getContext(2)).setPreferredMacCS(mac);
//	        ((Ssh2Context)con.getContext(2)).setPreferredMacSC(mac);
	        
	        ((Ssh2Context)con.getContext(2)).setPreferredPublicKey(Ssh2Context.PUBLIC_KEY_SSHDSS);
	        ((Ssh2Context)con.getContext(2)).setPublicKeyPreferredPosition(Ssh2Context.PUBLIC_KEY_ECDSA_521, 1);
	        
	        ((Ssh2Context)con.getContext(2)).setPreferredCipherCS(Ssh2Context.CIPHER_ARCFOUR_256);
	        ((Ssh2Context)con.getContext(2)).setCipherPreferredPositionCS(Ssh2Context.CIPHER_ARCFOUR, 1);
	        ((Ssh2Context)con.getContext(2)).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);
	        
	        ((Ssh2Context)con.getContext(2)).setPreferredCipherSC(Ssh2Context.CIPHER_ARCFOUR_256);
	        ((Ssh2Context)con.getContext(2)).setCipherPreferredPositionSC(Ssh2Context.CIPHER_ARCFOUR, 1);
	        ((Ssh2Context)con.getContext(2)).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);
	        
	        ((Ssh2Context)con.getContext(2)).setPreferredMacCS(Ssh2Context.HMAC_SHA256);
	        ((Ssh2Context)con.getContext(2)).setMacPreferredPositionCS(Ssh2Context.HMAC_SHA1, 1);
	        ((Ssh2Context)con.getContext(2)).setMacPreferredPositionCS(Ssh2Context.HMAC_MD5, 2);
	        
	        ((Ssh2Context)con.getContext(2)).setPreferredMacSC(Ssh2Context.HMAC_SHA256);
	        ((Ssh2Context)con.getContext(2)).setMacPreferredPositionSC(Ssh2Context.HMAC_SHA1, 1);
	        ((Ssh2Context)con.getContext(2)).setMacPreferredPositionSC(Ssh2Context.HMAC_MD5, 2);
	        
			// Verify server host keys using the users known_hosts file
			//con.setKnownHosts(new ConsoleKnownHostsKeyVerification());
			
			/**
			 * Connect to the host
			 */
//			if (StringUtils.isEmpty(proxyHost)) {
//				transport = new SocketTransport(hostname, port);
//			} else {
//				transport = new SocketTransport(proxyHost, proxyPort);
//			}
	        
	        SocketAddress sockaddr = null; 
            transport = new Socket();
            
            if (StringUtils.isEmpty(proxyHost)) {
                sockaddr = new InetSocketAddress(hostname, port);
            } else {
                sockaddr = new InetSocketAddress(proxyHost, proxyPort);
            }
            
            transport.connect(sockaddr, 15000);
            transport.setTcpNoDelay(true);
			transport.setSoTimeout(30000);
			
			ssh = con.connect(new SocketWrapper(transport), username, true);

			/**
			 * Determine the version
			 */
			if (ssh instanceof Ssh1Client)
			{
				ssh.disconnect();
				
				throw new RemoteDataException(hostname
						+ " is an SSH1 server!! SFTP is not supported");
			}
			
			ssh2 = (Ssh2Client) ssh;
			if (!StringUtils.isEmpty(publicKey) && !StringUtils.isEmpty(privateKey))
			{
				/**
				 * Authenticate the user using password authentication
				 */
				auth = new PublicKeyAuthentication();
				
				do {
					SshPrivateKeyFile pkfile = SshPrivateKeyFileFactory.parse(privateKey.getBytes());

					SshKeyPair pair;
					if (pkfile.isPassphraseProtected()) {
	                    pair = pkfile.toKeyPair(password);
					} else {
						pair = pkfile.toKeyPair(null);
					}

					((PublicKeyAuthentication)auth).setPrivateKey(pair.getPrivateKey());
					((PublicKeyAuthentication)auth).setPublicKey(pair.getPublicKey());
				}
				while (ssh2.authenticate(auth) != SshAuthentication.COMPLETE
						&& ssh.isConnected());
			}
			else
			{
				/**
				 * Authenticate the user using password authentication
				 */
				auth = new com.maverick.ssh.PasswordAuthentication();
	
				do
				{
					((PasswordAuthentication)auth).setPassword(password);
				}
				while (ssh2.authenticate(auth) != SshAuthentication.COMPLETE
						&& ssh.isConnected());
			}
			
			return ssh2.isAuthenticated();
		}
		catch (UnknownHostException e) {
			throw new RemoteExecutionException("Failed to resolve host " + hostname + ":" + port, e);
		}
		catch (Throwable t) {
			log.error("Unable to authenticate ", t);
			throw new RemoteExecutionException("Failed to authenticate to " + hostname + ":" + port, t);
		} 
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.remote.RemoteSubmissionClient#runCommand(java.lang.String)
	 */
	public String runCommand(String command) throws Exception
	{
		Shell shell = null;
		try 
		{
			log.debug("Forking command " + command + " on " + hostname + ":" + port);
			
			/**
			 * Start a session and do basic IO
			 */
			if (authenticate())
			{
				if (!StringUtils.isEmpty(proxyHost)) 
				{
					final ForwardingClient fwd = new ForwardingClient(ssh2);
					
					fwd.allowX11Forwarding("localhost:0");
					int resp = fwd.requestRemoteForwarding("127.0.0.1", 0,
							hostname, port);
					
					if ( resp == 0 ) {
						throw new RemoteDataException("Failed to establish a remote tunnel to " + 
								proxyHost + ":" + proxyPort);
					}

					/**
					 * Start the users session. It also acts as a thread to service
					 * incoming channel requests for the port forwarding for both
					 * versions. Since we have a single threaded API we have to do
					 * this to send a timely response
					 */
					final SshSession session = ssh.openSessionChannel();
					session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
					session.startShell();

					/**
					 * Start local forwardings after starting the users session.
					 */
					fwd.startLocalForwarding("127.0.0.1", resp, hostname, port);
					
					/**
					 * Now that the local proxy tunnel is running, make the call to
					 * the target server through the tunnel.
					 */
					MaverickSSHSubmissionClient proxySubmissionClient = null;
					String proxyResponse = null;
					try {
						proxySubmissionClient = new MaverickSSHSubmissionClient("127.0.0.1", resp, username,
								password, null, proxyPort, publicKey, privateKey);
						proxyResponse = proxySubmissionClient.runCommand(command);
						return proxyResponse;
					} 
					catch (RemoteDataException e) {
						throw e;
					}
					catch (Exception e) {
						throw new RemoteDataException("Failed to connect to destination server " + hostname + ":" + port, e);
					}
					finally {
						try { proxySubmissionClient.close(); } catch (Exception e) {}
					}
				} 
				else 
				{
					// Some old SSH2 servers kill the connection after the first
					// session has closed and there are no other sessions started;
					// so to avoid this we create the first session and dont ever use it
					session = ssh2.openSessionChannel();
					session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
					
					shell = new Shell(ssh2);
					
					ShellProcess process = shell.executeCommand(command, true, "UTF-8");
					InputStream in = null;
					try 
					{
//						// no idea why, but this fails if we don't wait for 8 seconds
						long start = System.currentTimeMillis();
						while (process.isActive() && (System.currentTimeMillis() - start) < (Settings.MAX_REMOTE_OPERATION_TIME * 1000)) {
							log.debug("Process has succeeded: " + process.hasSucceeded() + "\n"  
							            + "Process exit code: " + process.getExitCode() + "\n" 
							            + "Process command output: " + process.getCommandOutput());
							
							Thread.sleep(1000);
						}

						shell.exit();
						return process.getCommandOutput();
					} catch (Throwable t) {
						throw new RemoteExecutionException("Failed to read response from " + hostname, t);
					}
					finally {
						try { in.close(); } catch (Throwable e) {}
						try { shell.exit(); } catch (Throwable e) {}
					}
				}
			}
			else
			{
				throw new RemoteExecutionException("Failed to authenticate to " + hostname);
			}
		}
		catch (RemoteExecutionException e) {
			throw e;
		}
		catch (Throwable t)
		{
			throw new RemoteExecutionException("Failed to execute command on " + hostname, t);
		} 
		finally 
		{
			try { ssh.disconnect(); } catch (Throwable t) {}
			try { ssh2.disconnect(); } catch (Throwable t) {}
			try { forwardedConnection.disconnect(); } catch (Throwable t) {}
			try { shell.exit(); } catch (Throwable t) {}
			try { session.close(); } catch (Throwable t) {}
		}
	}

	@Override
	public void close()
	{
		try { ssh.disconnect(); } catch (Throwable t) {}
		try { session.close(); } catch (Throwable t) {}
		ssh = null;
		ssh2 = null;
		forwardedConnection = null;
		session = null;
	}

	@Override
	public boolean canAuthentication()
	{
		try {
			log.debug("Verifying authentication to " + hostname + ":" + port);
			return authenticate();
		} catch (RemoteExecutionException e) {
			return false;
		} finally {
			close();
		}
	}
}
