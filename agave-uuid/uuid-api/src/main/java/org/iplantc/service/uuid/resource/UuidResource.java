package org.iplantc.service.uuid.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("{uuid}")
public interface UuidResource {

    /**
     * Resolves a uuid into a resource. If expand is truthy, return the full
     * resource representation rather than just the metadata.
     * @param uuid resource uuid to resolve
     * @param expand truthy value indicating whether the resource should be resolved
     * @param filter filter parameters on the response json
     * @return 
     * @throws Exception
     */
    @GET
    Response getUuid(@PathParam("uuid") String uuid, 
			@QueryParam("expand") String expand, 
			@QueryParam("filter") String filter) throws Exception;

}
