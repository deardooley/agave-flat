/**
 * 
 */
package org.iplantc.service.io.manager.actions;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;

/**
 * @author dooley
 *
 */
public class MkdirOperation extends FileOperation<DataTask> {

	public MkdirOperation(DataTask entity) {
		super(entity);
	}

	@Override
	public void run() throws SystemUnavailableException,
			SystemUnknownException, ClosedByInterruptException,
			DomainException, DependencyException, PermissionException {
		
//		
//		String sourcePath = getTask().getSourcePath();
//		
//		String newDirectoryName = getTask().getDestPath();
//		
//		if (StringUtils.isEmpty(newDirectoryName)) {
//			throw new DomainException(
//            		"No path value provided. Please provide the path of "
//							+ "the new directory to create. Paths may be absolute or "
//							+ "relative to the path given in the url.");
//		}
//		
//		String newDirectoryPath;
//		if (StringUtils.isEmpty(sourcePath)) {
//			newDirectoryPath = getEntity().getDestPath();
//		} else {
//			newDirectoryPath = StringUtils.removeEnd(sourcePath, "/") + File.separator + newDirectoryName;
//		}
//		
//		PermissionManager pm = new PermissionManager();
//		
//		if (!pm.canWrite(getRemoteDataClient().resolvePath(newDirectoryPath))) {
//			throw new PermissionException("User does not have access to create the directory" + newDirectoryPath);
//		}
//		
//		String pemUser = StringUtils.isEmpty(owner) ? createdBy : owner;
//		boolean dirCreated = false;
//        
//		
//		if (getRemoteDataClient.doesExist(newDirectoryPath)) {
//			if (remoteDataClient.isDirectory(newDirectoryPath)) {
//				message = "Directory " + newdir + " already exists";
////				getResponse().setEntity(new IplantSuccessRepresentation(message));
////	            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
////	            return;
//			} else {
//				message = "A file already exists at " + newdir;
//				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, message);
//			}
//		} 
//        else {
//		    if (remoteDataClient.mkdirs(newdir, pemUser)) {
//				message = "Mkdir success";
//				dirCreated = true;
//			} 
//			else {
//                throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
//                		"Failed to create directory " + newdir);
//            }
//		}
//
//		try {
//			logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(newdir));
//		} catch(Exception e) {}
//
//		if (logicalFile == null) {
//			logicalFile = new LogicalFile();
//			logicalFile.setNativeFormat(LogicalFile.DIRECTORY);
//			logicalFile.setSourceUri(null);
//			logicalFile.setPath(remoteDataClient.resolvePath(newdir));
//			logicalFile.setName(FilenameUtils.getName(logicalFile.getPath()));
//			logicalFile.setSystem(system);
//			logicalFile.setOwner(pemUser);
//			logicalFile.setInternalUsername(internalUsername);
//			logicalFile.setLastUpdated(new Date());
//			LogicalFileDao.persist(logicalFile);
//			
//			logicalFile.addContentEvent(new FileEvent(FileEventType.CREATED, 
//					"New directory created at " + logicalFile.getPublicLink(), 
//					getAuthenticatedUsername()));
//			LogicalFileDao.persist(logicalFile);
//		}
//		else
//		{
//			logicalFile.setLastUpdated(new Date());
//			logicalFile.addContentEvent(new FileEvent(FileEventType.CREATED, 
//					"Directory recreated at " + logicalFile.getPublicLink(), 
//					getAuthenticatedUsername()));
//			LogicalFileDao.persist(logicalFile);
//		}
//		
//		if (dirCreated) {
//		    setStatus(Status.SUCCESS_CREATED);
//		    return new IplantSuccessRepresentation(logicalFile.toJSON());
//		} else {
//		    return new IplantSuccessRepresentation(message, "{}");
//		}
	}

}
