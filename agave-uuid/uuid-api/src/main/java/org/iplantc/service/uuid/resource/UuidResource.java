package org.iplantc.service.uuid.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("{uuid}")
public interface UuidResource {

    @GET
    Response getUuid(@PathParam("uuid") String uuid) throws Exception;

}
