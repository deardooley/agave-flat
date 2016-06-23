package org.iplantc.service.io.manager.actions;

import java.io.IOException;

import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class ImportAction extends AbstractAction {
	
	ActionContext data = null;
	public ImportAction(ActionContext context) {
		super(context);
	}

	@Override
	public LogicalFile doAction() throws RemoteDataException, IOException {
		return null;
		// parse the form to get the job specs

//    	Form form = Request.getCurrent().getEntityAsForm();
//		Map<String,String> pTable = form.getValuesMap();
//
//		// enable the user to specify the input formatting. Check against our internally
//		// supported formatting
//		// ignore user-defined formatting for now. Let the service auto-discover it
//		String format = pTable.get("fileType");
//
//		FileTransformProperties transformProperties = new FileTransformProperties();
//		FileTransform transform = null;
//
//		if (!ServiceUtils.isValid(format)) {
////			getResponse().setEntity(new IplantErrorRepresentation("File format " + format + " is not supported."));
////            getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
////            return;
//			format = "raw";
//			log.error("File uploaded with no format type. Defaulting to raw");
//		} else {
//			// check against xml formats
//			format = format.toLowerCase();
//			try {
//				transform = transformProperties.getTransform(format);
//
//				if (transform == null) {
//					if (format.equalsIgnoreCase("raw"))
//						format = "raw";
//					else
//						throw new TransformException("Format " + format + " is not recognized");
//				} else {
//					format = transform.getId();
//				}
//			} catch (TransformException e) {
//				format = "raw";
//				log.error("File uploaded with unsupported format type. Defaulting to raw");
//			}
//		}
//
//		String sCallback = pTable.get("notifications");
//		if (StringUtils.isEmpty(sCallback)) {
//			sCallback = pTable.get("callbackURL");
//		}
//
//		if (StringUtils.isNotEmpty(sCallback) && !ServiceUtils.isValidCallback(sCallback)) {
//			getResponse().setEntity(new IplantErrorRepresentation(
//					"Invalid notification url. notification must be a valid http address", prettyPrint));
//            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
//            return;
//		}
//
//		String name = pTable.get("fileName");
//		String sUri = pTable.get("urlToIngest");
//		
//		if (!StringUtils.isBlank(sUri)) 
//		{
//			try 
//			{
//				// resolve the uri
//				URI uri = new URI(StringUtils.trim(sUri));
//				if (StringUtils.isBlank(name)) {
//					// pull the file name from the url
//					name = FilenameUtils.getName(uri.getPath());
//
//					// make sure the name is valid
//					if (StringUtils.isEmpty(name)) {
//						name = "unknown";
//					}
//				}
//
//				name = StringUtils.trim(name);
//
//				// create a reference to the uri
//				String remotePath = null;
//				 if (StringUtils.isEmpty(data.path)) {
//                	remotePath = name;
//                } else {
//                	remotePath = data.path + File.separator + name;
//                }
//
//                remotePath = remotePath.replaceAll("//", "/");
//
//				LogicalFile logicalFile = null;
//
//                try {
//                    logicalFile = LogicalFileDao.findBySystemAndPath(data.system, data.remoteDataClient.resolvePath(remotePath));
//                } catch(Exception e) {}
//
////                PermissionManager pm;
////                if (remoteDataClient instanceof RemoteDataClientPermissionProvider) {
////                    pm = new PermissionManager(((RemoteDataClientPermissionProvider)remoteDataClient), logicalFile, username);
////                } else {
////                    pm = new PermissionManager(null, logicalFile, username);
////                }
//
//                PermissionManager pm = new PermissionManager(data.system, data.remoteDataClient, logicalFile, data.username);
//
//				if (logicalFile == null) {
//
//					if (!StringUtils.isEmpty(data.owner) && !data.owner.equals(data.username) && !ServiceUtils.isAdmin(data.username)) {
//						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
//								"User does not have access to modify " + remotePath);
//					}
//
//					logicalFile = new LogicalFile();
//					logicalFile.setSystem(data.system);
//					logicalFile.setInternalUsername(data.internalUsername);
//					logicalFile.setName(name);
//					logicalFile.setSourceUri(uri.toString());
//					logicalFile.setNativeFormat(format);
//					logicalFile.setPath(data.remoteDataClient.resolvePath(remotePath));
//					logicalFile.setOwner(data.owner == null ? data.username : data.owner);
//					logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
//					LogicalFileDao.persist(logicalFile);
//					logicalFile.addEvent(StagingTaskStatus.STAGING_QUEUED);
//					LogicalFileDao.persist(logicalFile);
//				} else {
//					if (!pm.canRead(logicalFile.getPath())) {
//						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
//								"User does not have access to modify " + data.path);
//					}
//					logicalFile.setSystem(data.system);
//					logicalFile.setInternalUsername(data.internalUsername);
//					logicalFile.setNativeFormat(format);
//					logicalFile.setSourceUri(uri.toString());
//					logicalFile.setLastUpdated(new Date());
//					logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
//					logicalFile.addEvent(StagingTaskStatus.STAGING_QUEUED);
//					LogicalFileDao.persist(logicalFile);
//				}
//
//				// set a callback event if desired
//				if (StringUtils.isNotBlank(sCallback)) {
//					logicalFile.addNotification(StagingTaskStatus.STAGING_COMPLETED.name(), sCallback);
//				}
//
//				// add the logical file to the staging queue
//				QueueTaskDao.enqueueStagingTask(logicalFile);
//
//				//if (ServiceUtils.isValid(eventId)) EventClient.update(Settings.EVENT_API_KEY, eventId, EventClient.EVENT_STATUS_QUEUED);
//        		LogicalFileDao.updateTransferStatus(logicalFile, StagingTaskStatus.STAGING_QUEUED);
//
//				getResponse().setEntity(new IplantSuccessRepresentation(logicalFile.toJSON(), prettyPrint));
//                getResponse().setStatus(Status.SUCCESS_ACCEPTED);
//			} catch (ResourceException e) {
//				getResponse().setStatus(e.getStatus());
//				getResponse().setEntity(new IplantErrorRepresentation(e.getMessage(), prettyPrint));
//				return;
//			} catch (Exception e) {
//				getResponse().setEntity(new IplantErrorRepresentation(e.getMessage(), prettyPrint));
//                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//                return;
//			} finally {
//				try {data.remoteDataClient.disconnect();} catch (Exception e) {}
//			}
//		} else {
//			getResponse().setEntity(new IplantErrorRepresentation("No valid URL specified", prettyPrint));
//            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//            try {data.remoteDataClient.disconnect();} catch (Exception e) {}
//            return;
//		}
//    }
	}

}
