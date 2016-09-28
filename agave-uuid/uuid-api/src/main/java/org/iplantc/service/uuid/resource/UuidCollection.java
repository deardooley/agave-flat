package org.iplantc.service.uuid.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("")
public interface UuidCollection {

  @POST
	public Response createUuid(Representation input);
}
