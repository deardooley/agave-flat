package org.iplantc.service.common.model;

import java.sql.Timestamp;
import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface AgaveEntityEvent {

	/**
	 * @return the id
	 */
	public abstract Long getId();

	/**
	 * @param id
	 *            the id to set
	 */
	public abstract void setId(Long id);

	/**
	 * @return the softwareUuid
	 */
	public abstract String getEntity();

	/**
	 * @param entity
	 *            the uuid of the entity to set
	 */
	public abstract void setEntity(String entityUuid);

	/**
	 * @return the status
	 */
	public abstract String getStatus();

	/**
	 * @param status
	 *            the status to set
	 */
	public abstract void setStatus(String status);

	/**
	 * @return the username
	 */
	public abstract String getCreatedBy();

	/**
	 * @param username
	 *            the creator to set
	 */
	public abstract void setCreatedBy(String createdBy);

	/**
	 * @return the message
	 */
	public abstract String getDescription();

	/**
	 * @param description
	 *            the description to set
	 */
	public abstract void setDescription(String description);

	/**
	 * @return the ipAddress
	 */
	public abstract String getIpAddress();

	/**
	 * @param ipAddress
	 *            the ipAddress to set
	 */
	public abstract void setIpAddress(String ipAddress);

	/**
	 * @return the tenantId
	 */
	public abstract String getTenantId();

	/**
	 * @param tenantId
	 *            the tenantId to set
	 */
	public abstract void setTenantId(String tenantId);

	/**
	 * @return the created
	 */
	public abstract Date getCreated();

	/**
	 * @return the uuid
	 */
	public abstract String getUuid();

	/**
	 * @param uuid
	 *            the uuid to set
	 */
	public abstract void setUuid(String uuid);

	/**
	 * @param created
	 *            the created to set
	 */
	public abstract void setCreated(Timestamp created);

	public abstract ObjectNode getLinks();

}