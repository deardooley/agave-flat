package org.iplantc.service.io.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.model.RemoteSystem;

public class LogicalFileDao {
	private static final Logger log = Logger.getLogger(LogicalFileDao.class);
	
	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		String tenantId = TenancyHelper.getCurrentTenantId();
		session.enableFilter("logicalFileTenantFilter").setParameter("tenantId", tenantId);
		//session.enableFilter("logicalFileEventTenantFilter").setParameter("tenantId", tenantId);
		return session;
	}
	
	public static LogicalFile findById(long id) 
	{	
		try {
// 			Session session = getSession();
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			LogicalFile file = (LogicalFile) session.get(LogicalFile.class, id);
			session.flush();
			return file;
		} 
		catch (ObjectNotFoundException e) 
		{
			return null;
		} 
		catch (HibernateException ex)
		{
			log.error("Failed to get logical file by id", ex);
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {log.error("Failed to rollback transaction", e);}
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}  
	}
	
//	public static LogicalFile findByPath(String path) {
//		
//		if (!ServiceUtils.isValid(path)) {
//			throw new HibernateException("Invalid path");
//		}
//		
//		try 
//		{
//			Session session = getSession();
//			
//			LogicalFile file = (LogicalFile) session
//				.createQuery("from LogicalFile where path = :path")
//				.setString("path", path)
//				.setMaxResults(1)
//				.uniqueResult();
//		
//			session.flush();
//			
//			return file;
//		}
//		catch (HibernateException ex)
//		{
//			log.error("Failed to get logical file by path", ex);
//			try
//			{
//				if (HibernateUtil.getSession().isOpen()) {
//					HibernateUtil.rollbackTransaction();
//				}
//			}
//			catch (Exception e) {log.error("Failed to rollback transaction", e);}
//			throw ex;
//		}
//		finally
//		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
//		}  
//	}
	
	public static void removeSubtree(LogicalFile file) 
	{
		if (file == null) {
			throw new HibernateException("No root node provided");
		}
		
		try 
		{
			Session session = getSession();
			
			String sql = "delete from logical_files where system_id = :systemid and BINARY path like :path";
			session.createSQLQuery(sql)
				.setLong("systemid", file.getSystem().getId())
				.setString("path", file.getPath() + "%")
				.executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			log.error("Failed to remove logical file subtree", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}  
	}
	
	public static LogicalFile findBySourceUrl(String sUrl) 
	{	
		if (!ServiceUtils.isValid(sUrl)) {
			throw new HibernateException("Invalid url");
		}
		
		try 
		{
			Session session = getSession();
			
			LogicalFile file = (LogicalFile) session
				.createSQLQuery("select * from logical_files where BINARY source = :url")
				.addEntity(LogicalFile.class)
				.setString("url", sUrl)
				.setMaxResults(1)
				.uniqueResult();
		
			session.flush();
			
			return file;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to find logical file by source url", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}

	public static void persist(LogicalFile file) throws HibernateException 
	{	
		if (file == null) {
			throw new HibernateException("Null file cannot be committed.");
		}
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			file.setLastUpdated(new Date());
			session.saveOrUpdate(file);
			session.flush();
		}
		catch (HibernateException ex)
		{
			log.error("Failed to persist logical file", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static void save(LogicalFile file) throws HibernateException 
	{		
		if (file == null) {
			throw new HibernateException("Null file cannot be committed.");
		}
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			file.setLastUpdated(new Date());
			session.save(file);
			session.flush();
		}
		catch (HibernateException ex)
		{
			log.error("Failed to save logical file", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static void remove(LogicalFile file) throws HibernateException {
		
		if (file == null) {
			throw new HibernateException("Null file cannot be deleted.");
		}
		
		try 
		{
			Session session = getSession();
			
			session.delete(file);
			session.flush();
		}
		catch (HibernateException ex)
		{
			log.error("Failed to delete logical file", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static List<LogicalFile> findByOwner(String username) {
		
		if (!ServiceUtils.isValid(username)) {
			throw new HibernateException("Invalid username");
		}
		
		try 
		{
			Session session = getSession();
			
			List<LogicalFile> files = session
					.createSQLQuery("select * from logical_files where BINARY owner = :username")
					.addEntity(LogicalFile.class)
					.setString("username",username)
	        		.list();
	        
			session.flush();
			
			return files;
		
		}
		catch (HibernateException ex)
		{
			log.error("Failed to find logical file by owner", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}  
	}

	private static void updateTransferStatus(LogicalFile file, String status) {
		
		if (file == null) {
			throw new HibernateException("Null file cannot be deleted.");
		}
		
		if (!ServiceUtils.isValid(status)) {
			throw new HibernateException("Invalid status");
		}
		
		file.setStatus(status);
		
		persist(file);
	}
	
	public static void updateTransferStatus(LogicalFile file, String status, String message, String createdBy) 
	{	
		if (file == null) {
			throw new HibernateException("Null file cannot be deleted.");
		}
		FileEventType eventType = FileEventType.valueOf(status);
		if (StringUtils.isEmpty(message)) {
			message = eventType.getDescription();
		}
		file.addContentEvent(new FileEvent(eventType, message, createdBy));
		updateTransferStatus(file, status);
	}
	
	public static void updateTransferStatus(LogicalFile file, StagingTaskStatus status, String createdBy) 
	{	
		updateTransferStatus(file,status.name(), null, createdBy);
	}
	
	public static void updateTransferStatus(LogicalFile file, TransformTaskStatus status, String createdBy) 
	{	
		updateTransferStatus(file,status.name(), null, createdBy);
	}
	
	public static void updateTransferStatus(RemoteSystem system, String path, String status) {
		
		if (!ServiceUtils.isValid(path)) {
			throw new HibernateException("Invalid url");
		}
		
		if (!ServiceUtils.isValid(status)) {
			throw new HibernateException("Invalid status");
		}
		
		LogicalFile file = findBySystemAndPath(system, path);
		
		if (file != null) {
			file.setStatus(status);
			file.setLastUpdated(new Date());
			persist(file);
		}
	}
	
	/**
	 * Returns all existing logical file records for the parents of the 
	 * current file/folder. There is no guarantee there are any entries
	 * for the parents. No check is done to determine where to stop, thus
	 * logical file records could be returned for paths outside of the 
	 * current system root if it had changed at some point.
	 *  
	 * @param logicalFile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<LogicalFile> findParents(RemoteSystem system, String path) 
	{
		if (StringUtils.isEmpty(path)) {
			throw new HibernateException("No path specified");
		} else if (StringUtils.equals(path, "/")) {
			return new ArrayList<LogicalFile>();
		}
		
		try 
		{
			Session session = getSession();
			
			String hql = "SELECT * FROM logical_files "
					+ "WHERE LOCATE(BINARY path, :path) = 1 and "
					+ "		 BINARY path <> :path and "  
					+ "		 BINARY native_format = :dirType and "
					+ "		 system_id = :systemId and "
					+ "		 tenant_id = :tenantid " 
					+ "ORDER BY path DESC";
			
			List<LogicalFile> files =  (List<LogicalFile>)session.createSQLQuery(hql)
				.addEntity(LogicalFile.class)
				.setString("path", path)
				.setString("dirType", LogicalFile.DIRECTORY)
				.setString("tenantid", TenancyHelper.getCurrentTenantId())
				.setLong("systemId", system.getId())
				.list();
			
			session.flush();
			
			return files;
			
		}
		catch (HibernateException ex)
		{
			log.error("Failed to get logical file parents", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Finds logical file reference corresponding to the immediate parent folder of the
	 * given logical file. If the parent does not have an entry, null will be returned.
	 *  
	 * @param logicalFile
	 * @return
	 */
	public static LogicalFile findParent(LogicalFile logicalFile) 
	{
		if (logicalFile == null) {
			throw new HibernateException("No file specified");
		}
		
		try 
		{
			Session session = getSession();
			
			String hql = "SELECT * FROM logical_files "
					   + "WHERE BINARY path = :path and "
					   + "		native_format = :dirtype and "
					   + "		 system_id = :systemid and "
					   + "		 tenant_id = :tenantid ";
			
			LogicalFile parent =  (LogicalFile) session.createSQLQuery(hql)
					.addEntity(LogicalFile.class)
					.setString("path", FilenameUtils.getFullPathNoEndSeparator(logicalFile.getPath()))
					.setString("dirtype", LogicalFile.DIRECTORY)
					.setLong("systemid", logicalFile.getSystem().getId())
					.setString("tenantid", TenancyHelper.getCurrentTenantId())
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			
			return parent;
			
		}
		catch (HibernateException ex)
		{
			log.error("Failed to get logical file parents", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Finds logical file reference corresponding to the closest parent folder of the
	 * given logical file. If there are no known parent entries, null will be returned.
	 *  
	 * @param logicalFile
	 * @return
	 */
	public static LogicalFile findClosestParent(RemoteSystem system, String path)
	{
		if (StringUtils.isEmpty(path)) {
			throw new HibernateException("No path specified");
		}
		
		if (system == null) {
			throw new HibernateException("No system specified");
		}
		
		try 
		{
			Session session = getSession();
			
			String hql = "SELECT * FROM logical_files "
					+ "WHERE LOCATE(BINARY path, :path) = 1 and "
					+ "		 BINARY path <> :path and "  
					+ "		 native_format = :dirtype and "
					+ "		 system_id = :systemid and "
					+ "		 tenant_id = :tenantid " 
					+ "ORDER BY path DESC";
			
			LogicalFile parent =  (LogicalFile) session.createSQLQuery(hql)
				.addEntity(LogicalFile.class)
				.setString("path", path)
				.setString("dirtype", LogicalFile.DIRECTORY)
				.setString("tenantid", TenancyHelper.getCurrentTenantId())
				.setLong("systemid", system.getId())
				.setMaxResults(1)
				.uniqueResult();
			
			session.flush();
			
			return parent;
			
		}
		catch (HibernateException ex)
		{
			log.error("Failed to get logical file parents", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

//	public static boolean parentsExist(LogicalFile logicalFile) {
//		return (StringUtils.split(logicalFile.getPath(), File.separatorChar).length == findParents(logicalFile).size());
//	}

//	public static void createParents(LogicalFile logicalFile) 
//	{
//		String[] tokens = StringUtils.split(logicalFile.getPath(), File.separatorChar);
//		String parentPath = "";
//		for (int i=0;i<tokens.length-1; i++) {
//			parentPath += File.separator + tokens[i];
//			LogicalFile newFile = new LogicalFile();
//			newFile.setName(tokens[i]);
//			newFile.setNativeFormat("folder");
//			newFile.setOwner(logicalFile.getOwner());
//			newFile.setSource("");
//			newFile.setPath(parentPath);
//			newFile.setStatus(StagingTaskStatus.STAGING_COMPLETED.name());
//            newFile.setSystem(logicalFile.getSystem());
//			LogicalFileDao.persist(newFile);
//		}
//	}

//	/**
//	 * Updates logical files after a rename operations. 
//	 * @param srcPath
//	 * @param srcSystemId
//	 * @param destPath
//	 * @param destSystemId
//	 */
//	public static void moveSubtreePath(String srcPath, Long srcSystemId, String destPath, Long destSystemId) 
//	{
//		if (StringUtils.isEmpty(srcPath)) {
//			throw new HibernateException("No original path specified");
//		}
//		
//		if (StringUtils.isEmpty(destPath)) {
//			throw new HibernateException("No new path specified");
//		}
//		
//		if (srcSystemId == null) {
//			throw new HibernateException("No source system id specified");
//		}
//		
//		if (destSystemId == null) {
//			throw new HibernateException("No destination system id specified");
//		}
//		
//		try {
//			Session session = getSession();
//			
//			// deleting logical files where the target already has a logical file entry
//			String deleteSql = "delete logical_files "
//					+ "where id in ("
//					+ "				select id "
//					+ "				from logical_files src inner join logical_files dest "
//					+ "					on dest.path = REPLACE(src.path, :srcpath, :destpath) and "
//					+ "					dest.system_id = :destsystemid and "
//					+ "					src.system_id = :srcsystemid and "
//					+ "					src.tenant_id = dest.tenant_id  "
//					+ "				where src.tenant_id = :tenantid and "
//					+ "					src.path like :srcpath"
//					+ "				)";
//					
//			session.createSQLQuery(deleteSql)
//			.setString("srcpath", srcPath)
//			.setString("destpath", destPath)
//			.setLong("srcsystemid", srcSystemId)
//			.setLong("destsystemid", destSystemId)
//			.setString("tenantid", TenancyHelper.getCurrentTenantId())
//			.executeUpdate();
//			
//			// update path of existing logical files where the is no corresponding destination logical_file
//			String sql = "update logical_files " + 
//					"set set last_udpated = CURRENT_TIMESTAMP, " + 
//					"	path = REPLACE(path, :srcpath, :destpath) " +  
//					"where system_id = :srcsystemid and " +  
//					"	path like :srcpathmatch and " +  
//					"	tenant_id = :tenantid and" +  
//					"	id not in ( " +  
//					"			select src.id " +
//					"			from logical_files src " + 
//					" 				inner join logical_files dest " +
//					" 					on dest.path = REPLACE(src.path, :srcpath, :destpath) and " +
//					"						dest.system_id = :destsystemid and " +
//					"						src.system_id = :srcsystemid and " +
//					"						src.tenant_id = dest.tenant_id " +
//					"			where src.tenant_id = :tenantid and " +
//					"				src.path like :srcpathmatch " +
//					"			)";
//			 
//			session.createSQLQuery(sql)
//				.setString("srcpath", srcPath)
//				.setString("destpath", destPath)
//				.setString("srcpathmatch", srcPath + "%")
//				.setString("destpathmatch", destPath + "%")
//				.setLong("srcsystemid", srcSystemId)
//				.setLong("destsystemid", destSystemId)
//				.setString("tenantid", TenancyHelper.getCurrentTenantId())
//				.executeUpdate();
//			session.flush();
//		}
//		catch (HibernateException ex)
//		{
//			log.error("Failed to update logical file parent path", ex);
//			
//			try
//			{
//				if (HibernateUtil.getSession().isOpen()) {
//					HibernateUtil.rollbackTransaction();
//				}
//			}
//			catch (Exception e) {}
//			
//			throw ex;
//		}
//		finally
//		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
//		}
//	}
	
	/**
	 * Finds logical files that would not be overwritten in a copy operation from 
	 * srcPath to destPath between the two systems.
	 *  
	 * @param srcPath
	 * @param srcSystemId
	 * @param destPath
	 * @param destSystemId
	 */
	@SuppressWarnings("unchecked")
	public static List<LogicalFile> findNonOverlappingChildren(String srcPath, Long srcSystemId, String destPath, Long destSystemId) 
	{
		if (StringUtils.isEmpty(srcPath)) {
			throw new HibernateException("No original path specified");
		}
		
		if (StringUtils.isEmpty(destPath)) {
			throw new HibernateException("No new path specified");
		}
		
		if (srcSystemId == null) {
			throw new HibernateException("No source system id specified");
		}
		
		if (destSystemId == null) {
			throw new HibernateException("No destination system id specified");
		}
		
		try {
			Session session = getSession();
			
			// update path of existing logical files where the is no corresponding destination logical_file
			String sql = "select * from logical_files " +
						"where system_id = :srcsystemid and " +  
						"	path like :srcpathmatch and " +
						"	path <> :srcpath and " +  
						"	tenant_id = :tenantid and" +  
						"	id not in ( " +  
						"			select src.id " +
						"			from logical_files src " + 
						" 				inner join logical_files dest " +
						" 					on dest.path = REPLACE(src.path, :srcpath, :destpath) and " +
						"						dest.system_id = :destsystemid and " +
						"						src.system_id = :srcsystemid and " +
						"						src.tenant_id = dest.tenant_id " +
						"			where src.tenant_id = :tenantid and " +
						"				src.path like :srcpathmatch " +
						"			)";
			
			List<LogicalFile> files = (List<LogicalFile>)session.createSQLQuery(sql)
				.addEntity(LogicalFile.class)
				.setString("srcpath", srcPath)
				.setString("destpath", destPath)
				.setString("srcpathmatch", StringUtils.replace(srcPath + "/%", "//", "/"))
				.setLong("srcsystemid", srcSystemId)
				.setLong("destsystemid", destSystemId)
				.setString("tenantid", TenancyHelper.getCurrentTenantId())
				.list();
			
			session.flush();
			
			return files;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to update logical file parent path", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Removes all logical files whose absolute path is prefixed by the given path + /.
	 * This should only be used when you discover that a logical file that was a folder 
	 * is now a file.
	 * 
	 * @param path
	 * @param systemId
	 */
	public static void deleteSubtreePath(String path, Long systemId) 
	{
		if (StringUtils.isEmpty(path)) {
			throw new HibernateException("No path specified");
		}
		
		if (systemId == null) {
			throw new HibernateException("No system id specified");
		}
		
		try {
			Session session = getSession();
			
			// update path of existing logical files where the is no corresponding destination logical_file
			String sql = "DELETE LogicalFile " +
						 "WHERE system.id = :systemid and " +  
						 "		path like :pathmatch and " +
						 "		path <> :path ";
			 
			session.createQuery(sql)
				.setString("path", path)
				.setString("pathmatch", StringUtils.replace(path + "/%", "//", "/"))
				.setLong("systemid", systemId)
				.executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			log.error("Failed to remove logical file children of " + path, ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns child logical files from another logical file
	 * 
	 * @param path
	 * @param systemId
	 */
	@SuppressWarnings("unchecked")
	public static List<LogicalFile> findChildren(String path, Long systemId) 
	{
		if (StringUtils.isEmpty(path)) {
			throw new HibernateException("No path specified");
		}
		
		if (systemId == null) {
			throw new HibernateException("No system id specified");
		}
		
		try {
			Session session = getSession();
			
			// update path of existing logical files where the is no corresponding destination logical_file
			String sql = "SELECT * " +
						 "FROM logical_files " +
						 "WHERE system_id = :systemid and " +  
						 "		BINARY path like :pathmatch and " +  
						 "		BINARY path <> BINARY :path and " +
						 "		tenant_id = :tenantid " +
						 "ORDER BY path ASC";
			 
			List<LogicalFile> children = (List<LogicalFile>)session.createSQLQuery(sql)
					.addEntity(LogicalFile.class)
					.setString("pathmatch", StringUtils.replace(path + "/%", "//", "/"))
					.setString("path", path)
					.setLong("systemid", systemId)
					.setString("tenantid", TenancyHelper.getCurrentTenantId())
					.list();
			
			session.flush();
			
			return children;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to find children of " + path, ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns child logical files ids from another logical file
	 * 
	 * @param path
	 * @param systemId
	 */
	@SuppressWarnings("unchecked")
	public static List<BigInteger> findChildIds(String path, Long systemId) 
	{
		if (StringUtils.isEmpty(path)) {
			throw new HibernateException("No path specified");
		}
		
		if (systemId == null) {
			throw new HibernateException("No system id specified");
		}
		
		try {
			Session session = getSession();
			
			// update path of existing logical files where the is no corresponding destination logical_file
			String sql = "SELECT id " +
						 "FROM logical_files " +
						 "WHERE system_id = :systemid and " +  
						 "		BINARY path like :pathmatch and " +  
						 "		BINARY path <> BINARY :path " +
						 "ORDER BY path ASC";
			 
			List<BigInteger> childIds = (List<BigInteger>)session.createSQLQuery(sql)
					.setString("pathmatch", StringUtils.replace(path + "/%", "//", "/"))
					.setString("path", path)
					.setLong("systemid", systemId)
	//				.setString("tenantid", TenancyHelper.getCurrentTenantId())
					.list();
			
			session.flush();
			
			return childIds;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to find children of " + path, ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static LogicalFile findBySystemAndPath(RemoteSystem system, String path)
	{
		if (!ServiceUtils.isValid(path)) {
			throw new HibernateException("Invalid path");
		} else {
			path = StringUtils.replace(path, "/+", "/");
			
			if (!StringUtils.equals(path, "/")) {
				path = StringUtils.removeEnd(path, "/");
			}
		}
		
		if (system == null) {
			throw new HibernateException("No remote system provided");
		}
		
		try 
		{
			Session session = getSession();
			
			
			LogicalFile file = (LogicalFile)session.createSQLQuery("select * from logical_files where BINARY path = :path and system_id = :systemId")
				.addEntity(LogicalFile.class)
				.setString("path", path)
				.setLong("systemId", system.getId())
				.setMaxResults(1)
				.uniqueResult();
		
			session.flush();
			
			return file;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to find logical file by user system path", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
		
	}

	@SuppressWarnings("unchecked")
	public static List<LogicalFile> getAll()
	{
		try 
		{
			Session session = getSession();
			
			List<LogicalFile> files = 
					(List<LogicalFile>) session.createQuery("from LogicalFile").list();
			
			session.flush();
			
			return files;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to get all logical files", ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<FileEvent> getEventsForLogicalFile(Long id) 
	{
		try 
		{
			Session session = getSession();
			
			String hql = "from FileEvent e where e.logicalFile.id = :logicalFileId";
			List<FileEvent> events = (List<FileEvent>) session.createQuery(hql)
					.setLong("logicalFileId",  id)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			log.error("Failed to get event history for logical file " + id, ex);
			
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
}
