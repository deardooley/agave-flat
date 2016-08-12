/**
 * 
 */
package org.iplantc.service.transfer.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.transfer.util.MD5Checksum;

/**
 * @author dooley
 *
 */
public class Local implements RemoteDataClient
{
    private static final Logger log = Logger.getLogger(Local.class);
    
	private RemoteSystem system;

	protected String homeDir;
	protected String rootDir;
    protected static final int MAX_BUFFER_SIZE = 65537;

    public Local() {
        this(null, "/", "/");
    }
	public Local(RemoteSystem system, String rootDir, String homeDir) {
		this.system = system;
		
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
            path = org.codehaus.plexus.util.FileUtils.normalize(adjustedPath);
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

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#mkdir(java.lang.String)
	 */
	@Override
	public boolean mkdir(String dir) throws IOException, RemoteDataException
	{
		String resolvedPath = resolvePath(dir);
		File remotePath = new File(resolvedPath);
		try {
		    if (!remotePath.mkdir()) {
		        if (remotePath.getParent() == null || remotePath.getParentFile().exists()) {
		            return false;
        		} else if (!remotePath.canWrite()) {
        		    throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
        		} else {
        		    throw new FileNotFoundException("No such file or directory");
        		}
		    } else {
		        return true;
		    }
		} 
		catch (RemoteDataException e) {
		    throw e;
		}
		catch (SecurityException e) {
		    throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
		}
		catch (IOException e) {
		    throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#mkdirs(java.lang.String)
	 */
	@Override
	public boolean mkdirs(String dir) throws IOException, RemoteDataException
	{
	    String resolvedPath = resolvePath(dir);
        File remotePath = new File(resolvedPath);
        try {
            
            if (!remotePath.mkdirs()) {
                if (!remotePath.canWrite()) {
                    throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } 
        catch (RemoteDataException e) {
            throw e;
        }
        catch (SecurityException e) {
            throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
        }
        catch (Exception e) {
            throw e;
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

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#authenticate()
	 */
	@Override
	public void authenticate() throws IOException, RemoteDataException
	{
		return;
	}

    @Override
    public int getMaxBufferSize() {
        return MAX_BUFFER_SIZE;
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getInputStream(java.lang.String, boolean)
	 */
	@Override
	public RemoteInputStream<Local> getInputStream(String path, boolean passive)
			throws IOException, RemoteDataException
	{
		return new LocalInputStream(this, path);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getOutputStream(java.lang.String, boolean, boolean)
	 */
	@Override
	public RemoteOutputStream<Local> getOutputStream(String path, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		return new LocalOutputStream(this, path);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#ls(java.lang.String)
	 */
	@Override
	public List<RemoteFileInfo> ls(String path) throws IOException,
			RemoteDataException
	{
		List<RemoteFileInfo> fileList = new ArrayList<RemoteFileInfo>();
		
		File remotePath = new File(resolvePath(path));
        if (!remotePath.exists()) {
            throw new FileNotFoundException("No such file or directory");
        } 
        else if (!remotePath.canRead()) {
            throw new RemoteDataException("Permission denied");
        }
        else {
            for(File file: remotePath.listFiles())
            {
                fileList.add(new RemoteFileInfo(file));
            }
            return fileList;
        }
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#get(java.lang.String, java.lang.String)
	 */
	@Override
	public void get(String localdir, String remotedir) throws IOException,
			RemoteDataException
	{
		get(localdir, remotedir, null);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#get(java.lang.String, java.lang.String, org.iplantc.service.jobs.io.RemoteTransferListener)
	 */
	@Override
	public void get(String sourcedir, String destdir,
			RemoteTransferListener listener) throws IOException,
			RemoteDataException
	{
	    try 
	    {
	        String resolvedSrc = resolvePath(sourcedir);
            
	        File destPath = new File(destdir);
            File srcPath = new File(resolvedSrc);
            
    	    if (listener != null) {
                listener.started(srcPath.length(), srcPath.getAbsolutePath());
                listener.completed();
            }
    	    
    	    if (srcPath.exists() && !srcPath.canRead()) {
                throw new RemoteDataException("Cannot read from " + srcPath + ": Permisison denied"); 
            }
    	    
    	    if (srcPath.isFile()) {
                if (destPath.isFile()) {
                    FileUtils.copyFile(srcPath, destPath);
                } else {
                    if (destPath.exists()) 
                    {
                        if (!destPath.canWrite()) {
                            throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                        } 
                        FileUtils.copyFileToDirectory(srcPath, destPath);
                    } else {
                        // rename to parent directory
                        File parentPath = destPath.getParentFile();
                        
                        if (parentPath != null) {
                            if (!parentPath.exists()) {
                                throw new FileNotFoundException("No such file or directory");
                            } else if (!parentPath.canWrite()) {
                                throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                            } 
                        }
                        FileUtils.copyFile(srcPath, destPath);
                    }
    
                    
                }
            
            } else {
                if (destPath.isFile()) {
                    throw new RemoteDataException("Cannot copy to " + destPath + ". Destination is not a directory.");
                } else if (!destPath.exists()){
                    // rename to parent directory
                    File parentPath = destPath.getParentFile();
                    
                    if (parentPath != null) {
                        if (!parentPath.exists()) {
                            throw new FileNotFoundException("No such file or directory");
                        } else if (!parentPath.canWrite()) {
                            throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                        } 
                    }
                } else {
                    destPath = new File(destPath, srcPath.getName());
                }
                FileUtils.copyDirectory(srcPath, destPath);
            }
    	    
    	    if (listener != null) {
                listener.progressed(srcPath.length());
                listener.completed();
            }
	    } 
	    finally {
	        // nothing to do here
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
        try 
        {
            String resolvedDest = resolvePath(remotepath);
            File srcPath = new File(localpath);
            File destPath = new File(resolvedDest);
            
            if (!srcPath.exists()) {
                throw new FileNotFoundException("No such file or directory");
            } else if (!srcPath.canRead()) {
                throw new RemoteDataException("Cannot read " + srcPath + ": Permisison denied"); 
            }
            
            if (!doesExist(remotepath)) 
            {
                put(localpath, remotepath, listener);
            }
            else if (srcPath.isDirectory()) {
                throw new RemoteDataException("cannot append directory");
            }
            else {
                if (listener != null) {
                    listener.started(srcPath.length(), srcPath.getAbsolutePath());
                }
                
                if (destPath.isFile()) 
                {
                    OutputStream out = null;
                    InputStream in = null;
                    out = FileUtils.openOutputStream(destPath, true);
                    in = FileUtils.openInputStream(srcPath);
                    StreamUtils.copyThenClose(in, out);
                } 
                else if (destPath.exists()) 
                {
                    File actualFile = new File(destPath, srcPath.getName());
                    if (actualFile.exists()) {
                        OutputStream out = null;
                        InputStream in = null;
                        out = FileUtils.openOutputStream(actualFile, true);
                        in = FileUtils.openInputStream(srcPath);
                        StreamUtils.copyThenClose(in, out);
                    } else {
                        if (!destPath.canWrite()) {
                            throw new RemoteDataException("Cannot create file in directory " + destPath + ": Permisison denied");
                        }
                        
                        FileUtils.copyFileToDirectory(srcPath, destPath);
                    }
                } else {
                    throw new FileNotFoundException("No such file or directory");
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
	public void put(String localdir, String remotedir,
			RemoteTransferListener listener) throws IOException,
			RemoteDataException
	{
		try {
		    String resolvedDest = resolvePath(remotedir);
		    File srcPath = new File(localdir);
	        File destPath = new File(resolvedDest);
	        
	        if (listener != null) {
                listener.started(srcPath.length(), srcPath.getAbsolutePath());
            }
	        
	        if (!srcPath.exists()) {
	            throw new FileNotFoundException("No such file or directory");
	        } else if (!srcPath.canRead()) {
	            throw new RemoteDataException("Cannot read " + srcPath + ": Permisison denied"); 
	        }
	        
	        if (srcPath.isFile()) {
	            if (destPath.isFile()) {
	                FileUtils.copyFile(srcPath, destPath);
	            } else {
	                if (destPath.exists()) {
    	                if (!destPath.canWrite()) {
                            throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                        } 
    	                FileUtils.copyFileToDirectory(srcPath, destPath);
    	            } else {
    	                // TODO: this is an error. dir no exist, throw exception, I think
    	                // check pems on parent directory
                        File parentPath = destPath.getParentFile();
                        
                        if (parentPath != null) {
                            if (!parentPath.exists()) {
                                throw new FileNotFoundException("No such file or directory");
                            } else if (!parentPath.canWrite()) {
                                throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                            } 
                        }
                        FileUtils.copyFile(srcPath, destPath);
    	            }
    	            
	            }
	            
	        } else {
	            if (destPath.isFile()) {
                    throw new RemoteDataException("Cannot copy to " + destPath + ". Destination is not a directory.");
                } else if (destPath.exists()) {
                    if (!destPath.canWrite()) {
                        throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                    } 
                    // rename to same path in destination folder
                    destPath = new File(destPath, srcPath.getName());
                } else {
                    // rename to parent directory
                    File parentPath = destPath.getParentFile();
                    
                    if (parentPath != null) {
                        if (!parentPath.exists()) {
                            throw new FileNotFoundException("No such file or directory");
                        } else if (!parentPath.canWrite()) {
                            throw new RemoteDataException("Cannot create directory " + destPath + ": Permisison denied");
                        } 
                    }
                } 
	            
	            FileUtils.copyDirectory(srcPath, destPath);
	        }
	        
	        if (listener != null) {
	            listener.progressed(srcPath.length());
	            listener.completed();
	        }
		}
		catch (RemoteDataException e) {
		    if (listener != null) {
		        listener.failed();
		    }
		    throw e;
		}
		catch (IOException e) {
		    if (listener != null) {
                listener.failed();
            }
            throw e;
		}
		finally {
		    // nothing to do here.
		}
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
            // invalidate this now so the existence check isn't stale
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
                        syncToRemote(child.getAbsolutePath(), adjustedRemoteDir, new RemoteTransferListener(childTask));
                    } 
                    else
                    {
                        syncToRemote(child.getAbsolutePath(), childRemotePath, new RemoteTransferListener(childTask));
                    }
                }
            } 
            else 
            {
                String resolvedPath = resolvePath(remotedir);
                
                // sync if file is not there
                if (!doesExist(remotedir))  
                {
                    put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
                }
                else 
                {
                    RemoteFileInfo fileInfo = getFileInfo(remotedir);
                    
                    // if the types mismatch, delete remote, use current
                    if (localFile.isDirectory() && !fileInfo.isDirectory() || 
                            localFile.isFile() && !fileInfo.isFile()) 
                    {
                        delete(remotedir);
                        
                        put(localFile.getAbsolutePath(), resolvedPath, listener);
                    } 
                    // or if the file sizes are different
                    else if (localFile.length() != fileInfo.getSize())
                    {
                        put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
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
        catch (RemoteDataException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        } 
        catch (Exception e) {
            throw new RemoteDataException("Failed to put data to " + remotedir, e);
        }
        finally {
            //
        }
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#isDirectory(java.lang.String)
	 */
	@Override
	public boolean isDirectory(String path) throws IOException,
			RemoteDataException
	{
	    File remotePath = new File(resolvePath(path));
		if (remotePath.exists()) {
		    return remotePath.isDirectory();
		} else {
		    throw new FileNotFoundException("No such file or directory");
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#isFile(java.lang.String)
	 */
	@Override
	public boolean isFile(String path) throws IOException, RemoteDataException
	{
	    File remotePath = new File(resolvePath(path));
        if (remotePath.exists()) {
            return remotePath.isFile();
        } else {
            throw new FileNotFoundException("No such file or directory");
        }
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#length(java.lang.String)
	 */
	@Override
	public long length(String remotepath) throws IOException,
			RemoteDataException
	{
	    File remotePath = new File(resolvePath(remotepath));
        if (remotePath.exists()) {
            return remotePath.length();
        } else {
            throw new FileNotFoundException("No such file or directory");
        }
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#checksum(java.lang.String)
	 */
	@Override
	public String checksum(String remotepath) throws IOException, RemoteDataException
	{
		try
		{
		    String resolvedPath = resolvePath(remotepath);
		    File f = new File(resolvedPath);
		    
		    return MD5Checksum.getMD5Checksum(f);
		}
		catch (FileNotFoundException e) {
		    throw e;
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to compute MD5 checksum of " + remotepath, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#doRename(java.lang.String, java.lang.String)
	 */
	@Override
	public void doRename(String oldpath, String newpath) throws IOException, RemoteDataException
	{
	    FileSystem fs = java.nio.file.FileSystems.getDefault();
	    try {
	        String resolvedOldPath = resolvePath(oldpath);
	        String resolvedNewPath = resolvePath(newpath);
	        
	        if (StringUtils.startsWith(resolvedNewPath, resolvedOldPath)) {
	            throw new RemoteDataException("Cannot rename a file or director into its own subtree");
	        }
	        
	        Path oldFsPath = fs.getPath(resolvedOldPath);
            Path newFsPath = fs.getPath(resolvedNewPath);
            
    	    java.nio.file.Files.move(oldFsPath, newFsPath, StandardCopyOption.REPLACE_EXISTING);
	    }
	    catch (NoSuchFileException e) {
	        throw new FileNotFoundException("No such file or directory");
	    }
	    catch (AccessDeniedException e) {
	        throw new RemoteDataException("Permission denied");
	    }
	    catch (RemoteDataException e) {
	        throw e;
	    }
	    
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#copy(java.lang.String, java.lang.String)
	 */
	@Override
	public void copy(String remotedir, String localdir) throws IOException, RemoteDataException
	{
		copy(remotedir, localdir, null);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#copy(java.lang.String, java.lang.String, org.iplantc.service.jobs.io.RemoteTransferListener)
	 */
	@Override
	public void copy(String srcPath, String destPath, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
	    put(resolvePath(srcPath), destPath, listener);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#getUriForPath(java.lang.String)
	 */
	@Override
	public URI getUriForPath(String path) throws IOException, RemoteDataException
	{
		try {
			return new URI("file:///" + path);	
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#delete(java.lang.String)
	 */
	@Override
	public void delete(String path) throws IOException, RemoteDataException
	{
	    String resolvedPath = resolvePath(path);
	    File pathToDelete = new File(resolvedPath);
	    try {
	        if (pathToDelete.exists()) {
        	    if (pathToDelete.isFile()) {
        	        pathToDelete.delete();
        	    } else {
        	        FileUtils.deleteDirectory(pathToDelete);
        	    }
	        } else {
	            throw new FileNotFoundException("No such file or directory");
	        }
	    }
	    catch (FileNotFoundException e) {
	        if (StringUtils.containsIgnoreCase(e.getMessage(), "permission")) {
                throw new RemoteDataException("permission denied");
            } else {
                throw e;
            }
	    } 
	    catch (Exception e) {
	        throw new RemoteDataException("Failed to delete file/folder", e);
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
	public void disconnect() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.io.RemoteDataClient#doesExist(java.lang.String)
	 */
	@Override
	public boolean doesExist(String path) throws IOException, RemoteDataException
	{
		return new File(resolvePath(path)).exists();
	}

	@Override
	public List<RemoteFilePermission> getAllPermissionsWithUserFirst(
			String path, String username) throws RemoteDataException
	{
		return null;
	}

	@Override
	public List<RemoteFilePermission> getAllPermissions(String path)
			throws RemoteDataException
	{
		return null;
	}

	@Override
	public PermissionType getPermissionForUser(String username, String path)
			throws RemoteDataException
	{
		return null;
	}

	@Override
	public boolean hasReadPermission(String path, String username)
			throws RemoteDataException
	{
		return false;
	}

	@Override
	public boolean hasWritePermission(String path, String username)
			throws RemoteDataException
	{
		return false;
	}

	@Override
	public boolean hasExecutePermission(String path, String username)
			throws RemoteDataException
	{
		return false;
	}

	@Override
	public void setPermissionForUser(String username, String path,
			PermissionType type, boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void setOwnerPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void setReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void removeReadPermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void setWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void removeWritePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void setExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void removeExecutePermission(String username, String path,
			boolean recursive) throws RemoteDataException
	{
		
	}

	@Override
	public void clearPermissions(String username, String path, boolean recursive)
			throws RemoteDataException
	{
		
	}

	@Override
	public String getPermissions(String path) throws RemoteDataException
	{
		return null;
	}

	@Override
	public boolean isPermissionMirroringRequired()
	{
		return false;
	}

	@Override
	public RemoteFileInfo getFileInfo(String path) throws RemoteDataException,
			IOException
	{
		return new RemoteFileInfo(new File(resolvePath(path)));
	}

	@Override
	public String getUsername()
	{
		return System.getProperty("user.name");
	}

	@Override
	public String getHost()
	{
		if (system == null) {
		    return Settings.getLocalHostname();
		} else {
		    return system.getStorageConfig().getHost();
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
		result = prime * result
				+ ( ( homeDir == null ) ? 0 : homeDir.hashCode() );
		result = prime * result
				+ ( ( rootDir == null ) ? 0 : rootDir.hashCode() );
		result = prime * result + ( ( system == null ) ? 0 : system.hashCode() );
		return result;
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
		Local other = (Local) obj;
		if (homeDir == null)
		{
			if (other.homeDir != null)
				return false;
		}
		else if (!homeDir.equals(other.homeDir))
			return false;
		if (rootDir == null)
		{
			if (other.rootDir != null)
				return false;
		}
		else if (!rootDir.equals(other.rootDir))
			return false;
		if (system == null)
		{
			if (other.system != null)
				return false;
		}
		else if (!system.equals(other.system))
			return false;
		return true;
	}

	
}
