package org.iplantc.service.transfer.gridftp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.globus.ftp.ChecksumAlgorithm;
import org.globus.ftp.DataSink;
import org.globus.ftp.DataSinkStream;
import org.globus.ftp.DataSource;
import org.globus.ftp.FTPClient;
import org.globus.ftp.FeatureList;
import org.globus.ftp.FileRandomIO;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.GridFTPSession;
import org.globus.ftp.HostPort;
import org.globus.ftp.HostPort6;
import org.globus.ftp.HostPortList;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.RetrieveOptions;
import org.globus.ftp.Session;
import org.globus.ftp.dc.SocketBox;
import org.globus.ftp.dc.SocketOperator;
import org.globus.ftp.exception.ClientException;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.exception.FTPReplyParseException;
import org.globus.ftp.exception.ServerException;
import org.globus.ftp.exception.UnexpectedReplyCodeException;
import org.globus.ftp.extended.GridFTPControlChannel;
import org.globus.ftp.extended.GridFTPServerFacade;
import org.globus.ftp.vanilla.Command;
import org.globus.ftp.vanilla.Reply;
import org.globus.net.ServerSocketFactory;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.auth.AgaveGSSCredentialImpl;
import org.iplantc.service.common.auth.AgaveX509Credential;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientPermissionProvider;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

/**
 * {@link RemoteDataClient} implementation for the GSI FTP protocol. This 
 * client is built on top of the {@link GridFTPClient} from the jGlobus 
 * project with several patches and features to make it friendlier and line
 * up with the file system abstraction used throughout the rest of Agave.  
 * 
 * @author dooley
 */
public class GridFTP extends GridFTPClient implements RemoteDataClient, RemoteDataClientPermissionProvider 
{	
	private static Logger log = Logger.getLogger(FTPClient.class);

	protected static final int MAX_BUFFER_SIZE = 1048576;

	//utility alias to session and localServer
	protected String host;
	protected int port;
	protected String username;
	protected String password;
	protected String homeDir;
	protected String rootDir;
	protected GSSCredential credential;
	protected int parallelism = 1;
	protected String trustedCAPath;
	private Map<String, RemoteFileInfo> fileInfoCache = new ConcurrentHashMap<String,RemoteFileInfo>();
	
	class ExtendedFeatureList extends FeatureList {
	    
	    public ExtendedFeatureList(String featReplyMsg) {
            super(featReplyMsg);
        }

        public List<Feature> getFeatures() {
	        return this.features;
	    }
	}
	
	/**
	 * Constructs client and connects it to the remote server.
	 * 
	 * @param system
	 *            RemoteSystem to interact with
	 * @throws IOException 
	 * @throws ServerException 
	 */
	public GridFTP(String host, int port, String username,
			GSSCredential credential, String rootDir, String homeDir) 
	throws ServerException, IOException
	{
		super(host,port);
		
		this.host = host;
		this.port = port;
		this.username = username;
		this.credential = credential;

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

        this.homeDir = this.homeDir.replaceAll("/+", "/");
        this.rootDir = this.rootDir.replaceAll("/+", "/");
	}

	/**
	 * @return the host
	 */
	public String getHost()
	{
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host)
	{
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port)
	{
		this.port = port;
	}

	public void changeDir(String sDir) throws IOException, ServerException {
		sDir = resolvePath(sDir);
		
		if (sDir.equals("..")) {
			sDir = getCurrentDir();
			int nIndex = sDir.lastIndexOf("/");
			if (nIndex <= 0) {
				nIndex++;
			}
			sDir = sDir.substring(0, nIndex);
		}
		super.changeDir(sDir);
	}
	
	/**
	 * Performs authentication with user credentials from the RemoteSystem.
	 * 
	 * @throws IOException
	 *             on i/o error
	 * @throws RemoteDataException
	 *             on server refusal or faulty server behavior
	 */
	public void authenticate() throws IOException, RemoteDataException
	{	
		if (gSession == null || !gSession.authorized)
		{
			try
			{
				synchronized(this) {
					System.setProperty("jsse.enableCBCProtection", "false");
//					System.setProperty("org.globus.gsi.gssapi.provider", 
//							"org.iplantc.service.common.auth.AgaveGSSManagerImpl");
				}
//				CoGProperties cogProperties = new CoGProperties();
//				File cogFile = new File(System.getProperty("user.home") +"/.globus/cog.properties");
//				if (cogFile.exists()) {
//					cogProperties.load(cogFile.getAbsolutePath());
//				} 
//				CoGProperties.setDefault(cogProperties);
				
				if (credential instanceof AgaveGSSCredentialImpl) {
					AgaveGSSCredentialImpl c = (AgaveGSSCredentialImpl) credential;
					
					this.trustedCAPath = ((AgaveX509Credential)c.getX509Credential()).getTrustedCALocation().getCaPath();
				}
				
				gSession = new GridFTPSession();
		        session = gSession;
		        session.maxWait = 30000;
	            
		        controlChannel = new GridFTPControlChannel(host, port);
		        controlChannel.open();
	
		        gLocalServer = new GridFTPServerFacade((GridFTPControlChannel)controlChannel) {
		            /**
	                 * Start the local server
	                 * @param port required server port; can be set to ANY_PORT
	                 * @param queue max size of queue of awaiting new connection requests 
	                 * @return the server address
	                 **/
	                @Override
	                public HostPort setPassive(int port, int queue) throws IOException 
	                {
	                    // remove existing sockets, if any
	                    socketPool.flush();

	                    if (serverSocket == null) {
	                        ServerSocketFactory factory = ServerSocketFactory.getDefault();
	                        serverSocket = factory.createServerSocket(port, queue);
	                        serverSocket.setSoTimeout(10000);
    	                 }
    
    	                 session.serverMode = Session.SERVER_PASSIVE;
    
    	                 String address = org.iplantc.service.common.Settings.getIpLocalAddress();
    	                 int localPort = serverSocket.getLocalPort();
    
    	                 if (remoteControlChannel.isIPv6()) {
    	                     String version = HostPort6.getIPAddressVersion(address);
    	                     session.serverAddress = new HostPort6(version, address, localPort);
    	                 } else {
    	                     session.serverAddress = new HostPort(address, localPort);
    	                 }
    
    	                 log.debug("started passive server at port " +
    	                              session.serverAddress.getPort());
    	                 
    	                 return session.serverAddress;
    
	                }
	                
	                public HostPortList setStripedPassive(int port, int queue) throws IOException 
	                {
                        // remove existing sockets, if any
                        socketPool.flush();

                        if (serverSocket == null) {
                            ServerSocketFactory factory =
                                ServerSocketFactory.getDefault();
                            serverSocket = factory.createServerSocket(port, queue);
                        }

                        gSession.serverMode = GridFTPSession.SERVER_EPAS;
                        gSession.serverAddressList = new HostPortList();

                        String address = org.iplantc.service.common.Settings.getIpLocalAddress();
                        int localPort = serverSocket.getLocalPort();

                        HostPort hp = null;
                        if (remoteControlChannel.isIPv6()) {
                            String version = HostPort6.getIPAddressVersion(address);
                            hp = new HostPort6(version, address, localPort);
                        } else {
                            hp = new HostPort(address, localPort);
                        }

                        gSession.serverAddressList.add(hp);

                        log.debug("started single striped passive server at port " +
                                     ((HostPort) gSession.serverAddressList.get(0)).getPort());

                        return gSession.serverAddressList;
                    }
	                
	                public void setTCPBufferSize(final int size) throws ClientException {
	                    log.debug("Changing local TCP buffer setting to " + size);

	                    gSession.TCPBufferSize = size;
	                    
	                    SocketOperator op = new SocketOperator() {
	                            public void operate(SocketBox s) throws Exception {

	                                // synchronize to prevent race condition against
	                                // the socket initialization code that also sets
	                                // TCP buffer (GridFTPActiveConnectTask)
	                                synchronized (s) {
	                                    log.debug(
	                                                 "Changing local socket's TCP buffer to " + size);
	                                    Socket mySocket = s.getSocket();
	                                    if (mySocket != null) {
	                                        mySocket.setReceiveBufferSize(size);
	                                        mySocket.setSendBufferSize(size);
	                                        mySocket.setSoLinger(true, 5000);
	                                        mySocket.setReuseAddress(true);
	                                    } else {
	                                        log.debug(
	                                                     "the socket is null. probably being initialized");
	                                    }
	                                }
	                            }
	                    };
	                    try {
	                        socketPool.applyToAll(op);
	                    } catch (Exception e) {
	                        ClientException ce =
	                            new ClientException(ClientException.SOCKET_OP_FAILED);
	                        ce.setRootCause(e);
	                        throw ce;
	                    }
	                }
	            };
		        localServer = gLocalServer;
		        gLocalServer.setTCPBufferSize(Session.SERVER_DEFAULT);
		        
		        gLocalServer.authorize();
		        this.useAllo = true;
		        
		        ((GridFTPControlChannel)controlChannel).authenticate(credential);
	
		        gLocalServer.setCredential(credential);
				gSession.authorized = true;
				
				setOptions(new RetrieveOptions(parallelism));				
			}
			catch (Exception e)
			{
				throw new RemoteDataException(
						"Failed to authenticated to gridftp server " + host, e);
			}
		}
	}
	
	public void setDTP(boolean bPassive) throws IOException, ServerException, ClientException {
		if (bPassive) {
			setPassive();
			setLocalActive();
		} else {
			setLocalPassive();
			setActive();
		}
	}
	
	public void setDTP(boolean bPassive, boolean bRemote) 
	throws IOException, ServerException, ClientException {
		if (bRemote) {
			if (bPassive) {
				setPassive();
			} else {
				setActive();
			}
		} else {
			if (bPassive) {
				setLocalPassive();
			} else {
				setLocalActive();
			}
		}
	}
	

    /**
     * Sets remote server to striped passive server mode (SPAS).
     **/
	@Override
    public HostPortList setStripedPassive() 
    throws IOException, ServerException 
	{
        Command cmd = new Command("SPAS",
                                  (controlChannel.isIPv6()) ? "2" : null);
        Reply reply = null;

        try {
            reply = controlChannel.execute(cmd);
        } catch (UnexpectedReplyCodeException urce) {
            throw ServerException.embedUnexpectedReplyCodeException(urce);
        } catch(FTPReplyParseException rpe) {
            throw ServerException.embedFTPReplyParseException(rpe);
        }

        this.gSession.serverMode = GridFTPSession.SERVER_EPAS;
        if (controlChannel.isIPv6()) {
            gSession.serverAddressList =
                HostPortList.parseIPv6Format(reply.getMessage());
            int size = gSession.serverAddressList.size();
            for (int i=0;i<size;i++) {
                HostPort6 hp = (HostPort6)gSession.serverAddressList.get(i);
                if (hp.getHost() == null) {
                    hp.setVersion(HostPort6.IPv6);
                    hp.setHost(controlChannel.getHost());
                }
            }
        } else {
            gSession.serverAddressList =
                HostPortList.parseIPv4Format(reply.getMessage());
        }
        return gSession.serverAddressList;
    }

	public List<RemoteFileInfo> ls(String remotepath) throws IOException,RemoteDataException
	{
		List<RemoteFileInfo> v = new ArrayList<RemoteFileInfo>();
		OutputStream o = new ByteArrayOutputStream();
		DataSinkStream iSink = new DataSinkStream(o);
		
		String resolvedPath = resolvePath(remotepath);
		
		// try adding mlsd support here. Will give info on files not staged from tape yet
		try
		{
			setProtectionBufferSize(16384);
			//Transfer type must be ASCII
			setType(Session.TYPE_ASCII);
			setMode(GridFTPSession.MODE_STREAM);
			try 
			{
				setPassive();
				setLocalActive();
				
				super.mlsd(resolvedPath, iSink);
			
			} 
			catch (ServerException e) 
			{
				if (e.getMessage().contains("451 refusing to store with active mode")) 
				{
					// try to reverse the mode and see if that helps
					setLocalPassive();
					setActive();
					super.mlsd(resolvedPath, iSink);
				} 
				else if (e.toString().contains("No such file or directory")) {
					throw new java.io.FileNotFoundException("No such file or directory");
				} else if (e.toString().contains("Permission denied")) {
					throw new RemoteDataException("Permission denied", e);
				} else {
					throw new RemoteDataException("Failed to list path " + remotepath, e);
				}
				
			}
			
			BufferedReader reader =
	            new BufferedReader(new StringReader(o.toString()));

	        MlsxEntry entry = null;
	        String line = null;

	        while ((line = reader.readLine()) != null) 
	        {
	            try {
	                entry = new MlsxEntry(line);
	            } catch (FTPException e) {
	                ClientException ce = new ClientException(
	                    		ClientException.UNSPECIFIED, 
	                    		"Could not create MlsxEntry");
	                ce.setRootCause(e);
	                throw ce;
	            }
	            if (!entry.getFileName().equals(".") && !entry.getFileName().equals("..")) 
	            {
	                RemoteFileInfo childFileInfo = new RemoteFileInfo(entry);
	                
	                fileInfoCache.put(resolvedPath + "/" + childFileInfo.getName(), childFileInfo);
	                
	            	v.add(childFileInfo);
	            }
	        }
			
			return v;
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to obtain directory listing from remote system.", e);
		} 
	}
	
//	public List<RemoteFileInfo> ls(String path) throws IOException, RemoteDataException
//	{	
//		Vector<RemoteFileInfo> v = new Vector<RemoteFileInfo>();
//		// InputStreamDataSink iSink = new InputStreamDataSink(); //This will
//		// cause some dirs cannot be listed
//		OutputStream o = new ByteArrayOutputStream();
//		DataSinkStream iSink = new DataSinkStream(o);
//		try {
//			setLocalPassive();
//			setActive();
//			
//			super.list("", "", iSink); // Cannot use any filter
//			
//			String response = o.toString().replaceAll("\r", "\n").replaceAll("\n\n", "\n");
//			String[] sLines = response.split("\n");
//			for (int i = 0, l = sLines.length; i < l; i++) {
//				String listing = "";
//				if (sLines[i].toLowerCase().startsWith("total")) continue;
//				if (super.getHost().contains("mss.ncsa")) {
//					int j=0;
//					String[] tokens = sLines[i].split("[\\s]+");
//					for (String token: tokens) {
//						if (j != 3 && j != 5) {
//							listing += token + " ";
//						}
//						j++;
//					}
//					listing = listing.trim();
//				} else {
//					listing = sLines[i];
//				}
//				
//				v.add(new RemoteFileInfo(listing));
//			}
//			return v;
//			// return super.list(sFilter); //This cannot work in GridFTP of GT
//			// higher than 3.9
//		} catch (Exception e) {
//			throw new RemoteDataException("Failed to obtain directory listing from remote system.", e);
//		}
//	}

//	public void put(String sLocalFile, String sRemoteFile,
//			MarkerListener listener, boolean bAppend) throws IOException,
//			ServerException, ClientException
//	{
//		DataSource source = null;
//		source = new FileRandomIO(new RandomAccessFile(sLocalFile, "rw"));
//		
//		setType(GridFTPSession.TYPE_IMAGE);
//		setMode(GridFTPSession.MODE_EBLOCK);
//		setPassive();
//		setLocalActive();
//		
//		extendedPut(sRemoteFile, source, listener);
//	}

//	public void setParallel(int nParallel) throws IOException, ServerException
//	{
//		gSession.parallel = nParallel;
//		if (nParallel > 1)
//		{
//			setMode(GridFTPSession.MODE_EBLOCK);
//			setOptions(new RetrieveOptions(nParallel));
//		}
//		else
//		{
//			setMode(GridFTPSession.MODE_STREAM);
//		}
//	}

	@Override
	public Reply site(String arg) throws IOException, ServerException
	{
		String[] args = arg.split(" ");
		
		return super.site("chmod " + args[1] + " " + args[2]);
	}

	@Override
	/**
     * Returns list of features supported by remote server.
     * @return list of features supported by remote server.
     */
    public FeatureList getFeatureList() throws IOException, ServerException {

        if (this.session.featureList != null) {
            return this.session.featureList;
        }

        // TODO: this can also be optimized. Instead of parsing the
        // reply after it is reveiced, we can parse it as it is
        // received.
        Reply featReply = null;
        try {
            featReply = controlChannel.execute(Command.FEAT);
            
            if (featReply.getCode() != 211) {
                throw ServerException.embedUnexpectedReplyCodeException(
                                  new UnexpectedReplyCodeException(featReply),
                                  "Server refused returning features");
            }
        } catch (FTPReplyParseException rpe) {
            throw ServerException.embedFTPReplyParseException(rpe);
        } catch (UnexpectedReplyCodeException urce) {
            throw ServerException.embedUnexpectedReplyCodeException(
                                urce,
                                "Server refused returning features");
        }

        
        this.session.featureList = new ExtendedFeatureList(featReply.getMessage());
        return session.featureList;
    }
	
	// basic compatibility API
	@Override 
	public void delete(String remotepath) throws IOException, RemoteDataException
	{
		
        if (remotepath == null) {
            throw new IllegalArgumentException("Required argument missing");
        }
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
            
        	Reply reply = controlChannel.exchange(new Command("SITE", "RDEL " + resolvedPath));
        	if (reply.getCode() != 250) {
        		if (reply.getMessage().contains("No such file or directory")) {
        			throw new FileNotFoundException("No such file or directory");
        		} else if (reply.getMessage().contains("Permission denied")) {
					throw new RemoteDataException("Permission denied");
        		} else {
        			log.error("Failed recursive server-side delete. Manually applying now.");
        			deleteManually(remotepath);
        		}
        	}
        }
        catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
        	log.error("Failed recursive server-side delete. Manually applying now.", e);
        	deleteManually(remotepath);
        }
    }
	
	public void deleteManually(String path) throws IOException, RemoteDataException
	{
		try 
		{	
			setType(Session.TYPE_ASCII);
			this.setDTP(false);
			
			if (isDirectory(path)) 
			{
                List<RemoteFileInfo> files = ls(path);
                for (RemoteFileInfo file: files) {
                	if (!file.getName().equals(".") && !file.getName().equals(".."))
        			{
                		delete((StringUtils.isEmpty(path) ? "" : path + "/") + file.getName());
        			}
                }
				super.deleteDir(resolvePath(path));
			} else {
				super.deleteFile(resolvePath(path));
			}
		} catch (IOException | RemoteDataException e) {
			throw e;
		} catch (Exception e) {
			throw new RemoteDataException("Failed to connect to remote system.", e);
		} 
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
     */
    @Override
    public void append(String localpath, String remotepath) throws IOException,
    RemoteDataException
    {
        append(localpath, remotepath, null);
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
     */
    @Override
    public void append(String localpath, String remotepath, RemoteTransferListener listener)  
    throws IOException, RemoteDataException
    {
        File localFile = new File(localpath);
        
        try 
        {
            if (!doesExist(remotepath)) 
            {
                putFile(localFile, remotepath, listener);
            }
            else if (localFile.isDirectory()) {
                throw new RemoteDataException("cannot append to a directory");
            }
            else {
                putFile(localFile, remotepath, listener, true);
            }
        } 
        catch (IOException e) {
            throw e;
        }
        catch (RemoteDataException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RemoteDataException("Failed to append data to " + remotepath, e);
        }
    }
	
    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#put(java.lang.String, java.lang.String)
     */
    @Override
    public void put(String localdir, String remotedir) throws IOException,
			RemoteDataException
	{
		put(localdir, remotedir, null);
	}

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#put(java.lang.String, java.lang.String, org.iplantc.service.transfer.RemoteTransferListener)
     */
    @Override
    public void put(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		File localFile = new File(localdir);
		
		try 
		{
			if (localFile.isDirectory()) {
				putDir(localFile, remotedir, listener);
			}
			else {
				putFile(localFile, remotedir, listener);
			}
		} 
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
		    e.printStackTrace();
			throw new RemoteDataException("Failed to copy file to remote system.", e);
		}
	}
	
	private void putFile(File localFile, String remotedir, RemoteTransferListener listener) 
	throws ServerException, ClientException, IOException, RemoteDataException
	{
		String resolvedPath = resolvePath(remotedir);
		if (localFile == null) {
			throw new FileNotFoundException("No local file specified.");
		} else if (!localFile.exists()) {
			throw new FileNotFoundException("No such local file or directory");
		} 
		else if (doesExist(remotedir))
		{
			if (isDirectory(remotedir)) {
				resolvedPath = new File(resolvedPath, localFile.getName()).getPath();
			}
		} else if (!doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/.."))) {
			throw new FileNotFoundException("No such local file or directory");
		}
		
		DataSource dataSource = null;
		try {
		    dataSource = new FileRandomIO(new RandomAccessFile(localFile, "rw"));
    		
    		setProtectionBufferSize(16384);
    		setType(GridFTPSession.TYPE_IMAGE);
    		setMode(GridFTPSession.MODE_EBLOCK);
    		setOptions(new RetrieveOptions(parallelism));
    		
    		// stream mode not supported with striping or parallelism
    		// always use striped transfer for performance reasons
    		ExtendedFeatureList features = (ExtendedFeatureList)getFeatureList();
    		
    		setStripedPassive();
    		setLocalStripedActive();
    		
    		fileInfoCache.remove(resolvedPath);
    		
    		extendedPut(resolvedPath, dataSource, listener);
    		
    		setStripedPassive();
    		setLocalStripedActive();
		}
		finally {
		    try { dataSource.close(); } catch (Exception e) {}
		}
	}
	
	private void putFile(File localFile, String remotedir, RemoteTransferListener listener, boolean append) 
    throws ServerException, ClientException, IOException, RemoteDataException
    {
        String resolvedPath = resolvePath(remotedir);
        if (localFile == null) {
            throw new FileNotFoundException("No local file specified.");
        } else if (!localFile.exists()) {
            throw new FileNotFoundException("No such local file or directory");
        } 
        else if (doesExist(remotedir))
        {
            if (isDirectory(remotedir)) {
                resolvedPath = new File(resolvedPath, localFile.getName()).getPath();
            }
        } else if (!doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/.."))) {
            throw new FileNotFoundException("No such local file or directory");
        }
        
        DataSource dataSource = null;
        try {
            dataSource = new FileRandomIO(new RandomAccessFile(localFile, "rw"));
        
            localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
            setType(Session.TYPE_IMAGE);
            setMode(Session.MODE_STREAM);
            
            setDTP(true);
            
            try 
            {
                // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);
                put(resolvedPath, dataSource, listener, append);
            } catch (ServerException e) {
                if (e.getCause().toString().toLowerCase().contains("permission denied")) {
                    throw new RemoteDataException("Permission denied", e);
                } else {
                    throw e;
                }
            }
            
            setDTP(true);
        }
        finally {
            try { dataSource.close(); } catch (Exception e) {}
        }
    }
	
	private void putDir(File localFile, String remotedir, RemoteTransferListener listener)
	throws ServerException, ClientException, IOException, RemoteDataException
	{
		String dest = remotedir;
		
		if (doesExist(dest)) 
		{
			// can't put dir to file
			if (localFile.isDirectory() && !isDirectory(dest)) {
				throw new RemoteDataException("cannot overwrite non-directory: " + 
						remotedir + " with directory " + localFile.getName());
			} 
//			else if (!localFile.isDirectory())
//			{
				dest += (StringUtils.isEmpty(dest) ? "" : "/") + localFile.getName();
				mkdir(dest);
//			}
		}
		else if (doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/..")))
		{
			mkdir(dest);
		}
		else
		{
			// upload and keep name.
			throw new FileNotFoundException("No such file or directory");
		}
		
		for (File fileItem : localFile.listFiles())
		{
			if (fileItem.getName().equals(".") || fileItem.getName().equals("..")) 
			{
				continue;
			} 
			else 
			{
				if (fileItem.isDirectory()) {
				    fileInfoCache.remove(resolvePath(remotedir) + "/" + fileItem.getName());
			        putDir(fileItem, dest, listener);
				} else {
					putFile(fileItem, dest, listener);
				}
			}
		}
	}
	
	public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		File localFile = new File(localdir);
		if (!localFile.exists()) {
			throw new FileNotFoundException("No such file or directory");
		} 
		
		try
		{
			if (!doesExist(remotedir)) {
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
						syncToRemote(child.getAbsolutePath(), adjustedRemoteDir, listener);
					} 
					else
					{
						syncToRemote(child.getAbsolutePath(), childRemotePath, listener);
					}
					
					
				}
			} 
			else 
			{
				// sync if file is not there
				if (!doesExist(remotedir))  
				{
					putFile(localFile, remotedir, listener);
				}
				else 
				{
					RemoteFileInfo fileInfo = getFileInfo(remotedir);
					
					// if the types mismatch, delete remote, use current
					if (localFile.isDirectory() && !fileInfo.isDirectory() || 
							localFile.isFile() && !fileInfo.isFile()) 
					{
						delete(remotedir);
						putFile(localFile, remotedir, listener);
					} 
					// or if the file sizes are different
					else if (localFile.length() != fileInfo.getSize())
					{
						putFile(localFile, remotedir, listener);
					}
					else
					{
						log.debug("Skipping transfer of " + localFile.getPath() + " to " + 
								remotedir + " because file is present and of equal size.");
					}
				}
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file to remote system.", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#checksum(java.lang.String)
	 */
	@Override
	public String checksum(String remotePath) throws IOException,
			RemoteDataException
	{
		try
		{
			RemoteFileInfo remoteFileInfo = getFileInfo(remotePath); 
			if (remoteFileInfo.isDirectory()) {
				throw new RemoteDataException("Cannot perform checksum on a directory");
			} else {
				throw new NotImplementedException("Checksum is not currently supported.");
			}
//			return checksum(ChecksumAlgorithm.MD5, 0, length(remotePath),
//					resolvePath(remotePath));
		}
		catch (NotImplementedException e) {
			throw e;
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to compute MD5 checksum of "
					+ remotePath, e);
		}

	}

	/**
	 * Computes and returns a checksum of a file. transferred.
	 * 
	 * @param algorithm the checksume algorithm
	 * @param offset the offset
	 * @param length the length
	 * @param file file to compute checksum of
	 * @return the computed checksum
	 * @throws ServerException
	 *             if an error occured.
	 */
	@Override
	public String checksum(ChecksumAlgorithm algorithm, long offset,
			long length, String file) throws IOException, ServerException
	{
		String arguments = algorithm.toFtpCmdArgument() + " "
				+ String.valueOf(offset) + " " + String.valueOf(length) + " "
				+ file;
		
		Command cmd = new Command("CKSM", arguments);
		Reply reply = null;
		try
		{
			reply = controlChannel.execute(cmd);
			return reply.getMessage();
		}
		catch (UnexpectedReplyCodeException urce)
		{
			throw ServerException.embedUnexpectedReplyCodeException(urce);
		}
		catch (FTPReplyParseException rpe)
		{
			throw ServerException.embedFTPReplyParseException(rpe);
		}
	}

	@Override
	public List<RemoteFilePermission> getAllPermissionsWithUserFirst(
			String path, String username) throws RemoteDataException
	{
		return new ArrayList<RemoteFilePermission>();
	}

	@Override
	public List<RemoteFilePermission> getAllPermissions(String path)
			throws RemoteDataException
	{
		return new ArrayList<RemoteFilePermission>();
	}

	@Override
	public PermissionType getPermissionForUser(String username, String path)
			throws RemoteDataException
	{
		try
		{
			int pem = getFileInfo(path).getMode();
			pem = pem % 1000;
			pem = pem / 100;
			
			for (PermissionType type: PermissionType.values()) {
				if (type.getUnixValue() == pem) {
					return type;
				}
			}
			
			return PermissionType.NONE;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve permissions for user.", e);
		}
	}

    @Override
    public boolean hasReadPermission(String path, String username) throws RemoteDataException 
    {
        // If the file is located under the root directory and exists on the server, return true
        try 
        {   
        	// check file exists
            if (!doesExist(path)) return false;

        	path = resolvePath(path);
            
            if (path.startsWith(rootDir)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasWritePermission(String path, String username) throws RemoteDataException 
    {
        // If the file is located under the root directory, return true
        try 
        {
            path = resolvePath(path);

            if (path.startsWith(rootDir)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

	@Override
	public boolean hasExecutePermission(String path, String username)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void setPermissionForUser(String username, String path,
			PermissionType type, boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void setOwnerPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void setReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void removeReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void setWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void removeWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void setExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void removeExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}

	@Override
	public void clearPermissions(String username, String path, boolean recursive)
			throws RemoteDataException
	{
		throw new NotImplementedException("Remote chmod is not supported universally via FTP.");
	}
	
	@Override
	public String getPermissions(String path) throws RemoteDataException
	{
		try 
		{   
			RemoteFileInfo file = getFileInfo(path);
			
            // modify this method to return all permissions for the given file
            if (file.userCanRead() && file.userCanWrite() && file.userCanExecute()) {
                return "all";
            } else if (file.userCanRead() && file.userCanWrite()) {
                return "read_write";
            } else if (file.userCanRead()) {
                if (file.userCanExecute()) return "read_execute";
                return "read";
            } else if (file.userCanWrite()) {
                if (file.userCanExecute()) return "write_execute";
                return "write";
            } else if (file.userCanExecute()) {
                return "execute";
            } else {
                return "none";
            }
        } 
		catch (IOException e) {
            throw new RemoteDataException("Failed to retreive remote permissions for " + path, e);
        }
	}
	
	/**
	 * Uses mlst to get the file info
	 * 
	 * @param path
	 * @return
	 * @throws RemoteDataException
	 * @throws IOException
	 */
	@Override
	public RemoteFileInfo getFileInfo(String path) throws RemoteDataException, IOException
	{
		
		String resolvedPath = resolvePath(path);
		RemoteFileInfo fileInfo = null;
		try
		{   
			// return from the cache first
			fileInfo = fileInfoCache.get(resolvedPath);
			
			if (fileInfo == null)
			{	
    			setType(Session.TYPE_ASCII);
    			setLocalPassive();
    			setActive();
    			
    			MlsxEntry entry = super.mlst(resolvedPath);
    			
    			fileInfo = new RemoteFileInfo(entry);
    			
    			fileInfoCache.put(resolvedPath, fileInfo);
			}
			
			return fileInfo;
		}
		catch (ServerException e) 
		{
		    fileInfoCache.remove(resolvedPath);
		
		    if (e.getCustomMessage().contains("Could not create MlsxEntry")) {
				try {
					disconnect();
					authenticate();
			        MlsxEntry entry = super.mlst(resolvedPath);
			        fileInfo = new RemoteFileInfo(entry);
					fileInfoCache.put(resolvedPath, fileInfo);
					return fileInfo;
				} catch (Exception e1) {
				    fileInfoCache.remove(resolvedPath);
			        throw new RemoteDataException("Failed to retrieve file info for " + path, e);
				}
			}
			else if (e.toString().contains("No such file or directory")) {
				throw new java.io.FileNotFoundException("No such file or directory");
			} else if (e.toString().contains("Permission denied")) {
				throw new RemoteDataException("Permission denied", e);
			} else {
				throw new RemoteDataException("Failed to retrieve file info for " + path, e);
			}
		}
		catch (Exception e) {
		    fileInfoCache.remove(resolvedPath);
	        throw new RemoteDataException("Failed to retrieve file info for " + path, e);
		}
		
		
//		String name = FilenameUtils.getName(path);
//		String parent = FilenameUtils.getPath(path);
//		List<RemoteFileInfo> children = ls(parent);
//		RemoteFileInfo file = null;
//		for(RemoteFileInfo fileInfo : children) {
//			if (fileInfo.getName().equals(name)) {
//				file = fileInfo;
//				break;
//			}
//		}
//		if (file == null) {
//			throw new RemoteDataException("Failed to find the remote file");
//		}
//		
//		return file;
	}

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return false;
	}

	@Override
	public boolean mkdir(String remotepath) throws IOException, RemoteDataException
	{
	    String resolvedPath = resolvePath(remotepath);
		
	    if (doesExist(remotepath)) {
			return false;
		} else {
			try {
			    fileInfoCache.remove(resolvedPath);
			    super.makeDir(resolvedPath);
				return true;
			} catch (ServerException e) {
				if (e.toString().contains("Permission denied")) {
					throw new RemoteDataException("Permission denied", e);
				} else {
					throw new RemoteDataException("Failed to create " + remotepath, e);
				}
			} catch (Exception e) {
				return false;
			}
		}
	}

	@Override
	public boolean mkdirs(String dir) throws IOException, RemoteDataException
	{
		String parent = StringUtils.isEmpty(dir) ? ".." : dir + "/..";
		
        if (!doesExist(parent)) {
            mkdirs(parent);
        }

        return mkdir(dir);
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
	public int getMaxBufferSize()
	{
		return MAX_BUFFER_SIZE;
	}

	@Override
	public GridFTPInputStream getInputStream(String path, boolean passive)
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(path)) {
			throw new RemoteDataException("Not input path specified.");
		} 
		try 
		{
			RemoteFileInfo remoteFileInfo = getFileInfo(path);
			if (remoteFileInfo.isDirectory()) {
				throw new RemoteDataException("Cannot open input stream to directory " + path);
			} else if ((StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.userCanRead()) ||
					(!StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.groupCanRead() && !remoteFileInfo.allCanRead())) { 
				throw new RemoteDataException("Permission denied");
			} else {
				setType(GridFTPSession.TYPE_IMAGE);
				setMode(GridFTPSession.MODE_STREAM);
				return new GridFTPInputStream(this, resolvePath(path), true);
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (FTPException e)
		{
			if (e.toString().contains("Permission denied")) {
				throw new RemoteDataException("Permission denied");
			} else {
				throw new RemoteDataException("Failed to create input stream from '" + path + "' on remote system.", e);
			}
		}
	}

	@Override
	public GridFTPOutputStream getOutputStream(String path, boolean passive, boolean append) 
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(path)) {
			throw new RemoteDataException("Not output path specified.");
		} 
		else 
		{
			if (doesExist(path)) 
			{
				RemoteFileInfo remoteFileInfo = getFileInfo(path);
				if (remoteFileInfo.isDirectory()) {
					throw new RemoteDataException("Cannot open output stream to directory " + path);
				} else if ((StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.userCanRead()) ||
						(!StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.groupCanWrite() && !remoteFileInfo.allCanWrite())) {
					throw new RemoteDataException("Permission denied");
				} else {
					return new GridFTPOutputStream(this, resolvePath(path), passive, append);
				}
			} 
			else 
			{ 
				String parentPath = (StringUtils.isEmpty(path) ? ".." : FilenameUtils.getPath(path));
				if (doesExist(parentPath))
				{
					RemoteFileInfo remoteFileInfo = getFileInfo(parentPath);
					if ((StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.userCanRead()) ||
							(!StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.groupCanWrite() && !remoteFileInfo.allCanWrite())) {
						throw new RemoteDataException("Permission denied");
					} else {
						return new GridFTPOutputStream(this, resolvePath(path), passive, append);
					}
				} else {
					throw new FileNotFoundException("No such file or directory");
				}
			}
		}
	}

	@Override
	public void get(String sRemoteFile, String sLocalFile) throws IOException,RemoteDataException
	{
		get(sRemoteFile, sLocalFile, null);
	}

	@Override
	public void get(String remotedir, String localdir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(localdir)) {
			throw new RemoteDataException("No local path specified.");
		}
		
		try 
		{
			RemoteFileInfo remoteFileInfo = getFileInfo(remotedir);
			
			File localDir = new File(localdir);
			
			if (remoteFileInfo.isDirectory())
			{
				if (!localDir.exists()) 
				{
					if (!localDir.getParentFile().exists()) {
						throw new java.io.FileNotFoundException("No such file or directory");
					} 
					// create the target directory
					else if (!localDir.mkdir()) {
						throw new IOException("Failed to create local download directory");
					}
				} 
				else if (localDir.isFile()) 
				{
					throw new RemoteDataException("cannot overwrite non-directory: " + localDir.getName() + " with directory " + remotedir);
				}
				else {
					localDir = new File(localDir, remoteFileInfo.getName());
					localDir.mkdir();
				}
				
				for (RemoteFileInfo fileInfo: ls(remotedir)) 
				{
					if (!fileInfo.getName().equals(".") && !fileInfo.getName().equals(".."))
					{
						String fileInfoPath = null;
						if (StringUtils.isEmpty(remotedir)) {
							fileInfoPath = fileInfo.getName();
						} else {
							fileInfoPath = remotedir + "/" + fileInfo.getName();
						}
						get(fileInfoPath, localDir.getPath() + File.separator + fileInfo.getName(), listener);
					}
				}
			} 
			else 
			{
				if (FileUtils.getFile(localdir).isDirectory()) {
					localdir = localdir + File.separator + FilenameUtils.getName(remotedir);
				}
				getFile(remotedir, localdir, listener );
			}
 		} 
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to get '" + remotedir + "' from remote system.", e);
		}
		
	}
	
	/**
	 * note: 
	 * @param remotedir
	 * @param localdir
	 * @param listener
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	private void getFile(String remotedir, String localdir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
//		try {
//			DataSink sink = new FileRandomIO(new RandomAccessFile(localdir, "rw"));
//			setProtectionBufferSize(16384);
//			setType(Session.TYPE_IMAGE);
//			setMode(GridFTPSession.MODE_EBLOCK);
//			setOptions(new RetrieveOptions(parallelism));
//			HostPortList hpl = setLocalStripedPassive(); 
//			setStripedActive(hpl);
//			get(resolvePath(remotedir), sink, null);
//		}
//		catch(Throwable e) {
//			throw new RemoteDataException("Failed to get '" + remotedir + "' from remote system.", e);
//		}
		
	    DataSink sink = null;
        
		try {
//			long size = length(remotedir);
			sink = new FileRandomIO(new RandomAccessFile(localdir, "rw"));
			
			// striping not supported on download...weird
			setProtectionBufferSize(16384);
			setType(Session.TYPE_IMAGE);
			setMode(GridFTPSession.MODE_STREAM);
			
			
//			setOptions(new RetrieveOptions(parallelism));
			
//			setDTP(true);
			
//			setLocalPassive();
//		    setActive();
			
			setPassive();
			setLocalActive();
		    get(resolvePath(remotedir), sink, listener);
		    
		    setLocalPassive();
		    setActive();
			
//		    extendedGet(resolvePath(remotedir), size, sink, listener);
//			try {
////				setDTP(true);
////				setStripedActive(setLocalStripedPassive());
//				extendedGet(resolvePath(remotedir), size, sink, listener);
//			} catch (ServerException e) {
////				setDTP(false);
////				setStripedPassive();
////				setLocalStripedActive();
//				extendedGet(resolvePath(remotedir), size, sink, listener);
//				
//			}
		} 
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to get '" + remotedir + "' from remote system.", e);
		}
		finally {
		    try { sink.close(); } catch (Exception e) {}
		}
	}
	
	@Override
	public boolean isDirectory(String path) 
	throws IOException, RemoteDataException
	{
		try {
			return getFileInfo(path).isDirectory();
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to connect to remote server.", e);
		}
	}

	@Override
	public boolean isFile(String path) throws IOException, RemoteDataException
	{
		return !isDirectory(path);
	}

	@Override
	public long length(String remotepath) 
	throws IOException, RemoteDataException
	{
		try {
			setType(Session.TYPE_IMAGE);
			return getSize(resolvePath(remotepath));
		} catch (Exception e) {
			throw new RemoteDataException("Failed to get '" + remotepath + 
					"' from remote system.", e);
		}
	}

	@Override
	public void doRename(String oldpath, String newpath) 
	throws IOException, RemoteDataException
	{
		try
		{
			if (!doesExist(oldpath)) {
				throw new java.io.FileNotFoundException("No such file or directory");
			} else if (!doesExist(newpath)) {
				if (!doesExist(newpath + (StringUtils.isEmpty(newpath) ? ".." : "/.."))) {
					throw new java.io.FileNotFoundException("No such file or directory: " + newpath);
				}
			}
			String src = resolvePath(oldpath);
			String dest = resolvePath(newpath);
			if (StringUtils.equals(src, dest)) {
				throw new RemoteDataException("Source and destination cannot be the same.");
			} else {
				super.rename(src, dest);
			}
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (IOException e) {
			throw e;
		}
		catch (ServerException e)
		{
			throw new RemoteDataException("Failed to rename '" + oldpath + 
					"' to '" + newpath + "' on remote system.", e);
		}
	}

	@Override
	public void copy(String remotesrc, String remotedest) throws IOException,
			RemoteDataException
	{
		copy(remotesrc, remotedest, null);
	}

	@Override
	public void copy(String remotesrc, String remotedest, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		GridFTP gridftp = null;
		try 
		{
			gridftp = new GridFTP(this.host, this.port, this.username,
				this.credential, this.rootDir, this.homeDir.substring(this.rootDir.length()));
			
			gridftp.authenticate();
			gridftp.setProtectionBufferSize(16384);
			gridftp.setType(GridFTPSession.TYPE_IMAGE);
			gridftp.setMode(GridFTPSession.MODE_EBLOCK);
			
			copy(remotesrc, remotedest, gridftp, listener);
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file to remote system.", e);
		}
		finally {
			try {gridftp.disconnect();} catch (Exception e){} 
		}
	}
	
	private void copy(String remotesrc, String remotedest, GridFTP remoteDestClient, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		RemoteFileInfo srcFileInfo = getFileInfo(remotesrc);
		remotedest = StringUtils.removeEnd(remotedest, "/");
		
		if (srcFileInfo.isDirectory())
		{
			if (doesExist(remotedest))
			{ 
				RemoteFileInfo destFileInfo = getFileInfo(remotedest);
				if (destFileInfo.isDirectory()) {
					remotedest += "/" + srcFileInfo.getName();
					mkdirs(remotedest);
				}
				else
				{
					throw new RemoteDataException("cannot overwrite non-directory: " + remotedest + " with directory " + remotesrc);
				}
			} 
			// essentially a rename, but without deleting the original file
			else  if (doesExist(remotedest + (StringUtils.isEmpty(remotedest) ? ".." : "/..")))
			{
				mkdir(remotedest);
			}
			else
			{
				// upload and keep name.
				throw new FileNotFoundException("No such file or directory");
			} 
			
			for (RemoteFileInfo fileInfo: ls(remotesrc)) 
			{
				if (fileInfo.getName().equals(".") || fileInfo.getName().equals("..")) {
					continue;
				}
				String newSourcePath = remotesrc + File.separator + fileInfo.getName();
				if (fileInfo.isDirectory()) {
					newSourcePath += "/";
				}
				String newDestPath = remotedest + File.separator + fileInfo.getName();
				
				copy(newSourcePath, newDestPath, remoteDestClient, listener);
			}
		} 
		else 
		{
			if (doesExist(remotedest))
			{
				if (isDirectory(remotedest)) {
					remotedest += "/" + srcFileInfo.getName();
				}
			} 
			else if (!doesExist(remotedest + (StringUtils.isEmpty(remotedest) ? ".." : "/.."))) {
				throw new java.io.FileNotFoundException("No such file or directory");
			}
				
			try
			{
				String newDestPath = remotedest;
				if (doesExist(remotedest) && isDirectory(remotedest)) {
					newDestPath += File.separator + FilenameUtils.getName(remotesrc);
				}
				
				setProtectionBufferSize(16384);
				setType(GridFTPSession.TYPE_IMAGE);
				setMode(GridFTPSession.MODE_EBLOCK);
				
				setActive(remoteDestClient.setPassive());
				
				fileInfoCache.remove(resolvePath(newDestPath));
				
				super.transfer(resolvePath(remotesrc), remoteDestClient, resolvePath(newDestPath), false, listener);
			}
			catch (Exception e)
			{
				throw new RemoteDataException("Failed to copy " + remotesrc + " to " + remotedest, e);
			}
		}
	}

	@Override
	public URI getUriForPath(String path)
	throws IOException, RemoteDataException
	{
		try {
			return new URI("gsiftp://" + 
					host + 
					(port == 2811 ? "" : ":" + port) +
					"/" + path);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isThirdPartyTransferSupported()
	{
		return true;
	}

	@Override
	public void disconnect()
	{
		try { 
		    close(true); 
		} catch (Throwable e) {
		    e.printStackTrace();
		}
		
		try {
		    gLocalServer.close();
		}
		catch (Throwable e) {
            e.printStackTrace();
        }
		finally {
		    gSession = null;
		}
	}

	@Override
	public boolean doesExist(String remotepath) 
	throws IOException, RemoteDataException
	{
		try
		{
			return super.exists(resolvePath(remotepath));
		}
		catch (ServerException e)
		{
			throw new RemoteDataException("Failed to verify the existence of '"+ 
					remotepath+"' on remote system.", e);
		}
	}

	@Override
	public String resolvePath(String path) throws FileNotFoundException
	{
		if (StringUtils.isEmpty(path)) {
			return homeDir;
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
			path = FileUtils.normalize(adjustedPath);
		} else {
			path = FilenameUtils.normalize(adjustedPath);
		}
		
		if (path == null) {
			throw new FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
		} else if (!path.startsWith(rootDir)) {
			if (path.equals(StringUtils.removeEnd(rootDir, "/"))) {
				return path;
			} else {
				throw new FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
			}
		} else {
			return path;
		}
	}

	@Override
	public String getUsername()
	{
		return this.username;
	}
}