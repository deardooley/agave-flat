package org.iplantc.service.tags.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("{entityId}/associatedIds/{uuid}")
public interface TagResourceResource {

    @GET
    public Response represent(@PathParam("entityId") String entityId,
    						  @PathParam("uuid") String associateUuid) throws Exception;

    @POST
    public Response accept(@PathParam("entityId") String entityId,
    						  @PathParam("uuid") String associateUuid) throws Exception;

    @DELETE
    public Response remove(@PathParam("entityId") String entityId,
			    @PathParam("uuid") String associateUuid) throws Exception;

}

