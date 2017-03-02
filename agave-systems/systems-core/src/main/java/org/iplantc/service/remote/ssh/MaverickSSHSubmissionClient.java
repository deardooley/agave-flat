package org.iplantc.service.remote.ssh;

/* HEADER */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.remote.ssh.net.SocketWrapper;
import org.iplantc.service.remote.ssh.shell.Shell;
import org.iplantc.service.remote.ssh.shell.ShellProcess;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.sftp.MultiFactorKBIRequestHandler;

import com.sshtools.net.ForwardingClient;
import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PublicKeyAuthentication;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshSession;
import com.sshtools.ssh.SshTransport;
import com.sshtools.ssh.SshTunnel;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh.components.jce.JCEComponentManager;
import com.sshtools.ssh2.KBIAuthentication;
import com.sshtools.ssh2.Ssh2Client;
import com.sshtools.ssh2.Ssh2Context;
import com.sshtools.ssh2.Ssh2PublicKeyAuthentication;


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
	
//	private SshClient ssh;
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
		try
		{
			/**
			 * Create an SshConnector instance
			 */
			con = SshConnector.createInstance();
			
			JCEComponentManager cm = (JCEComponentManager)ComponentManager.getInstance();
			cm.installArcFourCiphers(cm.supportedSsh2CiphersCS());
			cm.installArcFourCiphers(cm.supportedSsh2CiphersSC());
			
			((Ssh2Context)con.getContext()).setPreferredKeyExchange(Ssh2Context.KEX_DIFFIE_HELLMAN_GROUP14_SHA1);
//			((Ssh2Context)con.getContext()).setPreferredCipherCS(cipher);
//	        ((Ssh2Context)con.getContext()).setPreferredCipherSC(cipher);
//	        ((Ssh2Context)con.getContext()).setPreferredMacCS(mac);
//	        ((Ssh2Context)con.getContext()).setPreferredMacSC(mac);
	        
	        ((Ssh2Context)con.getContext()).setPreferredPublicKey(Ssh2Context.PUBLIC_KEY_SSHDSS);
	        ((Ssh2Context)con.getContext()).setPublicKeyPreferredPosition(Ssh2Context.PUBLIC_KEY_ECDSA_521, 1);
	        
	        ((Ssh2Context)con.getContext()).setPreferredCipherCS(Ssh2Context.CIPHER_ARCFOUR_256);
	        ((Ssh2Context)con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_ARCFOUR, 1);
	        ((Ssh2Context)con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);
	        
	        ((Ssh2Context)con.getContext()).setPreferredCipherSC(Ssh2Context.CIPHER_ARCFOUR_256);
	        ((Ssh2Context)con.getContext()).setCipherPreferredPositionSC(Ssh2Context.CIPHER_ARCFOUR, 1);
	        ((Ssh2Context)con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);
	        
	        ((Ssh2Context)con.getContext()).setPreferredMacCS(Ssh2Context.HMAC_SHA256);
	        ((Ssh2Context)con.getContext()).setMacPreferredPositionCS(Ssh2Context.HMAC_SHA1, 1);
	        ((Ssh2Context)con.getContext()).setMacPreferredPositionCS(Ssh2Context.HMAC_MD5, 2);
	        
	        ((Ssh2Context)con.getContext()).setPreferredMacSC(Ssh2Context.HMAC_SHA256);
	        ((Ssh2Context)con.getContext()).setMacPreferredPositionSC(Ssh2Context.HMAC_SHA1, 1);
	        ((Ssh2Context)con.getContext()).setMacPreferredPositionSC(Ssh2Context.HMAC_MD5, 2);
	        
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
			
			
			ssh2 = (Ssh2Client)con.connect(((SshTransport) new SocketWrapper(transport)), username, true);

			String[] authenticationMethods = ssh2.getAuthenticationMethods(username);
			int authStatus;
			
			if (!StringUtils.isEmpty(publicKey) && !StringUtils.isEmpty(privateKey))
			{
				/**
				 * Authenticate the user using password authentication
				 */
				auth = new Ssh2PublicKeyAuthentication();
				
				do {
					com.sshtools.publickey.SshPrivateKeyFile pkfile = com.sshtools.publickey.SshPrivateKeyFileFactory
							.parse(privateKey.getBytes());

					SshKeyPair pair;
					if (pkfile.isPassphraseProtected()) {
	                    pair = pkfile.toKeyPair(password);
					} else {
						pair = pkfile.toKeyPair(null);
					}

					((PublicKeyAuthentication)auth).setPrivateKey(pair.getPrivateKey());
					((PublicKeyAuthentication)auth).setPublicKey(pair.getPublicKey());
					
					authStatus = ssh2.authenticate(auth);
					
					if (authStatus == SshAuthentication.FURTHER_AUTHENTICATION_REQUIRED && 
							Arrays.asList(authenticationMethods).contains("keyboard-interactive")) {
						KBIAuthentication kbi = new KBIAuthentication();
						kbi.setUsername(username);
						kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, hostname, port));
						authStatus = ssh2.authenticate(kbi);
					}
				}
				while (authStatus != SshAuthentication.COMPLETE
						&& authStatus != SshAuthentication.FAILED
						&& authStatus != SshAuthentication.CANCELLED
						&& ssh2.isConnected());
			}
			else
			{
				/**
				 * Authenticate the user using password authentication
				 */
				auth = new com.sshtools.ssh.PasswordAuthentication();
				do
				{
					((PasswordAuthentication)auth).setPassword(password);
					
					auth = checkForPasswordOverKBI(authenticationMethods);
					
					authStatus = ssh2.authenticate(auth);
				}
				while (authStatus != SshAuthentication.COMPLETE
						&& authStatus != SshAuthentication.FAILED
						&& authStatus != SshAuthentication.CANCELLED
						&& ssh2.isConnected());
			}
			
			// now handle the tunnel if present to auth to the remote side
			if (!StringUtils.isEmpty(proxyHost)) 
			{	
				SshTunnel tunnel = ssh2.openForwardingChannel(hostname, port,
						"127.0.0.1", 22, "127.0.0.1", 22, null, null);

				forwardedConnection = con.connect(tunnel, username);
				
				if (StringUtils.isNotBlank(publicKey) && StringUtils.isNotBlank(privateKey))
				{
					/**
					 * Authenticate the user using password authentication
					 */
					auth = new Ssh2PublicKeyAuthentication();
					
					do {
//						com.sshtools.publickey.SshPrivateKeyFile pkfile = com.sshtools.publickey.SshPrivateKeyFileFactory
//								.parse(privateKey.getBytes());
//
//						SshKeyPair pair;
//						if (pkfile.isPassphraseProtected()) {
//		                    pair = pkfile.toKeyPair(password);
//						} else {
//							pair = pkfile.toKeyPair(null);
//						}
//
//						((PublicKeyAuthentication)auth).setPrivateKey(pair.getPrivateKey());
//						((PublicKeyAuthentication)auth).setPublicKey(pair.getPublicKey());
						
						authStatus = forwardedConnection.authenticate(auth);
						
						if (authStatus == SshAuthentication.FURTHER_AUTHENTICATION_REQUIRED && 
								Arrays.asList(authenticationMethods).contains("keyboard-interactive")) {
							KBIAuthentication kbi = new KBIAuthentication();
							kbi.setUsername(username);
							kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, hostname, port));
							authStatus = forwardedConnection.authenticate(kbi);
						}
					}
					while (authStatus != SshAuthentication.COMPLETE
							&& authStatus != SshAuthentication.FAILED
							&& authStatus != SshAuthentication.CANCELLED
							&& forwardedConnection.isConnected());
				}
				else
				{
					/**
					 * Authenticate the user using password authentication
					 */
//					auth = new com.sshtools.ssh.PasswordAuthentication();
					do
					{
//						((PasswordAuthentication)auth).setPassword(password);
						
						auth = checkForPasswordOverKBI(authenticationMethods);
						
						authStatus = forwardedConnection.authenticate(auth);
					}
					while (authStatus != SshAuthentication.COMPLETE
							&& authStatus != SshAuthentication.FAILED
							&& authStatus != SshAuthentication.CANCELLED
							&& forwardedConnection.isConnected());
				}
				
				return forwardedConnection.isAuthenticated();
			}
			else {
				return ssh2.isAuthenticated();
			}
		}
		catch (UnknownHostException e) {
			throw new RemoteExecutionException("Failed to resolve host " + hostname + ":" + port, e);
		}
		catch (Throwable t) {
			log.error("Unable to authenticate ", t);
			throw new RemoteExecutionException("Failed to authenticate to " + hostname + ":" + port, t);
		} 
	}
	
	/**
	 * Looks through the supported auth returned from the server and overrides the
	 * password auth type if the server only lists keyboard-interactive. This acts
	 * as a frontline check to override the default behavior and use our 
	 * {@link MultiFactorKBIRequestHandler}. 
	 *   
	 * @param authenticationMethods
	 * @return a {@link SshAuthentication} based on the ordering and existence of auth methods returned from the server.
	 */
	private SshAuthentication checkForPasswordOverKBI(String[] authenticationMethods) {
		boolean kbiAuthenticationPossible = false;
		for (int i = 0; i < authenticationMethods.length; i++) {
			if (authenticationMethods[i].equals("password")) {
				return auth;
			}
			if (authenticationMethods[i].equals("keyboard-interactive")) {

				kbiAuthenticationPossible = true;
			}
		}

		if (kbiAuthenticationPossible) {
			KBIAuthentication kbi = new KBIAuthentication();

			kbi.setUsername(username);

			kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, hostname, port));
			
			return kbi;
		}

		return auth;
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
					/**
					 * Start the users session. It also acts as a thread to service
					 * incoming channel requests for the port forwarding for both
					 * versions. Since we have a single threaded API we have to do
					 * this to send a timely response
					 */
					final SshSession session = forwardedConnection.openSessionChannel();
					session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
					if (session.startShell()) {
						shell = new Shell(forwardedConnection);
					}
					else {
						throw new RemoteExecutionException("Failed to establish interactive shell session to " 
								+ hostname + ":" + port + " when tunneled through " 
								+ proxyHost + ":" + proxyPort);
					}
						
					// fork the command on the remote system.
//					ShellProcess process = shell.executeCommand(command, true, "UTF-8");
//					InputStream in = null;
//					try 
//					{
////							// no idea why, but this fails if we don't wait for 8 seconds
//						long start = System.currentTimeMillis();
//						while (process.isActive() && (System.currentTimeMillis() - start) < (Settings.MAX_REMOTE_OPERATION_TIME * 1000)) {
//							log.debug("Process has succeeded: " + process.hasSucceeded() + "\n"  
//							            + "Process exit code: " + process.getExitCode() + "\n" 
//							            + "Process command output: " + process.getCommandOutput());
//							
//							Thread.sleep(1000);
//						}
	//
//						shell.exit();
//						return process.getCommandOutput();
//					} catch (Throwable t) {
//						throw new RemoteExecutionException("Failed to read response from " + hostname, t);
//					}
//					finally {
//						try { in.close(); } catch (Throwable e) {}
//						try { shell.exit(); } catch (Throwable e) {}
//					}
//				}
//				else {
//					throw new RemoteExecutionException("Failed to establish interactive shell session to " 
//							+ hostname + ":" + port + " when tunneled through " 
//							+ proxyHost + ":" + proxyPort);
//				}
					
					
//					final ForwardingClient fwd = new ForwardingClient(ssh2);
//					
//					fwd.allowX11Forwarding("localhost:0");
//					boolean remoteForwardingResponse = fwd.requestRemoteForwarding("127.0.0.1", 0,
//							hostname, port);
//					
//					if ( !remoteForwardingResponse ) {
//						throw new RemoteExecutionException("Failed to establish a remote tunnel to " + 
//								proxyHost + ":" + proxyPort);
//					}
//
//					/**
//					 * Start the users session. It also acts as a thread to service
//					 * incoming channel requests for the port forwarding for both
//					 * versions. Since we have a single threaded API we have to do
//					 * this to send a timely response
//					 */
//					final SshSession session = forwardedConnection.openSessionChannel();
//					session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
//					session.startShell();
//
//					/**
//					 * Start local forwarding after starting the users session.
//					 */
//					int randomlyChosenTunnelPort = fwd.startLocalForwardingOnRandomPort("127.0.0.1", 10, hostname, port);
//					
//					/**
//					 * Now that the local proxy tunnel is running, make the call to
//					 * the target server through the tunnel.
//					 */
//					MaverickSSHSubmissionClient proxySubmissionClient = null;
//					String proxyResponse = null;
//					try {
//						proxySubmissionClient = new MaverickSSHSubmissionClient("127.0.0.1", randomlyChosenTunnelPort, username,
//								password, null, proxyPort, publicKey, privateKey);
//						proxyResponse = proxySubmissionClient.runCommand(command);
//						return proxyResponse;
//					} 
//					catch (RemoteDataException e) {
//						throw e;
//					}
//					catch (Exception e) {
//						throw new RemoteExecutionException("Failed to connect to destination server " + hostname + ":" + port, e);
//					}
//					finally {
//						try { proxySubmissionClient.close(); } catch (Exception e) {}
//					}
				} 
				else 
				{
					// Some old SSH2 servers kill the connection after the first
					// session has closed and there are no other sessions started;
					// so to avoid this we create the first session and dont ever use it
					session = ssh2.openSessionChannel();
					session.requestPseudoTerminal("vt100", 80, 24, 0, 0);
					
					if (session.startShell()) {
						shell = new Shell(ssh2);
					}
					else {
						throw new RemoteExecutionException("Failed to establish interactive shell session to " 
								+ hostname + ":" + port);
					}
				}
				
//				
			
				// fork the command on the remote system.
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
			try { ssh2.disconnect(); } catch (Throwable t) {}
			try { forwardedConnection.disconnect(); } catch (Throwable t) {}
			try { session.close(); } catch (Throwable t) {}
		}
	}

	@Override
	public void close()
	{
		try { ssh2.disconnect(); } catch (Throwable t) {}
		try { session.close(); } catch (Throwable t) {}
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

	@Override
	public String getHost() {
		return hostname;
	}

	@Override
	public int getPort() {
		return port;
	}
}
