/**
 * 
 */
package org.iplantc.service.transfer.ftp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.globus.ftp.Buffer;
import org.globus.ftp.DataSink;
import org.globus.ftp.DataSource;
import org.globus.ftp.FTPClient;
import org.globus.ftp.FeatureList;
import org.globus.ftp.FileRandomIO;
import org.globus.ftp.HostPort;
import org.globus.ftp.HostPort6;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.Session;
import org.globus.ftp.exception.ClientException;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.exception.FTPReplyParseException;
import org.globus.ftp.exception.ServerException;
import org.globus.ftp.exception.UnexpectedReplyCodeException;
import org.globus.ftp.vanilla.Command;
import org.globus.ftp.vanilla.FTPControlChannel;
import org.globus.ftp.vanilla.FTPServerFacade;
import org.globus.ftp.vanilla.Reply;
import org.globus.net.ServerSocketFactory;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.exceptions.RemoteConnectionException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
public class FTP extends FTPClient implements RemoteDataClient 
{
	private static Logger log = Logger.getLogger(FTPClient.class);

	public static final String ANONYMOUS_USER = "anonymous";
	public static final String ANONYMOUS_PASSWORD = "guest";
	
//	protected RemoteSystem system;
	
	protected String host;
	protected int port;
	protected String username;
	protected String password;
	protected String homeDir;
	protected String rootDir;
	protected String systemType;
    protected static final int MAX_BUFFER_SIZE = 1048576;
    protected boolean bPassive = true;
    private Map<String, RemoteFileInfo> fileInfoCache = new ConcurrentHashMap<String, RemoteFileInfo>();

    private boolean disconnected = true;
    
	public FTP(String host, int port, String username, String password, String rootDir, String homeDir)
	{
//		this.system = system;
		this.host = host;
		this.port = port > 0 ? port : 21;
		this.username = username;
		this.password = password;
		
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
	 * @see org.iplantc.service.transfer.RemoteDataClient#getHost()
	 */
	@Override
	public String getHost() {
		return this.host;
	}	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getPort()
	 */
	@Override
	public int getPort() {
		return this.port;
	}	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#updateSystemRoots(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateSystemRoots(String rootDir, String homeDir)
	{
		rootDir = FilenameUtils.normalize(rootDir);
		rootDir = StringUtils.stripEnd(rootDir, " ");
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
	
	public void setDTP(boolean bPassive) throws IOException, ServerException, ClientException {
		if (bPassive) {
			setPassive();
			setLocalActive();
		} else {
			setLocalPassive();
			setActive();
		}
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
			path = FileUtils.normalize(adjustedPath);
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
		
		return StringUtils.stripEnd(path, " ");
	}

	@Override
	public boolean mkdirs(String dir) throws IOException, RemoteDataException
	{
	    try {
    		String parent = StringUtils.isEmpty(dir) ? ".." : dir + "/..";
    		String resolvedParentPath = resolvePath(parent);
            if (!doesExist(parent) 
                    && StringUtils.startsWith(resolvedParentPath, rootDir) 
                    && !StringUtils.equals(resolvedParentPath, rootDir)) 
            {
                mkdirs(parent);
            }
    
            return mkdir(dir); 
	    } catch (NullPointerException e) {
	        throw new RemoteDataException(e.getMessage(), e);
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
	public boolean mkdir(String dir) throws IOException, RemoteDataException
	{
	    fileInfoCache.remove(resolvePath(dir));
        
	    if (doesExist(dir)) {
			return false;
		} else {
			try {
				super.makeDir(resolvePath(dir));
				return true;
			} catch (ServerException e) {
				if (e.toString().contains("Permission denied")) {
					throw new RemoteDataException("Permission denied", e);
				} else {
					throw new RemoteDataException("Failed to create " + dir, e);
				}
			} catch (Exception e) {
				return false;
			}
		}
	}

    @Override
    public int getMaxBufferSize() {
        return MAX_BUFFER_SIZE;
    }

	@Override
	public void authenticate() throws RemoteDataException, IOException
	{
	    if (!disconnected && session != null && session.authorized) return;
	    
		try {
			session = new Session();
			// force timeout at 30 seconds rather than 5 minutes.
            session.maxWait = 30000;
            
	        controlChannel = new FTPControlChannel(host, port);
	        controlChannel.open();
//	        localServer = new FTPServerFacade(controlChannel);
	        localServer = new FTPServerFacade(controlChannel) {
	        	/**
	        	 * Start the local server
	        	 * @param port required server port; can be set to ANY_PORT
	        	 * @param queue max size of queue of awaiting new connection requests 
	        	 * @return the server address
	        	 **/
	        	@Override
	        	public HostPort setPassive(int port, int queue)
	             throws IOException{

	             if (serverSocket == null) {
	                 ServerSocketFactory factory =
	                     ServerSocketFactory.getDefault();
	                 serverSocket = factory.createServerSocket(port, queue);
	             }

	             session.serverMode = Session.SERVER_PASSIVE;

	             String address = org.iplantc.service.common.Settings.getIpLocalAddress();
	             int localPort = serverSocket.getLocalPort();

	             if (remoteControlChannel.isIPv6()) {
	                 String version = HostPort6.getIPAddressVersion(address);
	                 session.serverAddress =
	                     new HostPort6(version, address, localPort);
	             } else {
	                 session.serverAddress =
	                     new HostPort(address, localPort);
	             }

	             log.debug("started passive server at port " +
	                          session.serverAddress.getPort());
	             return session.serverAddress;

	         }
	        };
	        localServer.authorize();
	        
			authorize(username, password);
			
			try {
				// check whether passive mode will work
				setDTP(true);
				bPassive = true;
			} catch (ClientException | ServerException e) {
				// passive mode won't work. try active.
				setDTP(false);
				bPassive = false;
			}
			
			disconnected = false;
		} 
		catch (ClientException e) {
		    disconnected = true;
			throw new RemoteDataException("Failed to connect to remote system. Neither active nor passive modes are accepted", e);
		}
		catch (IOException e) { 
		    disconnected = true;
			throw new RemoteDataException("Failed to connect to remote system.", e);
		}
		catch (ServerException e) {
		    disconnected = true;
			throw new RemoteDataException("Failed to authenticate to remote system.", e);
		}
	}
	
	
	/**
	 * Fetches the system type from the server to determine proper 
	 * flags to throw.
	 * 
	 * @return
	 * @throws IOException
	 * @throws ServerException
	 */
	public String getFTPSystemType() throws IOException, ServerException {
		if (this.systemType != null) {
            return this.systemType;
        }

        Reply systReply = null;
        try {
            systReply = controlChannel.execute(new Command("SYST"));

            if (systReply.getCode() != 215) {
                throw ServerException.embedUnexpectedReplyCodeException(
                                  new UnexpectedReplyCodeException(systReply),
                                  "Server refused returning system type");
            }
        } catch (FTPReplyParseException rpe) {
            throw ServerException.embedFTPReplyParseException(rpe);
        } catch (UnexpectedReplyCodeException urce) {
            throw ServerException.embedUnexpectedReplyCodeException(
                                urce,
                                "Server refused returning system type");
        }

        this.systemType = systReply.getMessage();

        return systemType;
    }
	
	public void checkContentTypeUTF8() throws IOException, ServerException {
		if (isFeatureSupported("UTF8")) {
            
			if (log.isDebugEnabled()) {
            	log.debug("Setting UTF8 content type");
            }
            
	        Reply systReply = null;
	        try {
	            systReply = controlChannel.execute(new Command("OPTS", "UTF8 ON"));
	
	            if (systReply.getCode() != 200) {
	                throw ServerException.embedUnexpectedReplyCodeException(
	                                  new UnexpectedReplyCodeException(systReply),
	                                  "Server refused setting UTF8");
	            }
	        } catch (FTPReplyParseException rpe) {
	            throw ServerException.embedFTPReplyParseException(rpe);
	        } catch (UnexpectedReplyCodeException urce) {
	            throw ServerException.embedUnexpectedReplyCodeException(
	                                urce,
	                                "Server refused setting UTF8");
	        }
		}
	}
	
	/**
	 * Checks whether the server type is a flavor of windows. If so, 
	 * we should switch to using alternate flags on our listings.
	 * @return
	 * @throws IOException
	 * @throws ServerException
	 */
	public boolean isWindowsSystem() throws IOException, ServerException {
		return StringUtils.containsIgnoreCase(getFTPSystemType(), "windows");
	}
	

	@Override
	public RemoteInputStream<FTP> getInputStream(String path, boolean passive) throws IOException,
			RemoteDataException
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
				return new FTPInputStream(this, resolvePath(path), false);
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Throwable e)
		{
			if (e.toString().contains("Permission denied")) {
				throw new RemoteDataException("Permission denied");
			} else {
				throw new RemoteDataException("Failed to create input stream from '" + path + "' on remote system.", e);
			}
		}
	}

	@Override
	public FTPOutputStream getOutputStream(String path, boolean passive, boolean append) 
	throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(path)) {
			throw new RemoteDataException("No output path specified.");
		} 
		else 
		{
			if (doesExist(path)) 
			{
				if (isDirectory(path)) {
					throw new RemoteDataException("Cannot open output stream to directory " + path);
//				} else if ((StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.userCanRead()) ||
//						(!StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.groupCanWrite() && !remoteFileInfo.allCanWrite())) {
//					throw new RemoteDataException("Permission denied");
				} else {
					return new FTPOutputStream(this, resolvePath(path), passive, append);
				}
			} 
			else 
			{ 
				String parentPath = (StringUtils.isEmpty(path) ? ".." : FilenameUtils.getPath(path));
				if (doesExist(parentPath))
				{
//					RemoteFileInfo remoteFileInfo = getFileInfo(parentPath);
//					if ((StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.userCanRead()) ||
//							(!StringUtils.equals(remoteFileInfo.getOwner(), this.username) && !remoteFileInfo.groupCanWrite() && !remoteFileInfo.allCanWrite())) 
//					{
//						throw new RemoteDataException("Permission denied");
//					} else {
						return new FTPOutputStream(this, resolvePath(path), passive, append);
//					}
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
//			RemoteFileInfo remoteFileInfo = getFileInfo(remotedir);
			
			File localDir = new File(localdir);
			
			if (isDirectory(remotedir))
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
					localDir = new File(localDir, FilenameUtils.getName(remotedir));
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
		try {
			DataSink sink = null;
			sink = new FileRandomIO(new RandomAccessFile(localdir, "rw"));
			
			localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
			setType(Session.TYPE_IMAGE);
			setMode(Session.MODE_STREAM);
			
			setDTP(false);
			
			get(resolvePath(remotedir), sink, listener);
			
			setDTP(bPassive);
		} 
		catch (IOException e) {
			throw e;
		} 
		catch (ServerException e) {
			if (e.getCause().toString().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Permission denied", e);
			} else {
				throw new RemoteDataException("Failed to get '" + remotedir + "' from remote system.", e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to get '" + remotedir + "' from remote system.", e);
		}
	}
	
	@Override
	public boolean isDirectory(String path) 
	throws IOException, RemoteDataException
	{
		try {
			RemoteFileInfo fileInfo = getFileInfo(path);
			return fileInfo.isDirectory();
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
                putFile(localFile, remotepath, listener, false);
            }
            else if (localFile.isDirectory()) {
                throw new RemoteDataException("cannot append directory");
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
				putFile(localFile, remotedir, listener, false);
			}
		} 
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy data to " + remotedir, e);
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
			throw new FileNotFoundException("No such remote file or directory");
		}
		
		DataSource source = new FileRandomIO(new RandomAccessFile(localFile, "rw"));
		
		localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
		setType(Session.TYPE_IMAGE);
		setMode(Session.MODE_STREAM);
		
		setDTP(false);
//		setPassive();
//		setLocalActive();
		
		try 
		{
		    // bust cache since this file has now changed
            fileInfoCache.remove(resolvedPath);
            put(resolvedPath, source, listener, append);
		} catch (ServerException e) {
			if (e.getCause().toString().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Permission denied", e);
			} else {
				throw e;
			}
		}
		
		setDTP(bPassive);
//		setPassive();
//		setLocalActive();
		
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
			dest += (StringUtils.isEmpty(dest) ? "" : "/") + localFile.getName();
			mkdir(dest);
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
				    fileInfoCache.remove(resolvePath(remotedir));
                    putDir(fileItem, dest, listener);
				} else {
					putFile(fileItem, dest, listener, false);
				}
			}
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
		FTP ftp = null;
		try 
		{
			ftp = new FTP(this.host, this.port, this.username, this.password, this.rootDir, this.homeDir.substring(this.rootDir.length()));
			
			ftp.authenticate();
			ftp.localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
			ftp.setType(Session.TYPE_IMAGE);
			ftp.setMode(Session.MODE_STREAM);
			
			copy(remotesrc, remotedest, ftp, listener);
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
			try {ftp.disconnect();} catch (Exception e){} 
		}
	}

	private void copy(String remotesrc, String remotedest, FTP remoteDestClient, RemoteTransferListener listener) 
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
				
//				localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
//				setType(Session.TYPE_IMAGE);
//				setMode(Session.MODE_STREAM);
//				
//				setActive(remoteDestClient.setPassive());
//				
//				transfer(resolvePath(remotesrc), remoteDestClient, newDestPath, false, listener);
				File localTempDir = null;
				try {
					String src = (listener == null ? getUriForPath(remotesrc).toString() : listener.getTransferTask().getSource());
					String dest = (listener == null ? getUriForPath(remotedest).toString() : listener.getTransferTask().getDest());
					localTempDir = Files.createTempDir();
					String localPath = localTempDir.getAbsolutePath() + File.separator + FilenameUtils.getName(remotesrc);
					
					log.debug("Remote copy not supported by FTP. Beginning proxy download of " + 
							src + " to " + localTempDir.getAbsolutePath());
					
					get(remotesrc, localPath, listener);
					
					log.debug("Completed proxy download of " + src + 
							" to " + localPath);
					
					log.debug("Beginning proxy upload of " + localPath + " to " +  dest);
					
					put(localPath, remotedest, listener);
					
					log.debug("Completed proxy upload of " + localPath + " to " + 
							dest + ". Copy operation complete.");
				} 
				finally {
					try {
						if (localTempDir != null && localTempDir.exists()) {
							org.apache.commons.io.FileUtils.deleteDirectory(localTempDir);
						}
					} catch (Exception e) {
						log.error("Failed to delete local FTP copy cache directory " + localTempDir.getAbsolutePath(), e);
					}
				}
				
			}
			catch (Exception e)
			{
				throw new RemoteDataException("Failed to copy " + remotesrc + " to " + remotedest, e);
			}
		}
	}

	@Override
	public URI getUriForPath(String path) throws IOException,
			RemoteDataException
	{
		try {
			return new URI("ftp://" + 
					host + 
					(port == 21 ? "" : ":" + port) +
					"/" + path);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	// basic compatibility API
	@Override 
	public void delete(String dir) throws IOException, RemoteDataException
	{
		
        if (dir == null) {
            throw new IllegalArgumentException("Required argument missing");
        }
        try 
        {
            String resolvedPath = resolvePath(dir);
            
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
//        			log.error("Failed recursive server-side delete. Manually applying now.");
        			deleteManually(dir);
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
//        	log.error("Failed recursive server-side delete. Manually applying now.", e);
        	deleteManually(dir);
        }
    }
	
	public void deleteManually(String remotepath) throws IOException, RemoteDataException
	{
		try 
		{	
			setType(Session.TYPE_ASCII);
			setDTP(false);
//			setLocalPassive();
//			setActive();
			
			String resolvedPath = resolvePath(remotepath);
            
            if (isDirectory(remotepath)) 
			{
                List<RemoteFileInfo> files;
                
                try {
                    files = ls(remotepath);
                } 
                catch (RemoteConnectionException e) {
                    
                    // retry in event connection is closed
                    try {
                        disconnect();
                        authenticate();
                    } catch (Exception e1) {
                        throw new RemoteDataException("Connection closed by server. Unable to re-establish a connection", e);
                    }
                    
                    files = ls(remotepath);
                } 
                
                for (RemoteFileInfo file: files) {
                	if (!file.getName().equals(".") && !file.getName().equals("..")) {
                		deleteManually((StringUtils.isEmpty(remotepath) ? "" : remotepath + "/") + file.getName());
        			}
                }
                fileInfoCache.remove(resolvedPath);
                super.deleteDir(resolvedPath);
			} else {
			    fileInfoCache.remove(resolvedPath);
	            super.deleteFile(resolvedPath);
			}
		} catch (IOException | RemoteDataException e) {
			throw e;
		} catch (Exception e) {
			throw new RemoteDataException("Failed to connect to remote system.", e);
		} 
	}

	@Override
	public boolean isThirdPartyTransferSupported()
	{
		return false;
	}

	@Override
	public boolean doesExist(String path) throws IOException,
			RemoteDataException
	{
		try {
			if (StringUtils.equalsIgnoreCase(username, ANONYMOUS_USER)) {
				String resolvedPath = resolvePath(path);
				Reply reply =
		                controlChannel.exchange(new Command("CWD", resolvedPath));
				return Reply.isPositiveCompletion(reply) || reply.getMessage().contains("not a plain file");
			} else {
				return super.exists(resolvePath(path));
			}
		} catch (FTPReplyParseException rpe) {
            throw new RemoteDataException("Failed to check existence of file/folder", 
            		ServerException.embedFTPReplyParseException(rpe));
		} catch (ServerException e) {
			return false;
		} catch (FileNotFoundException e) {
		    throw new RemoteDataException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteDataException("Failed to connect to remote system.", e);
		}
		finally {
			try {changeDir(resolvePath(""));} catch (Exception e) {}
		}
	}
	
	@Override
	public List<RemoteFileInfo> ls(String path) 
	throws RemoteDataException, IOException
	{
		String resolvedPath = resolvePath(path);
		if (isFeatureSupported("MLSD")) {
			try {
			    return doLs(resolvedPath);
			} catch (RemoteDataException e) {
			    return doLegacyLs(resolvedPath);
			}
		} else {
			return doLegacyLs(resolvedPath);
		}
	}
	
	/**
	 * Uses legacy list command with file filter to get a directory listing
	 * 
	 * @param resolvedPath absolute path to file or folder on the system
	 * @return
	 * @throws RemoteDataException
	 * @throws IOException
	 */
	private List<RemoteFileInfo> doLegacyLs(String resolvedPath) 
	throws RemoteDataException, IOException
	{
		List<RemoteFileInfo> listing = new ArrayList<RemoteFileInfo>();
		BufferedReader reader = null;
		
		try 
		{
			changeDir(resolvedPath);
			
			ByteArrayDataSink sink = new ByteArrayDataSink();
	
			if (isWindowsSystem()) {
				list("*", "-a", sink);
			} else {
				list("*", "-d", sink);
			}
	        
	        ByteArrayOutputStream received = sink.getData();
	
	        // transfer done. Data is in received stream.
	        // convert it to a vector.
	
	        reader = new BufferedReader(new StringReader(received.toString()));
        
	        String line = null;
	
	        while ((line = reader.readLine()) != null) {
	            line = line.trim();
	            if(line.equals(""))
	            {
	                continue;
	            }
	            if (line.startsWith("total"))
	                continue;
	            RemoteFileInfo childInfo = new RemoteFileInfo(line);
				if (!StringUtils.equals(childInfo.getName(), ".") && 
						!StringUtils.equals(childInfo.getName(), FilenameUtils.getName(StringUtils.removeEnd(resolvedPath, "/")))) 
				{
				    // add to cache since this file may have changed
		            fileInfoCache.put(resolvedPath + "/" + childInfo.getName(), childInfo);
		            listing.add(childInfo);
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
		    if (e.getMessage().contains("425 No data connection")) {
                throw new RemoteConnectionException("Connection closed by server during listing. Resetting transfer.", e);
            }
            else {
                throw new RemoteDataException("Failed to obtain directory listing from remote system.", e);
            }
		} 
		finally {
			try {changeDir(resolvePath(""));} catch (Exception e) {}
			try {reader.close();} catch (Exception e) {}
		}
        
        return listing;
	}

	/**
	 * Performs a directory listing using the mlsd command.
	 * This is the default approach when the feature is supported.
	 * 
	 * @param resolvedPath absolute path to file or folder on the system
	 * @return
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	private List<RemoteFileInfo> doLs(String resolvedPath) 
	throws IOException, RemoteDataException
	{	
		final List<RemoteFileInfo> v = new ArrayList<RemoteFileInfo>();
		ByteArrayDataSink iSink = new ByteArrayDataSink();
		BufferedReader reader = null;
		// try adding mlsd support here. Will give info on files not staged from tape yet
		 
		try
		{
			// the ls command will not always throw a proper 404 exception, 
			// so we do a check her to catch those exceptions before making
			// the actual ls call.
			if (!super.exists(resolvedPath)) {
				throw new FileNotFoundException("No such file or directory");
			}
			
			localServer.setProtectionBufferSize(Session.SERVER_DEFAULT);
			//Transfer type must be ASCII
			setType(Session.TYPE_ASCII);
			setMode(Session.MODE_STREAM);
			try 
			{
				setDTP(false);
//				setPassive();
//				setLocalActive();
//				setProtectionBufferSize(16384)
				super.mlsd(resolvedPath, iSink);
			} 
			catch (ServerException e) 
			{
			    if (e.getMessage().contains("425 No data connection")) {
			        throw new RemoteConnectionException("Connection closed by server during listing. Resetting transfer.", e);
                }
			    else if (e.getMessage().contains("451 active connection failed")) {
					// try to reverse the mode and see if that helps
					setDTP(true);
//					setLocalPassive();
//					setActive();
					super.mlsd(resolvedPath, iSink);
				} 
				else if (e.getMessage().toLowerCase().contains("illegal port command")) {
					// try to reverse the mode and see if that helps
					this.bPassive = true;
					setDTP(true);
//					setLocalPassive();
//					setActive();
					return doLegacyLs(resolvedPath);
				} 
				else if (e.toString().contains("No such file or directory")) {
					throw new FileNotFoundException("No such file or directory");
				} else if (e.toString().contains("Permission denied")) {
					throw new RemoteDataException("Permission denied", e);
				} else {
				    throw new RemoteDataException("Failed to list path " + resolvedPath.replace(homeDir,  ""), e);
				}
				
			}
			
			reader = new BufferedReader(new StringReader(iSink.getData().toString()));

	        MlsxEntry entry = null;
	        String line = null;
	        long entriesReturned = 0;
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
	                RemoteFileInfo childInfo = new RemoteFileInfo(entry);
	                
	                fileInfoCache.put(resolvedPath + "/" + childInfo.getName(), childInfo);
                    
	                v.add(childInfo);
	            }
	            entriesReturned++;
	        }
	        
	        if (entriesReturned == 0) {
	        	throw new RemoteDataException("Permission denied"); 
	        }
			
			return v;
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to obtain directory listing from remote system.", e);
		} 
		finally {
			try { reader.close(); } catch (Exception e) {}
		}
		
	}
	
	private class ByteArrayDataSink implements DataSink {

        private ByteArrayOutputStream received;

        public ByteArrayDataSink() {
            this.received = new ByteArrayOutputStream(1000);
        }
        
        public void write(Buffer buffer) throws IOException {
            if (log.isDebugEnabled()) {
                log.debug(
                             "received "
                             + buffer.getLength()
                             + " bytes of directory listing");
            }
            this.received.write(buffer.getBuffer(), 0, buffer.getLength());
        }

        public void close() throws IOException {
        }
        
        public ByteArrayOutputStream getData() {
            return this.received;
        }
    }

	@Override
	public void disconnect()
	{
		try { this.abort(); } catch (Throwable t) {}
			
		try { super.close(true); } catch (Throwable e) {}

		this.disconnected  = true;
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
		return null;
	}

    @Override
    public boolean hasReadPermission(String path, String username) throws RemoteDataException {
        // If the file is located under the root directory and exists on the server, return true
        try {
            path = resolvePath(path);

            // check file exists
            if (!doesExist(path)) return false;

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
    public boolean hasWritePermission(String path, String username) throws RemoteDataException {
        // If the file is located under the root directory, return true
        try {
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
	public boolean isFeatureSupported(String feature) {
		try {
			return super.isFeatureSupported(feature);
		} catch (Exception e) {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteDataClient#getFileInfo(java.lang.String)
	 */
	@Override
	public RemoteFileInfo getFileInfo(String path) throws RemoteDataException, IOException
	{
		String resolvedPath = resolvePath(path);
		RemoteFileInfo fileInfo = fileInfoCache.get(resolvedPath);
		// check the cache so we can save a query when possible
		if (fileInfo == null) 
		{
			try {
			    if (isFeatureSupported("MLST")) {
    				fileInfo = doGetFileInfo(resolvedPath);
    			} else {
    				fileInfo = doGetLegacyFileInfo(resolvedPath);
    			}
    			fileInfoCache.put(resolvedPath, fileInfo);
			} 
			catch (RemoteDataException | IOException e) {
			    fileInfoCache.remove(resolvedPath);
			    throw e;
			}
		}
		
		return fileInfo;
	}
	
	/**
	 * Uses legacy list command with file filter to get the file info
	 * 
	 * @param resolvedPath absolute path to file or folder on the system
	 * @return
	 * @throws RemoteDataException
	 * @throws IOException
	 */
	private RemoteFileInfo doGetLegacyFileInfo(String resolvedPath) 
	throws RemoteDataException, IOException
	{
		RemoteFileInfo fileInfo = null;
		BufferedReader reader = null;
		try
		{
			// check the cache so we can save a query when possible
			if (fileInfoCache.containsKey(resolvedPath)) {
				return fileInfoCache.get(resolvedPath);
			}
			
			setType(Session.TYPE_ASCII);
			
			setDTP(true);
//			setLocalPassive();
//			setActive();
//			setActive(localServer.setPassive());
			
			changeDir(resolvedPath);
			
			ByteArrayDataSink sink = new ByteArrayDataSink();
			
			if (isWindowsSystem()) {
				list("*", "-a", sink);
			} else {
				list("*", "-d", sink);
			}
	        
	        ByteArrayOutputStream received = sink.getData();
	
	        // transfer done. Data is in received stream.
	        // convert it to a vector.
	
	        reader = new BufferedReader(new StringReader(received.toString()));
        
	        String line = null;
	
	        while ((line = reader.readLine()) != null) {
	            line = line.trim();
	            if(line.equals(""))
	            {
	                continue;
	            }
	            if (line.startsWith("total"))
	                continue;
	            RemoteFileInfo childInfo = new RemoteFileInfo(line);
				if (!StringUtils.equals(childInfo.getName(), ".") && 
						!StringUtils.equals(childInfo.getName(), FilenameUtils.getName(StringUtils.removeEnd(resolvedPath, "/")))) 
				{
				    // add to cache since this file may have changed
		            fileInfoCache.put(resolvedPath + "/" + childInfo.getName(), childInfo);
		            return childInfo;
				}
	        }
	        
	        
	        
				
//			ByteArrayDataSink sink = new ByteArrayDataSink();
//
//	        List<RemoteFileInfo> listing = doLegacyLs(resolvedPath);
//	        
//
//	        ByteArrayOutputStream received = sink.getData();
//
//	        // transfer done. Data is in received stream.
//	        // convert it to a vector.
//
//	        reader = new BufferedReader(new StringReader(received.toString()));
//
//	        String line = null;
//
//	        while ((line = reader.readLine()) != null) {
//	            line = line.trim();
//	            if(line.equals(""))
//	            {
//	                continue;
//	            }
//	            if (line.startsWith("total"))
//	                continue;
//	            RemoteFileInfo childInfo = new RemoteFileInfo(line);
//				if (StringUtils.equals(childInfo.getName(), ".") || 
//						StringUtils.equals(childInfo.getName(), FilenameUtils.getName(StringUtils.removeEnd(resolvedPath, "/")))) 
//				{
//					fileInfo = childInfo;
//				}
//	        }
	        changeDir(resolvePath(""));
			
			return fileInfo;
		}
		catch (ServerException e) 
		{
			if (e.toString().contains("check for file existence") ||
					e.toString().contains("No such file or directory")) 
			{
				throw new FileNotFoundException("No such file or directory");
			} else if (e.toString().contains("Permission denied")) {
				throw new RemoteDataException("Permission denied", e);
			} else {
				throw new RemoteDataException("Failed to retrieve file info for " + fileInfoCache.containsKey(resolvedPath), e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve file info for " + fileInfoCache.containsKey(resolvedPath), e);
		}
		finally {
			try { reader.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Uses mlst to get the file info
	 * 
	 * @param resolvedPath absolute path to file or folder on the system
	 * @return
	 * @throws RemoteDataException
	 * @throws IOException
	 */
	private RemoteFileInfo doGetFileInfo(String resolvedPath) throws RemoteDataException, IOException
	{	
		RemoteFileInfo fileInfo = null;
		try
		{
			setType(Session.TYPE_ASCII);
			
			setDTP(true);
//			setLocalPassive();
//			setActive();
//			setActive(localServer.setPassive());
			
			MlsxEntry entry = super.mlst(resolvedPath);
			
			fileInfo = new RemoteFileInfo(entry);
			fileInfoCache.put(resolvedPath, fileInfo);
			
			return fileInfo;
		}
		catch (ServerException e) {
			if (e.getCustomMessage().contains("Could not create MlsxEntry")) {
				try {
					disconnect();
					authenticate();
			        MlsxEntry entry = super.mlst(resolvedPath);
					return new RemoteFileInfo(entry);
				} catch (Exception e1) {
					throw new RemoteDataException("Failed to retrieve file info for " + resolvedPath.replace(homeDir,  ""), e);
				}
			}
			else if (e.toString().contains("check for file existence")) {
			    return doStat(resolvedPath);
			}
			else if (e.toString().contains("No such file or directory"))  
			{
//			    return doGetLegacyFileInfo(resolvedPath);
				throw new java.io.FileNotFoundException("No such file or directory");
			} else if (e.toString().contains("Permission denied")) {
				throw new RemoteDataException("Permission denied", e);
			} else {
				throw new RemoteDataException("Failed to retrieve file info for " + resolvedPath.replace(homeDir,  ""), e);
			}
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve file info for " + resolvedPath.replace(homeDir,  ""), e);
		}
	}
	
	/**
	 * Performs a stat on an absolute remote file path
	 * @param resolvedPath
	 * @return
	 * @throws FileNotFoundException 
	 * @throws RemoteDataException 
	 */
	private RemoteFileInfo doStat(String resolvedPath) throws FileNotFoundException, RemoteDataException {
	    List<RemoteFileInfo> listing = new ArrayList<RemoteFileInfo>();
	    try 
	    {
	        Reply reply = quote("STAT " + resolvedPath);
            String replyMessage = reply.getMessage();
            StringTokenizer replyLines =
                new StringTokenizer(
                                    replyMessage,
                                    System.getProperty("line.separator"));
            if (replyLines.hasMoreElements()) {
                replyLines.nextElement();
            } else {
                throw new RemoteDataException("Expected multiline reply");
            }
           
            String fileName = FilenameUtils.getName(resolvedPath);
            while (replyLines.hasMoreElements()) {
                String line = (String) replyLines.nextElement();
                // skip last line
                if (!replyLines.hasMoreElements()) break;
                
//                if (StringUtils.startsWithAny(line, new String[]{"STAT", "213 End."})) continue;
                
                RemoteFileInfo fileItem = new RemoteFileInfo(line);
                
                if (StringUtils.equals(fileItem.getName(), fileName)) {
                    return fileItem;
                }
                
                
//                if (StringUtils.endsWith(line, fileName)) {
//                    
//                }
            }
            
            if (Reply.isPositiveCompletion(reply) && !StringUtils.equals(reply.getMessage(), "STAT" + System.getProperty("line.separator") + "213 End.")) {
                RemoteFileInfo fileItem = new RemoteFileInfo();
                if (reply.getCode() == 212) {
                    fileItem.setFileType(RemoteFileInfo.FILE_TYPE);
                    fileItem.setName(fileName);
                    fileItem.setSize(super.size(resolvedPath));
                } else {
                    fileItem.setFileType(RemoteFileInfo.DIRECTORY_TYPE);
                    fileItem.setName(fileName);
                    fileItem.setSize(4096);
                }
                return fileItem;
            } 
            else 
            {
                throw new FileNotFoundException("Unknown file/folder.");
            }
	    }
	    catch (FileNotFoundException e) {
	        throw e;
	    }
	    catch (Throwable e) {
            throw new RemoteDataException("Unable to get remote file info.", e);
        }
        
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

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return false;
	}

	@Override
	public String getUsername()
	{
		return username;
	}

	@Override
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
					putFile(localFile, remotedir, listener, false);
				}
				else 
				{
					RemoteFileInfo fileInfo = getFileInfo(remotedir);
					
					// if the types mismatch, delete remote, use current
					if (localFile.isDirectory() && !fileInfo.isDirectory() || 
							localFile.isFile() && !fileInfo.isFile()) 
					{
						delete(remotedir);
						putFile(localFile, remotedir, listener, false);
					} 
					// or if the file sizes are different
					else if (localFile.length() != fileInfo.getSize())
					{
						putFile(localFile, remotedir, listener, false);
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
			RemoteDataException, NotImplementedException
	{
		try
		{
			RemoteFileInfo remoteFileInfo = getFileInfo(remotePath); 
			if (remoteFileInfo.isDirectory()) {
				throw new RemoteDataException("Cannot perform checksum on a directory");
			} else {
				throw new NotImplementedException("Checksum is not currently supported.");
			}
//					return checksum(ChecksumAlgorithm.MD5, 0, length(remotePath),
//							resolvePath(remotePath));
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((homeDir == null) ? 0 : homeDir.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + port;
		result = prime * result + ((rootDir == null) ? 0 : rootDir.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		FTP other = (FTP) obj;
		if (homeDir == null)
		{
			if (other.homeDir != null) return false;
		}
		else if (!homeDir.equals(other.homeDir)) return false;
		if (host == null)
		{
			if (other.host != null) return false;
		}
		else if (!host.equals(other.host)) return false;
		if (password == null)
		{
			if (other.password != null) return false;
		}
		else if (!password.equals(other.password)) return false;
		if (port != other.port) return false;
		if (rootDir == null)
		{
			if (other.rootDir != null) return false;
		}
		else if (!rootDir.equals(other.rootDir)) return false;
		if (username == null)
		{
			if (other.username != null) return false;
		}
		else if (!username.equals(other.username)) return false;
		return true;
	}
	
//	@Override
//	public boolean equals(Object o)
//	{
//		if (o != null && o instanceof FTP) {
//			FTP rdc = (FTP)o;
//			return (StringUtils.equalsIgnoreCase(host, rdc.host) && 
//				port == rdc.port && 
//				StringUtils.equalsIgnoreCase(username, rdc.username) &&
//				StringUtils.equalsIgnoreCase(password, rdc.password) &&
//				StringUtils.equalsIgnoreCase(homeDir, rdc.homeDir) &&
//				StringUtils.equalsIgnoreCase(rootDir, rdc.rootDir));
//		} else {
//			return false;
//		}
//	}
	
}
