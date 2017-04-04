package org.iplantc.service.transfer.sftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.remote.ssh.MaverickSSHSubmissionClient;
import org.iplantc.service.remote.ssh.shell.Shell;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.sftp.DirectoryOperation;
import com.sshtools.sftp.SftpClient;
import com.sshtools.sftp.SftpFile;
import com.sshtools.sftp.SftpFileAttributes;
import com.sshtools.sftp.SftpStatusException;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PublicKeyAuthentication;
//import com.sshtools.ssh.Shell;
//import com.sshtools.ssh.ShellProcess;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshTunnel;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh.components.jce.JCEComponentManager;
import com.sshtools.ssh2.KBIAuthentication;
import com.sshtools.ssh2.Ssh2Client;
import com.sshtools.ssh2.Ssh2Context;
import com.sshtools.ssh2.Ssh2PublicKeyAuthentication;

/**
 * Generic SFTP client to interact with remote systems.
 * 
 * @author dooley
 *
 */
public class MaverickSFTP implements RemoteDataClient
{
	private static final Logger log = Logger.getLogger(MaverickSFTP.class);
	
	static {
		try {
		
		} 
		catch (Throwable t) {
			log.error(t);
		}
		
	}
	
	
	private SftpClient sftpClient = null;
	private Ssh2Client ssh2 = null;
	private SshClient forwardedConnection = null;
	
	protected String host;
	protected int port;
	protected String username;
	protected String password;
	protected String rootDir = "";
	protected String homeDir = "";
	protected String proxyHost;
	protected int proxyPort;
	protected String publicKey;
	protected String privateKey;
	protected SshConnector con;
	protected SshAuthentication auth;
	private Map<String, SftpFileAttributes> fileInfoCache = new ConcurrentHashMap<String, SftpFileAttributes>();
	
    protected static final int MAX_BUFFER_SIZE = 32768 * 64; // 1MB
	
    public MaverickSFTP(String host, int port, String username, String password, String rootDir, String homeDir)
	{
		this.host = host;
		this.port = port > 0 ? port : 22;
		this.username = username;
		this.password = password;
		
		updateSystemRoots(rootDir, homeDir);
	}

	public MaverickSFTP(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort)
	{
		this.host = host;
		this.port = port > 0 ? port : 22;
		this.username = username;
		this.password = password;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		
		updateSystemRoots(rootDir, homeDir);
	}
	
	public MaverickSFTP(String host, int port, String username, String password, String rootDir, String homeDir, String publicKey, String privateKey)
	{
		this.host = host;
		this.port = port > 0 ? port : 22;
		this.username = username;
		this.password = password;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		
		updateSystemRoots(rootDir, homeDir);
	}

	public MaverickSFTP(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort, String publicKey, String privateKey)
	{
		this.host = host;
		this.port = port > 0 ? port : 22;
		this.username = username;
		this.password = password;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		
		updateSystemRoots(rootDir, homeDir);
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getHomeDir()
	 */
	@Override
	public String getHomeDir() {
		return this.homeDir;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getRootDir()
	 */
	@Override
	public String getRootDir() {
		return this.rootDir;
	}	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#updateSystemRoots(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateSystemRoots(String rootDir, String homeDir)
	{
		rootDir = FilenameUtils.normalize(rootDir);
		rootDir = StringUtils.stripEnd(rootDir," ");
        if (!StringUtils.isEmpty(rootDir)) {
			this.rootDir = rootDir;
			if (!this.rootDir.endsWith("/")) {
				this.rootDir += "/";
			}
		} else {
			this.rootDir = "/";
		}

        homeDir = FilenameUtils.normalize(homeDir);
        if (!StringUtils.isEmpty(homeDir)) {
            this.homeDir = this.rootDir +  homeDir;
            if (!this.homeDir.endsWith("/")) {
                this.homeDir += "/";
            }
        } else {
            this.homeDir = this.rootDir;
        }

        this.homeDir = StringUtils.stripEnd(this.homeDir.replaceAll("/+", "/")," ");
        this.rootDir = StringUtils.stripEnd(this.rootDir.replaceAll("/+", "/")," ");
        
    }
	
	@Override
	public void authenticate() throws IOException, RemoteDataException 
	{
	    // clear cache here as we may have stale information in between authentications
	    fileInfoCache.clear();
	    
		if (ssh2 != null && ssh2.isConnected() && ssh2.isAuthenticated()) {
			return;
		}
		
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
	        
	        /**
			 * Connect to the host
			 */
//	         SocketTransport t = null;
//          if (StringUtils.isEmpty(proxyHost)) {
//              t = new SocketTransport(host, port);
//          } else {
//              t = new SocketTransport(proxyHost, proxyPort);
//          }
	        
	        SocketAddress sockaddr = null; 
	        Socket t = new Socket();

			if (StringUtils.isEmpty(proxyHost)) {
			    sockaddr = new InetSocketAddress(host, port);
			} else {
				sockaddr = new InetSocketAddress(proxyHost, proxyPort);
			}
			
			t.connect(sockaddr, 15000);
			
			t.setTcpNoDelay(true);
			t.setPerformancePreferences(0, 1, 2);
			t.setSendBufferSize(MAX_BUFFER_SIZE);
	        t.setReceiveBufferSize(MAX_BUFFER_SIZE);
			
			SshClient ssh = con.connect(new com.sshtools.net.SocketWrapper(t), username);
			
			/**
			 * Determine the version
			 */
//			if (ssh instanceof Ssh1Client)
//			{
//				ssh.disconnect();
//				
//				throw new RemoteDataException(host
//						+ " is an SSH1 server!! SFTP is not supported");
//			}
			
			ssh2 = (Ssh2Client) ssh;
			
			String[] authenticationMethods = ssh2.getAuthenticationMethods(username);
			int authStatus;
			
			if (!StringUtils.isEmpty(publicKey) && !StringUtils.isEmpty(privateKey))
			{
				/**
				 * Authenticate the user using password authentication
				 */
				auth = new Ssh2PublicKeyAuthentication();
				
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
					authStatus = ssh2.authenticate(auth);
					
					if (authStatus == SshAuthentication.FURTHER_AUTHENTICATION_REQUIRED && 
							Arrays.asList(authenticationMethods).contains("keyboard-interactive")) {
						KBIAuthentication kbi = new KBIAuthentication();
						kbi.setUsername(username);
						kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));
						authStatus = ssh2.authenticate(kbi);
					}
				}
				while (authStatus != SshAuthentication.COMPLETE 
						&& authStatus != SshAuthentication.FAILED
						&& authStatus != SshAuthentication.CANCELLED
						&& ssh.isConnected());
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
						&& ssh.isConnected());
			}
			
			if (!ssh.isAuthenticated()) {
				throw new RemoteDataException("Failed to authenticate to " + host);
			}
			else if (!StringUtils.isEmpty(proxyHost)) {
				SshTunnel tunnel = ssh.openForwardingChannel(host, port,
						"127.0.0.1", 22, "127.0.0.1", 22, null, null);
				
				forwardedConnection = con.connect(tunnel, username);
				forwardedConnection.authenticate(auth);
			} 
			else {
				// we're connected. carry on
			}
		}
		catch (SshException e) {
			throw new RemoteDataException("Failed to authenticate to " + host, e);
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (ConnectException e) {
			throw new RemoteDataException("Connection refused: Unable to contact SFTP server at " + host + ":" + port, e);
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to authenticate to " + host, e);
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

			kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));
			
			return kbi;
		}

		return auth;
	}
	
	private boolean useTunnel() 
	{
		return (!StringUtils.isEmpty(proxyHost));
	}

    @Override
    public int getMaxBufferSize() {
        return MAX_BUFFER_SIZE;
    }

    protected SftpClient getClient() throws IOException, RemoteDataException
	{
		if (ssh2 == null || !ssh2.isConnected()) {
			authenticate();
		}
		
		try
		{
			if (sftpClient == null || sftpClient.isClosed()) {
				if (useTunnel()) {
					sftpClient = new SftpClient(forwardedConnection);
				} else {
					sftpClient = new SftpClient(ssh2);
				}
				sftpClient.setBufferSize(50 * MAX_BUFFER_SIZE);
				sftpClient.setMaxAsyncRequests(256);
			}
			
			return sftpClient;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to establish a connection to " + host, e);
		}
	}
	
	@Override
	public MaverickSFTPInputStream getInputStream(String path, boolean passive) throws IOException, RemoteDataException
	{
		path = resolvePath(path);
		
		return new MaverickSFTPInputStream(getClient(), path);
	}
	
	@Override
	public MaverickSFTPOutputStream getOutputStream(String path, boolean passive, boolean append)
	throws IOException, FileNotFoundException, RemoteDataException
	{	
		String resolvedPath = resolvePath(path);
		SftpClient client = getClient();
		
		// workaround because maverick throws an exception if an output stream is opened to
		// a file that does not exist.
		if (!doesExist(path)) 
		{
			try
			{
				// Upload a file with the content of an empty string.
				ByteArrayInputStream ins = new ByteArrayInputStream("".getBytes());
				client.put(ins, resolvedPath);
			}
			catch (SftpStatusException e) {
				if (e.getMessage().toLowerCase().contains("no such file")) {
					throw new FileNotFoundException("No such file or directory");
				} else {
					throw new RemoteDataException("Failed to establish output stream to " + path, e);
				}
			}
			catch (Exception e) {
				throw new RemoteDataException("Failed to open an output stream to " + path, e);
			} 
		}
		
		return new MaverickSFTPOutputStream(client, resolvedPath);
	}

	@Override
	public List<RemoteFileInfo> ls(String remotedir)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		remotedir = resolvePath(remotedir);
		
		List<RemoteFileInfo> fileList = new ArrayList<RemoteFileInfo>();
		
		try 
		{
			SftpFile[] files = getClient().ls(remotedir);

			for (SftpFile file: files) 
			{
				if (file.getFilename().equals(".") || file.getFilename().equals("..")) continue;
				fileList.add(new RemoteFileInfo(file));
			}
			Collections.sort(fileList);
			return fileList;
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to list directory " + remotedir, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to list directory " + remotedir, e);
		}
	}

	@Override
	public void get(String remotedir, String localdir)
	throws IOException, FileNotFoundException, RemoteDataException
    {
		get(remotedir, localdir, null);
	}

	@Override
	public void get(String remotedir, String localdir, RemoteTransferListener listener) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		try 
		{
			if (isDirectory(remotedir)) 
			{
				File localDir = new File(localdir);
				
				// if local directory is not there
				if (!localDir.exists()) 
				{
					// if parent is not there, throw exception
					if (!localDir.getParentFile().exists()) {
						throw new FileNotFoundException("No such file or directory");
					}
					// otherwise we will download folder and give it a new name locally
					else {
						//localDir = new File(localDir, FilenameUtils.getName(remotedir));
					}
				} 
				// can't download folder to an existing file
				else if (!localDir.isDirectory()) 
				{
					throw new RemoteDataException("Cannot download file to " + localdir + ". Local path is not a directory.");
				}
				else
				{
					// downloading to existing directory and keeping name
					localDir = new File(localDir, FilenameUtils.getName(remotedir));
				}
				DirectoryOperation operation = getClient().copyRemoteDirectory(resolvePath(remotedir), localDir.getAbsolutePath(), true, false, true, listener);
				if (operation != null && !operation.getFailedTransfers().isEmpty()) {
					throw new RemoteDataException("One or more files failed to be retrieved from " + remotedir);
				}
				if (!localDir.exists()) {
					throw new RemoteDataException("Failed get directory from " + remotedir);
				}
			} 
			else 
			{
				File localFile = new File(localdir);
				
				// verify local path and explicity resolve target path
				if (!localFile.exists()) {
					if (!localFile.getParentFile().exists()) {
						throw new FileNotFoundException("No such file or directory");
					} 
				} 
				// if not a directory, overwrite local file
				else if (!localFile.isDirectory()) {
					
				}
				// if a directory, resolve full path
				else {
					localFile = new File(localFile,  FilenameUtils.getName(remotedir));
				}
				
				getClient().get(resolvePath(remotedir), localFile.getAbsolutePath(), listener);
				// make sure file transferred
				if (!localFile.exists()) {
					throw new RemoteDataException("Failed get file from " + remotedir);
				} 
				// we could do a size check here...meah
			}
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to get data from " + remotedir, e);
			}
		}
		catch (IOException e) {
			throw e;
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to get data from " + remotedir, e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
	 */
	@Override
	public void append(String localpath, String remotepath)
    throws IOException, FileNotFoundException, RemoteDataException
    {
        append(localpath, remotepath, null);
    }
	
    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String, org.iplantc.service.transfer.RemoteTransferListener)
     */
    @Override
    public void append(String localpath, String remotepath, RemoteTransferListener listener) 
    throws IOException, FileNotFoundException, RemoteDataException
    {
        File localFile = new File(localpath);
        if (!localFile.exists()) {
            throw new FileNotFoundException("No such file or directory");
        } 
        
        try 
        {
            if (!doesExist(remotepath)) 
            {
                put(localpath, remotepath, listener);
            }
            else if (localFile.isDirectory()) 
            {   
                throw new RemoteDataException("cannot append to a directory");
            }
            else 
            {
                String resolvedPath = resolvePath(remotepath);
                
                // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);
                long position = Math.max(length(resolvedPath) - 1, 0);
                getClient().put(new FileInputStream(localFile), resolvedPath, listener, position);
            }
        }
        catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to append data to " + remotepath, e);
            }
        }
        catch (IOException e) {
            throw e;
        } 
        catch (Exception e) {
            throw new RemoteDataException("Failed to append data to " + remotepath, e);
        }
    }
	
	@Override
	public void put(String localdir, String remotedir)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		put(localdir, remotedir, null);
	}

	@Override
	public void put(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		File localFile = new File(localdir);
		if (!localFile.exists()) {
			throw new FileNotFoundException("No such file or directory");
		} 
		
		try
		{
		    if (localFile.isDirectory()) 
			{	
			    // can't upload folder to an existing file
				if (doesExist(remotedir)) 
				{
					// can't put dir to file
					if (!isDirectory(remotedir)) {
						throw new RemoteDataException("cannot overwrite non-directory: " + remotedir + " with directory " + localFile.getName());
					} 
					else 
					{
						remotedir += (StringUtils.isEmpty(remotedir) ? "" : "/") + localFile.getName();
					}
				}
				else if (doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/..")))
				{
					// this folder will be created
				}
				else
				{
					// upload and keep name.
					throw new FileNotFoundException("No such file or directory");
				}
				
				// bust cache since this file has now changed
				String resolvedPath = resolvePath(remotedir);
				
				fileInfoCache.remove(resolvedPath);
				
				DirectoryOperation operation = getClient().copyLocalDirectory(
						localFile.getAbsolutePath(), resolvedPath, true, false, true, listener);
				
				if (operation != null && !operation.getFailedTransfers().isEmpty()) {
					throw new RemoteDataException("One or more files failed to be transferred to " + remotedir);
				}
			} 
			else 
			{
			    String resolvedPath = resolvePath(remotedir);
			    
			    // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);
                
                getClient().put(localFile.getAbsolutePath(), resolvedPath, listener);
			}
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to put data to " + remotedir, e);
			}
		}
		catch (IOException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new RemoteDataException("Failed to put data to " + remotedir, e);
		}
	}
	
	@Override
	public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		File localFile = new File(localdir);
		if (!localFile.exists()) {
			throw new FileNotFoundException("No such file or directory");
		} 
		
		try
		{
		    // invalidate this now so the existence check isn't stale
		    fileInfoCache.remove(resolvePath(remotedir));
			if (!doesExist(remotedir) || !doesExist(remotedir)) {
				put(localdir, remotedir, listener);
				return;
			}
			else if (localFile.isDirectory()) 
			{	
				String adjustedRemoteDir = remotedir;
				
				// can't put dir to file
				if (!isDirectory(adjustedRemoteDir)) {
					delete(adjustedRemoteDir);
					put(localdir, adjustedRemoteDir, listener);
					return;
				} 
				else 
				{
					adjustedRemoteDir += (StringUtils.isEmpty(remotedir) ? "" : "/") + localFile.getName();
				}
				
				for (File child: localFile.listFiles())
				{
					String childRemotePath = adjustedRemoteDir + "/" + child.getName();
					TransferTask childTask = null;
					if (listener != null && listener.getTransferTask() != null) {
						TransferTask parentTask = listener.getTransferTask();
						String srcPath = parentTask.getSource() + 
								(StringUtils.endsWith(parentTask.getSource(), "/") ? "" : "/") + 
								child.getName();
						childTask = new TransferTask(srcPath, 
								                    resolvePath(childRemotePath), 
								                    parentTask.getOwner(), 
								                    parentTask.getRootTask(), 
								                    parentTask);
						TransferTaskDao.persist(childTask);
					}
					
					if (child.isDirectory()) 
					{
						// local is a directory, remote is a file. delete remote file. we will replace with local directory
						try {
							if (isFile(childRemotePath)) {
								delete(childRemotePath);
							}
						} catch (FileNotFoundException e) {}
						
						// now create the remote directory
						mkdir(childRemotePath);
						
						// sync the folder now that we've cleaned up
						syncToRemote(child.getAbsolutePath(), adjustedRemoteDir, childTask == null ? null : new RemoteTransferListener(childTask));
					} 
					else
					{
						syncToRemote(child.getAbsolutePath(), childRemotePath, childTask == null ? null : new RemoteTransferListener(childTask));
					}
				}
			} 
			else 
			{
			    String resolvedPath = resolvePath(remotedir);
                
				// sync if file is not there
				if (!doesExist(remotedir))  
				{
				    // bust cache since this file has now changed
                    fileInfoCache.remove(resolvedPath);
                    
                    getClient().put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
				}
				else 
				{
					RemoteFileInfo fileInfo = getFileInfo(remotedir);
					
	                // if the types mismatch, delete remote, use current
					if (localFile.isDirectory() && !fileInfo.isDirectory() || 
							localFile.isFile() && !fileInfo.isFile()) 
					{
						delete(remotedir);
						
						// bust cache since this file has now changed
                        fileInfoCache.remove(resolvedPath);
                        
                        getClient().put(localFile.getAbsolutePath(), resolvedPath, listener);
					} 
					// or if the file sizes are different
					else if (localFile.length() != fileInfo.getSize())
					{
					    // bust cache since this file has now changed
		                fileInfoCache.remove(resolvedPath);
		                
		                getClient().put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
					}
					else
					{
						// manually update the listener since there will be no callback from the underlying
						// client when we skip this transfer.
						if (listener != null) {
							listener.skipped(fileInfo.getSize(), resolvePath(remotedir));
						}
						log.debug("Skipping transfer of " + localFile.getPath() + " to " + 
								remotedir + " because file is present and of equal size.");
					}
				}
			}
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to put data to " + remotedir, e);
			}
		}
		catch (IOException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new RemoteDataException("Failed to put data to " + remotedir, e);
		}
	}
	
	/**
	 * Internal method to fetch and cache metadata for a remote file/folder.
	 * This speeds up most operations as the logic of this adaptor heavily
	 * relies on explicit metadata checking for each operation.
	 * 
	 * @param resolvedPath
	 * @return 
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	protected SftpFileAttributes stat(String resolvedPath) 
	throws SftpStatusException, SshException, IOException, RemoteDataException 
	{
//	    String resolvedPath = resolvePath(remotepath);
	    
	    try {
    	    
            SftpFileAttributes atts = fileInfoCache.get(resolvedPath);
            if (atts == null) 
            {
                atts = getClient().stat(StringUtils.removeEnd(resolvedPath, "/"));
                
                // adjust for links so we get info about the referenced file/folder
                if (atts != null && atts.isLink()) {
                    atts = getClient().statLink(resolvedPath);
                }
                
                if (atts != null) {
                    fileInfoCache.put(resolvedPath, atts);
                }
            }
            return atts;
	    } catch (SftpStatusException | SshException | IOException | RemoteDataException e) {
	        fileInfoCache.remove(resolvedPath);
	        throw e;
	    }
	}

	@Override
	public boolean isDirectory(String remotepath)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		try 
		{
			return stat(resolvePath(remotepath)).isDirectory();
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
		} 
	}

	@Override
	public boolean isFile(String remotepath) 
	throws IOException, FileNotFoundException, RemoteDataException		
	{
		try 
		{   
			return stat(resolvePath(remotepath)).isFile();
		} 
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
		} 
	}

	@Override
	public long length(String remotepath) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		try 
		{
			return stat(resolvePath(remotepath)).getSize().longValue();
		} 
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve length of " + remotepath, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
		} 
	}

	@Override
	public String checksum(String remotepath) 
	throws IOException, FileNotFoundException, RemoteDataException, NotImplementedException
	{
		try
		{
			if (isDirectory(remotepath)) {
				throw new RemoteDataException("Directory cannot be checksummed.");
			} else {
				throw new NotImplementedException();
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (NotImplementedException e) {
			throw e;
		}
	}

	@Override
	public void doRename(String oldpath, String newpath) 
	throws IOException, FileNotFoundException, RemoteDataException, RemoteDataSyntaxException
	{
		String resolvedSourcePath = null;
		String resolvedDestPath = null;
		try
		{
			resolvedSourcePath = resolvePath(oldpath);
			resolvedDestPath = resolvePath(newpath);
			
//			if (StringUtils.startsWith(resolvedDestPath, resolvedSourcePath)) {
//				throw new RemoteDataException("Cannot rename a file or director into its own subtree");
//			}
			resolvedSourcePath = StringUtils.removeEnd(resolvedSourcePath, "/");
			resolvedDestPath = StringUtils.removeEnd(resolvedDestPath, "/");
			
			getClient().rename(resolvedSourcePath, resolvedDestPath);
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} 
			else if (doesExistSafe(resolvedDestPath)) {
				throw new RemoteDataException("Destination already exists: " + newpath);
			}
			else {
				throw new RemoteDataException("Failed to rename " + oldpath + " to " + newpath, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to rename " + oldpath + " to " + newpath, e);
		} 
	}

	/* 
	 * The SFTP copy method performs a remote copy on a file or folder. Unlike
	 * other RemoteDataClients, this call is done purely server side and as such
	 * requires no data movement. It should be nearly instantaneous. The down side
	 * is that it opens a session to the remote system, so this may run into 
	 * trouble if the environment isn't stable. 
	 * 
	 * @param remotefromdir remote source
	 * @param remotetodir remote destination
	 * @throws IOException, RemoteDataException
	 */
	@Override
	public void copy(String remotefromdir, String remotetodir) 
	throws IOException, RemoteDataException, RemoteDataSyntaxException
	{
		copy(remotefromdir, remotetodir, null);
	}
	
	/* 
	 * The SFTP copy method performs a remote copy on a file or folder. Unlike
	 * other RemoteDataClients, this call is done purely server side and as such
	 * requires no data movement. It should be nearly instantaneous. The down side
	 * is that it opens a session to the remote system, so this may run into 
	 * trouble if the environment isn't stable. 
	 * 
	 * @param remotesrc remote source. If a folder, the contents will be copied to the {@code remotedest}
	 * @param remotedest remote destination. If a folder, it will receive the contents of the {@code remotesrc}
	 * @param listener The listener to update. This will be updated on start and 
	 * finish with no updated inbetween.
	 * @throws IOException, RemoteDataException
	 */
	@Override
	public void copy(String remotesrc, String remotedest, RemoteTransferListener listener) 
	throws IOException, FileNotFoundException, RemoteDataException, RemoteDataSyntaxException
	{
		if (!doesExist(remotesrc)) {
			throw new FileNotFoundException("No such file or directory");
		}
		
		String resolvedSrc = resolvePath(remotesrc);
		String resolvedDest = resolvePath(remotedest);
		
		Shell shell = null;
		try
		{
			if (ssh2.isAuthenticated()) 
			{	
//				if (useTunnel()) 
//				{	
//					SshTunnel tunnel = ssh2.openForwardingChannel(host, port,
//							"127.0.0.1", 22, "127.0.0.1", 22, null, null);
//	
//					forwardedConnection = con.connect(tunnel, username);
//					forwardedConnection.authenticate(auth);
////					session = forwardedConnection.openSessionChannel();
//					shell = new Shell(forwardedConnection);
//				} else {
//					// Some old SSH2 servers kill the connection after the first
//					// session has closed and there are no other sessions started;
//					// so to avoid this we create the first session and dont ever use it
//					//session = ssh2.openSessionChannel();
//					shell = new Shell(ssh2);
//				}
				
				long remoteDestLength = length(remotesrc);
				if (listener != null) {
					listener.started(remoteDestLength, remotedest);
				}
				
				fileInfoCache.remove(resolvedDest);
                
				if (isDirectory(remotesrc)) {
				    resolvedSrc = StringUtils.stripEnd(resolvedSrc, "/") + "/.";
				} 
				
//				String copyCommand = String.format("cp -rLf \"%s\" \"%s\"", resolvedSrc, resolvedDest);
//				log.debug("Performing remote copy on " + host + ": " + copyCommand);
//				ShellProcess process = shell.executeCommand(copyCommand);
//				
//				int r;
//				StringBuilder builder = new StringBuilder();
//				while((r = process.getInputStream().read()) > -1) {
//				    builder.append((char)r);
//				}
//
//				shell.exit();
				
				String copyCommand = String.format("cp -rLf \"%s\" \"%s\"", resolvedSrc, resolvedDest);
				log.debug("Performing remote copy on " + host + ": " + copyCommand);
				
				MaverickSSHSubmissionClient proxySubmissionClient = null;
				String proxyResponse = null;
				try {
					proxySubmissionClient = new MaverickSSHSubmissionClient(getHost(), port, username,
							password, proxyHost, proxyPort, publicKey, privateKey);
					proxyResponse = proxySubmissionClient.runCommand(copyCommand);
				} 
				catch (RemoteDataException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RemoteDataException("Failed to connect to destination server " + getHost() + ":" + port, e);
				}
				finally {
					try { proxySubmissionClient.close(); } catch (Exception e) {}
				}
				
				if (proxyResponse.length() > 0) {
					if (listener != null) {
						listener.failed();
					}
					if (StringUtils.containsIgnoreCase(proxyResponse, "No such file or directory")) {
					    throw new FileNotFoundException("No such file or directory");
					} 
					else if (StringUtils.startsWithIgnoreCase(proxyResponse, "cp:")) {
					    // We use the heuristic that a copy failure due to invalid 
					    // user input produces a message that begins with 'cp:'.
					    throw new RemoteDataException("Copy failure: " + proxyResponse.substring(3));
					} else {
					    throw new RemoteDataException("Failed to perform a remote copy command on " + host + ". " + 
					    		proxyResponse);
					}
				} else {
					if (listener != null) {
						listener.progressed(remoteDestLength);
						listener.completed();
					}
				}
				
			}
			else
			{
				throw new RemoteDataException("Failed to authenticate to remote host");
			}
		}
		catch(FileNotFoundException | RemoteDataException  e) {
			throw e;
		}
		catch (Throwable t)
		{
			throw new RemoteDataException("Failed to perform a remote copy command on " + host, t);
		} 
		finally 
		{
//			try { ssh2.disconnect(); } catch (Throwable t) {}
			try { shell.close(); } catch (Throwable t) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getUriForPath(java.lang.String)
	 */
	@Override
	public URI getUriForPath(String path) throws IOException,
			RemoteDataException
	{
		try {
			return new URI("sftp://" + host + (port == 22 ? "" : ":" + port) + "/" + path);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void delete(String remotepath) throws IOException, FileNotFoundException, RemoteDataException
	{
		try 
		{
		    String resolvedPath = resolvePath(remotepath);
            
            // bust cache since this file has now changed
		    fileInfoCache.remove(resolvedPath);
            String prefixPath = StringUtils.removeEnd(resolvedPath, "/") + "/";
		    for (String path: fileInfoCache.keySet()) {
                if (StringUtils.startsWith(path, prefixPath)) {
                    fileInfoCache.remove(path);
                }
            }
		    
            getClient().rm(resolvedPath, true, true);
		}
		catch (SftpStatusException e) {
		    if (e.getMessage().toLowerCase().contains("permission denied")) {
		        throw new RemoteDataException("The specified path " + remotepath + 
	                    " does not exist or the user does not have permission to view it.", e);
		    } else if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to delete " + remotepath, e);
			}
		} 
		catch (Exception e) {
			throw new RemoteDataException("Failed to delete " + remotepath, e);
		}
	}

	@Override
	public boolean isThirdPartyTransferSupported()
	{
		return false;
	}

	@Override
	public boolean mkdirs(String remotedir) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		String resolvedPath = null;
		try 
		{
			resolvedPath = resolvePath(remotedir);
			fileInfoCache.remove(resolvedPath);
			
			SftpClient client = getClient();
			client.mkdirs(resolvedPath);
			
			return client.stat(resolvedPath).isDirectory();
		} 
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else if (e.getMessage().toLowerCase().contains("directory already exists")) {
				return false;
			} else if (e.getMessage().toLowerCase().contains("file already exists")) {
				return false;
			} else if (e.getMessage().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
			} else {
				throw new RemoteDataException("Failed to create " + remotedir, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to create " + remotedir, e);
		}
	}
	
	@Override
    public boolean mkdirs(String remotepath, String authorizedUsername) 
    throws IOException, RemoteDataException 
    {
	    if (isPermissionMirroringRequired() && StringUtils.isNotEmpty(authorizedUsername)) {
            String pathBuilder = "";
            for (String token: StringUtils.split(remotepath, "/")) {
                if (StringUtils.isEmpty(token)) {
                    continue;
                } else if (StringUtils.isNotEmpty(pathBuilder) ) {
                    pathBuilder += "/" + token;
                } else {
                    pathBuilder = (StringUtils.startsWith(remotepath, "/") ? "/" : "") + token;
                }
                
                if (doesExist(pathBuilder)) {
                    continue;
                } else if (mkdir(pathBuilder)) {
                    setOwnerPermission(authorizedUsername, pathBuilder, true);
                } else {
                    return false;
                }
            }
            return true;
        } else {
            return mkdirs(remotepath);
        }
    }
	
	
	
	@Override
	public boolean mkdir(String remotedir) 
	throws IOException, FileNotFoundException, RemoteDataException
	{	
		String resolvedPath = null;
		try 
		{
			resolvedPath = resolvePath(remotedir);
			
			fileInfoCache.remove(resolvedPath);
			
			SftpClient client = getClient();
			client.mkdir(resolvedPath);
			
			return client.stat(resolvedPath).isDirectory();
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else if (e.getMessage().toLowerCase().contains("directory already exists")) {
				return false;
			} else if (e.getMessage().toLowerCase().contains("file already exists")) {
				return false;
			} else if (e.getMessage().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
			} else {
				throw new RemoteDataException("Failed to create " + remotedir, e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to create " + remotedir, e);
		}
	}

	@Override
	public void disconnect()
	{
		try { sftpClient.quit(); } catch (Exception e) {}
		try { forwardedConnection.disconnect(); } catch (Exception e) {}
		try { ssh2.disconnect(); } catch (Exception e) {}
		ssh2 = null;
		forwardedConnection = null;
		sftpClient = null;
	}
	
    /** Determine if a resolved path exists without throwing exceptions.
     * If the path exists, true is returned.  If the path does not exist
     * or if the command fails for any reason, false is returned.
     * 
     * @param resolvedPath a fully resolved pathname
     * @return true if there's positive confirmation that the path represents
     *         an existing object, false otherwise. 
     */
    private boolean doesExistSafe(String resolvedPath)
    {
        // Is it worth trying?
        if (resolvedPath == null) return false;
        
        // See if we get any attributes back.
        SftpFileAttributes atts = null;
        try {atts = stat(resolvedPath);}
          catch (Exception e){}
        if (atts == null) return false;
          else return true;  // object found
    }
    
	@Override
	public boolean doesExist(String path) throws IOException, RemoteDataException
	{
	    String resolvedPath = resolvePath(path);
		try 
		{
			SftpFileAttributes atts = stat(resolvedPath);
			return atts != null;
		} 
		catch (IOException e) {
			return false;
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
			    return false;
			} else {
				throw new RemoteDataException("Failed to check existence of " + path , e);
			}
		}
		catch (SshException e) {
			return false;
		}
	}

	@Override
	public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		// Return an empty list
		return new ArrayList<RemoteFilePermission>();
	}

	@Override
	public List<RemoteFilePermission> getAllPermissions(String path)
	throws IOException, FileNotFoundException, RemoteDataException
	{
        // Return an empty list
        return new ArrayList<RemoteFilePermission>();
	}

	@Override
	public PermissionType getPermissionForUser(String username, String path)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		int mode;
		try
		{
			mode = stat(resolvePath(path)).getPermissions().intValue();
			Integer pem = Integer.parseInt(Integer.toString(mode, 8), 10);
			pem = pem % 1000;
			pem = pem / 100;
			
			for (PermissionType type: PermissionType.values()) {
				if (type.getUnixValue() == pem) {
					return type;
				}
			}
			
			return PermissionType.NONE;
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve permissions for user.", e);
			}
		} 
		catch (IOException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve permissions for user.", e);
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean hasReadPermission(String path, String username)
	throws FileNotFoundException, RemoteDataException
	{
        // If the file is located under the root direectory and exists on the server, return true
        try {
            path = resolvePath(path);

            // check file exists
            SftpFileAttributes attrs = stat(path);
            return true;
        }
        catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve permissions for user.", e);
			}
		} 
		catch (Exception e) {
            return false;
        }
	}

	@Override
	public boolean hasWritePermission(String path, String username)
	throws IOException, FileNotFoundException, RemoteDataException
	{
        // If the file is located under the home direectory and exists on the server, return true
        try 
        {
        	// check file exists
            RemoteFileInfo fileInfo = getFileInfo(path);
            return fileInfo.userCanWrite();
        } 
        catch (IOException e) {
        	throw e;
        }
		catch (Exception e) {
            return false;
        }
	}

	@Override
	public boolean hasExecutePermission(String path, String username)
	throws IOException, FileNotFoundException, RemoteDataException
	{
		// If the file is located under the home direectory and exists on the server, return true
        try 
        {
        	// check file exists
            RemoteFileInfo fileInfo = getFileInfo(path);
            return fileInfo.userCanExecute();
            
//            path = resolvePath(path);
//            // Honors paths from the specified rootDir and checks permissions on the server
//
//            //if ((path.startsWith(rootDir) && ((attrs.permissions.intValue() & 00200) != 0)))  {
//            if (path.startsWith(rootDir)) {
//                return true;
//            } else {
//                return false;
//            }
        } catch (Exception e) {
            return false;
        }
	}

	@Override
	public void setPermissionForUser(String username, String path, PermissionType type, boolean recursive)
	throws IOException, FileNotFoundException, RemoteDataException
	{ 
		
	}

	@Override
	public void setOwnerPermission(String username, String path, boolean recursive)
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void setReadPermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void removeReadPermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void setWritePermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void removeWritePermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void setExecutePermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void removeExecutePermission(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public void clearPermissions(String username, String path, boolean recursive) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
	}

	@Override
	public String getPermissions(String path) 
	throws IOException, FileNotFoundException, RemoteDataException
	{
		return null;
	}

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return false;
	}
	
	public String escapeResolvedPath(String resolvedPath) {
	    String escapedPath = StringUtils.replaceEach(resolvedPath, new String[]{" ", "$"}, new String[]{"\\ ", "\\$"});
	    
	    return escapedPath;
	}

    @Override
	public String resolvePath(String path) throws FileNotFoundException
	{	
    	if (StringUtils.isEmpty(path)) {
    	    return StringUtils.stripEnd(homeDir, " ");
		}
		else if (path.startsWith("/")) 
		{
			path = rootDir + path.replaceFirst("/", "");
		}
		else
		{
			path = homeDir + path;
		}
		
		String adjustedPath = path;
		if (adjustedPath.endsWith("/..") || adjustedPath.endsWith("/.")) {
			adjustedPath += File.separator;
		}
		
		if (adjustedPath.startsWith("/")) {
			path = org.codehaus.plexus.util.FileUtils.normalize(adjustedPath);
		} else {
			path = FilenameUtils.normalize(adjustedPath);
		}
		
		if (path == null) {
			throw new FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
		} else if (!path.startsWith(rootDir)) {
			if (!path.equals(StringUtils.removeEnd(rootDir, "/"))) {
				throw new FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
			}
		} 
		return StringUtils.stripEnd(path," ");
	}

	@Override
	public RemoteFileInfo getFileInfo(String remotepath) 
	throws RemoteDataException, FileNotFoundException, IOException
	{
		String resolvedPath = resolvePath(remotepath);
		
		try
		{
		    SftpFileAttributes atts = stat(resolvedPath);
            return new RemoteFileInfo(resolvedPath, atts);
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				log.error("Failed to stat " + remotepath + " => " + resolvedPath, e);
				throw new FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to retrieve information for " + remotepath, e);
			}
		} 
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve information for " + remotepath, e);
		}
	}

	@Override
	public String getUsername()
	{
		return username;
	}

	@Override
	public String getHost()
	{
		return host;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MaverickSFTP other = (MaverickSFTP) obj;
		if (homeDir == null)
		{
			if (other.homeDir != null)
				return false;
		}
		else if (!homeDir.equals(other.homeDir))
			return false;
		if (host == null)
		{
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
			return false;
		if (password == null)
		{
			if (other.password != null)
				return false;
		}
		else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (proxyHost == null)
		{
			if (other.proxyHost != null)
				return false;
		}
		else if (!proxyHost.equals(other.proxyHost))
			return false;
		if (proxyPort != other.proxyPort)
			return false;
		if (rootDir == null)
		{
			if (other.rootDir != null)
				return false;
		}
		else if (!rootDir.equals(other.rootDir))
			return false;
		if (username == null)
		{
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	
}
