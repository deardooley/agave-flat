package org.iplantc.service.metadata.dao;

import org.apache.log4j.Logger;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class MetadataSchemaDao {
    
    private static final Logger log = Logger.getLogger(MetadataSchemaDao.class);
    
    private DB db = null;
    private MongoClient mongoClient = null;
    
    private static MetadataSchemaDao dao = null;
    
    public static MetadataSchemaDao getInstance() {
        if (dao == null) {
            dao = new MetadataSchemaDao();
        }
        
        return dao;
    }
    
//    public JacksonDBCollection<MetadataSchemaItem, String> getDefaultCollection() throws UnknownHostException {
//        return getCollection(Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_SCHEMATA_COLLECTION);
//    }
//    
//    public JacksonDBCollection<MetadataSchemaItem, String> getCollection(String dbName, String collectionName) throws UnknownHostException {
//        
//        db = getClient().getDB(dbName);
//        // Gets a collection, if it does not exist creates it
//        return JacksonDBCollection.wrap(db.getCollection(collectionName), MetadataSchemaItem.class,
//                String.class);
//    }
//    
//    @SuppressWarnings("deprecation")
//    private MongoClient getClient() throws UnknownHostException {
//        if (mongoClient == null) {
//            
//            mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), 
//                    Arrays.asList(MongoCredential.createMongoCRCredential(
//                            Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray())));
//        } 
//        else if (!mongoClient.getConnector().isOpen()) 
//        {
//            try { mongoClient.close(); } catch (Exception e) { log.error("Failed to close mongo client.", e); }
//            mongoClient = null;
//            mongoClient = new MongoClient(new ServerAddress(Settings.METADATA_DB_HOST, Settings.METADATA_DB_PORT), 
//                    Arrays.asList(MongoCredential.createMongoCRCredential(
//                            Settings.METADATA_DB_USER, "api", Settings.METADATA_DB_PWD.toCharArray())));
//        }
//            
//        return mongoClient;
//    }
//    
//    /**
//     * Generates a {@link Query} from the given {@code uuid} and {@link TenancyHelper#getCurrentTenantId()}
//     * @param uuid
//     * @return
//     */
//    private Query _getDBQueryForUuidAndTenant(String uuid) {
//        return _getDBQueryForUuidAndTenant(uuid, TenancyHelper.getCurrentTenantId());
//    }
//    
//    /**
//     * Generates a {@link Query} from the given {@code uuid} and {code tenantId}
//     * @param uuid
//     * @param tenantId
//     * @return
//     */
//    private Query _getDBQueryForUuidAndTenant(String uuid, String tenantId) {
//        return DBQuery.and(
//                DBQuery.is("uuid", uuid), 
//                DBQuery.is("tenantId", tenantId));
//    }
//
//    /**
//     * Returns a {@link MetadataSchemaItem} with the matching {@code uuid} and {code tenantId}
//     * @param uuid
//     * @param tenantId
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataSchemaItem findByUuidAndTenant(String uuid, String tenantId) 
//    throws MetadataQueryException 
//    {   
//        try {
//            return (MetadataSchemaItem)getDefaultCollection().findOne(_getDBQueryForUuidAndTenant(uuid, tenantId));
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to find metadata schema by UUID", e);
//        }        
//    }
//    
//    /**
//     * Insert a new metadata item
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataSchemaItem insert(MetadataSchemaItem item) throws MetadataQueryException {
//        try {
//            WriteResult<MetadataSchemaItem, String> result = getDefaultCollection().insert(item);
//            return result.getSavedObject();
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Insert a new metadata schema item if another {@link MetadataSchemaItem} with the same {@code schema}, 
//     * {@code tenantId}, {@code owner}, {@code associatedIds}, and {@code internalUser} 
//     * does not already exist.
//     *  
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataSchemaItem insertIfNotPresent(MetadataSchemaItem item) 
//    throws MetadataQueryException 
//    {
//        try {
//            MetadataSchemaItem existingItem = getDefaultCollection().findOne(DBQuery.and(
//                                                                    DBQuery.is("name", item.getUuid()), 
//                                                                    DBQuery.is("tenantId", item.getTenantId()),
//                                                                    DBQuery.is("owner",  item.getOwner()),
//                                                                    DBQuery.is("schema",  item.getSchema()),
//                                                                    DBQuery.is("internalUsername",  item.getInternalUsername())));
//            
//            if (existingItem == null) {
//                return getDefaultCollection().insert(item).getSavedObject();
//            } else {
//                return existingItem;
//            }
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Update one or more metadata schema items in part or whole atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataSchemaItem> update(String uuid, String tenantId, Map<String, JsonNode> updates) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (updates == null || updates.isEmpty()) {
//                return new ArrayList<MetadataSchemaItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: updates.keySet()) {
//                     if (builder == null) {
//                         builder = DBUpdate.set(key, updates.get(key));
//                     } else {
//                         builder = builder.set(key, updates.get(key));
//                     }
//                 }
//                 
//                 WriteResult<MetadataSchemaItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to update one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//                 return result.getSavedObjects();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Update one or more metadata schema items in part or whole atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public boolean delete(String uuid, String tenantId) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (StringUtils.isEmpty(uuid)) {
//                return false;
//            }
//            else 
//            {
//                WriteResult<MetadataSchemaItem,String> result = getDefaultCollection()
//                        .remove(DBQuery.and(
//                                DBQuery.is("uuid", uuid), 
//                                DBQuery.is("tenantId", tenantId)));
//                
//                // do we need to check for errors?
//                if (!result.getWriteResult().getLastError().isEmpty()) {
//                    throw new MetadataQueryException("Failed to delete the metadata schema item", 
//                            result.getWriteResult().getLastError().getException());
//                }
//                
//                return true;
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Update one or more metadata schema items in part or whole atomically 
//     * @param uuids
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public int delete(List<String> uuids, String tenantId) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (uuids == null || uuids.isEmpty()) {
//                return 0;
//            }
//            else 
//            {
//                WriteResult<MetadataSchemaItem,String> result = getDefaultCollection()
//                        .remove(DBQuery.and(
//                                DBQuery.in("uuid", uuids), 
//                                DBQuery.is("tenantId", tenantId)));
//                
//                // do we need to check for errors?
//                if (!result.getWriteResult().getLastError().isEmpty()) {
//                    throw new MetadataQueryException("Failed to update one or more metadata schema items", 
//                            result.getWriteResult().getLastError().getException());
//                }
//                return result.getWriteResult().getN();
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
}
