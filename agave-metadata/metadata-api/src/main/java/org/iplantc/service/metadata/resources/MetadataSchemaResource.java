package org.iplantc.service.metadata.resources;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaCreate;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaDelete;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaEdit;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaGetById;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.SchemaSearch;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.iplantc.service.metadata.dao.MetadataSchemaPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
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
public class MetadataSchemaResource extends AgaveResource 
{
    private static final Logger log = Logger.getLogger(MetadataSchemaResource.class);

    private String username;
    private String internalUsername;
    private String uuid;
    private String userQuery;

    private MongoClient mongoClient;
    private DB db;
    private DBCollection collection;

    
    /**
     * @param context
     * @param request
     * @param response
     */
    public MetadataSchemaResource(Context context, Request request, Response response)
    {
        super(context, request, response);

        this.username = getAuthenticatedUsername();
        this.uuid = (String)request.getAttributes().get("schemaId");
        this.internalUsername = (String) context.getAttributes().get("internalUsername");
        
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
//            try { mongoClient.close(); } catch (Exception e1) {}
        }

        
    }

    /**
     * This method represents the HTTP GET action. The schema is
     * retrieved from the database and sent to the user as a {@link MetadataSchemaItem}
     *
     */
    @Override
    public Representation represent(Variant variant) throws ResourceException
    {
        DBCursor cursor = null;
        MetadataSchemaPermissionManager pm = null;

        try 
        {
        	if (collection == null) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                		"Unable to connect to metadata store. If this problem persists, "
                		+ "please contact the system administrators.");
            }
        	
	        BasicDBObject query = null;
	        
	        // Include user defined query clauses given within the URL as q=<clauses>
            if (StringUtils.isEmpty(uuid))
            {
            	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
            			"Please provide a metadata schemata uuid", 
            			new NullPointerException("Metadata Schemata UUID cannot be null"));
            } 
            else {
            	AgaveLogServiceClient.log(METADATA02.name(), SchemaGetById.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
            	// do we not want to support general collection queries? 
            	// How would one browse all their metadata?
            	// does that even make sense?
            	query = new BasicDBObject("uuid", uuid);
            	query.append("tenantId", TenancyHelper.getCurrentTenantId());
            }
            
	        cursor = collection.find(query, new BasicDBObject("_id", false));
	        
            if (cursor.hasNext())  {
            	DBObject firstResult = cursor.next();
            	
        		pm = new MetadataSchemaPermissionManager((String)firstResult.get("uuid"), (String)firstResult.get("owner"));
                
        		if (pm.canRead(username)) {
                	firstResult = formatMetadataSchemaObject(firstResult);
	        	    return new IplantSuccessRepresentation(ServiceUtils.unescapeSchemaRefFieldNames(firstResult.toString()));
                }
                else {
                	throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
            			"User does not have permission to read metadata for uuid");
                }
            }
            else {
            	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No metadata schema item found for user with id " + uuid);
            }
        } 
        catch (ResourceException e) {
        	log.error("Failed to fetch metadata schema " + uuid, e);
        	throw e;
        } 
        catch (Throwable e) {
        	log.error("Failed to fetch metadata schema " + uuid, e);
        	
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"Failed to retrieve metadata schema information.", e);
        }
	    finally {
	    	try { cursor.close(); } catch (Exception e1) {}
	    }
    }

    /**
     * HTTP POST for Updating {@link MetadataSchemaItem}
     * @param entity
     */
    @Override
    public void acceptRepresentation(Representation entity)
    {
    	AgaveLogServiceClient.log(METADATA02.name(), SchemaEdit.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
    	
        DBCursor cursor = null;
        
        try 
        {
        	if (collection == null) {
        		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
        				"Unable to connect to metadata store. " +
                        "If this problem persists, please contact the system administrators.");
            }
        	
        	if (StringUtils.isEmpty(uuid)) {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
            			"No metadata schema identifier provided.",
            			new NullPointerException("Metadata Schemata UUID cannot be null"));
            } 
        	
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode jsonSchema = null;
        	try {
        		jsonSchema = super.getPostedEntityAsObjectNode(false);
        	
	        } 
        	catch (ResourceException e) {
	        	 throw e;
	        } 
        	catch(Exception e) {
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
	            doc = new BasicDBObject("uuid", uuid)
	                    .append("internalUsername", internalUsername)
	                    .append("lastUpdated", timestamp)
	                    .append("schema", JSON.parse(ServiceUtils.escapeSchemaRefFieldNames(mapper.writeValueAsString(jsonSchema))));
	        } 
	        catch (Exception e) {
	            // If schema is not valid JSON, throw exception
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	        			e.getMessage(), e);
	        }
	
	        // Insert or Update
	        MetadataSchemaPermissionManager pm = null;

        	BasicDBObject query = new BasicDBObject("uuid", uuid);
    		query.append("tenantId", TenancyHelper.getCurrentTenantId());
    		cursor = collection.find(query);
    		String sdoc = null;
    		if (cursor.hasNext()) {
    			
    			BasicDBObject currentMetadata = (BasicDBObject)cursor.next();
        		
	            pm = new MetadataSchemaPermissionManager(uuid, (String)currentMetadata.get("owner"));
                
                if (pm.canWrite(username)) {
                    //doc.remove("created");
                    doc.append("created", currentMetadata.get("created"));
                    doc.append("owner", currentMetadata.get("owner"));
                    doc.append("tenantId", currentMetadata.get("tenantId"));
                    collection.update(query, doc);
                    
                    sdoc = ServiceUtils.unescapeSchemaRefFieldNames(formatMetadataSchemaObject(doc).toString());
                    
                    NotificationManager.process(uuid, "UPDATED", username, sdoc);
                } 
                else {
                	throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
                    	"User does not have permission to update metadata schema");
                }
            } 
            else {
            	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No metadata schema found for user with id " + uuid);
            }
            
            getResponse().setEntity(new IplantSuccessRepresentation(sdoc));
        } 
        catch (ResourceException e) {
        	log.error("Failed to update metadata schema " + uuid, e);
        	
        	getResponse().setStatus(e.getStatus());
            getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        }
        catch (Throwable e) {
        	log.error("Failed to update metadata schema " + uuid, e);
        	
        	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        	getResponse().setEntity(new IplantErrorRepresentation("Unable to update the metadata schema object. " +
                    "If this problem persists, please contact the system administrators."));
        } 
        finally {
            try { cursor.close(); } catch (Exception e) {}
        }

    }

    /**
     * DELETE
     **/
    @Override
    public void removeRepresentations()
    {
    	AgaveLogServiceClient.log(METADATA02.name(), SchemaDelete.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
    	
    	DBCursor cursor = null;
        try
        {
        	if (collection == null) {
        		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                	"Unable to connect to metadata store. " +
                    "If this problem persists, please contact the system administrators.");
            }

        	if (StringUtils.isEmpty(uuid)) {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
            			"No metadata schema identifier provided.",
            			new NullPointerException("Metadata Schemata UUID cannot be null"));
            } 
            
            BasicDBObject query = new BasicDBObject("uuid", uuid);
            query.append("tenantId", TenancyHelper.getCurrentTenantId());
            
            // collection.findAndRemove() only operates on the first object returned and seems to have no option for "all", so...
            cursor = collection.find(query);
            if (!cursor.hasNext()) 
            {
            	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No metadata schema item found for user with id " + uuid);
            } 
            else 
            {
	            while (cursor.hasNext()) {
	                BasicDBObject schema = (BasicDBObject)cursor.next();
	                MetadataSchemaPermissionManager pm = new MetadataSchemaPermissionManager(uuid, (String)schema.get("owner"));
	                if (pm.canWrite(username))
	                {
	                    collection.remove(schema);
	                    MetadataSchemaPermissionDao.deleteBySchemaId(uuid);
		                
	                    NotificationManager.process(uuid, "DELETED", username, ServiceUtils.unescapeSchemaRefFieldNames(formatMetadataSchemaObject(schema).toString()));
	                }
	                else 
	                {
	                	throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
	                			"User does not have permission to update metadata schema");
	                }
	            }
	            getResponse().setStatus(Status.SUCCESS_OK);
                getResponse().setEntity(new IplantSuccessRepresentation());
            }
        }
        catch (ResourceException e) 
        {
        	log.error("Failed to delete schema " + uuid, e);
        	
        	getResponse().setStatus(e.getStatus());
            getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        }
        catch (Throwable e) 
        {
        	log.error("Failed to delete schema" + uuid, e);
        	
        	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            getResponse().setEntity(new IplantErrorRepresentation("Unable to delete the associated metadata schema. " +
                    "If this problem persists, please contact the system administrators."));
        }
        finally {
            try { cursor.close(); } catch (Exception e) {}
//            try { mongoClient.close(); } catch (Exception e) {}
        }
    }
    
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
