/**
 * 
 */
package org.iplantc.service.io.permissions;


/**
 * Management class for file and folder permissions. This class determines
 * whether a user other than the owner has permission to view/modify a 
 * file item.
 *  
 * @author dooley
 *
 */
public class PermissionManager2 {

//	private String username;
//	
//	public PermissionManager2(String username) {
//		this.username = username;
//	}
//	
//	public boolean canRead(LogicalFile file) {
//		
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isRead();
//			}
//		}
//	}
//	
//	public boolean canRead(String path, boolean isDirectory) {
//		// yes to ownership
//		if (path.startsWith(username + File.separator)) return true;
//		
//		LogicalFile file = LogicalFileDao.getByPath(path);
//		if (file == null) {
//			// see if parent folder is shared
//			if (isDirectory) return false;
//			
//			try {
//				String parentPath = new File("/", path).getParentFile().getCanonicalPath().substring(1);
//				file = LogicalFileDao.getByPath(parentPath);
//				if (file == null) {
//					return false;
//				}
//			} catch (IOException e) {
//				return false;
//			}
//		}
//		
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isRead();
//			}
//		}
//	}
//	
//	public boolean canWrite(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isWrite();
//			}
//		}
//	}
//	
//	public boolean canWrite(String path, boolean isDirectory) {
//		// yes to ownership
//		if (path.startsWith(username + File.separator)) return true;
//		
//		LogicalFile file = LogicalFileDao.getByPath(path);
//		if (file == null) {
//			// see if parent folder is shared
//			if (isDirectory) return false;
//			
//			try {
//				String parentPath = new File("/", path).getParentFile().getCanonicalPath().substring(1);
//				file = LogicalFileDao.getByPath(parentPath);
//				if (file == null) {
//					return false;
//				}
//			} catch (IOException e) {
//				return false;
//			}
//		}
//		
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isWrite();
//			}
//		}
//	}
//	
//	public boolean canExecute(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isExecute();
//			}
//		}
//	}
//	
//	public boolean canExecute(String path, boolean isDirectory) {
//		// yes to ownership
//		if (path.startsWith(username + File.separator)) return true;
//		
//		LogicalFile file = LogicalFileDao.getByPath(path);
//		if (file == null) {
//			// see if parent folder is shared
//			if (isDirectory) return false;
//			
//			try {
//				String parentPath = new File("/", path).getParentFile().getCanonicalPath().substring(1);
//				file = LogicalFileDao.getByPath(parentPath);
//				if (file == null) {
//					return false;
//				}
//			} catch (IOException e) {
//				return false;
//			}
//		}
//		
//		if (file.getOwner().equals(username)) {
//			return true;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				return false;
//			} else {
//				return pem.isExecute();
//			}
//		}
//	}
//	
//	
//	public void addReadPermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				pem = new SharePermission(file, username, SharePermission.READ);
//				SharePermissionDao.persist(pem);
//			} else {
//				if (!pem.isRead()) {
//					pem.setRead(true);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
//	
//	public void addWritePermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				pem = new SharePermission(file, username, SharePermission.WRITE);
//				SharePermissionDao.persist(pem);
//			} else {
//				if (!pem.isWrite()) {
//					pem.setWrite(true);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
//	
//	public void addExecutePermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				pem = new SharePermission(file, username, SharePermission.EXECUTE);
//				SharePermissionDao.persist(pem);
//			} else {
//				if (!pem.isExecute()) {
//					pem.setExecute(true);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
//	
//	public void removeReadPermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem == null) {
//				pem = new SharePermission(file, username, SharePermission.READ);
//				SharePermissionDao.persist(pem);
//			} else {
//				if (pem.isRead()) {
//					pem.setRead(false);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
//	
//	public void removeWritePermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem != null) {
//				if (pem.isWrite()) {
//					pem.setWrite(false);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
//	
//	public void removeExecutePermission(LogicalFile file) {
//		if (file.getOwner().equals(username)) {
//			return;
//		} else {
//			SharePermission pem = file.getPermissionForUser(username);
//			if (pem != null) {
//				if (pem.isExecute()) {
//					pem.setExecute(false);
//					SharePermissionDao.persist(pem);
//				}
//			}
//		}
//	}
}
