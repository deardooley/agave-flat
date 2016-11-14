/**
 *
 */
package org.iplantc.service.metadata.resources;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaDelete;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaEdit;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaGetById;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;

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
import org.iplantc.service.metadata.dao.MetadataPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.managers.MetadataPermissionManager;
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jsonschema.main.AgaveJsonSchemaFactory;
import com.github.fge.jsonschema.main.AgaveJsonValidator;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
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
 * Class to handle CRUD operations on metadata entities.
 *
 * @author dooley
 *
 */
public class MetadataResource extends AgaveResource
{
	private static final Logger log = Logger.getLogger(MetadataResource.class);

	private String username;
    private String internalUsername;
	private String uuid;
    private String userQuery;

    private MongoClient mongoClient;
    private DB db;
    private DBCollection collection;
    private DBCollection schemaCollection;

    /**
	 * @param context
	 * @param request
	 * @param response
	 */
	public MetadataResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();

		uuid = (String)request.getAttributes().get("uuid");

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

        internalUsername = (String) context.getAttributes().get("internalUsername");

        getVariants().add(new Variant(MediaType.APPLICATION_JSON));

        // Set up MongoDB connection
        try
        {
        	mongoClient = ((MetadataApplication)getApplication()).getMongoClient();
        	db = mongoClient.getDB(Settings.METADATA_DB_SCHEME);
            // Gets a collection, if it does not exist creates it
            collection = db.getCollection(Settings.METADATA_DB_COLLECTION);
            schemaCollection = db.getCollection(Settings.METADATA_DB_SCHEMATA_COLLECTION);
        }
        catch (Exception e)
        {
        	log.error("Unable to connect to metadata store", e);
//        	try { mongoClient.close(); } catch (Exception e1) {}
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            response.setEntity(new IplantErrorRepresentation("Unable to connect to metadata store."));
        }
	}

	/**
	 * This method represents the HTTP GET action. The input files for the authenticated user are
	 * retrieved from the database and sent to the user as a {@link org.json.JSONArray JSONArray}
	 * of {@link org.json.JSONObject JSONObject}.
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
        	AgaveLogServiceClient.log(METADATA02.name(), MetaGetById.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());

        	// do we not want to support general collection queries?
        	// How would one browse all their metadata?
        	// does that even make sense?
        	query = new BasicDBObject("uuid", uuid);
        	query.append("tenantId", TenancyHelper.getCurrentTenantId());
            
            cursor = collection.find(query, new BasicDBObject("_id", false));
            
            if (cursor.hasNext()) {
            	DBObject firstResult = cursor.next();

            	MetadataPermissionManager pm = new MetadataPermissionManager(uuid, (String)firstResult.get("owner"));
                if (pm.canRead(username)) {
                	firstResult = formatMetadataObject(firstResult);
                	return new IplantSuccessRepresentation(firstResult.toString());
                }
                else {
                    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                    return new IplantErrorRepresentation("User does not have permission to read this metadata entry.");
                }
            }
            else {
            	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
            			"No metadata item found for user with id " + uuid);
            }
        }
        catch (ResourceException e) {
        	log.error("Failed to fetch metadata item " + uuid, e);
        	throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to list metadata " + uuid, e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"An unexpected error occurred while fetching the metadata item. "
        			+ "If this continues, please contact your tenant administrator.", e);
        }
        finally {
            try { cursor.close(); } catch (Exception e) {}
        }
	}

    /**
     * HTTP POST for Creating and Updating Metadata
     * @param entity
     */
    @Override
    public void acceptRepresentation(Representation entity)
    {
    	AgaveLogServiceClient.log(METADATA02.name(), MetaEdit.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
    	
    	DBCursor cursor = null;

    	try
    	{
	    	if (collection == null) {
	    		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
	    				"Unable to connect to metadata store. " +
	        			"If this problem persists, please contact the system administrators.");
	        }

	        String name = null;
	        String value = null;
	        String schemaId = null;
	        ObjectMapper mapper = new ObjectMapper();
	        ArrayNode items = mapper.createArrayNode();

	        try
	        {
	        	JsonNode jsonMetadata = super.getPostedEntityAsObjectNode(false);

	        	if (jsonMetadata.has("name") && jsonMetadata.get("name").isTextual()
	            		&& !jsonMetadata.get("name").isNull()) {
	                name = jsonMetadata.get("name").asText();
	            } else {
	            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	            			"No name attribute specified. Please associate a value with the metadata name.");
	            }

	        	if (jsonMetadata.has("value") && !jsonMetadata.get("value").isNull())
	            {
	            	if (jsonMetadata.get("value").isObject() || jsonMetadata.get("value").isArray())
	            		value = jsonMetadata.get("value").toString();
	            	else
	            		value = jsonMetadata.get("value").asText();
	            } else {
	            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	            			"No value attribute specified. Please associate a value with the metadata value.");
	            }

	            if (jsonMetadata.has("associationIds")) {
	            	if (jsonMetadata.get("associationIds").isArray()) {
	            		items = (ArrayNode)jsonMetadata.get("associationIds");
	            	} else {
	            		if (jsonMetadata.get("associationIds").isTextual())
	            			items.add(jsonMetadata.get("associationIds").asText());
	            	}
	            }

	            if (jsonMetadata.has("schemaId") && jsonMetadata.get("schemaId").isTextual()) {
	                schemaId = jsonMetadata.get("schemaId").asText();
	            }
	        }
	        catch (ResourceException e) {
	        	 throw e;
	        }
	        catch(Exception e)
	        {
	        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	        			"Unable to parse form. " + e.getMessage());
	        }

	        // if a schema is given, validate the metadata against that registered schema
	        if (schemaId != null)
	        {
	            BasicDBObject schemaQuery = new BasicDBObject("uuid", schemaId);
	            schemaQuery.append("tenantId", TenancyHelper.getCurrentTenantId());
	            BasicDBObject schemaDBObj = (BasicDBObject)schemaCollection.findOne(schemaQuery);

	            // lookup the schema
	            if (schemaDBObj == null)
	            {
	            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	            			"Specified schema does not exist.");
	            }

	            // check user permsisions to view the schema
	            try
	            {
	                MetadataSchemaPermissionManager schemaPM = new MetadataSchemaPermissionManager(schemaId, (String)schemaDBObj.get("owner"));
	                if (!schemaPM.canRead(username)) {
	                    throw new MetadataException("User does not have permission to read metadata schema");
	                }
	            } 
	            catch(MetadataException e) {
	            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
	            			e.getMessage());
	            }

	            // now validate the json against the schema
	            String schema = ServiceUtils.unescapeSchemaRefFieldNames(schemaDBObj.getString("schema"));
	            try
	            {
	                JsonFactory factory = new ObjectMapper().getFactory();
	                JsonNode jsonSchemaNode = factory.createParser(schema).readValueAsTree();
	                JsonNode jsonMetadataNode = factory.createParser(value).readValueAsTree();
	                AgaveJsonValidator validator = AgaveJsonSchemaFactory.byDefault().getValidator();
	                ProcessingReport report = validator.validate(jsonSchemaNode, jsonMetadataNode);
	                if (!report.isSuccess())
	                {
	                	StringBuilder sb = new StringBuilder();
	                	for (Iterator<ProcessingMessage> reportMessageIterator = report.iterator(); reportMessageIterator.hasNext();) {
	                		sb.append(reportMessageIterator.next().toString() + "\n");

	                	}
	                	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	                			"Metadata value does not conform to schema. \n" + sb.toString());
	                }
	            }
	            catch (ResourceException e) {
		        	 throw e;
		        }
		        catch(Exception e)
	            {
	            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	            			"Metadata does not conform to schema.");
	            }
	        }

	        // lookup the associated ids to make sure they exist.
	        BasicDBList associations = new BasicDBList();
	        if (items != null)
	        {
	            for (int i = 0; i < items.size(); i++)
	            {
	                try
	                {
	                    String associationId = (String) items.get(i).asText();
	                    if (StringUtils.isEmpty(associationId))
	                    {
	                        continue;
	                    }
	                    else
	                    {
	                        AgaveUUID associationUuid = new AgaveUUID(associationId);
	                        if (UUIDType.METADATA == associationUuid.getResourceType() 
	                        		|| UUIDType.SCHEMA == associationUuid.getResourceType())
	                        {
	                            BasicDBObject associationQuery = new BasicDBObject("uuid", associationId);
	                            associationQuery.append("tenantId", TenancyHelper.getCurrentTenantId());
	                            BasicDBObject associationDBObj = (BasicDBObject) collection.findOne(associationQuery);

	                            if (associationDBObj == null) {
	                                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	                                		"No associated object found with id" + associationId);
	                            }
	                        }
	                        else
	                        {
	                            try {
	                            	associationUuid.getObjectReference();
	                            } catch(Exception e) {
	                            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	                                	"No associated object found with id" + associationId);
	                            }
	                        }

	                        associations.add(items.get(i).asText());
	                    }
	                }
	                catch (ResourceException e) {
	                	throw e;
		   	        }
		   	        catch (Exception e)
	                {
	                	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
	                			"Unable to parse association ids.");
	                }
	            }
	        }

	        BasicDBObject doc;
	        String timestamp = new DateTime().toString();
	        try
	        {
	            doc = new BasicDBObject("uuid", uuid)
	                    .append("schemaId", schemaId)
	                    .append("internalUsername", internalUsername)
	                    .append("associationIds", associations)
	                    .append("lastUpdated", timestamp)
	                    .append("name", name)
	                    .append("value", JSON.parse(value));
	        }
	        catch (JSONParseException e)
	        {
	            // If value is a String that cannot be parsed into JSON Objects, then store it as a String
	            doc = new BasicDBObject("uuid", uuid)
	                    .append("schemaId", schemaId)
	                    .append("internalUsername", internalUsername)
	                    .append("associationIds", associations)
	                    .append("lastUpdated", timestamp)
	                    .append("name", name)
	                    .append("value", value);
	        }

        	// Insert or Update - currently a name on a path must be unique, which may not be desirable
    		BasicDBObject query = new BasicDBObject("uuid", uuid);
        	query.append("tenantId", TenancyHelper.getCurrentTenantId());

        	cursor = collection.find(query);
	        if (cursor.hasNext())
	        {
                BasicDBObject currentMetadata = (BasicDBObject)cursor.next();

                // Check if user has permission to update the metadata
                MetadataPermissionManager permissionManager = new MetadataPermissionManager(uuid, (String)currentMetadata.get("owner"));
                if (permissionManager.canWrite(username)) {
                    
                	doc.append("created", currentMetadata.get("created"));
                    doc.append("owner", currentMetadata.get("owner"));
                    doc.append("tenantId", currentMetadata.get("tenantId"));
                    
                    collection.update(query, doc);

                    for (int i=0; i<items.size(); i++) {
    	                String aid = items.get(i).asText();
    	                NotificationManager.process(aid, "METADATA_UPDATED", username);
                    }

                    NotificationManager.process(uuid, "UPDATED", username);

	                getResponse().setStatus(Status.SUCCESS_OK);
                }
                else
                {
                    throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
                            "User does not have permission to update metadata");
                }
	        } else {
	        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No metadata item found for user with id " + uuid);
	        }
        	
	        getResponse().setEntity(new IplantSuccessRepresentation(formatMetadataObject(doc).toString()));
	        
	        return;
        }
        catch (ResourceException e)
    	{
        	log.error("Failed to update metadata item " + uuid, e);
        	
        	getResponse().setStatus(e.getStatus());
        	getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        }
        catch (Exception e)
    	{
        	log.error("Failed to update metadata item " + uuid, e);
        	
        	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        	getResponse().setEntity(new IplantErrorRepresentation("Unable to store the metadata object. " +
        			"If this problem persists, please contact the system administrators."));
        }
        finally {
        	try { cursor.close(); } catch (Exception e1) {}
        }
    }

    /**
      * DELETE
      **/
    @Override
    public void removeRepresentations()
    {
    	AgaveLogServiceClient.log(METADATA02.name(), MetaDelete.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());

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
        				"No object identifier provided.");
            }

	        BasicDBObject query = new BasicDBObject("uuid", uuid);
        	query.append("tenantId", TenancyHelper.getCurrentTenantId());

        	cursor = collection.find(query);

	        if (!cursor.hasNext())
	        {
	        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	        			"No object identifier found for the given uuid.");
	        }
	        else
	        {
		        while (cursor.hasNext()) {
	                BasicDBObject metadata = (BasicDBObject)cursor.next();
	                MetadataPermissionManager pm = new MetadataPermissionManager(uuid, (String)metadata.get("owner"));
	                if (pm.canWrite(username))
	                {
		                collection.remove(metadata);
		                MetadataPermissionDao.deleteByUuid(uuid);

		                BasicDBList aids = (BasicDBList)metadata.get("associationIds");
		                for(Object aid: aids) {
		                	NotificationManager.process((String)aid, "METADATA_DELETED", username);
		                }

		                NotificationManager.process(uuid, "DELETED", username);
	                }
	                else
	                {
	                    getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
	                    getResponse().setEntity(new IplantErrorRepresentation(
	                            "User does not have permission to update metadata"));
	                    return;
	                }
		        }

		        getResponse().setStatus(Status.SUCCESS_OK);
	        	getResponse().setEntity(new IplantSuccessRepresentation());
	        }
	    }
        catch (ResourceException e)
        {
        	log.error("Failed to delete metadata " + uuid, e);

        	getResponse().setStatus(e.getStatus());
        	getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
        }
        catch (Exception e)
        {
        	log.error("Failed to delete metadata " + uuid, e);

	    	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
	    	getResponse().setEntity(new IplantErrorRepresentation("Unable to delete the associated metadata. " +
	    			"If this problem persists, please contact the system administrators."));
	    }
	    finally {
	    	try { cursor.close(); } catch (Exception e) {}
//	       	try { mongoClient.close(); } catch (Exception e1) {}
	    }
    }

    private DBObject formatMetadataObject(DBObject metadataObject) throws UUIDException
    {
    	metadataObject.removeField("_id");
    	metadataObject.removeField("tenantId");
    	BasicDBObject hal = new BasicDBObject();
    	hal.put("self", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/" + metadataObject.get("uuid")));
    	hal.put("permissions", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/" + metadataObject.get("uuid") + "/pems"));
    	hal.put("owner", new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + metadataObject.get("owner")));
    	
    	if (metadataObject.containsField("associationIds"))
    	{
    		// TODO: break this into a list of object under the associationIds attribute so
    		// we dont' overwrite the objects in the event there are multiple of the same type.
    		BasicDBList halAssociationIds = new BasicDBList();
    		
        	for (Object associatedId : (BasicDBList)metadataObject.get("associationIds")) {
                AgaveUUID agaveUUID = new AgaveUUID((String)associatedId);

                try {
                    String resourceUrl = agaveUUID.getObjectReference();
                    BasicDBObject assocResource = new BasicDBObject();
                    assocResource.put("rel", (String)associatedId);
                    assocResource.put("href", TenancyHelper.resolveURLToCurrentTenant(resourceUrl));
                    assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
                    halAssociationIds.add(assocResource);
                }
                catch (UUIDException e) {
                	BasicDBObject assocResource = new BasicDBObject();
                    assocResource.put("rel", (String)associatedId);
                    assocResource.put("href", null);
                    if (agaveUUID != null) {
                    	assocResource.put("title", agaveUUID.getResourceType().name().toLowerCase());
                    }
                    halAssociationIds.add(assocResource);
                }
            }
        	
        	hal.put("associationIds", halAssociationIds);
        }

    	if (metadataObject.get("schemaId") != null && !StringUtils.isEmpty(metadataObject.get("schemaId").toString()))
    	{
    		AgaveUUID agaveUUID = new AgaveUUID((String)metadataObject.get("schemaId"));
    		hal.append(agaveUUID.getResourceType().name().toLowerCase(), new BasicDBObject("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference())));
    	}
    	metadataObject.put("_links", hal);

    	return metadataObject;
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
