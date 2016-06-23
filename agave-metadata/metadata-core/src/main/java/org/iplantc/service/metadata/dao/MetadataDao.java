package org.iplantc.service.metadata.dao;

import static org.iplantc.service.metadata.model.enumerations.PermissionType.ALL;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.exceptions.MetadataQueryException;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.enumerations.PermissionType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryBuilder;

public class MetadataDao {
    
    private static final Logger log = Logger.getLogger(MetadataDao.class);
    
    private DB db = null;
    private MongoClient mongoClient = null;
    
    private static MetadataDao dao = null;
    
    public static MetadataDao getInstance() {
        if (dao == null) {
            dao = new MetadataDao();
        }
        
        return dao;
    }
    
//    public JacksonDBCollection<MetadataItem, String> getDefaultCollection() throws UnknownHostException {
//        return getCollection(Settings.METADATA_DB_SCHEME, Settings.METADATA_DB_COLLECTION);
//    }
//    
//    public JacksonDBCollection<MetadataItem, String> getCollection(String dbName, String collectionName) throws UnknownHostException {
//        
//        db = getClient().getDB(dbName);
//        // Gets a collection, if it does not exist creates it
//        return JacksonDBCollection.wrap(db.getCollection(collectionName), MetadataItem.class,
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
//     * Returns a {@link MetadataItem} with the matching {@code uuid} and {code tenantId}
//     * @param uuid
//     * @param tenantId
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem findByUuidAndTenant(String uuid, String tenantId) 
//    throws MetadataQueryException 
//    {   
//        try {
//            return (MetadataItem)getDefaultCollection().findOne(_getDBQueryForUuidAndTenant(uuid, tenantId));
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to find metadata by UUID", e);
//        }        
//    }
//    
//    
//    /**
//     * Implements search for metadata items.
//     * 
//     * @param username
//     * @param tenantId
//     * @param userSearchTermMap
//     * @param offset
//     * @param limit
//     * @return
//     * @throws MetadataQueryException
//     */
//    public DBCursor<MetadataItem> findMatching(String username, String tenantId, Map<SearchTerm, Object> userSearchTermMap, int offset, int limit)
//    throws MetadataQueryException 
//    {   
//        try {
//            
//            DBObject userSearchCriteria = parseUserSearchCriteria(userSearchTermMap);
//            
//            DBObject tenantCriteria = QueryBuilder.start("tenantId").is(tenantId).get();
//            
//            DBCursor<MetadataItem> cursor = null; 
//                    
//            // skip permission queries if user is admin
//            if (AuthorizationHelper.isTenantAdmin(username)) {
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            } 
//            // non admins must check permissions for ownership or read grants
//            else {
//                DBObject authCriteria = createAuthCriteria(username, READ);
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    authCriteria,
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            }                
//        
//            return cursor;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Failed to fetch metadata from db", e);
//        }        
//    }
//    
//    public DBCursor<MetadataItem> legacyFindMatching(String username, String tenantId, Map<SearchTerm, Object> userSearchTermMap, int offset, int limit)
//    throws MetadataQueryException 
//    {
//        try {
//            DBObject userSearchCriteria = parseUserSearchCriteria(userSearchTermMap);
//            
//            DBObject tenantCriteria = QueryBuilder.start("tenantId").is(tenantId).get();
//            
//            DBCursor<MetadataItem> cursor = null; 
//                    
//            // skip permission queries if user is admin
//            if (AuthorizationHelper.isTenantAdmin(username)) {
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                                    userSearchCriteria,
//                                    tenantCriteria).get())
//                                .skip(offset)
//                                .limit(limit);
//            } 
//            // non admins must check permissions for ownership or read grants
//            else {
//                
//                DBObject ownerCriteria = QueryBuilder.start("owner").in(
//                        Arrays.asList(Settings.PUBLIC_USER_USERNAME,
//                                      Settings.WORLD_USER_USERNAME,
//                                      username)).get();
//                
//                List<String> relationalSharedMetadataUuid = 
//                        MetadataPermissionDao.getUuidOfAllSharedMetataItemReadableByUser(username);
//                
//                DBObject sharedMetadataCriteria = QueryBuilder.start("uuid").in(
//                        relationalSharedMetadataUuid).get();
//                
//                DBObject authCriteria = QueryBuilder.start()
//                            .or(
//                                ownerCriteria, 
//                                sharedMetadataCriteria
//                                )
//                        .get();
//                
//                cursor = getDefaultCollection().find(QueryBuilder.start().and(
//                        authCriteria, 
//                        userSearchCriteria,
//                        tenantCriteria).get())
//                    .skip(offset)
//                    .limit(limit);
//            }
//            
//            return cursor;
//        } 
//        catch (Exception e) {
//            throw new MetadataQueryException("Failed to fetch metadata from db", e);
//        } 
//    }
//    
//    /**
//     * Delete one or more metadata value fields atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @throws MetadataQueryException
//     */
//    public void delete(String uuid, String tenantId, List<String> uuids) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (uuids == null || uuids.isEmpty()) {
//                return;
//             }
//             else 
//             {
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().remove(
//                         DBQuery.and(DBQuery.in("uuid", uuids), DBQuery.is("tenantId", TenancyHelper.getCurrentTenantId())));
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to delete one or more items", 
//                             result.getWriteResult().getLastError().getException());
//                 }
//             }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    
//    /**
//     * Unsets one or more metadata value fields atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> unset(String uuid, String tenantId, List<String> fields) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (fields == null || fields.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: fields) {
//                     if (builder == null) {
//                         builder = DBUpdate.unset(key);
//                     } else {
//                         builder = builder.unset(key);
//                     }
//                 }
//                 
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to unset one or more item fields", 
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
//     * Insert a new metadata item
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem insert(MetadataItem item) throws MetadataQueryException {
//        try {
//            WriteResult<MetadataItem, String> result = getDefaultCollection().insert(item);
//            return result.getSavedObject();
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }
//    }
//    
//    /**
//     * Insert a new metadata item if another {@link MetadataItem} with the same {@code name}, 
//     * {@code tenantId}, {@code owner}, {@code value}, {@code associatedIds}, and {@code internalUser} 
//     * does not already exist.
//     *  
//     * @param item
//     * @return
//     * @throws MetadataQueryException
//     */
//    public MetadataItem insertIfNotPresent(MetadataItem item) 
//    throws MetadataQueryException 
//    {
//        try {
//            MetadataItem existingItem = getDefaultCollection().findOne(DBQuery.and(
//                                                                    DBQuery.is("name", item.getUuid()), 
//                                                                    DBQuery.is("tenantId", item.getTenantId()),
//                                                                    DBQuery.is("owner",  item.getOwner()),
//                                                                    DBQuery.is("value",  item.getValue()),
//                                                                    DBQuery.all("associatedIds", item.getAssociations().getRawUuid()),
//                                                                    DBQuery.is("internalUser",  item.getInternalUsername())));
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
//     * Update one or more metadata names or values in part or whole atomically 
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> update(String uuid, String tenantId, Map<String, JsonNode> updates) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (updates == null || updates.isEmpty()) {
//                return new ArrayList<MetadataItem>();
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
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
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
//     * Add the given value to the array value if it doesn't already exist in the specified field atomically
//     * @param uuid
//     * @param tenantId
//     * @param updates
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> add(String uuid, String tenantId, Map<String, JsonNode> updates) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (updates == null || updates.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: updates.keySet()) {
//                     if (builder == null) {
//                         builder = DBUpdate.addToSet(key, updates.get(key));
//                     } else {
//                         builder = builder.addToSet(key, updates.get(key));
//                     }
//                 }
//                 
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to add to one or more items", 
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
//     * Add one or ore values to the array value at each of the specified fields atomically
//     * @param uuid
//     * @param tenantId
//     * @param additions
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> append(String uuid, String tenantId, Map<String, JsonNode> additions) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (additions == null || additions.isEmpty()) {
//                return new ArrayList<MetadataItem>();
//             }
//             else 
//             {
//                 Builder builder = null;
//                 for (String key: additions.keySet()) {
//                     if (builder == null) {
//                         if (additions.get(key) instanceof List) {
//                             builder = DBUpdate.pushAll(key, additions.get(key));
//                         } else {
//                             builder = DBUpdate.push(key, additions.get(key));
//                         }
//                     } else {
//                         if (additions.get(key) instanceof List) {
//                             builder = builder.pushAll(key, additions.get(key));
//                         } else {
//                             builder = builder.push(key, additions.get(key));
//                         }
//                     }
//                 }
//                 WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                 // do we need to check for errors?
//                 if (!result.getWriteResult().getLastError().isEmpty()) {
//                     throw new MetadataQueryException("Failed to append to one or more items", 
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
//     * Perform an atomic increment action of a user-defined amount on the given metadata values(s).
//     * @param uuid
//     * @param tenantId
//     * @param increments
//     * @return
//     * @throws MetadataQueryException
//     */
//    public List<MetadataItem> increment(String uuid, String tenantId, Map<String, Integer> increments) 
//    throws MetadataQueryException 
//    {
//        try {
//            if (increments == null || increments.isEmpty()) {
//               return new ArrayList<MetadataItem>();
//            }
//            else 
//            {
//                Builder builder = null;
//                for (String key: increments.keySet()) {
//                    if (builder == null) {
//                        builder = DBUpdate.inc(key, increments.get(key).intValue());
//                    } else {
//                        builder = builder.inc(key, increments.get(key).intValue());
//                    }
//                }
//                WriteResult<MetadataItem,String> result = getDefaultCollection().update(_getDBQueryForUuidAndTenant(uuid, tenantId), builder);
//                // do we need to check for errors?
//                if (!result.getWriteResult().getLastError().isEmpty()) {
//                    throw new MetadataQueryException("Failed to increment one or more items", 
//                            result.getWriteResult().getLastError().getException());
//                }
//                return result.getSavedObjects();
//            }
//        } catch (MetadataQueryException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new MetadataQueryException("Unable to insert new metadata item", e);
//        }       
//    }
    
    /**
     * Creates a {@link DBObject} representing the appropriate permission check 
     * for {@code username} to establish they have @{link permission) for a 
     * {@link MetadataItem}.
     * 
     * @param username
     * @param permission
     * @return
     */
    protected DBObject createAuthCriteria(String username, PermissionType permission) {
        BasicDBList ownerList = new BasicDBList();
        ownerList.addAll(Arrays.asList(Settings.PUBLIC_USER_USERNAME, Settings.WORLD_USER_USERNAME, username));
        
        BasicDBList aclCriteria = new BasicDBList();
        aclCriteria.add(QueryBuilder.start("username").in(ownerList).get());
        if (permission == ALL) {
            aclCriteria.add(QueryBuilder.start("read").is(true).and("write").is(true).get());
        } 
        else if (permission.canRead() && permission.canWrite()) {
            aclCriteria.add(QueryBuilder.start("read").is(true).and("write").is(true).get());
        } 
        else if (permission.canRead()) {
            aclCriteria.add(QueryBuilder.start("read").is(true).get());
        } 
        else if (permission.canWrite()) {
            aclCriteria.add(QueryBuilder.start("write").is(true).get());
        }
        
        BasicDBList authConditions = new BasicDBList();
        authConditions.add(QueryBuilder.start("owner").in(ownerList).get());
        authConditions.add(QueryBuilder.start("acl").all(aclCriteria).get());
        
        DBObject authCriteria = QueryBuilder.start().all(
                authConditions).get();
        
        return authCriteria;
    }
    
    /**
     * Turns the search criteria supplied by the user in the URL query into a 
     * {@link DBObject} we can pass to the MongoDB driver.
     * 
     * @param searchCriteria
     * @return
     * @throws MetadataQueryException 
     */
    @SuppressWarnings("unchecked")
    protected DBObject parseUserSearchCriteria(Map<SearchTerm, Object> searchCriteria) throws MetadataQueryException {
        DBObject userCriteria = null;
        QueryBuilder queryBuilder = null;
        
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return new BasicDBObject();
        } 
        else {
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                
                // this is a freeform search query. Support regex then move on. if this exists, it is the only
                // search criteria
                if (searchCriteria.get(searchTerm) instanceof DBObject) {
                    
                    userCriteria = (DBObject)searchCriteria.get(searchTerm);
                    
                    // support regex in the freeform queries
                    for (String key: userCriteria.keySet()) {
                        
                        // TODO: throw exception on unsafe mongo keywords in freeform search
                        
                        // we're just going one layer deep on the regex support. anything else won't work anyway due to 
                        // the lack of freeform query support in the java driver
                        if (userCriteria.get(key) instanceof String) {
                            if (((String) userCriteria.get(key)).contains("*")) {
                                try {
                                    Pattern regexPattern = Pattern.compile((String)userCriteria.get(key), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                                    userCriteria.put(key, regexPattern);
                                } catch (Exception e) {
                                    throw new MetadataQueryException("Invalid regular expression for " + key + " query", e);
                                }
                            }
                        }
                    }
                }
                // they are using the json.sql notation to search their metadata value
                else { // if (searchTerm.getSearchField().equalsIgnoreCase("value")) {
                    if (queryBuilder == null) {
                        queryBuilder = QueryBuilder.start(searchTerm.getMappedField());
                    } else {
                        queryBuilder.and(searchTerm.getMappedField());
                    }
                    
                    if (searchTerm.getOperator() == SearchTerm.Operator.EQ) {
                        queryBuilder.is(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NEQ) {
                        queryBuilder.notEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.IN) {
                        queryBuilder.in(Arrays.asList(searchCriteria.get(searchTerm)));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NIN) {
                        queryBuilder.notIn(Arrays.asList(searchCriteria.get(searchTerm)));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.GT) {
                        queryBuilder.greaterThan(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.GTE) {
                        queryBuilder.greaterThanEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LT) {
                        queryBuilder.lessThan(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LTE) {
                        queryBuilder.lessThanEquals(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.LIKE) {
                        try {
                            Pattern regexPattern = Pattern.compile((String)searchCriteria.get(searchTerm), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                            queryBuilder.regex(regexPattern);
                        } catch (Exception e) {
                            throw new MetadataQueryException("Invalid regular expression for " + searchTerm.getMappedField() + " query", e);
                        }
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.NLIKE) {
                        try {
                            Pattern regexPattern = Pattern.compile((String)searchCriteria.get(searchTerm), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                            queryBuilder.not().regex(regexPattern);
                        } catch (Exception e) {
                            throw new MetadataQueryException("Invalid regular expression for " + searchTerm.getMappedField() + " query", e);
                        }
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.ON) {
                        queryBuilder.is(searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.BEFORE) {
                        
                        queryBuilder.lessThan((Date)searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.AFTER) {
                        queryBuilder.greaterThan((Date)searchCriteria.get(searchTerm));
                    }
                    else if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
                        List<Date> dateRange = (List<Date>)searchCriteria.get(searchTerm);
                        queryBuilder.greaterThan(dateRange.get(0))
                                    .and(searchTerm.getMappedField())
                                        .lessThan(dateRange.get(1));
                    }
                }
            }
        
            // generate the query if we used the query builder
            if (queryBuilder != null) {
                userCriteria = queryBuilder.get();
            }
            // if there wasn't a freeform search query, we need to init
            else if (userCriteria == null) {
                userCriteria = new BasicDBObject();
            } 
            
            return userCriteria;
        }
    }
}
