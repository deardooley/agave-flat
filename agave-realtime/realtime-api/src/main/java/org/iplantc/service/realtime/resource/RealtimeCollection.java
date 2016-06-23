package org.iplantc.service.realtime.resource;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.iplantc.service.realtime.model.RealtimeChannel;

public interface RealtimeCollection {

    @GET
    Response represent() throws Exception;

//    @POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    javax.ws.rs.core.Response add(Notification notification) throws Exception;

}

