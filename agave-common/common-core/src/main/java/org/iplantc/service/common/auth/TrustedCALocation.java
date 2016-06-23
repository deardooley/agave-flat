package org.iplantc.service.common.auth;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.bzip2.BZip2UnArchiver;
import org.codehaus.plexus.archiver.gzip.GZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.globus.common.CoGProperties;
import org.iplantc.service.common.clients.HTTPSClient;

/**
 * Manages the location of the trusted CA folder used
 * with GSI authenication mechanisms.
 * 
 * @author dooley
 *
 */
public class TrustedCALocation 
{	
	private final static Logger log = Logger.getLogger(TrustedCALocation.class);
	
	private String caPath = null;
	
	public TrustedCALocation() {
		this.caPath = CoGProperties.getDefault().getCaCertLocations();
	}
	
	public TrustedCALocation(String trustedCAPath) {
		if (StringUtils.isEmpty(trustedCAPath)) {
			this.caPath = CoGProperties.getDefault().getCaCertLocations();
		} else {
			this.caPath = trustedCAPath;
		}
	}
	
	public String getCaPath() {
		return caPath;
	}

	public void setCaPath(String caPath) {
		this.caPath = caPath;
	}

	public String toString() {
		return getCaPath();
	}
	
	public void fetchRemoteCACertBundle(String sURL) 
	throws IOException
	{
		URL url = null;
		if (StringUtils.isEmpty(sURL)) {
			log.debug("Remote trusted CA bundle URL was null. "
					+ "Using locally installed certificates "
					+ "installed at " + getCaPath());
			return;
		} else {
			url = new URL(sURL);
		}
		
		UnArchiver unarchiver = null;
		File tmpDownloadDir = null;
		try 
		{
			String extension = FilenameUtils.getExtension(url.getPath());
			if (StringUtils.isEmpty(extension)) {
				throw new IOException("Unable to extract trusted ca certs. Unknown file type.");
			}
			
			// fetch the remote file and save as a temp file in the local ca location 
			tmpDownloadDir = com.google.common.io.Files.createTempDir();
			
			File tmpCaDir = new File(caPath);
			tmpCaDir.mkdirs();
			
			File tmpFile = new File(tmpDownloadDir, FilenameUtils.getName(url.getPath()));
			HTTPSClient.doGet(url, tmpFile);
			
			// now unpack the file. we need to do some negotiation to figure out
			// how the file is compressed.
			unarchiver = getUnArchiver(tmpFile);
			unarchiver.setUseJvmChmod(true);
			unarchiver.setOverwrite(true);
			unarchiver.setIgnorePermissions(false);
			unarchiver.setDestDirectory(tmpDownloadDir);
			unarchiver.extract();
			
			// remove the archive now that we have extracted the contents. 
			tmpFile.delete();
			
			// copy unpacked contents to the caPath
			unpackTrustroots(tmpDownloadDir, tmpCaDir);
		}
		catch (MalformedURLException | URISyntaxException e) {
			throw new IOException("Invalid trusted CA certificate location", e);
		} 
		catch (Exception e) {
			throw new IOException("Failed to retrieve trusted CA certificates", e);
		}
		finally {
			try { org.apache.commons.io.FileUtils.deleteQuietly(tmpDownloadDir); } catch (Exception e) {}
		}
		
	}
	
	/**
	 * Unpacks expanded archive by flattening the source directory tree and copying all
	 * files to the dest directory.
	 * 
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	private void unpackTrustroots(final File source, final File dest) 
	throws IOException 
	{	
		Set<FileVisitOption> visitOptions = new HashSet<FileVisitOption>();
		visitOptions.add(FileVisitOption.FOLLOW_LINKS);
		Path src = FileSystems.getDefault().getPath(source.getAbsolutePath());
		Files.walkFileTree(src, visitOptions, 5, new SimpleFileVisitor<Path>() {	
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException
			{   
			    // need to add logic to maintain symlinks 
			    if (attrs.isRegularFile()) {
				    try {
				        FileUtils.copyFileToDirectory(file.toFile(), dest);
				    } catch (IOException e) {
				        // retry generally this happens when there are a lot of symbolic
				        // links. usually works second time with out a hitch.
				        if (e.getMessage().contains("Failed to copy full contents")) {
				            log.debug("Retrying failed unpacking of trustroots. " + e.getMessage());
				            FileUtils.copyFileToDirectory(file.toFile(), dest);
				        }
				    }
			    }
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e)
			throws IOException
			{
				if (e == null) {
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed
					throw e;
				}
			}
		});
	}

	/**
	 * Simple factory method to assign an {@link org.codehaus.plexus.archiver.zip.AbstractUnArchiver}
	 * implemenation class based on the file extension.
	 *  
	 * @param fileExtension
	 * @return instance {@link org.codehaus.plexus.archiver.zip.AbstractUnArchiver}
	 * @throws IOException
	 */
	private UnArchiver getUnArchiver(File sourceFile) 
	throws IOException
	{
		String fileExtension = getFileExtention(sourceFile);
		UnArchiver unarchiver = null;
		if (StringUtils.equals(fileExtension, "zip")) {
			unarchiver = new ZipUnArchiver(sourceFile);
		} else if (StringUtils.equals(fileExtension, "tar.gz")) {
			
			unarchiver = new TarGZipUnArchiver(sourceFile);
		} else if (StringUtils.equals(fileExtension, "tar.bz") || 
				StringUtils.equals(fileExtension, "tar.bz2")) {
			unarchiver = new TarBZip2UnArchiver(sourceFile);
		} else if (StringUtils.equals(fileExtension, "bz") || 
				StringUtils.equals(fileExtension, "bz2") || 
				StringUtils.equals(fileExtension, "bzip2")) {
			unarchiver = new BZip2UnArchiver(sourceFile);
		} else if (StringUtils.equals(fileExtension, "gzip")) {
			unarchiver = new GZipUnArchiver(sourceFile);
		} else if (StringUtils.equals(fileExtension, "tar")) {
			unarchiver = new TarUnArchiver(sourceFile);
		} else {
			throw new IOException("Unrecognized trusted ca archive file format " + fileExtension);
		}
		if ( unarchiver instanceof LogEnabled ) 
		{
			((LogEnabled)unarchiver).enableLogging(
					new ConsoleLogger(
							org.codehaus.plexus.logging.Logger.LEVEL_ERROR, 
							this.getClass().getName()));
		}
		
		return unarchiver;
	}

	private String getFileExtention( File file )
    {
        String path = file.getAbsolutePath();
        
        String archiveExt = FileUtils.getExtension( path ).toLowerCase( Locale.ENGLISH );
        
        if ( "gz".equals( archiveExt ) || "bz2".equals( archiveExt ) )
        {
            String [] tokens = StringUtils.split( path, "." );
            
            if ( tokens.length > 2  && "tar".equals( tokens[tokens.length -2].toLowerCase( Locale.ENGLISH ) ) )
            {
                archiveExt = "tar." + archiveExt;
            }
        }
        
        return archiveExt;
        
    }

}
