package org.iplantc.service.transfer.s3;

import static org.jclouds.Constants.PROPERTY_RELAX_HOSTNAME;
import static org.jclouds.Constants.PROPERTY_TRUST_ALL_CERTS;
import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import static org.jclouds.s3.reference.S3Constants.PROPERTY_S3_SERVICE_PATH;
import static org.jclouds.s3.reference.S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.jclouds.ContextBuilder;
//import org.jclouds.aws.s3.AWSS3AsyncClient;
import org.jclouds.aws.s3.AWSS3Client;
//import org.jclouds.aws.s3.blobstore.AWSS3BlobStore;
import org.jclouds.azureblob.AzureBlobClient;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.ContainerAccess;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.CopyOptions;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.http.HttpException;
import org.jclouds.http.options.GetOptions;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
//import org.jclouds.rest.RestContext;
//import org.jclouds.s3.S3AsyncClient;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.blobstore.S3BlobStore;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Module;

public class S3Jcloud implements RemoteDataClient 
{
	public static final Logger log = Logger.getLogger(S3Jcloud.class);
	
	public static final String AZURE_STORAGE_PROVIDER = "azureblob";
	public static final String AMAZON_STORAGE_PROVIDER = "aws-s3";
	public static final String OPENSTACK_STORAGE_PROVIDER = "swift";
	public static final String MEMORY_STORAGE_PROVIDER = "transient";
	
	protected String cloudProvider;
	protected String rootDir = "";
	protected String homeDir = "";
	protected String containerName = "";
	protected BlobStoreContext context;
	protected S3BlobStore blobStore = null;
	
	private String accountKey = null;
	private String accountSecret = null;
	private String host = null;
	private int port = 443;
	private Map<String, BlobMetadata> fileInfoCache = new ConcurrentHashMap<String, BlobMetadata>();
    
    protected static final int MAX_BUFFER_SIZE = 1*1024*1024;
    
    public S3Jcloud(String accountKey, String accountSecret, String rootDir, String homeDir, String containerName, String host, int port) 
	{
		this.accountSecret = accountSecret;
		this.accountKey = accountKey;
		this.cloudProvider = "aws-s3";
		this.containerName = containerName;
		
		updateEndpoint(host,port);
		
		updateSystemRoots(rootDir, homeDir);
	}
    
	public S3Jcloud(String accountKey, String accountSecret, String rootDir, String homeDir, String containerName) 
	{
		this(accountKey, accountSecret, rootDir, homeDir, containerName, null, -1);
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
	
	private void updateEndpoint(String host, int port)
	{
		if (StringUtils.isNotEmpty(host))
		{
		    URL endpoint = null;
			
		    try 
		    {
				endpoint = new URL(host);
				if (port > 0) {
					this.port = port;
				} else if (endpoint.getPort() > 0) {
					this.port = endpoint.getPort();
				} else { 
					if (endpoint.getDefaultPort() > 0) {
						this.port = endpoint.getDefaultPort();					
					} else {
						this.port = 80;
					}
					this.host = host;
					return;
				}
				
				this.host = String.format("%s://%s:%d%s", 
						endpoint.getProtocol(),
						endpoint.getHost(),
						this.port,
						endpoint.getPath());
			}
			catch (Exception e) {}
		} else {
			this.host = null;
			this.port = 443;
		}
	}

	@Override
	public void authenticate() throws IOException, RemoteDataException
	{
		if (StringUtils.isEmpty(host) 
		        || !StringUtils.startsWith(host, "http") 
		        || StringUtils.endsWith(host, "amazonaws.com")
		        || StringUtils.endsWith(host, "amazonaws.com:443")) 
		{
			context = ContextBuilder.newBuilder(cloudProvider)
				.credentials(accountKey, accountSecret)
				
                .buildView(BlobStoreContext.class);
		} 
		else 
		{
			try 
			{
			    this.cloudProvider = "s3";
				URL endpoint = new URL(host);
				Properties overrides = new Properties();
				overrides.setProperty(PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "false");
		        overrides.setProperty(PROPERTY_TRUST_ALL_CERTS, "true"); 
		        overrides.setProperty(PROPERTY_RELAX_HOSTNAME, "true");
		        Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
		        
		        
				if (StringUtils.isNotEmpty(endpoint.getPath()) && !StringUtils.equals(endpoint.getPath(), "/")) {
					overrides.setProperty(PROPERTY_S3_SERVICE_PATH, endpoint.getPath());
				} 
				else {
				    overrides.setProperty(PROPERTY_S3_SERVICE_PATH, "/");
				}
				
				context = ContextBuilder.newBuilder(this.cloudProvider)
						.endpoint(endpoint.toString())
						.overrides(overrides)
						.credentials(accountKey, accountSecret)
						.modules(modules)
						.buildView(BlobStoreContext.class);
			} catch (Exception e) {
				throw new RemoteDataException("Failed to parse service endpoint provided in the system.storage.host field.", e);
			}
		}
//		
//		PageSet<? extends StorageMetadata> pageSet = context.getBlobStore().list();
//        for (StorageMetadata storageMetadata: pageSet.toArray(new StorageMetadata[]{})) {
//            System.out.println(storageMetadata.getName());
//        }
        
		if (!getBlobStore().containerExists(containerName)) {
			if (!getBlobStore().createContainerInLocation(null, containerName)) {
				throw new RemoteDataException("Bucket " + containerName + " was not present and could not be created. "
						+ "No data operations can be performed until the bucket is created.");
			}
		}
	}
	
	/**
	 * Returns singleton of current blobstore.
	 * @return
	 */
	public S3BlobStore getBlobStore() throws RemoteDataException
	{
		if (context == null) {
			try {
				authenticate();
//			} catch (RemoteAuthenticationException e) {
			} catch (IOException e) {
				throw new RemoteDataException("Failed to authenticated to S3", e);
			}
		}
		
		if (blobStore == null) {
			blobStore = (S3BlobStore)context.getBlobStore();
		}
		
		return blobStore;
	}
	
//	public AWSS3AsyncClient s3AsyncClient() {
//	    RestContext<AWSS3Client, AWSS3AsyncClient> providerContext = context.unwrap();
//        return providerContext.getAsyncApi();
//	}


	@Override
	public boolean mkdir(String remotepath)
	throws IOException, RemoteDataException
	{
		String resolvedPath = _doResolvePath(remotepath);
		
		fileInfoCache.remove(resolvedPath);
		
		if (StringUtils.isEmpty(resolvedPath)) {
			if (!getBlobStore().containerExists(containerName)) {
				return getBlobStore().createContainerInLocation(null, containerName);
			} else {
				return false;
			}
		}
		else 
		{
			resolvedPath = StringUtils.removeEnd(resolvedPath, "/");
			
//			BlobMetadata meta = getFileMeta(resolvedPath);
//			if (meta == null)
			if (!getBlobStore().directoryExists(containerName, resolvedPath))
            {
			    String resolvedParentPath = _doResolvePath(getParentPath(remotepath));
                if (getBlobStore().directoryExists(containerName, resolvedParentPath)) {
			        getBlobStore().createDirectory(containerName, resolvedPath);
			    } else {
			        throw new FileNotFoundException("No such file or directory");
			    }
            }
            else {
                BlobMetadata meta = getFileMeta(resolvedPath);
                if (meta.getType() != StorageType.RELATIVE_PATH && meta.getType() != StorageType.FOLDER) {
                 // trying to create directory that is a file
                    throw new RemoteDataException("Failed to create " + resolvedPath + ". File already exists.");
                } else {
                    return false;
                }
            }
			
//			if (!getBlobStore().directoryExists(containerName, resolvedPath)) {
//			{
//			    
//			}
//				
//				if (StringUtils.isEmpty(resolvedParentPath)) 
//				{
//					getBlobStore().createDirectory(containerName, resolvedPath);
//				}
//				else 
//				{
//					BlobMetadata parentMeta = getFileMeta(resolvedParentPath);
//					if (parentMeta == null) 
//					{
//						throw new FileNotFoundException("No such file or directory");
//					} 
//					else if (parentMeta.getType() == StorageType.RELATIVE_PATH || parentMeta.getType() == StorageType.FOLDER) 
//					{
//						getBlobStore().createDirectory(containerName, resolvedPath);
//					}
//					else
//					{
//						// parent is a file object, this directory cannot be created
//						throw new RemoteDataException("Failed to create " + remotepath + ". Parent path is not a directory.");
//					}
//				}
//			}
//			else if (meta.getType() != StorageType.RELATIVE_PATH && meta.getType() != StorageType.FOLDER) 
//			{
//				// trying to create directory that is an object
//				throw new RemoteDataException("Failed to create " + remotepath + ". File already exists.");
//			}
//			else {
//				return false;
//			}
		}
		
		return true;
	}
	
	@Override
    public boolean mkdirs(String remotepath) 
    throws IOException, RemoteDataException 
    {
	    return mkdirs(remotepath, null);
    }

	@Override
	public boolean mkdirs(String remotepath, String authorizedUsername) 
	throws IOException, RemoteDataException 
	{
		try 
		{	
			String resolvedPath = _doResolvePath(remotepath);
			if (StringUtils.isEmpty(resolvedPath)) {
				if (!getBlobStore().containerExists(containerName)) {
					return getBlobStore().createContainerInLocation(null, containerName);
				} else {
					return false;
				}
			}
			else 
			{
				if (doesExist(remotepath))
				{
					if (isFile(remotepath)) {
						throw new RemoteDataException("Failed to create " + remotepath + ". File already exists.");
					} else {
						return false;
					}
				}
				else
				{
					String[] pathTokens = StringUtils.split(_doResolvePath(remotepath), "/");
					StringBuilder newdirectories = new StringBuilder();
					
					for(int i=0;i<ArrayUtils.getLength(pathTokens); i++)
					{
						if (StringUtils.isEmpty(pathTokens[i])) continue;
						
						newdirectories.append(pathTokens[i]);
						 
//						if (meta == null || meta.getProviderId() == null)
						if (!getBlobStore().directoryExists(containerName, newdirectories.toString()))
						{
							getBlobStore().createDirectory(containerName, newdirectories.toString());
							
							if (isPermissionMirroringRequired() && StringUtils.isNotEmpty(authorizedUsername)) {
			                    setOwnerPermission(authorizedUsername, containerName, true);
			                }
							
						}
						else {
					        BlobMetadata meta = getFileMeta(newdirectories.toString());
					        if (meta.getType() != StorageType.RELATIVE_PATH && meta.getType() != StorageType.FOLDER) {
					         // trying to create directory that is a file
	                            throw new RemoteDataException("Failed to create " + newdirectories + ". File already exists.");
					        }
					    }
						newdirectories.append("/");
					}
					
					return true;
				}
			}
		} catch (FileNotFoundException e) {
			getBlobStore().createDirectory(containerName, _doResolvePath(remotepath));
			return true;
		} catch (RemoteDataException e) {
			throw e;
		} catch (Exception e) {
			throw new RemoteDataException("Failed to create " + remotepath + " due to error on remote server", e);
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
				return new S3InputStream(getBlobStore().getBlob(containerName, _doResolvePath(remotePath)));
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
					Blob currentBlob = getBlobStore().getBlob(containerName, _doResolvePath(remotePath));
					if (currentBlob != null) {
						return new S3OutputStream(this, currentBlob);
					} else {
						throw new RemoteDataException("Failed to open input stream to " + remotePath);
					}
				}
			}
			else if (doesExist(getParentPath(remotePath)))
			{
				return new S3OutputStream(this, _doResolvePath(remotePath));
			}
			else 
			{
				throw new FileNotFoundException("No such file or directory");
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
	
	public RemoteOutputStream<?> getOutputStream(String remotePath, InputStream in)
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
					Blob currentBlob = getBlobStore().getBlob(containerName, _doResolvePath(remotePath));
					if (currentBlob != null) {
						return new S3OutputStream(this, currentBlob);
					} else {
						throw new RemoteDataException("Failed to open output stream to " + remotePath);
					}
				}
			}
			else if (doesExist(getParentPath(remotePath)))
			{
				return new S3OutputStream(this, _doResolvePath(remotePath));
			}
			else 
			{
				throw new FileNotFoundException("No such file or directory");
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
					String resolvedPath = _doResolvePath(remotepath);
					if (!StringUtils.isEmpty(resolvedPath) && !StringUtils.endsWith(resolvedPath, "/")) {
						resolvedPath += "/";
					}
					
					ListContainerOptions listContainerOptions = new ListContainerOptions();
					if (StringUtils.isNotEmpty(resolvedPath)) {
						listContainerOptions.inDirectory(resolvedPath);
					}
					listContainerOptions.maxResults(Integer.MAX_VALUE);
					
					if (pageSet != null && pageSet.getNextMarker() != null) {
						listContainerOptions.afterMarker(pageSet.getNextMarker());
					}
					
					pageSet = getBlobStore().list(containerName, listContainerOptions);
//					pageSet = getBlobStore().list(containerName);
					String folderName = StringUtils.removeEnd(resolvedPath, "/");
					for (StorageMetadata storageMetadata: pageSet.toArray(new StorageMetadata[]{})) {
						if (storageMetadata == null) {
						    continue;
						} else if (StringUtils.equalsIgnoreCase("/", folderName) || 
						        (StringUtils.equals(folderName, storageMetadata.getName()) && 
						                storageMetadata.getType() == StorageType.RELATIVE_PATH) || 
						        StringUtils.isEmpty(storageMetadata.getName())) {
						    continue;
						} else {
//						    log.debug(storageMetadata.getType() + " - " + storageMetadata.getName());
						    listing.add(new RemoteFileInfo(storageMetadata));
						}
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
    	InputStream in = null;
		try
		{
			remoteFileInfo = getFileInfo(remotepath);
		
			if (listener == null) {
//				listener = new RemoteTransferListener(null);
				// we should thorw an exception or force a transfer listener here
				// to capture the info
			}
			
			File localDir = new File(localpath);
			
			if (remoteFileInfo.isDirectory())
			{
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
				else if (localDir.isFile()) 
				{
					throw new RemoteDataException("Cannot download file to " + localpath + ". Local path is a file.");
				}
				else
				{
					localDir = new File(localDir, remoteFileInfo.getName());
					
					// create the target directory 
					if (!localDir.mkdir()) {
						throw new IOException("Failed to create local download directory");
					}
				}
				
				if (listener != null) {
					listener.started(0, remotepath);
				}
				
				// recursively copy files into the local folder since irods won't let you specify 
				// the target folder name 
				for (RemoteFileInfo fileInfo : ls(remotepath))
				{
					String remoteChild = remotepath + "/" + fileInfo.getName();
					String localChild = localDir.getAbsolutePath() + "/" + fileInfo.getName();
				
					if (fileInfo.isFile()) 
					{	
						
						Blob blob = getBlobStore().getBlob(containerName, _doResolvePath(remoteChild));
						if (blob == null) {
							throw new RemoteDataException("Failed to retrieve remote file " + remoteChild );
						} 
						else 
						{
							if (listener != null) {
								listener.started(blob.getMetadata().getContentMetadata().getContentLength(), remoteChild);
							}
							
							File localFile = new File(localChild);
							in = blob.getPayload().openStream();
							FileUtils.copyInputStreamToFile(in, localFile);
							
							if (listener != null) {
								listener.progressed(localFile.length());
							}
							
						}
					}
					else
					{
						get(remoteChild, localChild, listener); 
					}
				}
				
				if (listener != null) {
					listener.completed();
				}
			}
			else 
			{
				if (!localDir.exists()) 
				{
					if(!localDir.getParentFile().exists()) {
						throw new FileNotFoundException("No such file or directory");
					}
				}
				else if (!localDir.isDirectory()) {
					// nothing to do here. handling links
				} else {
					localDir = new File(localDir.getAbsolutePath(), remoteFileInfo.getName());
				}

				Blob blob = getBlobStore().getBlob(containerName, _doResolvePath(remotepath));
				if (blob == null) {
					throw new RemoteDataException("Failed to get file from " + remotepath );
				} 
				else 
				{
					if (listener != null) {
						listener.started(blob.getMetadata().getContentMetadata().getContentLength(), remotepath);
					}
					
					in = blob.getPayload().openStream();
					FileUtils.copyInputStreamToFile(in, localDir);
					
					if (listener != null) {
						listener.progressed(localDir.length());
						listener.completed();
					}
					
				}
			}
		} 
		catch (FileNotFoundException e) {
			if (listener != null) {
				listener.failed();
			}
			throw new FileNotFoundException("No such file or directory");
		}
		catch (ContainerNotFoundException e) {
			if (listener != null) {
				listener.failed();
			}
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (IOException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		}
		catch (RemoteDataException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		}
		catch (Exception e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException("Failed to copy file from S3.", e);
		}
		finally {
			try { in.close(); } catch (Exception e) {}
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
                // TODO: implement file appends functionality
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
//		File localFile = new File(localdir);
//		if (!localFile.exists()) {
//			throw new FileNotFoundException("No such file or directory");
//		} 
		
		try
		{
			File localFile = new File(localdir);
			if (!localFile.exists()) {
				throw new FileNotFoundException("No such file or directory");
			}
			else if (doesExist(remotedir)) 
			{
				// can't put dir to file
				if (localFile.isDirectory() && !isDirectory(remotedir)) {
					throw new RemoteDataException("cannot overwrite non-directory: " + remotedir + " with directory " + localFile.getName());
				} 
				else 
				{
					remotedir += (StringUtils.isEmpty(remotedir) ? "" : "/") + localFile.getName();
				
					if (localFile.isDirectory()) {
						mkdir(remotedir);
					}
				}
			}
			else if (doesExist(getParentPath(remotedir)))
			{
				if (localFile.isDirectory())
					mkdir(remotedir);
			}
			else
			{
				// upload and keep name.
				throw new FileNotFoundException("No such file or directory");
			}
			
			if (localFile.isDirectory()) {
				for (File child: localFile.listFiles()) {
					String remoteChildDir = remotedir + "/" + child.getName();
					if (child.isDirectory()) 
					{
						if (!doesExist(remoteChildDir))  {
							mkdir(remoteChildDir);
						}
						fileInfoCache.remove(containerName + "/" + remoteChildDir);
						put(child.getAbsolutePath(), remoteChildDir, listener);
					} else {
					    fileInfoCache.remove(containerName + "/" + remotedir);
						put(child.getAbsolutePath(), remotedir, listener);
					}
				}
			} else { 
			
				ByteSource payload = Files.asByteSource(localFile);
				String resolvedPath = _doResolvePath(remotedir);
				Blob blob = getBlobStore().blobBuilder(resolvedPath)
						  .payload(payload)
						  .contentLength(localFile.length())
						  .contentType("application/octet-stream")
						  .build();
				
				if (listener != null) {
					listener.started(localFile.length(), remotedir);
				}
				
				fileInfoCache.remove(containerName + "/" + resolvedPath);
				getBlobStore().putBlob(containerName, blob, multipart());
				
				if (listener != null) {
					listener.progressed(localFile.length());
				}
			}
			
			if (listener != null) {
				listener.completed();
			}
		} 
		catch (FileNotFoundException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		} 
		catch (IllegalArgumentException e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException("cannot overwrite non-directory: " + remotedir + " with directory " + localdir);
		} 
		catch (RemoteDataException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		}
		catch (Exception e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException("Remote put failed.", e);
		}
	}
	
	@Override
	public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener) 
	throws IOException, RemoteDataException
	{
		File sourceFile = new File(localdir);
		if (!sourceFile.exists()) {
			throw new FileNotFoundException("No such file or directory");
		}
		
		try
		{
            if (!doesExist(remotedir)) 
			{
            	put(localdir, remotedir, listener);
				return;
			}
            else if (sourceFile.isDirectory()) 
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
					adjustedRemoteDir += (StringUtils.isEmpty(remotedir) ? "" : "/") + sourceFile.getName();
				}
				
				for (File child: sourceFile.listFiles())
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
					ByteSource payload = Files.asByteSource(sourceFile);
					String resolvedPath = _doResolvePath(remotedir);
					Blob blob = getBlobStore().blobBuilder(resolvedPath)
							  .payload(payload)
							  .contentLength(sourceFile.length())
							  .contentType(new MimetypesFileTypeMap().getContentType(sourceFile))
							  .contentMD5((HashCode)null)
							  .build();
					
					if (listener != null) {
						listener.started(sourceFile.length(), remotedir);
					}
					
					fileInfoCache.remove(containerName + "/" + resolvedPath);
	                getBlobStore().putBlob(containerName, blob, multipart());
					
					if (listener != null) {
						listener.progressed(sourceFile.length());
					}
				}
				else 
				{	
				    String resolvedPath = _doResolvePath(remotedir);
					BlobMetadata blobMeta = getFileMeta(resolvedPath);
					if (blobMeta == null)
					{
						ByteSource payload = Files.asByteSource(sourceFile);
						Blob blob = getBlobStore().blobBuilder(resolvedPath)
								  .payload(payload)
								  .contentLength(sourceFile.length())
								  .contentType(new MimetypesFileTypeMap().getContentType(sourceFile))
								  .contentMD5((HashCode)null)
								  .build();
						
						if (listener != null) {
							listener.started(sourceFile.length(), remotedir);
						}
						
						fileInfoCache.remove(containerName + "/" + resolvedPath);
	                    getBlobStore().putBlob(containerName, blob, multipart());
						
						if (listener != null) {
							listener.progressed(sourceFile.length());
						}
					}
					// if the types mismatch, delete remote, use current
					else if (sourceFile.isDirectory() && (blobMeta.getType() != StorageType.RELATIVE_PATH && blobMeta.getType() != StorageType.FOLDER) || 
							sourceFile.isFile() && !(blobMeta.getType() != StorageType.RELATIVE_PATH && blobMeta.getType() != StorageType.FOLDER)) 
					{
						delete(remotedir);
						ByteSource payload = Files.asByteSource(sourceFile);
						Blob blob = getBlobStore().blobBuilder(resolvedPath)
								  .payload(payload)
								  .contentLength(sourceFile.length())
								  .contentType(new MimetypesFileTypeMap().getContentType(sourceFile))
								  .contentMD5((HashCode)null)
								  .build();
						
						if (listener != null) {
							listener.started(sourceFile.length(), remotedir);
						}
						
						fileInfoCache.remove(containerName + "/" + resolvedPath);
	                    getBlobStore().putBlob(containerName, blob, multipart());
						
						if (listener != null) {
							listener.progressed(sourceFile.length());
						}
					}
					// or if the hashes or file sizes are different,  use current
					else if (sourceFile.length() != blobMeta.getContentMetadata().getContentLength())  
					{
						ByteSource payload = Files.asByteSource(sourceFile);
						Blob blob = getBlobStore().blobBuilder(resolvedPath)
								  .payload(payload)
								  .contentLength(sourceFile.length())
								  .contentType(new MimetypesFileTypeMap().getContentType(sourceFile))
								  .contentMD5((HashCode)null)
								  .build();
						
						if (listener != null) {
							listener.started(sourceFile.length(), remotedir);
						}
						
						fileInfoCache.remove(containerName + "/" + resolvedPath);
	                    getBlobStore().putBlob(containerName, blob, multipart());
						
						if (listener != null) {
							listener.progressed(sourceFile.length());
						}
					} 
					else 
					{
						log.debug("Skipping transfer of " + sourceFile.getPath() + " to " + 
								remotedir + " because file is present and of equal size.");
					}
				}
				
				if (listener != null) {
					listener.completed();
				}
			}
		} 
		catch (FileNotFoundException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		} 
		catch (IllegalArgumentException e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException("cannot overwrite non-directory: " + remotedir + " with directory " + localdir);
		} 
		catch (RemoteDataException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		}
		catch (Exception e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException("Remote put failed.", e);
		}
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
 	throws IOException, RemoteDataException 
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
		doCopy(srcPath, destPath, null, true);
	}

	@Override
	public void copy(String remotedir, String localdir)  
	throws IOException, RemoteDataException 
	{
		copy(remotedir, localdir, null);
	}

	@Override
	public void copy(String srcPath, String destPath, RemoteTransferListener listener) 
	throws IOException, RemoteDataException 
	{
		doCopy(srcPath, destPath, listener, false);
	}
	
	@SuppressWarnings({ "unused", "deprecation" })
	private void doCopy(String srcPath, String destPath, RemoteTransferListener listener, boolean deleteSource)
	throws IOException, RemoteDataException 
	{
		RemoteFileInfo sourceFileInfo = null;
		try  
		{
			String resolvedSourcePath = _doResolvePath(srcPath); 
			resolvedSourcePath = StringUtils.removeEnd(resolvedSourcePath, "/");
			String resolvedDestPath = _doResolvePath(destPath);
			resolvedDestPath = StringUtils.removeEnd(resolvedDestPath, "/");
			String resolvedDestParentPath = _doResolvePath(getParentPath(destPath));
			resolvedDestParentPath =StringUtils.removeEnd(resolvedDestParentPath, "/");
			
			sourceFileInfo = getFileInfo(srcPath);
			
			if (sourceFileInfo.isFile()) 
			{
				if (doesExist(destPath)) 
				{
				    if (!isFile(destPath)) {
						throw new RemoteDataException("Cannot rename a file to an existing directory path.");
					}
				}
			} 
			else if (doesExist(destPath)) 
			{
				if (isFile(destPath)) {
					throw new RemoteDataException("Cannot rename a directory to an existing file path.");
				} 
				else {
					if (StringUtils.isEmpty(destPath)) {
						resolvedDestPath = _doResolvePath(sourceFileInfo.getName());
					} else {
					    resolvedDestParentPath = resolvedDestPath;
						resolvedDestPath += "/" + sourceFileInfo.getName();
					}
				}
			}
			else if (!doesExist(getParentPath(destPath))) 
			{
				throw new FileNotFoundException("No such file or directory");
			}
			else 
			{
				// if the source directory is being copied to the home directory, use the name
				// of the source directory as the name of the new folder.
				if (StringUtils.isEmpty(_doResolvePath(destPath)) 
						&& !StringUtils.endsWith(_doResolvePath(srcPath), "/")) 
				{
					resolvedDestPath = _doResolvePath(sourceFileInfo.getName());
				}
//				
//				else if (StringUtils.isNotEmpty(_doResolvePath(destPath)) && StringUtils.endsWith(_doResolvePath(destPath), "/") && 
//						!StringUtils.endsWith(_doResolvePath(srcPath), "/")){
//					resolvedDestPath += "/" + sourceFileInfo.getName();
//				}
			}
			
			String resolvedSourceParentPath = _doResolvePath(getParentPath(srcPath));
			
			if (StringUtils.equals(resolvedSourcePath, resolvedDestPath)) {
                throw new RemoteDataException("Cannot rename a file to itself.");
            }
            else if (!StringUtils.equals(resolvedDestParentPath, resolvedSourceParentPath) && 
			        StringUtils.startsWith(resolvedDestParentPath, resolvedSourcePath)) {
				throw new RemoteDataException("Cannot rename a file or director into its own subtree");
			}
			
//			BlobMetadata meta = getFileMeta(resolvedSourcePath);
//            
//			if (listener != null) {
//			    listener.started(meta.getSize(), resolvedDestPath);
//            }
//            
//            // copying source file
//            log.debug("CP " + resolvedSourcePath + " " + resolvedDestPath);
//            getBlobStore().copyBlob(containerName, resolvedSourcePath, containerName, resolvedDestPath, CopyOptions.NONE);
//            
//            if (listener != null) {
//                listener.progressed(meta.getSize());
//            }
//            
//			if (deleteSource) 
//                getBlobStore().removeBlob(containerName, resolvedSourcePath);
			
        	if (sourceFileInfo.isDirectory()) 
        	{
                PageSet<? extends StorageMetadata> pageset = null;
                do 
        		{
                	if (!StringUtils.isEmpty(resolvedSourcePath) && !StringUtils.endsWith(resolvedSourcePath, "/")) {
                		resolvedSourcePath += "/";
					}
					
        			ListContainerOptions options = new ListContainerOptions();
        			options.inDirectory(resolvedSourcePath);
        			options.withDetails();
        			options.recursive();
        			if (pageset != null && StringUtils.isEmpty(pageset.getNextMarker())) {
        				options.afterMarker(pageset.getNextMarker());
        			}
        			
        			pageset = getBlobStore().list(containerName, options);
        			
        			for (StorageMetadata meta: pageset.toArray(new StorageMetadata[]{})) {
        				if (meta == null) continue;
//        				log.debug(meta.getType().name() + " - " + meta.getName());
        				String destsubfolder = StringUtils.replaceOnce(meta.getName(), resolvedSourcePath, resolvedDestPath);
        				
        				if (meta.getType() == StorageType.FOLDER || meta.getType() == StorageType.RELATIVE_PATH) 
        				{
        					// creating remote destination folder
//        					log.debug("MKDIRS " + destsubfolder);
        					mkdirs(destsubfolder);
        					if (deleteSource) 
        						getBlobStore().deleteDirectory(containerName, meta.getName());
        				}
        				else 
        				{
        					if (listener != null) {
								listener.started(((BlobMetadata)meta).getContentMetadata().getContentLength(), destsubfolder + "/" + meta.getName());
							}
        					
        					// copying source file
//        					log.debug("CP " + meta.getName() + " " + destsubfolder);
        					getBlobStore().copyBlob(containerName, meta.getName(), containerName, destsubfolder, CopyOptions.NONE);
        					
        					if (listener != null) {
        						listener.progressed(((BlobMetadata)meta).getContentMetadata().getContentLength());
        					}
        					
        					if (deleteSource) 
        						getBlobStore().removeBlob(containerName, meta.getName());
        				}
        			}
        		} 
        		while (!StringUtils.isEmpty(pageset.getNextMarker()));
        	}
            
        	if (listener != null) {
				listener.started(sourceFileInfo.getSize(), resolvedDestPath);
			}
            
        	getBlobStore().copyBlob(containerName, resolvedSourcePath, containerName, resolvedDestPath, CopyOptions.NONE);
            
            if (listener != null) {
				listener.progressed(sourceFileInfo.getSize());
			}
            
            if (deleteSource) 
            	getBlobStore().deleteDirectory(containerName, resolvedSourcePath);
    		
            if (listener != null) {
				listener.completed();
			}
//	            } 
//	            else if (rest.getApi() instanceof AzureBlobClient) 
//	            {
//	            	if (listener != null) {
//	    				listener.failed();
//	    			}
//	               throw new NotImplementedException();
////	               RestContext<AzureBlobClient, AzureBlobAsyncClient> providerContext = context.unwrap();
////	               BlobProperties objectMetadata = providerContext.getApi().getBlobProperties(containerName, resolvedSourcePath);
////	               objectMetadata.getMetadata().put("name", resolvedDestPath);
////	               providerContext.getApi().setBlobMetadata(containerName, resolvedSourcePath, objectMetadata.getMetadata());
//	            }
//	            else
//	            {
//	            	if (listener != null) {
//	    				listener.failed();
//	    			}
//	            	throw new NotImplementedException();
//	            }
	            	
//	            else if (rest.getApi() instanceof SwiftClient) {
//	               RestContext<SwiftClient, SwiftAsyncClient> providerContext = context.unwrap();
//	               object = providerContext.getApi().getObjectInfo(containerName, blobName);   
//	            } else if (rest.getApi() instanceof AtmosClient) {
//	               RestContext<AtmosClient, AtmosAsyncClient> providerContext = context.unwrap();
//	               object = providerContext.getApi().headFile(containerName + "/" + blobName);
//	            }
	            
	            
//			} else {
//				if (listener != null) {
//					listener.failed();
//				}
//			}
			
		}
		catch (RemoteDataException | IOException e) {
			if (listener != null) {
				listener.failed();
			}
			throw e;
		}
		catch (Throwable e) {
			if (listener != null) {
				listener.failed();
			}
			throw new RemoteDataException(String.format("Internal error when attempting to copy %s to %s on the remote server.", srcPath, destPath), e);
		}
		finally {}
	}

	@Override
	public URI getUriForPath(String path)
	throws IOException, RemoteDataException 
	{
		try
		{
			return new URI("s3://" + host + (port == 80 || port == 443 ? "" : ":" + port) + "/" + path);
		}
		catch (URISyntaxException e)
		{
			throw new IOException(e);
		}
//		if (doesExist(path)) {
//			return getFileMeta(_doResolvePath(path)).getUri();
//		} else {
//			throw new FileNotFoundException("No such file or directory");
//		}
	}

	@Override
	public void delete(String path) 
	throws IOException, RemoteDataException 
	{
		String resolvedPath = null;
		
		try 
		{
			resolvedPath = _doResolvePath(path);
			if (isFile(path))
			{
			    fileInfoCache.remove(containerName + "/" + resolvedPath);
			    getBlobStore().removeBlob(containerName, resolvedPath);
			}
			else
			{
				PageSet<? extends StorageMetadata> pageset = null;
				
				do 
				{
					ListContainerOptions options = new ListContainerOptions();
					options.inDirectory(resolvedPath);
					options.recursive();
					if (pageset != null && StringUtils.isEmpty(pageset.getNextMarker())) {
						options.afterMarker(pageset.getNextMarker());
					}
					
					pageset = getBlobStore().list(containerName, options);
					
					for (StorageMetadata meta: pageset.toArray(new StorageMetadata[]{}))  {
						if (meta.getType() == StorageType.FOLDER || meta.getType() == StorageType.RELATIVE_PATH) {
						    fileInfoCache.remove(containerName + "/" + meta.getName());
							getBlobStore().deleteDirectory(containerName, meta.getName());
						}
						else {
						    fileInfoCache.remove(containerName + "/" + meta.getName());
						    getBlobStore().removeBlob(containerName, meta.getName());
						}
					}
				} 
				while (!StringUtils.isEmpty(pageset.getNextMarker()));
				
				fileInfoCache.remove(containerName + "/" + resolvedPath);
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
		// TODO: support server side copies
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
			resolvedPath = _doResolvePath(remotePath);
			if (StringUtils.isEmpty(resolvedPath)) {
				return getBlobStore().containerExists(containerName);
			} 
			else 
			{
				BlobMetadata meta = getFileMeta(resolvedPath, true);
				return meta != null;
			}
		}
		catch (HttpException e) {
			return false;
		}
		catch (FileNotFoundException e) {
			throw e;
		}
		catch (ContainerNotFoundException e) {
			throw new FileNotFoundException("No such file or directory");
		} 
		catch (UncheckedExecutionException e) {
		    if (e.getCause() instanceof org.jclouds.rest.AuthorizationException) {
		        return true;
		    } else {
		        throw new RemoteDataException("Failed to retrieve information for " + remotePath, e);
		    }
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to retrieve information for " + remotePath, e);
		}
	}

	protected String _doResolvePath(String path) throws FileNotFoundException {
	    return StringUtils.strip(resolvePath(path), "/");
	}
	
	@Override
    public String resolvePath(String path) throws FileNotFoundException {
		if (StringUtils.isEmpty(path)) {
		    return homeDir;
//			return StringUtils.removeStart(homeDir, "/");
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
		
		return path;
	}
	
	public String getParentPath(String path) {
	    
		if (StringUtils.isEmpty(path)) {
			return "../";
		}
		else if (StringUtils.contains(path, '/')) {
		    return path + "/..";
		}
		else {
			return "";
		}
	}

	/**
	 * Convenience method to work around AWS being picky about trailing slashes
	 * and to cache the result for quicker complex operations.
	 * 
	 * @param remotepath
	 * @return
	 * @throws FileNotFoundException
	 * @throws RemoteDataException 
	 */
	private BlobMetadata getFileMeta(String resolvedPath) 
    throws FileNotFoundException, RemoteDataException
    {
	    return getFileMeta(resolvedPath, false);
    }
	
	/**
     * Convenience method to work around AWS being picky about trailing slashes.
     * @param remotepath
     * @param forceCheck whether to break the cache and force a remote check
     * @return
     * @throws FileNotFoundException
     * @throws RemoteDataException 
     */
    private BlobMetadata getFileMeta(String resolvedPath, boolean forceCheck) 
	throws FileNotFoundException, RemoteDataException
	{
	    BlobMetadata blobMeta = fileInfoCache.get(containerName + "/" + resolvedPath);
		if (blobMeta == null || forceCheck)
		{
		    try 
		    {
		        blobMeta = getBlobStore().blobMetadata(containerName, resolvedPath);
    		    if (blobMeta != null) {
        			// cool, found it
        		    fileInfoCache.put(containerName + "/" + resolvedPath, blobMeta);
        		} else if (!StringUtils.endsWith(resolvedPath, "/")) { 
        		    blobMeta = getBlobStore().blobMetadata(containerName, resolvedPath + "/");
        		    if (blobMeta != null) {
        		        fileInfoCache.put(containerName + "/" + resolvedPath, blobMeta);
        		    }
        		} else {
        			return null;
        		}
		    } catch (ContainerNotFoundException e) {
		        fileInfoCache.remove(containerName + "/" + resolvedPath);
		        throw new FileNotFoundException("No such file or directory");
		    }
		}
		return blobMeta;
	}
	
	@Override
	public RemoteFileInfo getFileInfo(String path) 
	throws RemoteDataException, IOException 
	{
	    String resolvedPath = _doResolvePath(path);
		try 
		{
			RemoteFileInfo fileInfo = null;
			if (StringUtils.isEmpty(resolvedPath)) {
				if (getBlobStore().containerExists(containerName)) {
					fileInfo = new RemoteFileInfo();
					fileInfo.setName("/");
					fileInfo.setFileType(RemoteFileInfo.DIRECTORY_TYPE);
					fileInfo.setLastModified(new Date());
					fileInfo.setOwner(RemoteFileInfo.UNKNOWN_STRING);
					fileInfo.setSize(0);
				} else {
					throw new FileNotFoundException("No such file or directory");
				}
			} 
			else
			{
				BlobMetadata blobMetadata = getFileMeta(resolvedPath);
				if (blobMetadata == null) 
				{
					throw new FileNotFoundException("No such file or directory");
				} else {
					fileInfo = new RemoteFileInfo(blobMetadata);
				}
			}
			
			return fileInfo;
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
}
