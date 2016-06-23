package org.iplantc.service.realtime.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
public interface RealtimeResource {

    @GET
    @Path("{channelId}")
    javax.ws.rs.core.Response represent() throws Exception;

//    @PUT
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Path("{channelId}")
//    javax.ws.rs.core.Response store(@PathParam("channelId") String entityId, byte[] bytes) throws Exception;
//    
    @DELETE
    @Path("{channelId}")
    void remove() throws Exception;

}

