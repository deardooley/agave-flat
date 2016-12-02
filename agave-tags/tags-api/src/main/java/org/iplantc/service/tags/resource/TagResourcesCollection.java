package org.iplantc.service.tags.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("{entityId}/resources")
public interface TagResourcesCollection {

    @GET
    public Response getTagAssociationIds(@PathParam("entityId") String tagId) throws Exception;

	@POST
	public Response addTagAssociationIds(@PathParam("entityId") String tagId, Representation input);
	
    @DELETE
    public Response clearTagAssociationIds(@PathParam("entityId") String tagId) throws Exception;

}

