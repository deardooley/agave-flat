package org.iplantc.service.metadata.resources;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaCreate;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaSearch;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.MetadataApplication;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataPermissionDao;
import org.iplantc.service.metadata.dao.MetadataSchemaPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataSchemaValidationException;
import org.iplantc.service.metadata.managers.MetadataSchemaPermissionManager;
import org.iplantc.service.metadata.util.ServiceUtils;
import org.iplantc.service.notification.managers.NotificationManager;
import org.joda.time.DateTime;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;


/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 8/20/13
 * Time: 10:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class MetadataSchemaCollection extends AgaveResource 
{
    private static final Logger log = Logger.getLogger(MetadataSchemaCollection.class);

    private String username;
    private String internalUsername;
    private String userQuery;
    private boolean includeRecordsWithImplicitPermissions = true;
    
    private MongoClient mongoClient;
    private DB db;
    private DBCollection collection;

    
    /**
     * Entrypoint for {@link MetadataSchemeItem} Collections
     * @param context
     * @param request
     * @param response
     */
    public MetadataSchemaCollection(Context context, Request request, Response response)
    {
        super(context, request, response);

        this.username = getAuthenticatedUsername();
		
        internalUsername = (String) context.getAttributes().get("internalUsername");
        
        Form form = request.getOriginalRef().getQueryAsForm();
		if (form != null) {
			userQuery = (String)form.getFirstValue("q");
		
	        if (!StringUtils.isEmpty(userQuery)) {
	            try {
	                userQuery = URLDecoder.decode(userQuery, "UTF-8");
	            } catch (UnsupportedEncodingException e) {
	                log.error("Invalid URL encoding in URL. Apparently.", e);
	                response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
	                response.setEntity(new IplantErrorRepresentation("Invalid URL-encoded Character(s)."));
	
	            }
	        }
	        
	        // allow admins to de-escelate permissions for querying metadata
 			// so they don't get back every record for every user.
 			if (AuthorizationHelper.isTenantAdmin(this.username)) {
 				// check whether they explicitly ask for unprivileged results..basically query
 				// as a normal user
 				if (form.getNames().contains("privileged") && 
 						!BooleanUtils.toBoolean((String)form.getFirstValue("privileged"))) {
 					this.includeRecordsWithImplicitPermissions = false;
 				}
 				// either they did not provide a "privileged" query parameter or it was true
 				// either way, they get back all results regardless of ownership
 				else {
 					this.includeRecordsWithImplicitPermissions = true;
 				}
 			}
 			// non-admins do not inherit any implicit permissions
 			else {
 				this.includeRecordsWithImplicitPermissions = false;
 			}
		}
		
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        // Set up MongoDB connection
        try 
        {
        	mongoClient = ((MetadataApplication)getApplication()).getMongoClient();
            db = mongoClient.getDB(Settings.METADATA_DB_SCHEME);
            // Gets a collection, if it does not exist creates it
            collection = db.getCollection(Settings.METADATA_DB_SCHEMATA_COLLECTION);
        } 
        catch (Throwable e) 
        {
        	log.error("Unable to connect to metadata store", e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            response.setEntity(new IplantErrorRepresentation("Unable to connect to metadata store."));
        }
    }

    /**
     * This method represents the HTTP GET action. The schema is
     * retrieved from the database and sent to the user as a {@link org.json.JSONArray JSONArray} of {@link org.json.JSONObject JSONObject}.
     *
     */
    @Override
    public Representation represent(Variant variant) throws ResourceException
    {
        DBCursor cursor = null;
        
        try 
        {
        	if (collection == null) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                		"Unable to connect to metadata store. If this problem persists, "
                		+ "please contact the system administrators.");
            }
        	
	        BasicDBObject query = null;
	        
	        // Include user defined query clauses given within the URL as q=<clauses>
            if (StringUtils.isEmpty(userQuery)) {
                
            	AgaveLogServiceClient.log(METADATA02.name(), SchemaList.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
                query = new BasicDBObject("tenantId", TenancyHelper.getCurrentTenantId());
            	
                // filter results if querying without implicity permissions
            	if (!includeRecordsWithImplicitPermissions) {
            		List<String> accessibleUuids = MetadataSchemaPermissionDao.getUuidOfAllSharedMetataSchemaItemReadableByUser(this.username);
	            	
	            	BasicDBList or = new BasicDBList();
	            	or.add(new BasicDBObject("uuid", new BasicDBObject("$in", accessibleUuids)));
	            	or.add(new BasicDBObject("owner", this.username));
	            	
	            	query.append("$or", or);
            	}
            } 
        	else {
        		AgaveLogServiceClient.log(METADATA02.name(), SchemaSearch.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
        		try
                {
                    query = ((BasicDBObject)JSON.parse(userQuery));
                    for (String key: query.keySet()) {
                        if (query.get(key) instanceof String) {
//                        	if (!StringUtils.equalsIgnoreCase("owner", key) && !AuthorizationHelper.isTenantAdmin(username)) {
//                    			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
//                    					"User does not have permission to perform ownership queries");
//                    		}
//                        	else 
                    		if (((String) query.get(key)).contains("*")) {
                                try {
                                    Pattern regexPattern = Pattern.compile((String)query.getString(key), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
                                    query.put(key, regexPattern);
                                } catch (Exception e) {
                                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid regular expression for " + key + " query");
                                }
                            }
                        }
                    }
                    // append tenancy info
                    query.append("tenantId", TenancyHelper.getCurrentTenantId());
                    
                    // filter results if querying without implicity permissions
                    if (!includeRecordsWithImplicitPermissions) {
	                    // permissions are separated from the metadata, so we need to look up available uuid for user.
	                	List<String> accessibleUuids = MetadataPermissionDao.getUuidOfAllSharedMetataItemReadableByUser(this.username);
	                	List<String> accessibleOwners = Arrays.asList(this.username, Settings.PUBLIC_USER_USERNAME, Settings.WORLD_USER_USERNAME);
	                	BasicDBList or = new BasicDBList();
	                	or.add(new BasicDBObject("uuid", new BasicDBObject("$in", accessibleUuids)));
	                	or.add(new BasicDBObject("owner", new BasicDBObject("$in", accessibleOwners)));
	                	query.append("$or", or);
                    }
                }
                catch(JSONParseException e) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Malformed JSON Query");
                }
            } 
	       
            cursor = collection.find(query, new BasicDBObject("_id", false))
					.sort(new BasicDBObject("lastModified", -1))
					.skip(offset)
					.limit(limit);

			List<DBObject> permittedResults = new ArrayList<DBObject>();
			
			for(DBObject result: cursor.toArray()) {
				// permission check is not needed since the list came from
				// a white list of allowsed uuids
				result = formatMetadataSchemaObject(result);
				permittedResults.add(result);
			}
			
			return new IplantSuccessRepresentation(ServiceUtils.unescapeSchemaRefFieldNames(permittedResults.toString()));
        } 
        catch(JSONParseException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Malformed JSON Query", e);
        }
        catch (ResourceException e) {
        	log.error("Failed to list metadata schema", e);
        	throw e;
        } 
        catch (Throwable e) 
        {
        	log.error("Failed to list metadata schema", e);
        	throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL, 
                    "An error occurred while fetching the metadata schema. " +
                    "If this problem persists, " +
                    "please contact the system administrators.", e);
        }
	    finally {
	    	try { cursor.close(); } catch (Exception e1) {}
//            try { mongoClient.close(); } catch (Exception e) {}
	    }
    }

    /**
     * HTTP POST for Creating and Updating Metadata JSON schema
     * @param entity
     */
    @Override
    public void acceptRepresentation(Representation entity)
    {
    	AgaveLogServiceClient.log(METADATA02.name(), SchemaCreate.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
    	
        DBCursor cursor = null;
        
        try 
        {
        	if (collection == null) {
        		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
        				"Unable to connect to metadata store. " +
                        "If this problem persists, please contact the system administrators.");
            }
        	
        	ObjectMapper mapper = new ObjectMapper();
	        JsonNode jsonSchema = null;
        	try 
        	{
        		jsonSchema = super.getPostedEntityAsObjectNode(false);
        	
	        } catch (ResourceException e) {
	        	 throw e;
	        } catch(Exception e) {
	            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	            		"Unable to parse form. " + e.getMessage());
	        }
	        
	        // validate that the schema is valid json
	        try 
	        {
	            SyntaxValidator validator = JsonSchemaFactory.byDefault().getSyntaxValidator();
	
	            if (!validator.schemaIsValid(jsonSchema)) {
	            	ProcessingReport report = validator.validateSchema(jsonSchema);
	            	StringBuilder errorMessages = new StringBuilder();
	            	for (Iterator<ProcessingMessage> iter = report.iterator(); iter.hasNext(); ) {
	            		ProcessingMessage message = iter.next();
	            		errorMessages.append(message.getMessage() + "\n");
	            	}
	                throw new MetadataSchemaValidationException("The supplied JSON Schema definition is invalid. " +
	                		"For more information on JSON Schema, please visit http://json-schema.org/.\n" + 
	                		errorMessages.toString());
	            }
	        } 
	        catch (MetadataSchemaValidationException e) {
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
	        			e.getMessage(), e);
	        }
	        catch(Throwable e) {
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	        			"The supplied JSON Schema definition is invalid. " +
	            		"For more information on JSON Schema, please visit  http://json-schema.org/", e);
	        }
	
	        // persist the MetadataSchema object
	        BasicDBObject doc;
	        String timestamp = new DateTime().toString();
	        try 
	        {
	            doc = new BasicDBObject("internalUsername", internalUsername)
	                    .append("lastUpdated", timestamp)
	                    .append("schema", JSON.parse(ServiceUtils.escapeSchemaRefFieldNames(mapper.writeValueAsString(jsonSchema))));
	        } 
	        catch (JSONParseException e) {
	            // If schema is not valid JSON, throw exception
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	        			"Unable to parse jsonSchema object.", e);
	        }
	        catch (JsonProcessingException e) {
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	        			"Unable to parse jsonSchema object.", e);
	        }
	
	        // Insert or Update
	        MetadataSchemaPermissionManager pm = null;
        	
			String uuid = new AgaveUUID(UUIDType.SCHEMA).toString();
			doc.put("uuid", uuid);
			doc.append("created", timestamp);
			doc.append("owner", username);
			doc.append("tenantId", TenancyHelper.getCurrentTenantId());
			
			collection.insert(doc);
			
			pm = new MetadataSchemaPermissionManager(uuid, username);
			pm.setPermission(username, "ALL");
			
			String sdoc = ServiceUtils.unescapeSchemaRefFieldNames(formatMetadataSchemaObject(doc).toString());
			NotificationManager.process(uuid, "CREATED", username, sdoc);
			
			getResponse().setStatus(Status.SUCCESS_CREATED);
            
            getResponse().setEntity(new IplantSuccessRepresentation(sdoc));
        } 
        catch (ResourceException e) {
        	log.error("Failed to add metadata schema", e);
        	getResponse().setStatus(e.getStatus());
            getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        }
        catch (Throwable e) {
        	log.error("Internal error attempting to add metadata schema", e);
        	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity(new IplantErrorRepresentation("Unable to add the metadata schema object. " +
                        "If this problem persists, please contact the system administrators."));
        } 
        finally {
            try { cursor.close(); } catch (Exception e) {}
        }

    }

    /**
     * Adds hypermedia and strips MongoDB document into a valid Agave response object.
     * @param metadataSchemaObject
     * @return
     * @throws UUIDException
     */
    private DBObject formatMetadataSchemaObject(DBObject metadataSchemaObject) throws UUIDException 
    {
    	metadataSchemaObject.removeField("_id");
    	metadataSchemaObject.removeField("tenantId");
    	
    	BasicDBObject hal = new BasicDBObject();
    	hal.append("self",new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "schemas/" + metadataSchemaObject.get("uuid")));
    	hal.append("permissions", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE + "schemas/" + metadataSchemaObject.get("uuid") + "/pems")));
    	hal.append("owner", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE + metadataSchemaObject.get("owner"))));
    	
    	metadataSchemaObject.put("_links", hal);		
    	
    	return metadataSchemaObject;
    }

    /* (non-Javadoc)
     * @see org.restlet.resource.Resource#allowPost()
     */
    @Override
    public boolean allowPost() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.restlet.resource.Resource#allowDelete()
     */
    @Override
    public boolean allowDelete() {
        return true;
    }

}
