/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.exceptions.PermissionValidationException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.managers.TagManager;
import org.iplantc.service.tags.managers.TagPermissionManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.TaggedResource;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.iplantc.service.tags.resource.TagPermissionResource;
import org.iplantc.service.tags.resource.TagResourceResource;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles {@link SoftwarePermission} for a single user on a single
 * {@link Software} object.
 * 
 * @author dooley
 *
 */
@Path("{entityId}/associationIds/{uuid}")
public class TagResourceResourceImpl extends AbstractTagResource implements
		TagResourceResource {

	private static final Logger log = Logger.getLogger(TagResourceResourceImpl.class);

	public TagResourceResourceImpl() {
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResourceResource#represent(java.lang.String, java.lang.String)
	 */
	@Override
	public Response represent(@PathParam("entityId") String entityId,
			@PathParam("uuid") String associatedUuid) {

		logUsage(AgaveLogServiceClient.ActivityKeys.TagResourceGetById);

		try {
			Tag tag = getResourceFromPathValue(entityId);

			TaggedResource tr = tag.getTaggedResource(associatedUuid);

			if (tr == null) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"No resource found for the given tag with uuid "
								+ associatedUuid);
			} else {
				ObjectMapper mapper = new ObjectMapper();

				return Response.ok(
						new AgaveSuccessRepresentation(mapper
								.writeValueAsString(tr))).build();
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Throwable e) {
			log.error("Failed to retrieve user permissions", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve user permissions.", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResourceResource#accept(java.lang.String, java.lang.String)
	 */
	@Override
	public Response accept(@PathParam("entityId") String entityId,
			@PathParam("uuid") String associatedUuid) {

		logUsage(AgaveLogServiceClient.ActivityKeys.TagResourceAdd);

		try {
			Tag tag = getResourceFromPathValue(entityId);

			TagPermissionManager pm = new TagPermissionManager(tag);

			if (!pm.canWrite(getAuthenticatedUsername())) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to set permissions for tag "
								+ tag.getUuid() + "");
			} else {
				try {
					TagManager manager = new TagManager();
					ObjectMapper mapper = new ObjectMapper();

					Tag updatedTag = manager.updateTagAssociationId(tag,
							mapper.createArrayNode().add(associatedUuid),
							getAuthenticatedUsername());

					TaggedResource tr = updatedTag
							.getTaggedResource(associatedUuid);
					if (tr == null) {
						throw new ResourceException(
								Status.SERVER_ERROR_INTERNAL,
								"Unable to locate tagged resource after tagging");
					} else {
						return Response.ok(
								new AgaveSuccessRepresentation(mapper
										.writeValueAsString(tr))).build();
					}
				} catch (IllegalArgumentException iae) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST,
							"Invalid permission value. Valid values are: "
									+ PermissionType.supportedValuesAsString());
				} catch (PermissionValidationException e) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"Invalid permission value. " + e.getMessage());
				}
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to updated permission", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update permission.", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResourceResource#remove(java.lang.String, java.lang.String)
	 */
	@Override
	public Response remove(@PathParam("entityId") String entityId,
			@PathParam("uuid") String associatedUuid) {
		logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionDelete);

		try {
			Tag tag = getResourceFromPathValue(entityId);

			TagPermissionManager pm = new TagPermissionManager(tag);

			if (!pm.canWrite(getAuthenticatedUsername())) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to remove associated Id for tag "
								+ tag.getUuid());
			} else {
				TagManager manager = new TagManager();
				manager.deleteUserTagAssociatedId(tag, associatedUuid,
						getAuthenticatedUsername());
			}

			return Response.ok(new AgaveSuccessRepresentation()).build();

		} catch (IllegalArgumentException iae) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Invalid permission value. Valid values are: "
							+ PermissionType.supportedValuesAsString());
		} catch (TagPermissionException e) {
			log.error(e);
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL,
					"Failed to remove "
							+ associatedUuid
							+ " from tag "
							+ entityId
							+ ". If this problem persists, please contact your administrator.");
		} catch (PermissionValidationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					e.getMessage());
		} catch (Exception e) {
			log.error("Failed to remove user permissions", e);
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL,
					"Failed to remove "
							+ associatedUuid
							+ " from tag "
							+ entityId
							+ ". If this problem persists, please contact your administrator.",
					e);
		}
	}
}
