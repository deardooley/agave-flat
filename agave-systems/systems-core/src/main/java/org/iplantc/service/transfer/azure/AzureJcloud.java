package org.iplantc.service.transfer.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;

public class AzureJcloud implements RemoteDataClient 
{
	public static final String AZURE_STORAGE_PROVIDER = "azureblob";
	public static final String AMAZON_STORAGE_PROVIDER = "aws-s3";
	public static final String OPENSTACK_STORAGE_PROVIDER = "swift";
	public static final String MEMORY_STORAGE_PROVIDER = "transient";
	
	protected String cloudProvider;
	protected String rootDir = "";
	protected String homeDir = "";
	protected String containerName = "";
	protected BlobStoreContext context;
	protected BlobStore blobStore = null;
	
	private String accountKey = null;
	private String accountSecret = null;
	private String host = null;
	private int port = 443;
    protected static final int MAX_BUFFER_SIZE = 65537;
    
	public AzureJcloud(String accountKey, String accountSecret, String rootDir, String homeDir, String containerName, String cloudProvider) 
	{
		this.accountSecret = accountSecret;
		this.accountKey = accountKey;
		this.cloudProvider = cloudProvider;
		this.containerName = containerName;
		
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
	public void authenticate()
	{
		context = ContextBuilder.newBuilder(cloudProvider)
                .credentials(accountKey, accountSecret)
                .buildView(BlobStoreContext.class);
	}
	
	/**
	 * Returns singleton of current blobstore.
	 * @return
	 */
	public BlobStore getBlobStore()
	{
		if (context == null) {
			authenticate();
		}
		
		if (blobStore == null) {
			blobStore = context.getBlobStore();
		}
		
		return blobStore;
	}

	@Override
	public boolean mkdir(String remotepath)
	throws IOException, RemoteDataException
	{
		if (doesExist(remotepath)) {
			if (isFile(remotepath)) {
				return false;
			}
		} else if (!doesExist(FilenameUtils.getPath(remotepath))) {
			throw new FileNotFoundException("No such file or directory");
		} else {
			getBlobStore().createDirectory(containerName, resolvePath(remotepath));
		}
		return true;
	}

	@Override
	public boolean mkdirs(String remotepath) 
	throws IOException, RemoteDataException 
	{
		RemoteFileInfo fileInfo = null;
		try 
		{
			fileInfo = getFileInfo(remotepath);
			if (fileInfo.isFile()) {
				throw new RemoteDataException("Failed to create " + remotepath + ". File already exists.");
			}
			return false;
		} catch (FileNotFoundException e) {
			getBlobStore().createDirectory(containerName, resolvePath(remotepath));
			return true;
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
	public int getMaxBufferSize() {
		return MAX_BUFFER_SIZE;
	}

	@Override
	public RemoteInputStream<?> getInputStream(String remotePath, boolean passive)
	throws IOException, RemoteDataException 
	{
		try 
		{
			if (isFile(remotePath)) {
				return new AzureInputStream(getBlobStore().getBlob(containerName, resolvePath(remotePath)));
			} else {
				throw new RemoteDataException("Cannot open input stream for directory " + remotePath);
			}
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to open input stream to " + remotePath, e);
		}	
	}

	@Override
	public RemoteOutputStream<?> getOutputStream(String remotePath, boolean passive, boolean append)
	throws IOException, RemoteDataException
	{
		try 
		{
			if (doesExist(remotePath)) 
			{
				if (isDirectory(remotePath))
				{
					throw new RemoteDataException("Cannot open output stream to directory " + remotePath);
				}
				else
				{
					Blob currentBlob = getBlobStore().getBlob(containerName, resolvePath(remotePath));
					if (currentBlob != null) {
						return new AzureOutputStream(this, currentBlob);
					} else {
						throw new RemoteDataException("Failed to open input stream to " + remotePath);
					}
				}
			}
			else {
				return new AzureOutputStream(this, resolvePath(remotePath));
			}
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to open input stream to " + remotePath, e);
		}	
	}

	@Override
	public List<RemoteFileInfo> ls(String remotepath)
	throws IOException, RemoteDataException 
	{
		try 
		{
			List<RemoteFileInfo> listing = new ArrayList<RemoteFileInfo>();
			
			if (isFile(remotepath)) 
			{
				RemoteFileInfo fileInfo = getFileInfo(remotepath);
				listing.add(fileInfo);
			}
			else
			{
				PageSet<? extends StorageMetadata> pageSet = null;
				do 
				{
					ListContainerOptions listContainerOptions = new ListContainerOptions();
					listContainerOptions.inDirectory(resolvePath(remotepath));
					
					if (pageSet != null && pageSet.getNextMarker() != null) {
						listContainerOptions.afterMarker(pageSet.getNextMarker());
					}
					pageSet = getBlobStore().list(containerName, listContainerOptions);
					
					StorageMetadata storageMetadata = null;
					for (Iterator<? extends StorageMetadata> iter = pageSet.iterator(); iter.hasNext(); storageMetadata = iter.next()) {
						listing.add(new RemoteFileInfo(storageMetadata));
					}
					
				} while (pageSet.getNextMarker() != null);
			}
			
			return listing;	
		} 
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (ContainerNotFoundException e) {
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (RemoteDataException e) {
			throw new RemoteDataException("Failed to list contents of " + remotepath, e);
		}
	}

	@Override
	public void get(String remotedir, String localdir)
	throws IOException, RemoteDataException 
	{
		get(remotedir, localdir, null);
	}

	@Override
	public void get(String remotepath, String localpath, RemoteTransferListener listener) 
	throws IOException, RemoteDataException 
	{
		RemoteFileInfo remoteFileInfo = null;
    	
		try
		{
			remoteFileInfo = getFileInfo(remotepath);
		
			if (listener == null) {
				listener = new RemoteTransferListener(null);
			}
			
			File localDir = new File(localpath);
			
			if (remoteFileInfo.isDirectory())
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
				} 
				else 
				{
					localDir = new File(localDir, localDir.getName());
					// create the target directory 
					if (!localDir.mkdir()) {
						throw new IOException("Failed to create local download directory");
					}
				}
				
				// recursively copy files into the local folder since irods won't let you specify 
				// the target folder name 
				for (RemoteFileInfo fileInfo : ls(remotepath))
				{
					String remoteChild = remotepath + "/" + fileInfo.getName();
					String localChild = localDir.getAbsolutePath() + "/" + fileInfo.getName();
				
					if (fileInfo.isFile()) 
					{	
						
						Blob blob = getBlobStore().getBlob(containerName, resolvePath(remoteChild));
						if (blob == null) {
							throw new RemoteDataException("Failed to retrieve remote file " + remoteChild );
						} 
						else 
						{
							File localFile = new File(localChild);
							FileUtils.copyInputStreamToFile(blob.getPayload().openStream(), localFile);
						}
					}
					else
					{
						get(remoteChild, localChild, listener); 
					}
				}
			}
			else 
			{
				if(localDir.isDirectory()) {
					localDir = new File(localDir.getAbsolutePath(), remoteFileInfo.getName());
				}

				Blob blob = getBlobStore().getBlob(containerName, resolvePath(remotepath));
				if (blob == null) {
					throw new RemoteDataException("Failed to retrieve remote file " + remotepath );
				} 
				else 
				{
					FileUtils.copyInputStreamToFile(blob.getPayload().openStream(), localDir);
				}
			}
		} 
		catch (FileNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		}
		catch (ContainerNotFoundException e) {
			throw new java.io.FileNotFoundException("No such file or directory");
		} 
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to copy file to irods.", e);
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
                put(localpath, remotepath, listener);
            }
            else if (localFile.isDirectory()) {
                throw new RemoteDataException("cannot append directory");
            }
            else {
                throw new NotImplementedException();
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

	@Override
	public void put(String localdir, String remotedir) 
	throws IOException, RemoteDataException 
	{
		put(localdir, remotedir, null);
	}

	@Override
	public void put(String localdir, String remotedir, RemoteTransferListener listener)
 	throws IOException, RemoteDataException 
	{
		throw new NotImplementedException();
		
//		File localFile = new File(localdir);
//		if (!localFile.exists()) {
//			throw new FileNotFoundException("Local file " + localdir + " does not exist.");
//		} 
//		
//		try
//		{
//			if (localFile.isDirectory()) {
//				if (!StringUtils.isEmpty(localFile.getName())) {
//					if (!StringUtils.isEmpty(remotedir)) {
//						remotedir = remotedir + "/" + localFile.getName();
//					} else {
//						remotedir = localFile.getName();
//					}
//					if (!doesExist(remotedir)) {
//						mkdir(remotedir);
//					}
//				}
//				
//				for (File child: localFile.listFiles()) {
//					String remoteChildDir = remotedir + "/" + child.getName();
//					if (child.isDirectory()) {
//						if (!doesExist(remoteChildDir)) 
//							mkdir(remoteChildDir);
//					}
//					put(child.getAbsolutePath(), remoteChildDir, listener);
//				}
//			} else { 
//				// Create or overwrite the "myimage.jpg" blob with contents from a local file
//				CloudBlockBlob blob = serviceClient.getBlockBlobReference(resolvePath(remotedir));
//				blob.upload(new FileInputStream(localFile), localFile.length());
//			}
//		} catch (Exception e) {
//			throw new RemoteDataException("Remote put failed.", e);
//		}
		
	}
	
	@Override
	public boolean isDirectory(String path)
 	throws IOException, RemoteDataException 
	{
		return getFileInfo(path).isDirectory();
	}

	@Override
	public boolean isFile(String path)
	throws IOException, RemoteDataException
	{
		return getFileInfo(path).isFile();
	}

	@Override
	public long length(String path)
 	throws IOException, RemoteDataException 
	{
		return getFileInfo(path).getSize();
	}

	@Override
	public String checksum(String remotepath)
 	throws IOException, RemoteDataException, NotImplementedException 
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
	public void doRename(String srcPath, String destPath) 
	throws IOException, RemoteDataException 
	{
		copy(srcPath, destPath);
		delete(srcPath);
	}

	@Override
	public void copy(String remotedir, String localdir)  
	throws IOException, RemoteDataException 
	{
		copy(remotedir, localdir, null);
	}

	@SuppressWarnings({ "unused", "deprecation" })
	@Override
	public void copy(String srcPath, String destPath, RemoteTransferListener listener) 
	throws IOException, RemoteDataException 
	{
//		RemoteFileInfo sourceFileInfo = null;
//		RemoteFileInfo destFileInfo = null;
//		try  
//		{
//			String resolvedSourcePath = resolvePath(srcPath);
//			resolvedSourcePath = StringUtils.removeEnd(resolvedSourcePath, "/");
//			String resolvedDestPath = resolvePath(destPath);
//			resolvedDestPath = StringUtils.removeEnd(resolvedDestPath, "/");
//			
//			if (StringUtils.startsWith(resolvedDestPath, resolvedSourcePath)) {
//				throw new RemoteDataException("Cannot rename a file or director into its own subtree");
//			}
//			
//			sourceFileInfo = getFileInfo(srcPath);
//			
//			if (sourceFileInfo.isFile()) {
//				if (doesExist(destPath)) {
//					if (!isFile(destPath)) {
//						throw new RemoteDataException("Cannot rename a file to an existing directory path.");
//					}
//				}
//			} 
//			else if (doesExist(destPath) && isFile(destPath)) 
//			{
//				throw new RemoteDataException("Cannot rename a directory to an existing file path.");
//			}
//			
//			if (context.getBackendType().getRawType().equals(RestContext.class)) 
//			{
//				RestContext<?, ?> rest = context.unwrap();
//	            Object object = null;
//	            if (rest.getApi() instanceof S3Client) {
//	               RestContext<S3Client, S3Client> providerContext = context.unwrap();
//	               ObjectMetadata objectMetadata = providerContext.getApi().headObject(containerName, resolvedSourcePath);
//	               
//	               providerContext.getApi().copyObject(objectMetadata.getBucket(), resolvedSourcePath, objectMetadata.getBucket(), resolvedDestPath);
//	            } 
//	            else if (rest.getApi() instanceof AzureBlobClient) 
//	            {
//	               throw new NotImplementedException();
////	               RestContext<AzureBlobClient, AzureBlobAsyncClient> providerContext = context.unwrap();
////	               BlobProperties objectMetadata = providerContext.getApi().getBlobProperties(containerName, resolvedSourcePath);
////	               objectMetadata.getMetadata().put("name", resolvedDestPath);
////	               providerContext.getApi().setBlobMetadata(containerName, resolvedSourcePath, objectMetadata.getMetadata());
//	            }
//	            else
//	            {
	            	throw new NotImplementedException();
//	            }
//	            	
////	            else if (rest.getApi() instanceof SwiftClient) {
////	               RestContext<SwiftClient, SwiftAsyncClient> providerContext = context.unwrap();
////	               object = providerContext.getApi().getObjectInfo(containerName, blobName);   
////	            } else if (rest.getApi() instanceof AtmosClient) {
////	               RestContext<AtmosClient, AtmosAsyncClient> providerContext = context.unwrap();
////	               object = providerContext.getApi().headFile(containerName + "/" + blobName);
////	            }
//			}
//		}
//		finally {}
	}

	@Override
	public URI getUriForPath(String path) throws IOException,
			RemoteDataException
	{
		try {
			return new URI("azure://" + host + ":443/" + path);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void delete(String path) 
	throws IOException, RemoteDataException 
	{
		String resolvedPath = null;
		
		try 
		{
			resolvedPath = resolvePath(path);
			
			if (isFile(path))
			{
				getBlobStore().removeBlob(containerName, resolvedPath);
			}
			else
			{
				getBlobStore().deleteDirectory(containerName, resolvedPath);
			}
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (ContainerNotFoundException e) {
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to delete " + path, e);
		}
	}

	@Override
	public boolean isThirdPartyTransferSupported() 
	{
		return false;
	}

	@Override
	public void disconnect() {
		if (context != null) {
			context.close();
		}
		blobStore = null;
		context = null;
	}

	@Override
	public boolean doesExist(String remotePath) 
	throws IOException, RemoteDataException 
	{
		String resolvedPath = null;
		
		try 
		{
			resolvedPath = resolvePath(remotePath);
			return (getBlobStore().directoryExists(containerName, resolvedPath) || 
					getBlobStore().blobExists(containerName, resolvedPath)); 
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (ContainerNotFoundException e) {
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve information for " + remotePath, e);
		}
	}

	@Override
	public String resolvePath(String path) throws FileNotFoundException {
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

	@Override
	public RemoteFileInfo getFileInfo(String path) 
	throws RemoteDataException, IOException 
	{
		String resolvedPath = null;
		try 
		{
			resolvedPath = resolvePath(path);
			if (doesExist(path)) 
			{
				BlobMetadata blobMetadata = getBlobStore().blobMetadata(containerName, resolvedPath);
				
				if (blobMetadata == null) {
					throw new FileNotFoundException("No such file or directory");
				} else {
					return new RemoteFileInfo(blobMetadata);
				}
			}
			else {
				throw new FileNotFoundException("No such file or directory");
			}
			
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (ContainerNotFoundException e) {
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve information for " + path, e);
		}
	}

	@Override
	public String getUsername() {
		return this.accountKey;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	@Override
	public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username) 
	throws RemoteDataException 
	{
		
		return Arrays.asList(new RemoteFilePermission(username, null, PermissionType.ALL, true));
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
		return PermissionType.ALL;
	}

	@Override
	public boolean hasReadPermission(String path, String username)
	throws RemoteDataException 
	{
		return true;
	}

	@Override
	public boolean hasWritePermission(String path, String username)
	throws RemoteDataException 
	{
		return true;
	}

	@Override
	public boolean hasExecutePermission(String path, String username)
	throws RemoteDataException 
	{
		return true;
	}

	@Override
	public void setPermissionForUser(String username, String path, PermissionType type, boolean recursive) 
	throws RemoteDataException 
	{
		
	}

	@Override
	public void setOwnerPermission(String username, String path, boolean recursive) 
	throws RemoteDataException 
	{	
	}

	@Override
	public void setReadPermission(String username, String path, boolean recursive)
	throws RemoteDataException 
	{
	}

	@Override
	public void removeReadPermission(String username, String path, boolean recursive)
	throws RemoteDataException 
	{	
	}

	@Override
	public void setWritePermission(String username, String path, boolean recursive)
	throws RemoteDataException 
	{	
	}

	@Override
	public void removeWritePermission(String username, String path, boolean recursive)
	throws RemoteDataException 
	{	
	}

	@Override
	public void setExecutePermission(String username, String path, boolean recursive)
	throws RemoteDataException 
	{
	}

	@Override
	public void removeExecutePermission(String username, String path, boolean recursive) 
	throws RemoteDataException 
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
		RemoteFileInfo file;
		try 
		{
			file = getFileInfo(path);
			
			if (file == null) {
				throw new RemoteDataException("No file found at " + path);
			} else {
				return file.getPermissionType().name();
			}
		} 
		catch (IOException e) 
		{
			throw new RemoteDataException("No file found at " + path);
		}
	}

	@Override
	public boolean isPermissionMirroringRequired() {
		return false;
	}

	@Override
	public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener) throws IOException, RemoteDataException
	{
		put(localdir, remotedir, listener);
	}
}
