/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * @author dooley
 *
 */
public interface NotificationResource {

	@GET
	public Response getNotification(@PathParam("uuid") String uuid);
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateNotificationFromForm(@PathParam("uuid") String uuid,
											@FormParam("url") String callbackUrl,
											@FormParam("event") String notificationEvent,
											@FormParam("associatedUuid") String associatedUuid,
                                            @FormParam("persistent") String persistent,
                                			@FormParam("status") String status,
                                			@FormParam("policy.retryStrategyType") String retryStrategyType,
                                			@FormParam("policy.retryLimit") Integer retryLimit,
                                			@FormParam("policy.retryRate") String retryRate,
                                			@FormParam("policy.retryDelay") String retryDelay,
                                			@FormParam("policy.saveOnFailure") Boolean saveOnFailure);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateNotification(@PathParam("uuid") String uuid, Representation input);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateNotification(@PathParam("uuid") String uuid, byte[] bytes);
	
	@DELETE
	public Response deleteNotification(@PathParam("uuid") String uuid);

	

}
