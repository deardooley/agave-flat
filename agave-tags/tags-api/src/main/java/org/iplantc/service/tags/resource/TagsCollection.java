package org.iplantc.service.tags.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

@Path("")
public interface TagsCollection {

    @GET
    public Response getTags() throws Exception;

    @POST
	public Response addTag(Representation input);
}

