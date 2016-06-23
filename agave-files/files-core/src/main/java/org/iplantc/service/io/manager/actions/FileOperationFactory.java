package org.iplantc.service.io.manager.actions;

import static org.iplantc.service.io.model.enumerations.FileOperationType.*;

import org.iplantc.service.io.exceptions.FileProcessingException;
import org.iplantc.service.io.model.enumerations.FileOperationType;
import org.iplantc.service.systems.model.RemoteSystem;

public class FileOperationFactory {

	public FileOperationFactory() {}
	
//	public FileOperation getFileOperation(RemoteSystem system, String path, String createdBy, FileOperationType operation) {
//		
//		if (operation == MKDIR) {
//			return new MkdirOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == COPY) {
//			return new CopyOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == RENAME) {
//			return new RenameOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == MOVE) {
//			return new MoveOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == LIST) {
//			return new ListOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == TOUCH) {
//			return new TouchOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == INDEX) {
//			return new IndexOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == REMOVE) {
//			return new RemoveOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else if (operation == CHECKSUM) {
//			return new ChecksumOperation(RemoteSystem system, String path, String createdBy);
//		}
//		else {
//			throw new FileProcessingException("Invalid file operation " + operation.name());
//		}
//	}

}
