/**
 * 
 */
package org.iplantc.service.profile.resource.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.*;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveErrorRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.dao.ProfileDAO;
import org.iplantc.service.profile.dao.ProfileDAOFactory;
import org.iplantc.service.profile.model.Profile;
import org.iplantc.service.profile.model.enumeration.SearchFieldType;
import org.iplantc.service.profile.resource.ProfileResource;
import org.restlet.Request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author dooley
 *
 */
@Produces("application/json")
public class ProfileResourceImpl extends AbstractAgaveResource implements ProfileResource 
{
	private ProfileDAO dao;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.profile.resource.ProfileResource#getProfiles()
	 */
	@Override
	@GET
	public Response getProfiles(@QueryParam("username") String username,
								@QueryParam("email") String email,
								@QueryParam("name") String name,
								@QueryParam("status") String status,
								@QueryParam("pretty") boolean pretty)
	{
		dao = new ProfileDAOFactory().getProfileDAO(Settings.IPLANT_DATA_SOURCE);
		
		try
		{
			List<Profile> profiles = new ArrayList<Profile>();
			
			if (!StringUtils.isEmpty(username)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), 
						ProfileSearchUsername.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				profiles.addAll(dao.searchByUsername(username));
			}
			else if (!StringUtils.isEmpty(email)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), 
						ProfileSearchEmail.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				profiles.addAll(dao.searchByEmail(email));
			}
			else if (!StringUtils.isEmpty(name)) 
			{
				AgaveLogServiceClient.log(PROFILES02.name(), 
						ProfileSearchName.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				
				profiles.addAll(dao.searchByFullName(name));
			}
			
//			final ProfileList profileList = new ProfileList();
			//profileList.addAll(dao.searchByUsername(""));
			
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode jsonArray = mapper.createArrayNode();
			
			for (int i=getOffset(); i< Math.min((getLimit()+getOffset()), profiles.size()); i++)
			{
				jsonArray.add(mapper.readTree(profiles.get(i).toJSON()));
			}	
			
			return Response.ok(new AgaveSuccessRepresentation(jsonArray.toString())).build();
			
//			return Response.ok(profileList).build();
		}
		catch (Exception e)
		{
			return Response.serverError().entity(
					new AgaveErrorRepresentation("Failed to retrieve profile information.")).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.profile.resource.ProfileResource#getProfile(java.lang.String)
	 */
	@Override
	@GET
	@Path("{username}")
	public Response getProfile(@PathParam("username") String username)
	{
		AgaveLogServiceClient.log(PROFILES02.name(), 
				ProfileSearchUsername.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		dao = new ProfileDAOFactory().getProfileDAO(Settings.IPLANT_DATA_SOURCE);
		
		try
		{
			if (StringUtils.equals(username, "me")) {
				username = getAuthenticatedUsername();
			}
			
			List<Profile> profiles = dao.searchByUsername(username);
			for (int i=getOffset(); i< Math.min((getLimit()+getOffset()), profiles.size()); i++)
			{
				Profile profile = profiles.get(i);
				
				if (profile.getUsername().equalsIgnoreCase(username)) {
					return Response.ok(new AgaveSuccessRepresentation(profile.toJSON())).build();
				}
			}
			
			return Response.status(Status.NOT_FOUND)
					.entity(new AgaveErrorRepresentation("No user found matching " + username)).build(); 
		}
		catch (Exception e)
		{
			return Response.serverError().entity(new AgaveErrorRepresentation(
					"Failed to retrieve profile information for " + username)).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.profile.resource.ProfileResource#findProfilesByPathTerms(org.iplantc.service.profile.model.enumeration.SearchFieldType, java.lang.String)
	 */
	@Override
	@GET
	@Path("search/{term}/{value}")
	public Response findProfilesByPathTerms(
			@PathParam("term") String type,
			@PathParam("value") String value)
	{
		String logActivity = "USERNAME";
		
		if (StringUtils.equalsIgnoreCase("email",type))
			logActivity = "EMAIL";
		else if (StringUtils.equalsIgnoreCase("name",type))
			logActivity = "NAME";
			
		AgaveLogServiceClient.log(PROFILES02.name(), 
				logActivity, 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		dao = new ProfileDAOFactory().getProfileDAO(Settings.IPLANT_DATA_SOURCE);
		
		try
		{
			SearchFieldType searchType = SearchFieldType.valueOf(type.toUpperCase());
			
			List<Profile> profiles = dao.searchByType(searchType.name().toLowerCase(), value);
			
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode jsonArray = mapper.createArrayNode();
			
			for (int i=getOffset(); i< Math.min((getLimit()+getOffset()), profiles.size()); i++)
			{
				jsonArray.add(mapper.readTree(profiles.get(i).toJSON()));
			}	
			
			return Response.ok(new AgaveSuccessRepresentation(jsonArray.toString())).build();
		}
		catch (IllegalArgumentException e) {
			return Response.serverError().entity(new AgaveErrorRepresentation("Invalid search term. Valid search terms are: username, email, and name.")).build();
		}
		catch (Exception e)
		{
			return Response.serverError().entity(new AgaveErrorRepresentation("Failed to search for profiles matching " + type + " = " + value)).build();
//			throw new WebApplicationException(
//					new ProfileException("Failed to search for profiles matching " + type + " = " + value, e), 
//					Status.INTERNAL_SERVER_ERROR);
		}
	}

//	/* (non-Javadoc)
//	 * @see org.iplantc.service.profile.resource.ProfileResource#findProfilesByQueryTerms(org.iplantc.service.profile.model.enumeration.SearchFieldType, java.lang.String)
//	 */
//	@Override
//	@GET
//	public Response findProfilesByQueryTerms(
//			@QueryParam("term") String type,
//			@QueryParam("value") String value)
//	{
//		dao = new ProfileDAOFactory().getProfileDAO(Settings.IPLANT_DATA_SOURCE);
//		
//		try
//		{
//			SearchFieldType searchType = SearchFieldType.valueOf(type.toUpperCase());
//			
//			final ProfileList profileList = new ProfileList();
//			//profileList.setProfiles(dao.searchByType(searchType.name().toLowerCase(), value));
//			
//			return Response.ok(dao.searchByType(searchType.name().toLowerCase(), value)).build();
//		}
//		catch (IllegalArgumentException e) {
//			return Response.serverError().entity(new AgaveErrorRepresentation("Invalid search term. Valid search terms are: username, email, and name.")).build();
//		}
//		catch (Exception e)
//		{
//			return Response.serverError().entity(
//					new AgaveErrorRepresentation(
//							"Failed to search for profiles matching " + type + " like " + value)).build();
//		}
//	}

}
