/**
 * 
 */
package org.iplantc.service.transfer.irods;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.systems.exceptions.EncryptionException;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.Settings;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.transfer.util.ServiceUtils;
import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.DefaultPropertiesJargonConfig;
import org.irods.jargon.core.connection.GSIIRODSAccount;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSProtocolManager;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.connection.JargonProperties;
import org.irods.jargon.core.connection.SettableJargonProperties;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.CatNoAccessException;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.exception.SpecificQueryException;
import org.irods.jargon.core.packinstr.TransferOptions;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.io.FileIOOperations.SeekWhenceType;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSRandomAccessFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
import org.irods.jargon.core.transfer.DefaultTransferControlBlock;
import org.irods.jargon.core.transfer.TransferControlBlock;
import org.irods.jargon.core.transfer.TransferStatus;
//import org.iplantc.service.apps.Settings;
//import org.iplantc.service.apps.util.ServiceUtils;

/**
 * @author dooley
 */
public class IRODS implements RemoteDataClient 
{
	private static final Logger log = Logger.getLogger(IRODS.class);
	/** integer from some queries signifying user can read a file */
	static final int					READ_PERMISSIONS	= 1050;
	/** integer from some queries signifying user can write to a file */
	
	static final int					WRITE_PERMISSIONS	= 1120;

	
	private static final ThreadLocal<Hashtable<String, IRODSSession>> 
		threadLocalIRODSSession = new ThreadLocal<Hashtable<String, IRODSSession>>() {
			@Override protected Hashtable<String, IRODSSession> initialValue() {
				return new Hashtable<String, IRODSSession>();
			}
		};
	private IRODSAccount				irodsAccount;
	private IRODSAccessObjectFactory	accessObjectFactory;
	
	private CollectionAndDataObjectListAndSearchAO collectionAndDataObjectListAndSearchAO;
	private DataObjectAO dataObjectAO;
	private CollectionAO collectionAO; 
	private IRODSFileSystemAO fileSystemAO;
	private DataTransferOperations dataTransferOperations;
	private IRODSFileFactory irodsFileFactory;
	
	private AuthScheme type = AuthScheme.STANDARD;
	
	protected String host;
	protected int port;
	protected String username;
	protected String password;
	protected String resource;
	protected String zone;
	protected String homeDir;
	protected String rootDir;
	protected String internalUsername;
	protected GSSCredential credential;
	protected boolean permissionMirroringRequired = true;
	private Map<String, IRODSFile> fileInfoCache = new ConcurrentHashMap<String, IRODSFile>();
	
	protected static final int MAX_BUFFER_SIZE = 4194304; // 4MB
	
	public IRODS() {}
	
	public IRODS(String host, int port, String username, String password,
			String resource, String zone, String rootDir, String internalUsername, 
			String homeDir, AuthScheme type)
	{
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.resource = StringUtils.isEmpty(resource) ? "" : resource;
		this.zone = zone;
		this.rootDir = rootDir;
		this.internalUsername = internalUsername;
		if (type != null) {
			this.type = type;
		}
		
		updateSystemRoots(rootDir, homeDir);
	}
	
	public IRODS(String host, int port, GSSCredential credential,
			String resource, String zone, String rootDir, String internalUsername, 
			String homeDir)
	{
		this(host, port, null, null, resource, zone, rootDir, internalUsername, homeDir, AuthScheme.GSI);
		
		this.credential = credential;
		
		updateSystemRoots(rootDir, homeDir);
	}
	
	public IRODS clone()
	{
		IRODS irods = new IRODS(host, 
				  port, 
				  username, 
				  password, 
				  resource, 
				  zone, 
				  rootDir, 
				  internalUsername, 
				  homeDir, AuthScheme.GSI);

		// don't double resolve homedir
		irods.rootDir = this.rootDir;
		irods.homeDir = this.homeDir;
		
		// hand over the existing credential
		irods.credential = this.credential;
		
		if (type != null) {
			irods.type = type;
		}
		
		return irods;
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
	
	public IRODSSession getThreadLocalSession(IRODSAccount account) throws JargonException {
		IRODSSession session = threadLocalIRODSSession.get().get(account.toString());
		if ( session == null) {
			AgaveJargonProperties props = new AgaveJargonProperties();
			session = new IRODSSession(props);
			session.setIrodsConnectionManager(IRODSSimpleProtocolManager.instance());
			threadLocalIRODSSession.get().put(account.toString(), session);
			log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + Thread.currentThread().getId() + " created new session for thread");
		}
		
		return session;
	}

	@Override
	public void authenticate() throws RemoteDataException, IOException
	{
	    // clear cache here as we may have stale information in between authentications
        fileInfoCache.clear();
        
        try 
		{
			this.irodsAccount = createAccount();
	
			accessObjectFactory = IRODSAccessObjectFactoryImpl.instance(getThreadLocalSession(irodsAccount));
			log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " open connection for thread");
			doesExist("/");
		} 
		catch (AuthenticationException e) {
			disconnect();
			throw new RemoteDataException("Failed to authenticate to remote server. " + e.getMessage(), e);
		}
		catch(JargonException e) 
		{
			disconnect();
			if (e.getMessage().toLowerCase().contains("unable to start ssl socket")) {
				throw new RemoteDataException("Unable to validate SSL certificate on the IRODS server used for PAM authentication.", e);
			} else if (e.getMessage().toLowerCase().contains("connection refused")) {
				throw new RemoteDataException("Connection refused: Unable to contact IRODS server at " + host + ":" + port, e);
			} else {
				throw new RemoteDataException("Failed to connect to remote server.", e);
			}
		}
		catch (ConnectException e) {
			disconnect();
			throw new IOException("Connection refused: Unable to contact IRODS server at " + host + ":" + port, e);
		}
		catch (Exception e) {
			disconnect();
			throw new RemoteDataException("Failed to authenticate to remote server.", e);
		}
	}
	
	
	
	private CollectionAndDataObjectListAndSearchAO getCollectionAndDataObjectListAndSearchAO() throws JargonException
	{
		if (collectionAndDataObjectListAndSearchAO == null) {
			collectionAndDataObjectListAndSearchAO = accessObjectFactory.getCollectionAndDataObjectListAndSearchAO(getIRODSAccount());
		}
		
		return collectionAndDataObjectListAndSearchAO;
	}
	
	private CollectionAO getCollectionAO() throws JargonException
	{
		if (collectionAO == null) {
			collectionAO = accessObjectFactory.getCollectionAO(getIRODSAccount()); 
		}
		
		return collectionAO;
	}
	
	private IRODSFileSystemAO getIRODSFileSystemAO() throws JargonException
	{
		if (fileSystemAO == null) {
		    fileSystemAO = accessObjectFactory.getIRODSFileSystemAO(getIRODSAccount());
		}
		
		return fileSystemAO;
	}
	
	private DataObjectAO getDataObjectAO() throws JargonException
	{
		if (dataObjectAO == null) {
			dataObjectAO = accessObjectFactory.getDataObjectAO(getIRODSAccount());
		}
		
		return dataObjectAO;
	}
	
	
	private DataTransferOperations getDataTransferOperations() throws JargonException
	{
		if (dataTransferOperations == null) {
			dataTransferOperations = accessObjectFactory.getDataTransferOperations(getIRODSAccount());
		}
		
		return dataTransferOperations;
	}
	
	private IRODSFileFactory getIRODSFileFactory() throws JargonException
	{
		if (irodsFileFactory == null) {
			irodsFileFactory = accessObjectFactory.getIRODSFileFactory(getIRODSAccount());
		}
		
		return irodsFileFactory;
	}
	
	/**
	 * Returns the current {@link IRODSAccount} or creates a new
	 * one if none has been set.
	 * @return
	 * @throws JargonException 
	 * @throws EncryptionException 
	 */
	private IRODSAccount getIRODSAccount() throws JargonException {
		if (this.irodsAccount == null) {
			try {
				this.irodsAccount = createAccount();
			} catch (EncryptionException e) {
				throw new JargonException("Failed to process IRODS credentials.", e);
			}
		}
		
		return this.irodsAccount;
	}
	
	/**
	 * Creates a new {@link IRODSAccount} using the connection parameters
	 * provided in the constructor.
	 * 
	 * @return unverified {@link IRODSAccount} object with the proper {@link AuthScheme} configured.
	 * @throws EncryptionException
	 * @throws JargonException
	 */
	private IRODSAccount createAccount() throws EncryptionException, JargonException
	{
		IRODSAccount account = null;
		if (type.equals(AuthScheme.GSI)) {
			account = GSIIRODSAccount.instance(host, port, credential, resource);
			account.setZone(zone);
			account.setHomeDirectory(rootDir);
		}
		else 
		{
			account = new IRODSAccount(host,port,username, password, rootDir, zone, resource);
			
			if (type != null) 
			{
				account.setAuthenticationScheme(type);
			}
		}
		
		return account;
	}
	
	@Override
	public String resolvePath(String path) throws java.io.FileNotFoundException
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
			path = FileUtils.normalize(adjustedPath);
		} else {
			path = FilenameUtils.normalize(adjustedPath);
		}
		
		if (path == null) {
			throw new java.io.FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
		} else if (!path.startsWith(rootDir)) {
			if (!path.equals(StringUtils.removeEnd(rootDir, "/"))) {
				throw new java.io.FileNotFoundException("The specified path " + path + 
					" does not exist or the user does not have permission to view it.");
			}
		}
			
		return StringUtils.stripEnd(path, " ");
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#mkdir(java.lang.String)
	 */
	@Override
	public boolean mkdir(String remotedir) 
	throws IOException, RemoteDataException
	{
		IRODSFile file = null;
    	
		try 
		{
			file = getFile(remotedir);
			getIRODSFileSystemAO().mkdir(file, false);
		} 
		catch (DuplicateDataException e) {
			return false;
		} 
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to create " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (JargonException e) {
			// check if this means that it already exists, and call that a
			// 'false' instead of an error
			if (e.getMessage().indexOf("-809000") > -1) {
				return false;
			}
			throw new RemoteDataException("Failed to create " + remotedir, e);
		} 
        
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#mkdirs(java.lang.String)
	 */
	@Override
	public boolean mkdirs(String remotedir) throws IOException, RemoteDataException
	{
		IRODSFile file = null;
    	
		try 
		{
			file = getFile(remotedir);
			getIRODSFileSystemAO().mkdir(file, true);
		} 
		catch (DuplicateDataException e) {
			return false;
		} 
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to create " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (JargonException e) {
			// check if this means that it already exists, and call that a
			// 'false' instead of an error
			if (e.getMessage().indexOf("-809000") > -1) {
				return false;
			}
			throw new RemoteDataException("Failed to create " + remotedir, e);
		} 
        
		
		return true;
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
    public int getMaxBufferSize() {
        return MAX_BUFFER_SIZE;
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getInputStream(java.lang.String, boolean)
	 */
	@Override
	public IRODSInputStream getInputStream(String path, boolean passive)
			throws IOException, RemoteDataException
	{
//		IRODS irods = this.clone();
//		irods.authenticate();
//		
		return new IRODSInputStream(this, path, false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getOutputStream(java.lang.String, boolean, boolean)
	 */
	@Override
	public IRODSOutputStream getOutputStream(String path, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
//		IRODS irods = this.clone();
//		irods.authenticate();
		
		return new IRODSOutputStream(this, path, append, false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#ls(java.lang.String)
	 */
	@Override
	public List<RemoteFileInfo> ls(String remotedir) 
	throws IOException, RemoteDataException
	{
		List<RemoteFileInfo> files = new ArrayList<RemoteFileInfo>();
		IRODSFile file = null;
    	
		try
		{
			// list irods folder
			String resolvedPath = resolvePath(remotedir);
			
			if (resolvedPath.endsWith("/"))
			{
				resolvedPath = resolvedPath.substring(0, resolvedPath.length() - 1);
			}
			
			file = getFile(remotedir);
			
			if (!file.exists()) 
			{
				throw new FileNotFoundException("No such file or directory");
			} 
			else if (file.isFile() && file.canRead()) 
			{
				files.add(new RemoteFileInfo(file));
			} 
			else 
			{	
				int pem = getIRODSFileSystemAO().getDirectoryPermissionsForGivenUser(file, username);
				
				if (pem < FilePermissionEnum.READ.getPermissionNumericValue()) { //getPermissionForUser(username,remotedir).canRead()) {
					throw new RemoteDataException("Failed to list " + remotedir + " due to insufficient privileges.");
				}
			
				List<CollectionAndDataObjectListingEntry> entries = 
						getCollectionAndDataObjectListAndSearchAO().listDataObjectsAndCollectionsUnderPath(resolvedPath);
				
				for (CollectionAndDataObjectListingEntry entry : entries)
				{
				    RemoteFileInfo fileInfo = new RemoteFileInfo(entry);
					files.add(fileInfo);
				}
			}
			return files;
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to create " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (FileNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch(JargonException e) {
			throw new RemoteDataException("Failed to list directory " + remotedir, e);
		}
		catch(JargonRuntimeException e) {
			throw new RemoteDataException("Failed to list directory " + remotedir, e);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#get(java.lang.String, java.lang.String)
	 */
	@Override
	public void get(String remotedir, String localdir) throws IOException,
			RemoteDataException
	{
		get(remotedir, localdir, null);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#get(java.lang.String, java.lang.String, org.iplantc.service.jobs.io.RemoteTransferListener)
	 */
	@Override
	public void get(String remotedir, String localdir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		IRODSFile file = null;
    	
		try
		{
		    file = getFile(remotedir);
			
			if (listener == null) {
				listener = new RemoteTransferListener(null);
			}
			
			if (file.exists())
			{
				File localDir = new File(localdir);
				
				if (file.isDirectory())
				{
					if (!localDir.exists()) 
					{
						if (!localDir.getParentFile().exists()) {
							throw new java.io.FileNotFoundException("No such file or directory");
						} else {
							// create the target directory 
							if (!localDir.mkdir()) {
								throw new IOException("Failed to create local download directory");
							}
						}
						
						// recursively copy files into the local folder since irods won't let you specify 
						// the target folder name 
						List<CollectionAndDataObjectListingEntry> entries = 
								getCollectionAndDataObjectListAndSearchAO().listDataObjectsAndCollectionsUnderPath(resolvePath(remotedir));
						
						for (CollectionAndDataObjectListingEntry entry : entries)
						{
						    getDataTransferOperations().getOperation(
							        file.getAbsolutePath() + "/" + entry.getNodeLabelDisplayValue(), 
									localDir.getAbsolutePath() + File.separator + entry.getNodeLabelDisplayValue(), 
									resource, 
									listener, 
									getTransferControlBlock());
							
							TransferStatus statusCallback = listener.getOverallStatusCallback();
							if (statusCallback != null && statusCallback.getTransferException() != null) {
								throw statusCallback.getTransferException();
							}
						}
					} 
					else 
					{
						getDataTransferOperations().getOperation(
						        file, 
								localDir, 
								listener, 
								getTransferControlBlock());
						
						TransferStatus statusCallback = listener.getOverallStatusCallback();
						if (statusCallback != null && statusCallback.getTransferException() != null) {
							throw statusCallback.getTransferException();
						}
					}
				}
				else
				{
					getDataTransferOperations().getOperation(
					        file, 
							localDir, 
							listener, 
							getTransferControlBlock());
					
					TransferStatus statusCallback = listener.getOverallStatusCallback();
					if (statusCallback != null && statusCallback.getTransferException() != null) {
						throw statusCallback.getTransferException();
					}	
				}
			}
			else 
			{
				throw new java.io.FileNotFoundException("No such file or directory");
			}
		} 
		catch (FileNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		}
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to get " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (IOException e) {
			throw e;
		}
		catch (JargonException e) {
			if (e.getCause() instanceof java.io.IOException) {
				throw new java.io.FileNotFoundException("No such file or directory");
			} else {
				throw new RemoteDataException("Failed to get file from irods.", e);
			}
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file to irods.", e);
		} 
        
		
	}
	
	/**
	 * Creates a transfer control block to steer an irods transfer and
	 * configure the transfer options with {@link #getTransferOptions()}.
	 * 
	 * @return 
	 * @throws JargonException 
	 */
	private TransferControlBlock getTransferControlBlock() 
	throws JargonException 
	{
	    TransferControlBlock transferControlBlock = DefaultTransferControlBlock.instance();
        transferControlBlock.setTransferOptions(getTransferOptions());
        return transferControlBlock;
    }

    /**
	 * Sets the transfer options for the given transfer.
	 * This will enable parallelism and set the max thread
	 * count to 4. Buffer size and TCP window tuning are
	 * not handled here.
	 * 
	 * @return
	 */
	private TransferOptions getTransferOptions() 
	{
	    TransferOptions transferOptions = new TransferOptions();
	    // disabling parallel transfers in hopes of improving performance overall
//        transferOptions.setUseParallelTransfer(true);
//        transferOptions.setMaxThreads(4);
	    transferOptions.setUseParallelTransfer(false);
        return transferOptions;
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
        try 
        {
            IRODSFile localFile = getFile(localpath);
            IRODSFile destPath = getFile(remotepath);
            
            if (!doesExist(remotepath)) 
            {
                put(localpath, remotepath, listener);
            }
            else if (localFile.isDirectory()) {
                throw new RemoteDataException("cannot append directory");
            }
            else {
                if (listener != null) {
                    listener.started(localFile.length(), localFile.getAbsolutePath());
                }
                
                IRODSRandomAccessFile randomAccessFile = null;
                
                if (destPath.isFile()) 
                {
                    randomAccessFile = getDataTransferOperations().getIRODSFileFactory().instanceIRODSRandomAccessFile(destPath);
                } 
                else {
                    randomAccessFile = getDataTransferOperations().getIRODSFileFactory().instanceIRODSRandomAccessFile(destPath.getAbsolutePath() + "/" + localFile.getName());
                }
                
                InputStream in = null;
                BufferedInputStream bis = null;
                byte[] b = new byte[MAX_BUFFER_SIZE];
                
                try {
                    in = new FileInputStream(localFile.getAbsolutePath());
                    bis = new BufferedInputStream(in);
                    
                    randomAccessFile = getDataTransferOperations().getIRODSFileFactory().instanceIRODSRandomAccessFile(destPath);
                    
                    // move to end of file
                    randomAccessFile.seek(0, SeekWhenceType.SEEK_END);
                    
                    long bytesSoFar = 0;
                    int length = 0;
                    long callbackTime = System.currentTimeMillis();
                    
                    if (listener != null) {
                        listener.started(localFile.length(), localpath);
                    }
                    
                    while (( length = bis.read(b, 0, MAX_BUFFER_SIZE)) != -1) 
                    {
                        bytesSoFar += length;
                        
                        randomAccessFile.write(b, 0, length);
                        if (listener != null) {
                            // update the progress every 15 seconds buffer cycle. This reduced the impact
                            // from the observing process while keeping the update interval at a 
                            // rate the user can somewhat trust
                            if (System.currentTimeMillis() > (callbackTime + 10000))
                            {
                                callbackTime = System.currentTimeMillis();
                                
                                listener.progressed(bytesSoFar);
                            }
                        }
                        
                    }
                    
                    if (listener != null) {
                        // update with the final transferred blocks and wrap the transfer.
                        listener.progressed(bytesSoFar);
                        listener.completed();
                    }
                } 
                finally {
                    try { in.close(); } catch (Exception e) {}
                    try { bis.close(); } catch (Exception e) {}
                    try { randomAccessFile.close(); } catch (Exception e) {}
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
            throw new RemoteDataException("Failed to append data to " + remotepath, e);
        }
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#put(java.lang.String, java.lang.String)
	 */
	@Override
	public void put(String localdir, String remotedir) throws IOException,
			RemoteDataException
	{
		put(localdir, remotedir, null);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#put(java.lang.String, java.lang.String, org.iplantc.service.jobs.io.RemoteTransferListener)
	 */
	@Override
	public void put(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(localdir)) {
			throw new RemoteDataException("localdir cannot be null");
		}
		
		if ((listener == null)) {
            listener = new RemoteTransferListener(null);
        }
		
		IRODSFile destFile = null;
		try
		{
            destFile = getFile(remotedir);
			File sourceFile = new File(localdir);
			
			if (destFile.exists()) 
			{
				// can't put dir to file
				if (sourceFile.isDirectory() && !destFile.isDirectory()) {
					throw new RemoteDataException("cannot overwrite non-directory: " + remotedir + " with directory " + sourceFile.getName());
				} 
				else if (!sourceFile.isDirectory())
				{
					if (destFile.isDirectory())
					{
						remotedir += (StringUtils.isEmpty(remotedir) ? "" : "/") + sourceFile.getName();
						destFile = null;
						destFile = getFile(remotedir);
					}
				}
			}
			// dest path isn't there. make sure the parent directory exists
			else if (doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/..")))
			{
			    // if we are uploading a directory, and source and dest  
			    if (sourceFile.isDirectory()) 
			    {
			        try {
    			        listener.started(org.codehaus.plexus.util.FileUtils.sizeOfDirectory(sourceFile), sourceFile.getAbsolutePath());
    			        
    			        mkdir(remotedir);
    			        
    			        for (File child: sourceFile.listFiles()) 
    			        {   
    			            String childRemotePath = remotedir + "/" + child.getName();
    	                    TransferTask childTask = null;
    	                    if (listener.getTransferTask() != null) {
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
    	                    IRODSFile childFile = getFile(childRemotePath);
    	                    
    	                    RemoteTransferListener childListener = new RemoteTransferListener(childTask);
    			            
    	                    TransferControlBlock transferControlBlock = getTransferControlBlock();
    	                    getDataTransferOperations().putOperation(child, childFile, childListener, transferControlBlock);
	                        
	                        TransferStatus statusCallback = childListener.getOverallStatusCallback();
	                        if (statusCallback != null && statusCallback.getTransferException() != null) {
                                throw statusCallback.getTransferException();
                            }
    			        }
    			        
    			        listener.completed();
			        }
			        catch (Throwable t) 
			        {
			            listener.failed();
			            throw t;
			        } 
			        
			        return;
			    }
			}
			else
			{
				// throw exception since this cannot happen without a valid parent path
			    // to reference.
				throw new FileNotFoundException("No such file or directory.");
			}
			
			// bust cache since this file has now changed
            fileInfoCache.remove(resolvePath(remotedir));
            
            getDataTransferOperations().putOperation(sourceFile, destFile, listener, getTransferControlBlock());
            
            TransferStatus statusCallback = listener.getOverallStatusCallback();
            if (statusCallback != null && statusCallback.getTransferException() != null) {
                throw statusCallback.getTransferException();
            }
		} 
		catch (CatNoAccessException e) {
		    throw new RemoteDataException("Failed to put " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (FileNotFoundException | DataNotFoundException e) {
		    throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (RemoteDataException | IOException e) {
		    throw e;
		}
		catch (Throwable e) {
		    throw new RemoteDataException("Failed to transfer file to irods.", e);
		}
	}
	
	@Override
	public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(localdir)) {
			throw new RemoteDataException("localdir cannot be null");
		}
		
		IRODSFile destFile = null;
		try
		{
            destFile = getFile(remotedir);
			File sourceFile = new File(localdir);
			
			if (!destFile.exists()) 
			{
				put(localdir, remotedir, listener);
				return;
			}
			else if (sourceFile.isDirectory())
			{
				String adjustedRemoteDir = remotedir;
				
				// can't put dir to file
				if (!isDirectory(adjustedRemoteDir)) {
					getFile(adjustedRemoteDir).deleteWithForceOption();
					put(localdir, adjustedRemoteDir, listener);
					return;
				} 
				else 
				{
					adjustedRemoteDir += (StringUtils.isEmpty(remotedir) ? "" : "/") + sourceFile.getName();
					destFile = null;
					destFile = getFile(adjustedRemoteDir);
				}
				
				// Need to check for both source and destination files, as overwrite will not work
	            // if destination file is not present and will not throw and exception if there is no source file.
				if ((listener == null)) 
				{
					listener = new RemoteTransferListener(null);
				}
				
				for (File child: sourceFile.listFiles())
				{
					String remoteChildPath = adjustedRemoteDir + "/" + child.getName();
					if (child.isDirectory()) 
					{
						// local is a directory, remote is a file. delete remote file. we will replace with local directory
						if (!isDirectory(remoteChildPath)) {
							delete(remoteChildPath);
						}
						
						// now create the remote directory
						mkdir(remoteChildPath);
						
						syncToRemote(child.getAbsolutePath(), adjustedRemoteDir, listener);
					}
					else 
					{	
						// sync the file or folder now that we've cleaned up
						syncToRemote(child.getAbsolutePath(), remoteChildPath, listener);
					}
				}	
			} 
			else
			{
			    if ((listener == null)) 
                {
                    listener = new RemoteTransferListener(null);
                }
			    
				// sync if file is not there
				if (!doesExist(remotedir))  
				{
				    // bust cache since this file has now changed
		            fileInfoCache.remove(resolvePath(remotedir));
		            
		            getDataTransferOperations().putOperation(sourceFile, destFile, listener, getTransferControlBlock());
					
					TransferStatus statusCallback = listener.getOverallStatusCallback();
					if (statusCallback != null && statusCallback.getTransferException() != null) {
						throw statusCallback.getTransferException();
					}
				}
				else 
				{
					// if the types mismatch, delete remote, use current
					if (sourceFile.isDirectory() && !destFile.isDirectory() || 
							sourceFile.isFile() && !destFile.isFile()) 
					{
						destFile.deleteWithForceOption();
						getDataTransferOperations().putOperation(sourceFile, destFile, listener, getTransferControlBlock());
						
						TransferStatus statusCallback = listener.getOverallStatusCallback();
						if (statusCallback != null && statusCallback.getTransferException() != null) {
							throw statusCallback.getTransferException();
						}
					} 
					// or if the file sizes are different
					else if (sourceFile.length() != destFile.length())
					{
					    // bust cache since this file has now changed
			            fileInfoCache.remove(resolvePath(remotedir));
			            
			            getDataTransferOperations().putOperation(sourceFile, destFile, listener, getTransferControlBlock());
						
						TransferStatus statusCallback = listener.getOverallStatusCallback();
						if (statusCallback != null && statusCallback.getTransferException() != null) {
							throw statusCallback.getTransferException();
						}
					}
					else
					{
						log.debug("Skipping transfer of " + sourceFile.getPath() + " to " + 
								destFile.getPath() + " because file is present and of equal size.");
					}
				}
			}
		} 
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to put " + remotedir + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (IOException e) {
			throw e;
		}
		catch (FileNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		}
		catch (JargonException e) {
			throw new RemoteDataException("Failed to copy file to irods.", e);
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file to irods.", e);
		}
	}
	
	protected ObjStat stat(String remotepath) 
    throws IOException, RemoteDataException
    {
	    String resolvedPath = StringUtils.removeEnd(resolvePath(remotepath), "/");
        try
        {
        	try {
                
                return getIRODSFileSystemAO().getObjStat(resolvedPath);
            } 
            catch (JargonException e) {
            	// catch the wrapped socket exception from a dropped connection
            	// clean up the connection, and retry.
            	if (e.getCause() instanceof java.net.SocketException) {
            		log.error("Connection timeout attempting to contact the remote server. Retrying one more time");
                    // connection should have been closed, but we clean up here
            		// just to be safe and ensure we don't have an open
            		// session lingering around anywhere in the underlying code.
            		disconnect();
            		
            		// now re-authenticate to init the new session and 
            		// get a valid connection to the server
                    authenticate();
                    
                    // retry the stat with the fresh connection
                    return getIRODSFileSystemAO().getObjStat(resolvedPath);
            	} else {
            		throw e;
            	}
            }
            catch(Throwable e) {
                log.error("Connection timeout attempting to contact the remote server. Retrying one more time");
                disconnect();
                authenticate();
                return getIRODSFileSystemAO().getObjStat(resolvedPath);
            }   
        } 
        catch (FileNotFoundException e) {
            throw new java.io.FileNotFoundException("No such file or directory");
        }
        catch(JargonException e) {
            if (e.getMessage().toLowerCase().contains("unable to start ssl socket")) {
                throw new RemoteDataException("Unable to validate SSL certificate on the IRODS server used for PAM authentication.", e);
            } else if (e.getMessage().toLowerCase().contains("connection refused")) {
                throw new RemoteDataException("Connection refused: Unable to contact IRODS server at " + host + ":" + port);
            } else {
                throw new RemoteDataException("Failed to connect to remote server.", e);
            }
        } 
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#isDirectory(java.lang.String)
	 */
	@Override
	public boolean isDirectory(String remotepath) 
	throws IOException, RemoteDataException
	{
	    ObjStat stat = stat(remotepath);
        
        // should be redundant. Anything other than a collection at the path will
        // throw a org.irods.jargon.core.exception.FileNotFoundException
        return stat.getObjectType() == ObjectType.COLLECTION || 
               stat.getObjectType() == ObjectType.LOCAL_DIR;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#isFile(java.lang.String)
	 */
	@Override
	public boolean isFile(String remotepath) throws IOException, RemoteDataException
	{
	    ObjStat stat = stat(remotepath);
        
        // should be redundant. Anything other than a collection at the path will
        // throw a org.irods.jargon.core.exception.FileNotFoundException
        return stat.getObjectType() == ObjectType.DATA_OBJECT || 
               stat.getObjectType() == ObjectType.LOCAL_FILE || 
               stat.getObjectType() == ObjectType.UNKNOWN_FILE;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#length(java.lang.String)
	 */
	@Override
	public long length(String remotepath) 
	throws IOException, RemoteDataException
	{
	    ObjStat stat = stat(remotepath);
        return stat.getObjSize();
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#checksum(java.lang.String)
	 */
	@Override
	public String checksum(String remotepath) 
	throws IOException, RemoteDataException
	{
//		
    	
		try
		{
		    if (isDirectory(remotepath)) {
			    throw new RemoteDataException("Cannot perform checksum on a directory");
			} else {
//			    IRODSFile file = getFile(remotepath);
//			    ChecksumValue chksumValue = getDataObjectAO().computeChecksumOnDataObject(file);
//			    return chksumValue.getChecksumStringValue();
			    throw new NotImplementedException("Checksum is not currently supported.");
			}
		} 
	    catch (NotImplementedException e) {
			throw e;
		}
//		catch (CatNoAccessException e) {
//			throw new RemoteDataException("Failed to perform checksum on " + remotepath + " due to insufficient privileges.", e);
//		} 
//		catch (DataNotFoundException e) {
//			throw new java.io.FileNotFoundException("No such file or directory");
//		} 
//		catch(JargonException e) {
//			throw new RemoteDataException("Failed to connect to remote server.", e);
//		} 
        
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#doRename(java.lang.String, java.lang.String)
	 */
	@Override
	public void doRename(String oldpath, String newpath) throws IOException, RemoteDataException
	{
		IRODSFile newFile = null;
		IRODSFile oldFile = null;
    	
		try
		{
			if (!doesExist(oldpath)) {
				throw new java.io.FileNotFoundException("No such file or directory");
			} else if (!doesExist(newpath)) {
				if (!doesExist(newpath + (StringUtils.isEmpty(newpath) ? ".." : "/.."))) {
					throw new java.io.FileNotFoundException("No such file or directory: " + newpath);
				}
			}
			
			newFile = getFile(newpath);
			oldFile = getFile(oldpath);
			
			oldFile.renameTo(newFile);
		}
		catch (IOException e) {
			throw e;
		}
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to rename " + oldpath + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (JargonException e) {
			throw new RemoteDataException("Rename operation failed: " + e.getCause().getMessage(), e);
		} 
		catch (JargonRuntimeException e) {
			if (e.getCause() instanceof CatNoAccessException) {
				throw new RemoteDataException("Failed to rename " + oldpath + " due to insufficient privileges.", e);
			} else {
				throw new RemoteDataException("Failed to rename " + oldpath, e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#copy(java.lang.String, java.lang.String)
	 */
	@Override
	public void copy(String remotesrc, String remotedest) throws IOException,
			RemoteDataException
	{
		copy(remotesrc, remotedest, null);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#copy(java.lang.String, java.lang.String, org.iplantc.service.jobs.io.RemoteTransferListener)
	 */
	
	@Override
	public void copy(String sourcePath, String destPath, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		IRODSFile sourceFile = null;
		IRODSFile destFile = null;
		
		try
		{
			if (listener == null) {
				listener = new RemoteTransferListener(null);
			}
			
			if (StringUtils.equals(sourcePath, destPath)) {
				// ignore copies onto self
				listener.completed();
				return;
			}
			
			sourceFile = getFile(sourcePath);
			destFile = getFile(destPath);
			
			
			if (isDirectory(sourcePath))
			{
				if (doesExist(destPath)) 
				{
					// can't put dir to file
					if (!isDirectory(destPath)) {
						throw new RemoteDataException("cannot overwrite non-directory: " + destPath + " with directory " + sourceFile.getName());
					} 
					else 
					{
//						remotedir += (StringUtils.isEmpty(remotedir) ? "" : "/") + sourceFile.getName();
//						destFile = null;
//						destFile = getFile(remotedir);
					}
				}
				else if (!doesExist(destPath + (StringUtils.isEmpty(destPath) ? ".." : "/..")))
				{
					// nothing we can do here
					throw new FileNotFoundException("No such file or directory");
				}
			} 
			
			if (!getPermissionForUser(username, destPath).canWrite()) {
				throw new RemoteDataException("Failed to copy " + sourcePath + " due to insufficient privileges.");
			}
			
			getDataTransferOperations().copy(sourceFile, destFile, listener, null);
			
			TransferStatus statusCallback = listener.getOverallStatusCallback();
			if (statusCallback != null && statusCallback.getTransferException() != null) {
				throw statusCallback.getTransferException();
			}	
		} 
		catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to copy " + sourcePath + " due to insufficient privileges.", e);
		} 
		catch (DataNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (IOException e) {
			throw e;
		}
		catch (FileNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		}
		catch (JargonException e) {
			throw new RemoteDataException("Failed to copy file or directory .", e);
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file or directory to irods.", e);
		}
		 
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getUrlForPath(java.lang.String)
	 */
	@Override
	public URI getUriForPath(String path) throws IOException,
			RemoteDataException
	{
		try {
			return new URI("irods://" + host + (port == 1247 ? "" : ":" + port) + "/" + path);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#delete(java.lang.String)
	 */
	@Override
	public void delete(String remotedir) throws IOException, RemoteDataException
	{
		IRODSFile file = null;
		try
		{
		    
            // bust cache since this file has now changed
		    String resolvedPath = resolvePath(remotedir);
		    fileInfoCache.remove(resolvedPath);
            String prefixPath = StringUtils.removeEnd(resolvedPath, "/") + "/";
            for (String path: fileInfoCache.keySet()) {
                if (StringUtils.startsWith(path, prefixPath)) {
                    fileInfoCache.remove(path);
                }
            }
            
			if (isDirectory(remotedir)) {
			    file = getFile(remotedir);
	            file.delete();               
			} else {
			    file = getFile(remotedir);
			    file.deleteWithForceOption();
			}
		} 
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		} 
		catch (JargonRuntimeException | CatNoAccessException e) {
			throw new RemoteDataException("Failed to delete " + remotedir + " due to insufficient privileges.", e);
		}
		catch(JargonException e) {
			throw new RemoteDataException("Failed to connect to remote server.", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#isThirdPartyTransferSupported()
	 */
	@Override
	public boolean isThirdPartyTransferSupported()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		try { 
			this.accessObjectFactory.closeSessionAndEatExceptions(irodsAccount); 
		} catch (Throwable e) {}
		this.accessObjectFactory = null;
		log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " closed connection for thread");
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#doesExist(java.lang.String)
	 */
	@Override
	public boolean doesExist(String path) throws IOException, RemoteDataException
	{
		try
		{
		    stat(path);
		    return true;
		} 
		catch (java.io.FileNotFoundException e) { 
		    return false;
		}
		catch(IOException | RemoteDataException e) {
			throw e;
		} 
	}

	public InputStream getRawInputStream(String remotepath) throws JargonException, RemoteDataException, IOException
	{
		IRODSFile irodsFile = null;
		try
		{
		    try 
		    {
		        if (isDirectory(remotepath)) {
					throw new RemoteDataException("Cannot open output stream to directory " + remotepath);
				} else {
				    irodsFile = getFile(remotepath);
				    if (!irodsFile.canRead()) {
				        throw new RemoteDataException("Permission denied");
				    }
				}
			} 
		    // no such file/folder
		    catch (java.io.FileNotFoundException e) {
			    String resolvedParentPath = remotepath + (StringUtils.isEmpty(remotepath) ? ".." : "/..");
			    if (!doesExist(resolvedParentPath)) {
					throw new java.io.FileNotFoundException("No such file or directory");
				} else if (!hasWritePermission(resolvedParentPath, username)) {
				    throw new RemoteDataException("Permission denied");
				} else {
				    irodsFile = getFile(remotepath);
				}
			}	
			
		    log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " opening raw input stream connection for thread");
			InputStream in =  irodsFileFactory
					.instanceIRODSFileInputStream(irodsFile);

			return in;
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to obtain remote output stream for " + remotepath);
		} 
	}
	
	public OutputStream getRawOutputStream(String remotepath) throws JargonException, RemoteDataException, IOException
	{
		IRODSFile file = null;
		try 
		{
			if (doesExist(remotepath)) 
			{
				if (isDirectory(remotepath)) {
					throw new RemoteDataException("Cannot open output stream to directory " + remotepath);
				} else {
				    file = getFile(remotepath);
		            if (!file.canWrite()) {
    					throw new RemoteDataException("Permission denied");
    				}
				}
			} 
			else {
			    String resolvedParentPath = remotepath + (StringUtils.isEmpty(remotepath) ? ".." : "/..");
                if (!doesExist(resolvedParentPath)) {
					throw new java.io.FileNotFoundException("No such file or directory");
				} else if (!hasWritePermission(resolvedParentPath, username)) {
					throw new RemoteDataException("Permission denied");
				} else {
				    file = getFile(remotepath);
				}
			}
			
			log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " opening raw output stream connection for thread");
			OutputStream out = new BufferedOutputStream(getIRODSFileFactory()
					.instanceIRODSFileOutputStream(file));

			return out;
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to obtain remote output stream for " + remotepath);
		} 
        
	}
	
	public DataOutput getRawRandomAccessOutputStream(String remotepath) throws JargonException, RemoteDataException, IOException
    {
        IRODSFile file = null;
        try 
        {
            if (doesExist(remotepath)) 
            {
                if (isDirectory(remotepath)) {
                    throw new RemoteDataException("Cannot open output stream to directory " + remotepath);
                } else {
                    file = getFile(remotepath);
                    if (!file.canWrite()) {
                        throw new RemoteDataException("Permission denied");
                    }
                }
            } 
            else {
                String resolvedParentPath = remotepath + (StringUtils.isEmpty(remotepath) ? ".." : "/..");
                if (!doesExist(resolvedParentPath)) {
                    throw new java.io.FileNotFoundException("No such file or directory");
                } else if (!hasWritePermission(resolvedParentPath, username)) {
                    throw new RemoteDataException("Permission denied");
                } else {
                    file = getFile(remotepath);
                }
            } 
          
            log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  
            		+ " opening raw random output stream connection for thread");
            return getIRODSFileFactory().instanceIRODSRandomAccessFile(file);

        }
        catch (IOException e) {
            throw e;
        }
        catch (RemoteDataException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RemoteDataException("Failed to obtain remote output stream for " + remotepath);
        } 
        
    }
	
	/**
	 * Creates an instance of an {@link IRODSFile} at the given virtual path. Note that the
	 * virutal path will be validated and resolved to an absolute path prior to the  {@link IRODSFile} 
	 * instance being created.
	 * 
	 * @param path
	 * @return instance of an {@link IRODSFile}. The underlying object or collection is not guaranteed to exist.
	 * @throws JargonException
	 * @throws RemoteDataException
	 * @throws IOException
	 */
	public IRODSFile getFile(String path) throws JargonException, RemoteDataException, IOException
	{
	    String resolvedPath = resolvePath(path);
	    try {
    	    IRODSFile irodsFile = fileInfoCache.get(resolvedPath);
            // check the cache so we can save a query when possible
            if (irodsFile == null) 
            {
                IRODSFileFactory irodsFileFactory = getIRODSFileFactory();
                irodsFile = irodsFileFactory.instanceIRODSFile(resolvedPath);
                fileInfoCache.put(resolvedPath, irodsFile);
            }
            
            return irodsFile;
	    } catch (JargonException e) {
	        // invalidate cache of this entry
	        fileInfoCache.remove(resolvedPath);
	        throw e;
	    }
	}
	
	@Override
	public RemoteFileInfo getFileInfo(String path) throws RemoteDataException, IOException
	{
    	try {
		    return new RemoteFileInfo(getFile(path));
		} catch (JargonException e) {
			throw new RemoteDataException("Failed to retrieve file info for " + path, e);
		}
	}

    /**
     * Returns a list of all the permissions for the object represented by the given path with the
     * permission of the user with the given username first.
     *
     * @param path
     * @param username
     * @return
     * @throws RemoteDataException
     */
    @Override
    public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username) 
    throws RemoteDataException, IOException 
    {

        List<RemoteFilePermission> pems = getAllPermissions(path);
        List<RemoteFilePermission> filteredPems = new ArrayList<RemoteFilePermission>();
        
        for (RemoteFilePermission pem : pems) {
            if (pem.getUsername().equals(username)) {
                filteredPems.add(pem);
                
                break;
            }
        }
        
        for (RemoteFilePermission pem : pems) {
        	if (!pem.getUsername().equals(username)) {
                filteredPems.add(pem);
            }
        }
        
        return filteredPems;
    }

    @Override
    public List<RemoteFilePermission> getAllPermissions(String path) 
    throws RemoteDataException, IOException 
    {
    	IRODSFile file = null; 
    	List<RemoteFilePermission> remotePems = new ArrayList<RemoteFilePermission>();
        
    	try 
    	{
    		if (!doesExist(path)) {
    			throw new java.io.FileNotFoundException("No such file or directory");
    		} 

    		// jargon will roll up groups to user lists and return everyone. On public files and collections
    		// this is brutal. We can't know for sure when this might happen, so we proactively look for our 
    		// public and community users to hopefully avoid this situation.
    		try {
    			PermissionType publicUserPermission = getPermissionForUser(Settings.PUBLIC_USER_USERNAME, path, true);
    			PermissionType worldUserPermission = getPermissionForUser(Settings.WORLD_USER_USERNAME, path, true);
    			if (!publicUserPermission.equals(PermissionType.NONE)) {
    				remotePems.add(new RemoteFilePermission(Settings.PUBLIC_USER_USERNAME, internalUsername, publicUserPermission, false));
    			}
    			if (!worldUserPermission.equals(PermissionType.NONE)) {
    				remotePems.add(new RemoteFilePermission(Settings.WORLD_USER_USERNAME, internalUsername, worldUserPermission, false));
    			}

    			if (!remotePems.isEmpty()) {
    				return remotePems;
    			}
    		} catch (Exception e) {}
    		
            file = getFile(path);
            
            List<UserFilePermission> pems = null;
            // UPDATE jargon-core so you have the list method!!
            
            if (file.isFile()) {
            	pems = getDataObjectAO().listPermissionsForDataObject(file.getAbsolutePath());
            } else {
                pems = getCollectionAO().listPermissionsForCollection(file.getAbsolutePath());
            }

            for (UserFilePermission pem : pems) {
                FilePermissionEnum pemEnum = pem.getFilePermissionEnum();

                if (pemEnum.equals(FilePermissionEnum.READ)) {
                    remotePems.add(new RemoteFilePermission(pem.getUserName(), internalUsername, PermissionType.READ, false));
                } else if (pemEnum.equals(FilePermissionEnum.WRITE)) {
                    remotePems.add(new RemoteFilePermission(pem.getUserName(), internalUsername, PermissionType.WRITE, true));
                } else if (pemEnum.equals(FilePermissionEnum.OWN)) {
                    remotePems.add(new RemoteFilePermission(pem.getUserName(), internalUsername, PermissionType.READ_WRITE, true));
                } else if (pemEnum.equals(FilePermissionEnum.NONE) || pemEnum.equals(FilePermissionEnum.NULL)) {
                    remotePems.add(new RemoteFilePermission(pem.getUserName(), internalUsername, PermissionType.NONE, false));
                } else if (pemEnum.equals(FilePermissionEnum.EXECUTE)) {
                    remotePems.add(new RemoteFilePermission(pem.getUserName(), internalUsername, PermissionType.EXECUTE, false));
                } else throw new RemoteDataException("Unhandled FilePermissionEnum in conversion to RemoteFilePermission");
            }
            return remotePems;
        } catch (JargonException e) {
            throw new RemoteDataException(e);
        } catch (java.io.FileNotFoundException e) {
            throw e;
        } catch (Throwable e) {
        	throw new RemoteDataException(e);
        } 
    }

    
    @Override
    public PermissionType getPermissionForUser(String username, String path) 
    throws RemoteDataException, IOException 
    {
    	return getPermissionForUser(username, path, false);
    }
    
    private PermissionType getPermissionForUser(String username, String path, boolean skipOwnerCheck) 
    throws RemoteDataException, IOException 
    {
    	IRODSFile file = null;
    	
        try 
        {
        	FilePermissionEnum pem = null;
        	
        	if (doesExist(path)) 
        	{
        		if (isDirectory(path)) 
        		{
                	try {
                		pem = getCollectionAO().getPermissionForCollection(StringUtils.removeEnd(resolvePath(path), "/"), username, zone);//.getPermissionForUserName(file.getAbsolutePath(), username);
                	} catch (SpecificQueryException e) {
                		throw new RemoteDataException("Group ACL setup scripts have not been run on the IRODS server yet. Please contact your system administrator and ask him to complete the setup process.");
                	}
                }
            	else
                {
            		UserFilePermission userPem = getDataObjectAO().getPermissionForDataObjectForUserName(StringUtils.removeEnd(resolvePath(path), "/"), username);
                	
                	// need to check parent for permissions due to strict permissions.
                    if (userPem == null || userPem.getFilePermissionEnum().compareTo(FilePermissionEnum.NULL) <= 0) {
                    	userPem = getDataObjectAO().getPermissionForDataObjectForUserName(
                    			getDataObjectAO().instanceIRODSFileForPath(resolvePath(path)).getParent(), username);
                    }
                    
                    pem = userPem == null ? null : userPem.getFilePermissionEnum();
                }
        	}
        	else
        	{
        		file = getFile(path);
                
            	// check the parent folder to get the parent pem
            	do {
    				file = (IRODSFile) file.getParentFile();
    			} while (!file.exists() && file.getAbsolutePath().startsWith(rootDir));
    			
                UserFilePermission userPem = getCollectionAO().getPermissionForUserName(file.getAbsolutePath(), username);
                pem = userPem == null ? null : userPem.getFilePermissionEnum();
        	}
            
            if (pem == null) {
                return PermissionType.NONE;
            }
            
//            System.out.println("Permission for " + username + " on "  + resolvePath(path) + " is " + pem.name());
            
            if (pem.getPermissionNumericValue() <= FilePermissionEnum.NULL.getPermissionNumericValue()) {
                return PermissionType.NONE;
            } else if (pem.getPermissionNumericValue() == FilePermissionEnum.EXECUTE.getPermissionNumericValue()) {
                return PermissionType.EXECUTE;
            } else if (pem.getPermissionNumericValue() < FilePermissionEnum.WRITE.getPermissionNumericValue()){
                return PermissionType.READ;
            } else if (pem.getPermissionNumericValue() <= FilePermissionEnum.DELETE_OBJECT.getPermissionNumericValue()) {
                return PermissionType.WRITE;
            } else if (pem.getPermissionNumericValue() > FilePermissionEnum.DELETE_OBJECT.getPermissionNumericValue()) {
                return PermissionType.ALL;
            } else {
                throw new RemoteDataException("Unhandled FilePermissionEnum in conversion to RemoteFilePermission");
            }
        } 
        catch (IOException e) {
        	throw e;
        }
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (FileNotFoundException e) {
            throw new RemoteDataException("File/folder not found");
        } 
        catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
    }


    @Override
    public boolean hasReadPermission(String path, String username) 
    throws RemoteDataException, IOException 
    {
    	PermissionType userPem = getPermissionForUser(username, path);
        if (userPem.equals(PermissionType.READ) || userPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        PermissionType publicPem = getPermissionForUser(Settings.PUBLIC_USER_USERNAME, path, true);
        if (publicPem.equals(PermissionType.READ) || publicPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        PermissionType worldPem = getPermissionForUser(Settings.WORLD_USER_USERNAME, path, true);
        if (worldPem.equals(PermissionType.READ) || worldPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        return false;

    }

    @Override
    public boolean hasWritePermission(String path, String username)
    throws RemoteDataException, IOException 
    {
    	PermissionType userPem = getPermissionForUser(username, path);
        if (userPem.equals(PermissionType.WRITE) || userPem.equals(PermissionType.ALL)) {
        	return true;
        }
         
        PermissionType publicPem = getPermissionForUser(Settings.PUBLIC_USER_USERNAME, path, true);
        if (publicPem.equals(PermissionType.WRITE) || publicPem.equals(PermissionType.ALL)) {
        	return true;
        }
         
        PermissionType worldPem = getPermissionForUser(Settings.WORLD_USER_USERNAME, path, true);
        if (worldPem.equals(PermissionType.WRITE) || worldPem.equals(PermissionType.ALL)) {
         	return true;
        }
         
        return false;
    }

    @Override
    public boolean hasExecutePermission(String path, String username)
    throws RemoteDataException, IOException 
    {
        PermissionType userPem = getPermissionForUser(username, path);
        if (userPem.equals(PermissionType.EXECUTE) || userPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        PermissionType publicPem = getPermissionForUser(Settings.PUBLIC_USER_USERNAME, path, true);
        if (publicPem.equals(PermissionType.EXECUTE) || publicPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        PermissionType worldPem = getPermissionForUser(Settings.WORLD_USER_USERNAME, path, true);
        if (worldPem.equals(PermissionType.EXECUTE) || worldPem.equals(PermissionType.ALL)) {
        	return true;
        }
        
        return false;
    }


    @Override
    public void setOwnerPermission(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	IRODSFile file = null;
        try 
        {
        	if (getPermissionForUser(username, path).equals(PermissionType.ALL)) return;
        	
            file = getFile(path);
            
            if (file.isFile()) {
            	getDataObjectAO().setAccessPermissionOwn(zone, file.getAbsolutePath(), username);
            } else {
                getCollectionAO().setAccessPermissionOwn(zone, file.getAbsolutePath(), username, recursive);
            }
        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (IOException e) {
            throw e;
        } 
        
    }

    @Override
    public void setReadPermission(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	IRODSFile file = null;
        try 
        {
            PermissionType userPem = getPermissionForUser(username, path);
        	if (userPem.canRead()) return;
        	
            
            if (userPem.canWrite()) {
                setOwnerPermission(username, path, recursive);
            } else {
                file = getFile(path);
                
                if (file.isFile()) {
                	getDataObjectAO().setAccessPermissionRead(zone, file.getAbsolutePath(), username);
                } else {
                    getCollectionAO().setAccessPermissionRead(zone, file.getAbsolutePath(), username, recursive);
                }
            }
        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (IOException e) {
            throw e;
        } 
        
    }

    @Override
    public void removeReadPermission(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	try 
        {
    		PermissionType pem = getPermissionForUser(username, path);
    		if (!pem.canRead()) {
    			return;
    		} 
    		else if (pem.canWrite()) 
            {
                if (isFile(path)) {
                	getDataObjectAO().setAccessPermissionWrite(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().setAccessPermissionWrite(zone, resolvePath(path), username, recursive);
                }
            } 
            else 
            {
                if (isFile(path)) {
                    getDataObjectAO().removeAccessPermissionsForUser(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().removeAccessPermissionForUser(zone, resolvePath(path), username, recursive);
                }
            }
        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        }
    }

    @Override
    public void setPermissionForUser(String username, String path, PermissionType type, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	try 
        {
            int perms = type.getUnixValue();
            
            if (perms == 1 || perms == 3 || perms == 5) 
            {
            	throw new RemoteDataException("Execute permissions are not supported on IRODS systems.");
            } 
            else if (perms<2) {
                if (isFile(path)) {
                    getDataObjectAO().removeAccessPermissionsForUser(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().removeAccessPermissionForUser(zone, resolvePath(path), username, recursive);
                }
            } else if (perms<4) {
                if (isFile(path)) {
                	getDataObjectAO().setAccessPermissionWrite(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().setAccessPermissionWrite(zone, resolvePath(path), username, recursive);
                }
            } else if (perms<6) {
                if (isFile(path)) {
                    getDataObjectAO().setAccessPermissionRead(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().setAccessPermissionRead(zone, resolvePath(path), username, recursive);
                }
            } else if (perms<8) {
                if (isFile(path)) {
                	getDataObjectAO().setAccessPermissionOwn(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().setAccessPermissionOwn(zone, resolvePath(path), username, recursive);
                }
            } else {
                throw new RemoteDataException("undefined permission type specified");
            }

            // todo Handle Execution Here - There does not seem to be a setAccessPermissionExecute
            if (perms % 2 == 1) {
                //Execution currently not supported.
            }

        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch(JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        }
    }

    @Override
    public void setWritePermission(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	try 
        {
    		PermissionType pem = getPermissionForUser(username, path);
        	if (!doesExist(path)) {
        		throw new java.io.FileNotFoundException("No such file or directory");
        	}
        	else if (pem.canWrite()) {
        		return;
        	}
        	else if (pem.canRead()) {
                setOwnerPermission(username, path, recursive);
            } 
            else if (isFile(path)) {
                getDataObjectAO().setAccessPermissionWrite(zone, resolvePath(path), username);
            }
            else {
                getCollectionAO().setAccessPermissionWrite(zone, resolvePath(path), username, recursive);
            }
        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        }
    }

    @Override
    public void removeWritePermission(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	try 
    	{
    		PermissionType pem = getPermissionForUser(username, path);
    		if (!doesExist(path)) {
        		throw new java.io.FileNotFoundException("No such file or directory");
        	}
    		else if (!pem.canWrite()) {
    			return;
    		}
    		else if (hasReadPermission(path, username)) 
            {
            	if (isFile(path)) {
                	getDataObjectAO().setAccessPermissionRead(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().setAccessPermissionRead(zone, resolvePath(path), username, recursive);
                }
            } 
            else 
            {
                if (isFile(path)) {
                	getDataObjectAO().removeAccessPermissionsForUser(zone, resolvePath(path), username);
                } else {
                    getCollectionAO().removeAccessPermissionForUser(zone, resolvePath(path), username, recursive);
                }
            }
        } 
    	catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
    	catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        }
    }

    @Override
    public void setExecutePermission(String username, String path, boolean recursive) throws RemoteDataException 
    {
    	throw new RemoteDataException("Execute permissions are not supported on IRODS systems.");
    }

    @Override
    public void removeExecutePermission(String username, String path, boolean recursive) throws RemoteDataException {
        throw new RemoteDataException("Execute permissions are not supported on IRODS systems.");
    }

    @Override
    public void clearPermissions(String username, String path, boolean recursive) 
    throws RemoteDataException, IOException 
    {
    	try 
        {
            //List<UserFilePermission> pems = null;
            if (isFile(path)) {
            	getDataObjectAO().removeAccessPermissionsForUser(zone, resolvePath(path), username);
                
//                pems = dao.listPermissionsForDataObject(file.getAbsolutePath());
//                for (UserFilePermission pem: pems) {
//                	if (StringUtils.equals(pem.getUserName(), username)) {
//                		dao.removeAccessPermissionsForUser(zone, file.getAbsolutePath(), pem.getUserName());
//                		break;
//                	}
//                }
//                dao.removeAccessPermissionsForUser(zone, file.getAbsolutePath(), username);
            } else {
            	getCollectionAO().removeAccessPermissionForUser(zone, resolvePath(path), username, recursive);
//                pems = cao.listPermissionsForCollection(file.getAbsolutePath());
//                for (UserFilePermission pem: pems) {
//                	if (StringUtils.equals(pem.getUserName(), username)) {
//                		cao.removeAccessPermissionForUser(zone, file.getAbsolutePath(), pem.getUserName(), recursive);
//                	}
//                }
            }
        } 
        catch (CatNoAccessException e) {
			throw new RemoteDataException("Failed to change permission on " + path + " due to insufficient privileges.", e);
		}
		catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        } 
    }

    // Does this function make sense without the context of a user?

    @Override
    @Deprecated
    public String getPermissions(String path) throws RemoteDataException, IOException 
    {
    	IRODSFile file = null;
    	
        try 
        {
        	if (!doesExist(path)) {
        		throw new java.io.FileNotFoundException("No such file or directory");
        	}
        	
            file = this.irodsFileFactory.instanceIRODSFile(resolvePath(path));
            
            // modify this method to return all permissions for the given file
            if (file.canRead() && file.canWrite() && file.canExecute()) {
                return "all";
            } else if (file.canRead() && file.canWrite()) {
                return "read_write";
            } else if (file.canRead()) {
                if (file.canExecute()) return "read_execute";
                return "read";
            } else if (file.canWrite()) {
                if (file.canExecute()) return "write_execute";
                return "write";
            } else if (file.canExecute()) {
                return "execute";
            } else {
                return "none";
            }
        } 
        catch (JargonException e) {
            throw new RemoteDataException(e);
        } 
        catch (java.io.FileNotFoundException e) {
            throw new RemoteDataException(e);
        } 
        
    }
	
	public String resolveFileName(String name) 
	throws JargonException, IOException, RemoteDataException
	{
		String newName = name.trim();
		
		for (int i = 0; doesExist(newName); i++)
		{
			String basename = FilenameUtils.getBaseName(name);
			String extension = FilenameUtils.getExtension(name);
			newName = basename + "-" + i;
			if (ServiceUtils.isValid(extension))
			{
				newName = newName + "." + extension;
			}
		}

		return newName;
	}

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return permissionMirroringRequired;
	}
	
	/**
	 * Overrides the default behavior of requiring permission mirroring on irods 
	 * systems.
	 * 
	 * @param permissionMirroringRequired
	 */
	public void setPermissionMirroringRequired(boolean permissionMirroringRequired) {
		this.permissionMirroringRequired = permissionMirroringRequired;
	}

	@Override
	public String getUsername()
	{
		return username;
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
		IRODS other = (IRODS) obj;
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
		if (internalUsername == null)
		{
			if (other.internalUsername != null)
				return false;
		}
		else if (!internalUsername.equals(other.internalUsername))
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
		if (resource == null)
		{
			if (other.resource != null)
				return false;
		}
		else if (!resource.equals(other.resource))
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
		if (zone == null)
		{
			if (other.zone != null)
				return false;
		}
		else if (!zone.equals(other.zone))
			return false;
		return true;
	}

	@Override
	public String getHost()
	{
		return host;
	}
	
	
//	
//	AgaveJargonProperties inputStreamSessionProperties = new AgaveJargonProperties() {
//	    public void mergeProperties() {
//	        
//            
//            // primary tcp receive window size in KB
//            super.setPrimaryTcpReceiveWindowSize(primaryTcpReceiveWindowSize);
//            
//            // primary tcp send window size in KB
//            super.setPrimaryTcpSendWindowSize(primaryTcpSendWindowSize);
//            
//            // primary internal input stream buffer in KB
//            super.setInternalInputStreamBufferSize(MAX_BUFFER_SIZE);
//            
//            // primary internal input stream buffer in KB
//            super.setInternalOutputStreamBufferSize(MAX_BUFFER_SIZE);
//            
//            super.setInternalInputStreamBufferSize(MAX_BUFFER_SIZE);
//            
//            super.setInternalOutputStreamBufferSize(MAX_BUFFER_SIZE);
//            
//            
//            // max 4 thread transfer pool on directory copy
//            jargonProperties.setTransferThreadPoolMaxSimultaneousTransfers(4);
//    
//            // 15 second timeout
//            jargonProperties.setTransferThreadPoolTimeoutMillis(15000);
//	    }
//	}
//	
//	private JargonProperties outputStreamSessionProperties = new SettableJargonProperties() {
//        public SettableJargonProperties() {
//            // do use a thread pool for connections
//            jargonProperties.setUseTransferThreadsPool(true);
//            
//            // use parallel transfers whenever possible
//            jargonProperties.setUseParallelTransfer(true);
//            
//            jargonProperties.setMaxParallelThreads(0);
//            
//            jargonProperties.setParallelTcpReceiveWindowSize(parallelTcpReceiveWindowSize);
//            
//            jargonProperties.setUseParallelTransfer(true);
//            
//            // max 4 thread transfer pool on directory copy
//            jargonProperties.setTransferThreadPoolMaxSimultaneousTransfers(4);
//    
//            // 15 second timeout
//            jargonProperties.setTransferThreadPoolTimeoutMillis(15000);
//        }
//    };
//	
//	private JargonProperties getOperationSessionProperties = new SettableJargonProperties() {
//        public SettableJargonProperties() {
//            // do use a thread pool for connections
//            jargonProperties.setUseTransferThreadsPool(true);
//            
//            // use parallel transfers whenever possible
//            jargonProperties.setUseParallelTransfer(true);
//            
//            jargonProperties.setMaxParallelThreads(0);
//            
//            jargonProperties.setParallelTcpReceiveWindowSize(parallelTcpReceiveWindowSize);
//            
//            jargonProperties.setUseParallelTransfer(true);
//            
//            // max 4 thread transfer pool on directory copy
//            jargonProperties.setTransferThreadPoolMaxSimultaneousTransfers(4);
//    
//            // 15 second timeout
//            jargonProperties.setTransferThreadPoolTimeoutMillis(15000);
//        }
//    };
//    
//    private JargonProperties putOperationSessionProperties = new SettableJargonProperties() {
//        public SettableJargonProperties() {
//            // do use a thread pool for connections
//            jargonProperties.setUseTransferThreadsPool(true);
//            
//            // use parallel transfers whenever possible
//            jargonProperties.setUseParallelTransfer(true);
//            
//            jargonProperties.setMaxParallelThreads(0);
//            
//            jargonProperties.setParallelTcpReceiveWindowSize(parallelTcpReceiveWindowSize);
//            
//            jargonProperties.setUseParallelTransfer(true);
//            
//            // max 4 thread transfer pool on directory copy
//            jargonProperties.setTransferThreadPoolMaxSimultaneousTransfers(4);
//    
//            // 15 second timeout
//            jargonProperties.setTransferThreadPoolTimeoutMillis(15000);
//        }
//    };

}
