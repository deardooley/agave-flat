package org.iplantc.service.tags.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("{entityId}")
public interface TagResource {

    @GET
    Response represent(@PathParam("entityId") String entityId) throws Exception;

    @POST
    Response accept(@PathParam("entityId") String entityId, Representation input) throws Exception;
    
    @DELETE
    Response remove(@PathParam("entityId") String entityId) throws Exception;

}

