package org.iplantc.service.uuid.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("")
public interface UuidCollection {

	/**
	 * Create a new uuid for the resource type supplied in the body.
	 * @param input
	 * @return
	 */
	@POST
	public Response createUuid(Representation input);

	/**
	 * Lookup the resources for the given uuid. Optionally expanding the resources
	 * into their full representations
	 * @param uuids
	 * @param expand
	 * @param filter
	 * @return
	 */
	@GET
	public Response searchUuid(@QueryParam("uuids") String uuids,
							   @QueryParam("expand") String expand,
							   @QueryParam("filter") String filter);
}
