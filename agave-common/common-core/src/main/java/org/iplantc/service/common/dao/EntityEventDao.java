package org.iplantc.service.common.dao;

import java.util.List;

import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.model.AgaveEntityEvent;

public interface EntityEventDao<T extends AgaveEntityEvent, V extends Enum> {

	/**
	 * Returns the {@link AbstractEntityEvent} with the given id.
	 * 
	 * @param uuid
	 * @return
	 * @throws EntityEventPersistenceException
	 */
	public abstract T getEntityEventByUuid(String uuid)
			throws EntityEventPersistenceException;

	/**
	 * Returns all software {@link EntityEvent} for the software with the given id.
	 * 
	 * @param softwareUuid
	 * @return
	 * @throws EntityEventPersistenceException
	 */
	public abstract List<T> getEntityEventByEntityUuid(String uuid)
			throws EntityEventPersistenceException;

	/**
	 * Returns all {@link AgaveEntityEvent} for the given id. Pagination
	 * is supported.
	 * 
	 * @param uuid
	 * @param limit
	 * @param offset
	 * @return
	 * @throws EntityEventPersistenceException
	 */
	public abstract List<T> getEntityEventByEntityUuid(String uuid, int limit,
			int offset) throws EntityEventPersistenceException;

	/**
	 * Fetches an {@link EntityEvent} for the given parent uuid. This serves as a
	 * quick check for the correctness of the {@link EntityEvent#getEntity()} value.
	 * 
	 * @param entityUuid
	 * @param entityEventUuid
	 * @return the matching {@link EntityEvent} or null of none can be found
	 * @throws EntityEventPersistenceException
	 */
	public abstract T getByEntityUuidAndEntityEventUuid(String entityUuid,
			String entityEventUuid) throws EntityEventPersistenceException;

	/**
	 * Gets the {@link EntityEvent} for the specified software id and software status
	 * 
	 * @param eventUuid
	 * @param eventType
	 * @return
	 * @throws EntityEventPersistenceException
	 */
	public abstract List<T> getAllEntityEventWithStatusForEntityUuid(
			String eventUuid, V eventType)
			throws EntityEventPersistenceException;

	/**
	 * Saves or updates a given {@link AgaveEntityEvent}
	 * @param event
	 * @throws EntityEventPersistenceException
	 */
	public abstract void persist(T event)
			throws EntityEventPersistenceException;

	/**
	 * Deletes the given {@link AgaveEntityEvent}
	 * 
	 * @param event
	 * @throws EntityEventPersistenceException
	 */
	public abstract void delete(T event) throws EntityEventPersistenceException;

	/**
	 * Deletes all {@link AgaveEntityEvent} for the given entity uuid
	 * 
	 * @param entityUuid
	 * @throws EntityEventPersistenceException
	 */
	public abstract void deleteByEntityId(String entityUuid)
			throws EntityEventPersistenceException;

}