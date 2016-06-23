package org.iplantc.service.profile.resource.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.*;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
//import javax.ws.rs.core.Response.Status;



import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.manager.InternalUserManager;
import org.iplantc.service.profile.model.InternalUser;
import org.iplantc.service.profile.resource.InternalUserResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

@Path("{username}/users")
@Produces("application/json")
public class InternalUserResourceImpl extends AbstractAgaveResource implements InternalUserResource 
{
	private InternalUserManager manager = new InternalUserManager();
	
	@Override
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Path("")
	public Response addInternalUserFromForm(@PathParam("username") String username,
										@MatrixParam("") InternalUser jsonInternalUser)
	{
		checkClientPrivileges(username);
		
		try
		{
			if (jsonInternalUser == null) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No internal user provided.");
			} 
	    	else 
	    	{	
	    		InternalUser internalUser = null;
	    		
	    		// parse and validate the json
        		if (!StringUtils.isEmpty(jsonInternalUser.getUsername())) {
        			internalUser = manager.getInternalUser(jsonInternalUser.getUsername(), username);
        		} 
        		
        		if (internalUser == null) 
        		{
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUsersRegistration.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.addInternalUser(new JSONObject(jsonInternalUser.toJSON()), username);
        		} 
        		else 
        		{
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.updateInternalUser(new JSONObject(jsonInternalUser.toJSON()), internalUser);
        		}
	    		
        		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
	    	}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
	}

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("")
	public Response addInternalUser(@PathParam("username") String username,
			byte[] bytes)
	{
		checkClientPrivileges(username);
		try
		{
			if (bytes == null || bytes.length == 0) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No internal user provided.");
			} 
	    	else 
	    	{	
	    		JSONObject jsonInternalUser = new JSONObject(new String(bytes));
	    		
	    		InternalUser internalUser = null;
	    		
	    		// parse and validate the json
        		if (jsonInternalUser.has("username")) {
        			internalUser = manager.getInternalUser(jsonInternalUser.getString("username"), username);
        		} 
        		
        		if (internalUser == null) 
        		{
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUsersRegistration.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.addInternalUser(jsonInternalUser, username);
        		} 
        		else 
        		{
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.updateInternalUser(jsonInternalUser, internalUser);
        		}
        		
        		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
	    	}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("")
    public Response addInternalUsersFromFile(@PathParam("username") String username, 
    												   Representation input)
    {
		checkClientPrivileges(username);
		
		try
		{
			JSONObject jsonInternalUser = super.getPostedContentAsJsonObject(input);
			
			if (jsonInternalUser == null) {
				
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No internal user provided.");
			} 
	    	else 
	    	{	
	    		InternalUser internalUser = null;
	    		
	    		// parse and validate the json
        		if (jsonInternalUser.has("username")) {
        			internalUser = manager.getInternalUser(jsonInternalUser.getString("username"), username);
        		} 
        		
        		if (internalUser == null) {
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUsersRegistration.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.addInternalUser(jsonInternalUser, username);
        		} else {
        			AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        			internalUser = manager.updateInternalUser(jsonInternalUser, internalUser);
        		}
	    		
        		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
	    	}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
    }
	
	@Override
	@DELETE
	@Path("")
	public Response deleteInternalUsers(@PathParam("username") String username)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserClear.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		try
		{
			for (InternalUser internalUser: manager.getActiveInternalUsers(username)) {
				manager.deleteInternalUser(internalUser);
			}
			
			return Response.ok(new AgaveSuccessRepresentation()).build();
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal users", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to delete internal users", e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal users", e);
		}
	}

	@Override
	@GET
	@Path("{internalUsername}")
	public Response getInternalUser(@PathParam("username") String username,
			@PathParam("internalUsername") String internalUsername)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserGet.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		try
		{
			InternalUser internalUser = manager.getInternalUser(internalUsername, username);
			
			return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"Failed to retrieve internal user " + internalUsername, e);
		}
	}

	@Override
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Path("{internalUsername}")
	public Response updateInternalUserFromForm(
			@PathParam("username") String username,
			@PathParam("internalUsername") String internalUsername,
			@MatrixParam("") InternalUser jsonInternalUser)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		try 
		{
			InternalUser internalUser = manager.getInternalUser(internalUsername, username);
			
			if (internalUser == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No internal user matching that username provided.");
			}
			else if (!internalUser.isActive()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Internal user '" + internalUser.getUsername() + "' has already been marked as inactive.");
			}
			else 
			{
				if (jsonInternalUser == null) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"No internal user provided.");
				}
		    	else 
		    	{	
		    		if (StringUtils.isEmpty(jsonInternalUser.getUsername())) {
		    			throw new ProfileArgumentException("Please specify a valid string value for the 'username' field.");
		    		}
		    		else if (!StringUtils.equals(jsonInternalUser.getUsername(), internalUsername)) 
		    		{
		    			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
								"Uploaded user description does not match target uri. You uploaded a " +
										"user description for \"" + jsonInternalUser.getUsername() + "\" to the uri " +
										"for \"" + internalUsername + "\". Please either register a new " +
										"user or post your update to " + 
										Settings.IPLANT_PROFILE_SERVICE + "users/" + internalUsername +
										" to update that internal user.");
		    		}
	
		    		internalUser = manager.updateInternalUser(new JSONObject(jsonInternalUser.toJSON()), internalUser);
		    		
		    		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
		    	}
			}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
	}

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{internalUsername}")
	public Response updateInternalUserFromJSON(
			@PathParam("username") String username,
			@PathParam("internalUsername") String internalUsername, 
			byte[] bytes)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		try 
		{
			InternalUser internalUser = manager.getInternalUser(internalUsername, username);
			
			if (internalUser == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						"No internal user matching that username provided.");
			}
			else if (!internalUser.isActive()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Internal user '" + internalUser.getUsername() + "' has already been marked as inactive.");
			}
			else 
			{
				if (bytes == null || bytes.length == 0) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"No internal user provided.");
				} 
				else 
		    	{	
		    		JSONObject jsonInternalUser = new JSONObject(new String(bytes));
				
		    		// parse and validate the json
		    		if (jsonInternalUser.has("username")) 
		    		{
						if (!jsonInternalUser.getString("username").equalsIgnoreCase(internalUsername)) 
						{
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
									"Uploaded user description does not match target uri. You uploaded a " +
											"user description for \"" + jsonInternalUser.getString("username") + "\" to the uri " +
											"for \"" + internalUsername + "\". Please either register a new " +
											"user or post your update to " + 
											Settings.IPLANT_PROFILE_SERVICE + "users/" + internalUsername +
											" to update that internal user.");
		                }
		    		} 
		    		else 
		    		{
		    			throw new ProfileArgumentException("Please specify a valid string value for the 'username' field.");
		    		} 
		    		
		    		internalUser = manager.updateInternalUser(jsonInternalUser, internalUser);
		    		
		    		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
		    	}
			}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
	}
	
	@Override
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("{internalUsername}")
	public Response updateInternalUsersFromFile(
			@PathParam("username") String username, 
			@PathParam("internalUsername") String internalUsername, 
			Representation input)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserUpdate.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		try 
		{
			InternalUser internalUser = manager.getInternalUser(internalUsername, username);
			
			if (internalUser == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No internal user matching that username provided.");
			}
			else if (!internalUser.isActive()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Internal user '" + internalUser.getUsername() + "' has already been marked as inactive.");
			}
			else 
			{
				RestletFileUpload fileUpload = new RestletFileUpload(new DiskFileItemFactory());

		        // this list is always empty !!
		        List<FileItem> fileItems = fileUpload.parseRepresentation(input);
		        JSONObject jsonInternalUser = null;
		        for (FileItem fileItem : fileItems) {
		            if (!fileItem.isFormField()) {
		            	jsonInternalUser = new JSONObject(fileItem.getInputStream());
		            }
		        }
		        
				if (jsonInternalUser == null) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"No internal user provided.");
				}  
				else 
		    	{	
		    		// parse and validate the json
		    		if (jsonInternalUser.has("username")) 
		    		{
						if (!jsonInternalUser.getString("username").equalsIgnoreCase(internalUsername)) 
						{
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
									"Uploaded user description does not match target uri. You uploaded a " +
											"user description for \"" + jsonInternalUser.getString("username") + "\" to the uri " +
											"for \"" + internalUsername + "\". Please either register a new " +
											"user or post your update to " + 
											Settings.IPLANT_PROFILE_SERVICE + "users/" + internalUsername +
											" to update that internal user.");
		                }
		    		} 
		    		else 
		    		{
		    			throw new ProfileArgumentException("Please specify a valid string value for the 'username' field.");
		    		} 
		    		
		    		internalUser = manager.updateInternalUser(jsonInternalUser, internalUser);
		    		
		    		return Response.ok(new AgaveSuccessRepresentation(internalUser.toJSON())).build();
		    	}
			}
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save internal user: \"" + e.getMessage() + "\"", e);
	    }
		catch (JSONException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the internal user json description.", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update internal user: " + e.getMessage(), e);
		}
	}

	@Override
	@DELETE
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("{internalUsername}")
	public Response deleteInternalUser(@PathParam("username") String username,
			@PathParam("internalUsername") String internalUsername)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), InternalUserDelete.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		checkClientPrivileges(username);
		
		if (StringUtils.isEmpty(internalUsername)) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"No internal username value provided");
		}
		
		InternalUser internalUser;
		try
		{
			internalUser = manager.getInternalUser(internalUsername, username);
			
			if (internalUser == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No internal user matching that username provided.");
			}
			else if (!internalUser.isActive()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Internal user '" + internalUser.getUsername() + "' has already been deleted.");
			}
			
			manager.deleteInternalUser(internalUser);
			
			return Response.ok(new AgaveSuccessRepresentation()).build();
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Unable to delete internal user: \"" + e.getMessage() + "\"", e);
		}
		catch (ProfileException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to delete internal users " + internalUsername, e);
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to delete internal user: " + e.getMessage(), e);
		}
	}
	
	@Override
	@GET
	@Path("")
	public Response getInternalUsers(
			@PathParam("username") String username,
			@QueryParam("username") String internalUsername,
			@QueryParam("email") String email,
			@QueryParam("name") String name,
			@QueryParam("status") String status,
			@QueryParam("pretty") boolean pretty)
	{
		checkClientPrivileges(username);
		
		try
		{
			List<InternalUser> internalUsers = new ArrayList<InternalUser>();
			InternalUserDao dao = new InternalUserDao();
			
			if (!StringUtils.isEmpty(internalUsername)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), InternalUserSearchUsername.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				internalUsers.addAll(dao.findByExample(username, "username", internalUsername));
			}
			else if (!StringUtils.isEmpty(email)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), InternalUserSearchEmail.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				internalUsers.addAll(dao.findByExample(username, "email", email));
			}
			else if (!StringUtils.isEmpty(name)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), InternalUserSearchName.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				internalUsers.addAll(dao.findByExample(username, "name", name));
			}
			else if (!StringUtils.isEmpty(status)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), InternalUserSearchStatus.name(), getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				if (StringUtils.equalsIgnoreCase("active", status) || StringUtils.equals(status, "deleted"))
				{	
					internalUsers.addAll(dao.findByExample(username, "active", StringUtils.equalsIgnoreCase("active", status)));
				} 
				else
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid value for status. Valid internal user status values are: active, deleted.");
				}
			}
			else 
			{
				internalUsers.addAll(manager.getActiveInternalUsers(username));
			}
			
			JSONWriter writer = new JSONStringer();
			writer.array();
			
			for (int i=getOffset(); i< Math.min((getLimit()+getOffset()), internalUsers.size()); i++)
			{
				InternalUser user = internalUsers.get(i);
				
				writer.object()
					.key("uuid").value(user.getUuid())
					.key("createdBy").value(user.getCreatedBy())
					.key("active").value(user.isActive())
					.key("email").value(user.getEmail())
					.key("username").value(user.getUsername())
					.key("firstName").value(user.getFirstName())
					.key("lastName").value(user.getLastName())
					.key("_links").object()
			        	.key("self").object()
			        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + user.getCreatedBy() + "/" + user.getUuid())
				        .endObject()
						.key("profile").object()
				        	.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + user.getCreatedBy())
					    .endObject()
					.endObject()
		        .endObject();
			}
			writer.endArray();
			
			return Response.ok(new AgaveSuccessRepresentation(writer.toString())).build();
		}
		catch (ProfileException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
					"Failed to find internal users for the given query", e);
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to find internal users for the given query", e);
		}
	}
}
