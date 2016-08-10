package org.iplantc.service.common.uuid;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;

public class UUIDEntityLookup {

	public static String getResourceUrl(UUIDType entityType, String uuid) throws UUIDException
	{
		if (entityType == null) { 
			throw new UUIDException("Unknown resource type");
		} else if (entityType.equals(UUIDType.TOKEN)) {
			Object tokenNonce = getEntityFieldByUuid("authentication_tokens", "token", uuid);
			return Settings.IPLANT_AUTH_SERVICE + "tokens/" + tokenNonce.toString();
		} else if (entityType.equals(UUIDType.FILE)) {
			return resolveLogicalFileURLFromUUID(uuid);
		} else if (entityType.equals(UUIDType.PROFILE)) {
			String[] uuidTokens = StringUtils.split(uuid, "-");
			String username = null;
			if (uuidTokens.length == 4) {
				username = uuidTokens[1];
			} 
			return Settings.IPLANT_PROFILE_SERVICE + "profiles/" + username;
		} else if (entityType.equals(UUIDType.INTERNALUSER)) {
			Object internalUserUsername = getEntityFieldByUuid(entityType.name(), "username", uuid);
			Object profileUsername = getEntityFieldByUuid(entityType.name(), "created_by", uuid);
			return Settings.IPLANT_PROFILE_SERVICE + "profiles/" + 
				profileUsername.toString() + "/" + internalUserUsername.toString();
		} else if (entityType.equals(UUIDType.JOB)) {
			return Settings.IPLANT_JOB_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.SYSTEM)) {
			Object systemId = getEntityFieldByUuid(entityType.name(), "system_id", uuid);
			return Settings.IPLANT_SYSTEM_SERVICE + systemId.toString();
		} else if (entityType.equals(UUIDType.APP)) {
			Map<String,Object> map = getEntityFieldByUuid("select `name`, `version`, `publicly_available`, `revision_count` from softwares where uuid = '" + uuid + "' and `available` = 1");
			if (map.isEmpty()) 
			{
				throw new UUIDException("Resource id cannot be null");
			} 
			else 
			{
				String softwareUniqueName = (String)map.get("name") + "-" + (String)map.get("version");
				Object available = map.get("publicly_available");
				if (available instanceof Byte) {
				    if ((Byte)map.get("publicly_available") == 1) {
				        softwareUniqueName += "u" + ((Integer)map.get("revision_count")).toString();
				    }
				}
				else if (available instanceof Boolean) {
					if ((Boolean)available) {
						softwareUniqueName += "u" + ((Integer)map.get("revision_count")).toString();
					}
				}
				else if (available instanceof Integer) {
					if ((Integer)map.get("publicly_available") == 1) {
						softwareUniqueName += "u" + ((Integer)map.get("revision_count")).toString();
					}
				}
				
				return Settings.IPLANT_APP_SERVICE + softwareUniqueName;
			}
		} else if (entityType.equals(UUIDType.POSTIT)) {
			Object postIt = getEntityFieldByUuid(entityType.name(), "postit_key", uuid);
			return Settings.IPLANT_POSTIT_SERVICE + postIt.toString();
		} else if (entityType.equals(UUIDType.TRANSFORM)) {
			return Settings.IPLANT_TRANSFORM_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.TRANSFER)) {
			return Settings.IPLANT_TRANSFER_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.SCHEMA)) {
			return Settings.IPLANT_METADATA_SERVICE + "schema/" + uuid;
		} else if (entityType.equals(UUIDType.METADATA)) {
			return Settings.IPLANT_METADATA_SERVICE + "data/" + uuid;
		} else if (entityType.equals(UUIDType.NOTIFICATION)) {
			return Settings.IPLANT_NOTIFICATION_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.MONITOR)) {
			return Settings.IPLANT_MONITOR_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.TRANSFER)) {
			return Settings.IPLANT_TRANSFER_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.TAG)) {
			return Settings.IPLANT_TAGS_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.REALTIME_CHANNEL)) {
			return Settings.IPLANT_REALTIME_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.GROUP)) {
			return Settings.IPLANT_GROUPS_SERVICE + uuid;
//		} else if (entityType.equals(UUIDType.USAGETRIGGER)) {
//			return Settings.IPLANT_USAGETRIGGER_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.CLIENTS)) {
			return Settings.IPLANT_CLIENTS_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.CLIENTS)) {
			return Settings.IPLANT_CLIENTS_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.ROLE)) {
			return Settings.IPLANT_ROLES_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.TENANT)) {
			return Settings.IPLANT_TENANTS_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.REACTOR)) {
			return Settings.IPLANT_REACTOR_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.ABACO_AGENT)) {
			return Settings.IPLANT_ABACO_SERVICE + uuid;
		} else if (entityType.equals(UUIDType.REPOSITORY)) {
			return Settings.IPLANT_REPOSITORY_SERVICE + uuid;
		} else {
			throw new UUIDException("Unable to resolve " + entityType.name().toLowerCase() 
					+ " identifier to a known resource.");
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
    private static Object getEntityFieldByUuid(String entityType, String fieldName, String uuid) 
    throws UUIDException
    {
        // ObjectType should be an enum value and prevent injection attacks.
		if (StringUtils.isEmpty(entityType))
            throw new UUIDException("Resouurce type cannot be null");
        
        if (StringUtils.isEmpty(uuid))
            throw new UUIDException("Resource id cannot be null");
        
        String tableName = entityType.toLowerCase() + "s";

        try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            List<Object> fieldValues = session
            		.createSQLQuery("select " + fieldName + " from " + tableName + " where uuid = :uuid")
            		.setString("uuid", uuid.toString())
            		.list();

            session.flush();
            
            if (fieldValues.isEmpty())
                throw new UUIDException("No such uuid present");
            
            return fieldValues.get(0);
        }
        catch (UUIDException e) {
        	throw e;
        }
        catch(Throwable e) 
        {
            throw new UUIDException(e);
        } 
        finally 
        {
        	try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }
	
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getEntityFieldByUuid(String sql) 
    throws UUIDException
    {
        // ObjectType should be an enum value and prevent injection attacks.
		if (StringUtils.isEmpty(sql))
            throw new UUIDException("SQL query cannot be null");
        
        try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            Map<String, Object> row = (Map<String, Object>)session
            		.createSQLQuery(sql)
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();

            session.flush();
            
            if (row == null)
                throw new UUIDException("No such UUID present");
            
            return row;
        }
        catch(Throwable e) 
        {
            throw new UUIDException(e);
        } 
        finally 
        {
        	try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }
	
	protected static String resolveLogicalFileURLFromUUID(String uuid) throws UUIDException
	{
		String sql = "SELECT s.system_id as fileitem_systemid, s.home_dir, s.root_dir, f.path as absolutepath, f.tenant_id "
				+ "FROM logical_files f LEFT JOIN "
				+ "		("
				+ "			SELECT sys.id as id, sys.system_id as system_id, st.home_dir as home_dir, st.root_dir as root_dir "
				+ "			FROM systems sys LEFT JOIN storageconfigs st ON sys.storage_config = st.id "
				+ "		) s ON f.system_id = s.id " + 
        		"WHERE f.uuid = '" + uuid + "'";
		
		Map<String,Object> map = getEntityFieldByUuid(sql);
		if (map.isEmpty()) {
			throw new UUIDException("No such UUID present");
		}
		else 
		{
			String resolvedPath = getAgaveRelativePathFromAbsolutePath((String)map.get("absolutepath"), 
																	(String)map.get("root_dir"), 
																	(String)map.get("home_dir"));
			
			return Settings.IPLANT_FILE_SERVICE + 
						"media/system/" + 
						(String)map.get("fileitem_systemid") + 
						File.separator + resolvedPath;
		}
	}
	
	protected static String getAgaveRelativePathFromAbsolutePath(String absolutepath, String rootDir, String homeDir) 
	{	
		rootDir = FilenameUtils.normalize(rootDir);
		if (!StringUtils.isEmpty(rootDir)) {
			if (!rootDir.endsWith("/")) {
				rootDir += "/";
			}
		} else {
			rootDir = "/";
		}

		homeDir = FilenameUtils.normalize(homeDir);
        if (!StringUtils.isEmpty(homeDir)) {
            homeDir = rootDir +  homeDir;
            if (!homeDir.endsWith("/")) {
                homeDir += "/";
            }
        } else {
            homeDir = rootDir;
        }

        homeDir = homeDir.replaceAll("/+", "/");
        rootDir = rootDir.replaceAll("/+", "/");
        
		if (StringUtils.isEmpty(absolutepath)) {
			return homeDir;
		}
		
		String adjustedPath = absolutepath;
		if (adjustedPath.endsWith("/..") || adjustedPath.endsWith("/.")) {
			adjustedPath += File.separator;
		}
		
		if (adjustedPath.startsWith("/")) {
			absolutepath = FileUtils.normalize(adjustedPath);
		} else {
			absolutepath = FilenameUtils.normalize(adjustedPath);
		}
		
		absolutepath = absolutepath.replaceAll("/+", "/");
		
		if (StringUtils.startsWith(absolutepath, homeDir)) {
			return StringUtils.substringAfter(absolutepath, homeDir);
		} else {
			return "/" + StringUtils.substringAfter(absolutepath, rootDir);
		}
	}

}
