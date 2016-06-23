package org.iplantc.service.transfer;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.globus.ftp.MlsxEntry;
import org.globus.ftp.exception.FTPException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;

import com.maverick.sftp.SftpFile;
import com.maverick.sftp.SftpFileAttributes;

//import com.sshtools.j2ssh.sftp.FileAttributes;
//import com.sshtools.j2ssh.sftp.SftpFile;
//import com.trilead.ssh2.SFTPv3DirectoryEntry;
//import com.trilead.ssh2.SFTPv3FileAttributes;

/**
 * Represents the properties of a remote file
 * such as size, name, modification date and time, etc.
 * Can represent a regular file as well as a directory
 * or a soft link.
 */
public class RemoteFileInfo implements Comparable<RemoteFileInfo> {
  
    public static final byte UNKNOWN_TYPE   = 0;
    public static final byte FILE_TYPE      = 1;
    public static final byte DIRECTORY_TYPE = 2;
    public static final byte SOFTLINK_TYPE  = 3;
    public static final byte DEVICE_TYPE  = 4;
    
    public static final String UNKNOWN_STRING = "?";  
    public static final int UNKNOWN_NUMBER = -1;  
    
    private String owner = UNKNOWN_STRING;
    private long size = UNKNOWN_NUMBER;
    private String name = UNKNOWN_STRING;
    private String date = UNKNOWN_STRING;
    private String time = UNKNOWN_STRING;
    private byte fileType;
    private int mode = 0;
    private Date lastModified = null;
    
   public RemoteFileInfo() {}
    
    /**
     * Parses the file information from one line of response to
     * the FTP LIST command. Note: There is no commonly accepted
     * standard for the format of LIST response. 
     * This parsing method only accepts 
     * the most common Unix file listing formats:
     * System V or Berkeley (BSD) 'ls -l'
     *
     * @see #parseUnixListReply(String reply) 
     * @param unixListReply a single line from ls -l command
     */
    public RemoteFileInfo(String unixListReply) throws RemoteDataException {
        parseUnixListReply(unixListReply);
    }
    
    /**
     * Parse a mlst entry into a RemoteFileInfo object. MLST gives a standard
     * structure for a listing entry vs the unpredictable output from an ls
     * command. Fields are:
     *
     * size       -- Size in octets
     * modify     -- Last modification time
     * create     -- Creation time
     * type       -- Entry type
     * unique     -- Unique id of file/directory
     * perm       -- File permissions, whether read, write, execute is
     *               allowed for the login id.
     * lang       -- Language of the file name per IANA[12] registry.
     * media-type -- MIME media-type of file contents per IANA registry.
     * charset    -- Character set per IANA registry (if not UTF-8)
	 *
	 * @param mlsxEntry
     * @throws RemoteDataException
     */
    public RemoteFileInfo(MlsxEntry mlsxEntry) throws RemoteDataException {
    	
    	this.name = StringUtils.removeEnd(mlsxEntry.getFileName(), "/");
    	this.name = FilenameUtils.getName(this.name);
    	
    	if (mlsxEntry.get(MlsxEntry.TYPE).equals(MlsxEntry.TYPE_FILE))
    		this.fileType = FILE_TYPE;
    	else if (mlsxEntry.get(MlsxEntry.TYPE).equals(MlsxEntry.TYPE_DIR) || 
    			mlsxEntry.get(MlsxEntry.TYPE).equals(MlsxEntry.TYPE_PDIR) || 
    			mlsxEntry.get(MlsxEntry.TYPE).equals(MlsxEntry.TYPE_CDIR))
    		this.fileType = DIRECTORY_TYPE;
    	else 
    		this.fileType = UNKNOWN_TYPE;
    	
    	String sSize = mlsxEntry.get(MlsxEntry.SIZE);
    	if (sSize == null) {
    		sSize = mlsxEntry.get("sizd");
    	}
    	this.size = Long.valueOf(sSize);
    	String tmp = mlsxEntry.get(MlsxEntry.MODIFY);
    	this.lastModified = timeValToDate(tmp);
		if (this.lastModified == null) {
			this.lastModified = new Date(0L);
		}
		
    	this.owner = mlsxEntry.get(MlsxEntry.UNIX_OWNER);
    	if (this.owner == null) {
    		this.owner = mlsxEntry.get(MlsxEntry.UNIX_UID);
    	}
    	
    	try {
    		String token = mlsxEntry.get(MlsxEntry.UNIX_MODE);
        	if (StringUtils.length(token) == 4) {
        		token = token.substring(1);
        	}
        	
        	token = formatUnixListReply(token, (fileType == DIRECTORY_TYPE));
        	
        	for(int i=1;i<=9;i++) {
                if (token.charAt(i) != '-') {
                    mode += 1 << (9 - i);
                }
            }
        	
        } catch (IndexOutOfBoundsException e) {
            throw new RemoteDataException("Could not parse access permission bits");
        }
    }
    
    public RemoteFileInfo(File file) throws RemoteDataException 
    {
    	this.name = file.getName();
    	
    	if (file.isFile())
    		this.fileType = FILE_TYPE;
    	else if (file.isDirectory())
    		this.fileType = DIRECTORY_TYPE;
    	else 
    		this.fileType = UNKNOWN_TYPE;
    	
    	this.size = file.length();
    	
    	this.mode = 0;
    	if (file.canRead()) {
    		mode += 4;
    	}
    	if (file.canWrite()) {
    		mode += 2;
    	}
    	if (file.canExecute()) {
    		mode += 1;
    	}
    	
    	this.lastModified = new Date(file.lastModified());
    	
    	this.owner = UNKNOWN_STRING;
    }
    
//    public RemoteFileInfo(SftpFile sftpFile) throws RemoteDataException {
//    	this.name = sftpFile.getFilename();
//    	
//    	if (sftpFile.isFile())
//    		this.fileType = FILE_TYPE;
//    	else if (sftpFile.isDirectory())
//    		this.fileType = DIRECTORY_TYPE;
//    	else if (sftpFile.isLink())
//    		this.fileType = SOFTLINK_TYPE;
//    	else 
//    		this.fileType = UNKNOWN_TYPE;
//    	
//    	FileAttributes atts = sftpFile.getAttributes();
//    	
//    	this.size = atts.getSize().longValue();
//    	
//    	try {
//    		String token = atts.getPermissionsString();
//        	
//            for(int i=1;i<=9;i++) {
//                if (token.charAt(i) != '-') {
//                    mode += 1 << (9 - i);
//                }
//            }
//        } catch (IndexOutOfBoundsException e) {
//            throw new RemoteDataException("Could not parse access permission bits");
//        }
//    }
    
    public RemoteFileInfo(CollectionAndDataObjectListingEntry irodsEntry) 
    throws RemoteDataException 
    {
    	this.name = irodsEntry.getNodeLabelDisplayValue();
    	this.owner = irodsEntry.getOwnerName();
    	this.lastModified = irodsEntry.getModifiedAt();
    	
    	if (irodsEntry.isDataObject())
    		this.fileType = FILE_TYPE;
    	else 
    		this.fileType = DIRECTORY_TYPE;
    		
    	this.size = irodsEntry.getDataSize();
    	
    	try {
    		String token = "-r--r-----";
    		
            for(int i=1;i<=9;i++) {
                if (token.charAt(i) != '-') {
                    mode += 1 << (9 - i);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RemoteDataException("Could not parse access permission bits");
        }
    }
    
    public RemoteFileInfo(IRODSFile irodsFile) 
    throws RemoteDataException 
    {
    	this.name = irodsFile.getName();
    	this.owner = UNKNOWN_STRING;
    	this.lastModified = new Date(irodsFile.lastModified());
    	
    	if (irodsFile.isFile())
    		this.fileType = FILE_TYPE;
    	else 
    		this.fileType = DIRECTORY_TYPE;
    		
    	this.size = irodsFile.length();
    	
    	try {
    		String token = "-r--r-----";
    		
            for(int i=1;i<=9;i++) {
                if (token.charAt(i) != '-') {
                    mode += 1 << (9 - i);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RemoteDataException("Could not parse access permission bits");
        }
    }
    
//    public RemoteFileInfo(SFTPv3DirectoryEntry sftpEntry)
//    {
//    	this.name = sftpEntry.filename;
//    	this.owner = UNKNOWN_STRING;
//    	
//    	this.lastModified = new Date(sftpEntry.attributes.atime == null ? sftpEntry.attributes.mtime * 1000 : sftpEntry.attributes.atime * 1000);
//    	
//    	if (sftpEntry.attributes.isRegularFile())
//    		this.fileType = FILE_TYPE;
//    	else if (sftpEntry.attributes.isSymlink())
//    		this.fileType = SOFTLINK_TYPE;
//    	else 
//    		this.fileType = DIRECTORY_TYPE;
//    		
//    	this.size = sftpEntry.attributes.size;
//    	
//    	this.mode = sftpEntry.attributes.permissions;
//    }
    
    public RemoteFileInfo(String filename, SftpFileAttributes sftpEntry)
    {
    	this.name = filename;
    	this.owner = UNKNOWN_STRING;
    	
    	this.lastModified = sftpEntry.getModifiedDateTime();
    	
    	if (sftpEntry.isFile())
    		this.fileType = FILE_TYPE;
    	else if (sftpEntry.isLink())
    		this.fileType = SOFTLINK_TYPE;
    	else 
    		this.fileType = DIRECTORY_TYPE;
    		
    	this.size = sftpEntry.getSize().longValue();
    	
    	this.mode = sftpEntry.getPermissions().intValue();
    }
    
    public RemoteFileInfo(SftpFile file) throws RemoteDataException
	{
    	this.name = file.getFilename();
    	this.owner = UNKNOWN_STRING;
    	
    	try
		{
			this.lastModified = file.getAttributes().getModifiedDateTime();
			if (file.isFile())
	    		this.fileType = FILE_TYPE;
	    	else if (file.isLink())
	    		this.fileType = SOFTLINK_TYPE;
	    	else 
	    		this.fileType = DIRECTORY_TYPE;
	    		
	    	this.size = file.getAttributes().getSize().longValue();
	    	
	    	this.mode = file.getAttributes().getPermissions().intValue();
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to parse file information for " + file.getFilename());
		}
	}
    
//    public RemoteFileInfo(ListBlobItem blobItem) throws RemoteDataException
//	{
//    	this.name = FilenameUtils.getName(blobItem.getUri().getPath());
//    	this.owner = UNKNOWN_STRING;
//    	String pemString = "drwx------";
//    	try
//		{
//    		if (blobItem instanceof CloudBlobDirectory) 
//    		{
//    			BlobContainerProperties props = ((CloudBlobDirectory)blobItem).getContainer().getProperties();
//    			this.lastModified = props.getLastModified();
//    			this.fileType = DIRECTORY_TYPE;
//    			this.size = 0;
//			}
//    		else
//    		{
//    			BlobProperties props = ((CloudBlob)blobItem).getProperties();
//    			this.lastModified = props.getLastModified();
//    			this.fileType = FILE_TYPE;
//    			this.size = props.getLength();
//    		}
//    		
//    		try {
//                for(int i=1;i<=9;i++) {
//                    if (pemString.charAt(i) != '-') {
//                        mode += 1 << (9 - i);
//                    }
//                }
//            } catch (IndexOutOfBoundsException e) {
//                throw new RemoteDataException("Could not parse access permission bits");
//            }
//		}
//		catch (Exception e)
//		{
//			throw new RemoteDataException("Failed to parse file information for " + name);
//		}
//	}

	public RemoteFileInfo(StorageMetadata storageMetadata) throws RemoteDataException
	{
		this.name = FilenameUtils.getName(storageMetadata.getName());
    	this.owner = UNKNOWN_STRING;
    	String pemString = "drwx------";
    	try
		{
    		this.lastModified = storageMetadata.getLastModified();
			
    		if (storageMetadata.getType() == StorageType.RELATIVE_PATH || storageMetadata.getType() == StorageType.FOLDER)
    		{
    			this.fileType = DIRECTORY_TYPE;
    			this.size = 0;
			}
    		else
    		{
    			this.fileType = FILE_TYPE;
    			this.size = ((BlobMetadata)storageMetadata).getContentMetadata().getContentLength();
    		}
    		
    		try 
    		{
    			for(int i=1;i<=9;i++) {
                    if (pemString.charAt(i) != '-') {
                        mode += 1 << (9 - i);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                throw new RemoteDataException("Could not parse access permission bits");
            }
		}
		catch (Exception e)
		{
			throw new RemoteDataException("Failed to parse file information for " + name);
		}
	}

	/**
     * Given a line of reply received as the result of "LIST" command,
     * this method will set all the attributes(name,size,time,date and file type)
     * of the named file. This method requires the reply to be in 
     * FTP server format, corresponding to either Unix System V or 
     * Berkeley (BSD) output of 'ls -l'. For example,
     * <pre>drwxr-xr-x   2      guest  other  1536  Jan 31 15:15  run.bat</pre>
     *  or
     * <pre>-rw-rw-r--   1      globus    117579 Nov 29 13:24 AdGriP.pdf</pre>
     * If the entry corresponds to a device file, only the file type 
     * will be set and the other parameters will be set to UNKNOWN.
     * 
     * @param     reply reply of FTP server for "dir" command.
     * @exception FTPException if unable to parse the reply
     */
    //protected void parseUnixListReply(String reply) 
    public void parseUnixListReply(String reply) throws RemoteDataException 
    {
        if (reply == null) return;
        
        StringTokenizer tokens = new StringTokenizer(reply);
        String token, previousToken;
        
        int numTokens = tokens.countTokens();
        
        if (numTokens < 8) {
            throw new RemoteDataException(
                           "Invalid number of tokens in the list reply [" + 
                                   reply + "]");
        }
        
        token = tokens.nextToken();
        
        // permissions
        switch( token.charAt(0) ) {
        case 'd':
            setFileType(DIRECTORY_TYPE); break;
        case '-':
            setFileType(FILE_TYPE); break;
        case 'l':
            setFileType(SOFTLINK_TYPE); break;
        case 'c':
        case 'b':
            // do not try to parse device entries;
            // they aren't important anyway
            setFileType(DEVICE_TYPE); 
            return;
        default:
            setFileType(UNKNOWN_TYPE);
        }
        
        try {
            for(int i=1;i<=9;i++) {
                if (token.charAt(i) != '-') {
                    mode += 1 << (9 - i);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RemoteDataException("Could not parse access permission bits");
        }

        
        // ??? can ignore
        tokens.nextToken();
        
        // next token is the owner
        setOwner(tokens.nextToken());
        
        // In ls from System V, next token is the group
        // In ls from Berkeley (BSD), group token is missing
        previousToken = tokens.nextToken();
        
        // size
        token = tokens.nextToken();
        
        /*
         * if the group is missing this will try to parse the date field
         * as an integer and will fail. if so, then the previous field is the size field
         * and the current token is part of the date. 
         */
        try {
            setSize( Long.parseLong(token) );
            token = null;
        } catch(NumberFormatException e) {
            // this might mean that the group is missing
            // and this token is part of date.
            try {
                setSize( Long.parseLong(previousToken) );
            } catch(NumberFormatException ee) {
                throw new RemoteDataException(
                		"Invalid size number in the ftp reply [" + 
                        previousToken + ", " + token + "]");
            }
        }
        
        // date - two fields together
        if (token == null) {
            token = tokens.nextToken();
        }
        String month = token;
        setDate(token + " " + tokens.nextToken());
        
        //next token is either date or time
        token = tokens.nextToken();
        this.setTime(token);
        
        SimpleDateFormat oldSDF = new SimpleDateFormat("MMM d yyyy");
        SimpleDateFormat newSDF = new SimpleDateFormat("MMM d HH:mm");
        SimpleDateFormat currentSDF = new SimpleDateFormat("MMM d");
        try {
        	setLastModified(oldSDF.parse(getDate() + " " + getTime()));
        } catch (Exception e) {
        	try {
        		setLastModified(newSDF.parse(getDate() + " " + getTime()));
        	} catch (Exception e1) {
        		try { setLastModified(currentSDF.parse(getDate())); } catch (ParseException e2) {}
        	}
        }
        
        // this is to handle spaces in the filenames
        // as well filenames with dates in them
        int ps = reply.indexOf(month);
        if (ps == -1) {
            // this should never happen
            throw new RemoteDataException("Could not find date token");
        } else {
            ps = reply.indexOf(this.time, ps+month.length());
            if (ps == -1) {
                // this should never happen
                throw new RemoteDataException("Could not find time token");
            } else {
                this.setName(reply.substring(1+ps+this.time.length()));
            }
        }
    }
    
    public String formatUnixListReply(String unixMode, boolean isDirectory) throws RemoteDataException 
    {
    	String unixListString = isDirectory ? "d" : "-";
    	if (StringUtils.length(unixMode) == 4) {
    		unixMode = StringUtils.substring(unixMode, 1);
    	}
    	
    	for (int i=0; i<3; i++) {
    		unixListString += formatPermissionDigit(unixMode.charAt(i));
    	}
    	return unixListString;
    }
    
    private String formatPermissionDigit(char digit) {
    	if (digit == '7') {
    		return "rwx";
    	} else if (digit == '6') {
    		return "rw-";
    	} else if (digit == '5') {
    		return "r-x";
    	} else if (digit == '4') {
    		return "r--";
    	} else if (digit == '3') {
    		return "-wx";
    	} else if (digit == '2') {
    		return "-w-";
    	} else if (digit == '1') {
    		return "--x";
    	} else {
    		return "---";
    	}
    }
    
    // --------------------------------
    
    /**
     * Sets the file size.
     *
     * @param size size of the file
     */
    public void setSize(long size) {
        this.size = size;
    }
    
    /**
     * Sets the file name.
     *
     * @param name name of the file.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Sets the file date.
     * 
     * @param date date of the file.
     */
    public void setDate(String date) {
        this.date = date;
    }
    
    /**
     * Sets modification time of the file.
     * 
     * @param time time of the file.
     */
    public void setTime(String time) {
        this.time = time;
    }
    
    /**
     * Sets the file type.
     *
     * @param type one of the file types, 
     *             e.g. FILE_TYPE, DIRECTORY_TYPE
     *             
     */
    public void setFileType(byte type) {
        this.fileType = type;
    }
    
    // ---------------------------------
    
    /**
     * Returns size of the file.
     *
     * @return size of the file in bytes
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Returns name of the file.
     * 
     * @return name of the file.
     */
    public String getName() {
        return name;
    }
    
    /**
	 * @return the owner
	 */
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	/**
     * Returns date of the file.
     * 
     * @return date of the file.
     */
    public String getDate() {
        return date;
    }
    
    /**
	 * @return the lastModified
	 */
	public Date getLastModified()
	{
		return lastModified;
	}

	/**
	 * @param lastModified the lastModified to set
	 */
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	/**
     * Returns modification time of the file.
     * 
     * @return time of the file.
     */
    public String getTime() {
        return time;
    }
    
    /**
     * Tests if this file is a file.
     *
     * @return true if this represents a file,
     *         otherwise, false.
     */
    public boolean isFile() {
        return (fileType == FILE_TYPE);
    }
    
    /**
     * Tests if this file is a directory.
     * 
     * @return true if this reprensets a directory,
     *         otherwise, false.
     */
    public boolean isDirectory() {
        return (fileType == DIRECTORY_TYPE);
    }
    
    /**
     * Tests if this file is a softlink.
     * 
     * @return true if this reprensets a softlink,
     *         otherwise, false.
     */
    public boolean isSoftLink() {
        return (fileType == SOFTLINK_TYPE);
    }
    
    /**
     *  Tests if this file is a device.
     */
    
    public boolean isDevice() {
        return (fileType == DEVICE_TYPE);
    }
    
    public PermissionType getPermissionType()
    {
    	int pem = Integer.valueOf(getModeAsString());
    	if (pem > 1000) {
    		pem = pem % 1000;
    	}
    	if (pem > 100) {
    		pem = pem / 100;
    	}
		
		for (PermissionType type: PermissionType.values()) {
			if (type.getUnixValue() == pem) {
				return type;
			}
		}
		
		return PermissionType.NONE;
    }

    public int getMode() {
      return mode;
    }

    public String getModeAsString() {
      StringBuffer modeStr = new StringBuffer();
      for(int j=2;j>=0;j--) {
          int oct = 0;
          for(int i=2;i>=0;i--) {
              if ((mode & (1 << j*3+i)) != 0) {
                  oct += (int)Math.pow(2,i);
              }
          }
          modeStr.append(String.valueOf(oct));
      }
      return modeStr.toString();
    }

    public boolean userCanRead() {
      return ((mode & (1 << 8)) != 0);
    }

    public boolean userCanWrite() {
      return ((mode & (1 << 7)) != 0);
    }

    public boolean userCanExecute() {
      return ((mode & (1 << 6)) != 0);
    }

    public boolean groupCanRead() {
      return ((mode & (1 << 5)) != 0);
    }

    public boolean groupCanWrite() {
      return ((mode & (1 << 4)) != 0);
    }

    public boolean groupCanExecute() {
      return ((mode & (1 << 3)) != 0);
    }

    public boolean allCanRead() {
      return ((mode & (1 << 2)) != 0);
    }

    public boolean allCanWrite() {
      return ((mode & (1 << 1)) != 0);
    }

    public boolean allCanExecute() {
      return ((mode & (1 << 0)) != 0);
    }
  
    // --------------------------------
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("FileInfo: ");
        buf.append(getName() + " ");
        buf.append(getSize() + " ");
        buf.append(getDate() + " ");
        buf.append(getTime() + " ");
        
        switch( fileType  ) {
        case DIRECTORY_TYPE:
            buf.append("directory");
            break;
        case FILE_TYPE:
            buf.append("file");
            break;
        case SOFTLINK_TYPE:
            buf.append("softlink");
            break;
        default:
            buf.append("unknown type");
        }
        buf.append(" "+getModeAsString());
        
        return buf.toString();
    }

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(RemoteFileInfo o)
	{
		if (o == null) {
			throw new ClassCastException("Cannot compare RemoteFileInfo with null object.");
		} else { 
			return this.name.compareToIgnoreCase(((RemoteFileInfo)o).name);
		}
	}
	
	private Date timeValToDate(String timeVal)
	{
		SimpleDateFormat timeValFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		
		try
		{
			return timeValFormat.parse(timeVal);
		}
		catch (Exception e)
		{
			return null;
		}
	}
    
}
