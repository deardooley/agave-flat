/**
 * 
 */
package org.iplantc.service.metadata.resources;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaPemsCreate;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaPemsDelete;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaPemsList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MetaPemsUpdate;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.METADATA02;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.metadata.MetadataApplication;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.managers.MetadataPermissionManager;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * The MetadataShareResource object enables HTTP GET and POST actions on permissions.
 * 
 * @author dooley
 * 
 */
public class MetadataShareResource extends AgaveResource {
	private static final Logger	log	= Logger.getLogger(MetadataShareResource.class);

	private String username; // authenticated user
	private String uuid;  // object id
    private String owner;
	private String sharedUsername; // user receiving permissions
    private MongoClient mongoClient;
    private DB db;
    private DBCollection collection;

    /**
	 * @param context
	 * @param request
	 * @param response
	 */
	public MetadataShareResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		

		this.uuid = (String) request.getAttributes().get("uuid");

		this.sharedUsername = (String) request.getAttributes().get("user");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));

		//log.info("uuid:" + uuid + ", username:" + username + "," + getRequest().getClientInfo().getUpstreamAddress());

        try 
        {
        	mongoClient = ((MetadataApplication)getApplication()).getMongoClient();
        	
            db = mongoClient.getDB(Settings.METADATA_DB_SCHEME);
            // Gets a collection, if it does not exist creates it
            collection = db.getCollection(Settings.METADATA_DB_COLLECTION);
            
            if (!StringUtils.isEmpty(uuid)) 
            {
    	        DBObject returnVal = collection.findOne(new BasicDBObject("uuid", uuid));
    	
    	        if (returnVal == null) {
    	            throw new MetadataException("No metadata item found for user with id " + uuid);
    	        }
    	        
    	        owner = (String)returnVal.get("owner");
            }
            else
            {
            	throw new MetadataException("No metadata id provided.");
            }
        } catch (MetadataException e) {
        	response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new IplantErrorRepresentation(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Unable to connect to metadata store", e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            response.setEntity(new IplantErrorRepresentation("Unable to connect to metadata store."));
        }
//        finally {
//        	
//        	try { mongoClient.close(); } catch (Throwable e) {}
//        }
	}

	/**
	 * This method represents the HTTP GET action. Gets Perms on specified iod.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		AgaveLogServiceClient.log(METADATA02.name(), MetaPemsList.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(uuid))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("No metadata id provided");
		}

		try
		{
			MetadataPermissionManager pm = new MetadataPermissionManager(uuid, owner);

			if (pm.canRead(username))
			{
				List<MetadataPermission> permissions = MetadataPermissionDao.getByUuid(uuid, offset, limit);
				
				if (StringUtils.isEmpty(sharedUsername)) 
				{
					String jPems = new MetadataPermission(uuid, owner, PermissionType.ALL).toJSON();
					for (MetadataPermission permission: permissions)
	    			{
						if (!StringUtils.equals(permission.getUsername(), owner)) {
							jPems += "," + permission.toJSON();
						}
					}
					return new IplantSuccessRepresentation("[" + jPems + "]");
				} 
				else 
				{
					if (ServiceUtils.isAdmin(sharedUsername) || StringUtils.equals(owner, sharedUsername))
					{
						MetadataPermission pem = new MetadataPermission(uuid, sharedUsername, PermissionType.ALL);
						return new IplantSuccessRepresentation(pem.toJSON());
					}
					else 
					{
						MetadataPermission pem = MetadataPermissionDao.getByUsernameAndUuid(sharedUsername, uuid);
						if (pem == null) 
						{
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No permissions found for user " + sharedUsername);
						}
						else 
						{
							return new IplantSuccessRepresentation(pem.toJSON());
						}
					}
				}
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to view this resource");
			}

		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			// Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
		
	}

	/**
	 * Post action for adding (and overwriting) permissions on a metadata iod
	 * 
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		try
		{
			if (StringUtils.isEmpty(uuid))
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No metadata id provided.");
			}
			
			String name = null;
            String sPermission = null;

            JSONObject postPermissionData = super.getPostedEntityAsJsonObject(true);
            
            if (StringUtils.isEmpty(sharedUsername))
            {
            	AgaveLogServiceClient.log(METADATA02.name(), MetaPemsCreate.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
                if (postPermissionData.has("username")) 
                {
                    name = postPermissionData.getString("username");
            	} 
                else
                {	
                	// a username must be provided either in the form or the body
                	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                			"No username specified. Please specify a valid user to whom the permission will apply."); 
                }
            }
            else
            {
            	AgaveLogServiceClient.log(METADATA02.name(), MetaPemsUpdate.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
            	
            	// name in url and json, if provided, should match
            	if (postPermissionData.has("username") && 
            			!StringUtils.equalsIgnoreCase(postPermissionData.getString("username"), sharedUsername)) 
                {
            		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
            				"The username value in the POST body, " + postPermissionData.getString("username") + 
                			", does not match the username in the URL, " + sharedUsername);            		
            	} 
                else
                {
                	name = sharedUsername;
                }
            }
            
            if (postPermissionData.has("permission")) 
            {
                sPermission = postPermissionData.getString("permission");
                if (StringUtils.equalsIgnoreCase(sPermission, "none") ||
                		StringUtils.equalsIgnoreCase(sPermission, "null")) {
                	sPermission = null;
                }
            } 
            else 
            {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Missing permission field. Please specify a valid permission of READ, WRITE, or READ_WRITE.");
            }
            
			if (!ServiceUtils.isValid(name)) { 
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST, "No user found matching " + name); 
			} 
			else 
			{
				// validate the user they are giving permissions to exists
				AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
						Settings.IPLANT_PROFILE_SERVICE, 
						Settings.IRODS_USERNAME, 
						Settings.IRODS_PASSWORD);
				
				if (authClient.getUser(name) == null) {
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No user found matching " + name);
				}
			}
			
			MetadataPermissionManager pm = new MetadataPermissionManager(uuid, owner);

			if (pm.canWrite(username))
			{
				// if the permission is null or empty, the permission
				// will be removed
				try 
				{
					pm.setPermission(name, sPermission );
					if (StringUtils.isEmpty(sPermission)) {
						getResponse().setStatus(Status.SUCCESS_OK);
					} else {
						getResponse().setStatus(Status.SUCCESS_CREATED);
					}
					
					MetadataPermission permission = MetadataPermissionDao.getByUsernameAndUuid(name, uuid);
					if (permission == null) {
						permission = new MetadataPermission(uuid, name, PermissionType.NONE);
					}
					
					getResponse().setEntity(new IplantSuccessRepresentation(permission.toJSON()));
				} 
				catch (PermissionException e) {
					throw new ResourceException(
							Status.CLIENT_ERROR_FORBIDDEN,
							e.getMessage(), e);
				}
				catch (IllegalArgumentException iae) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST,
							"Invalid permission value. Valid values are: " + PermissionType.supportedValuesAsString());
				}
			}
			else
			{
				throw new ResourceException(
						Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to modify this resource.");
			}

		}
		catch (ResourceException e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Exception e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to update metadata permissions: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#removeRepresentations()
	 */
	@Override
	public void removeRepresentations() throws ResourceException
	{
		AgaveLogServiceClient.log(METADATA02.name(), MetaPemsDelete.name(), username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(uuid))
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("No metadata id provided"));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}
		
		try
		{
			MetadataPermissionManager pm = new MetadataPermissionManager(uuid, owner);

			if (pm.canWrite(username))
			{
				if (StringUtils.isEmpty(sharedUsername)) {
					// clear all permissions
					pm.clearPermissions();
				} else { // clear pems for user
					pm.setPermission(sharedUsername, null);
				}
				
				getResponse().setEntity(new IplantSuccessRepresentation());
			}
			else
			{
				throw new ResourceException(
						Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to modify this resource.");
			}
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to remove metadata permissions: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}

	/**
	 * Allow the resource to be modified
	 * 
	 * @return
	 */
	public boolean setModifiable()
	{
		return true;
	}

	/**
	 * Allow the resource to be read
	 * 
	 * @return
	 */
	public boolean setReadable()
	{
		return true;
	}
}
