/**
 * 
 */
package org.iplantc.service.common.uuid2;

/**
 * Parent test for all UUID Tests. Handles abstract definitions of the
 * entity types and provides a general framework for adding tests in 
 * every package.
 * 
 * @author dooley
 *
 */
public interface AbstractUUIDTest<T> {
	/**
	 * Get the type of the entity. This is the type of UUID to test.
	 * @return
	 */
	abstract UUIDType getEntityType();
	
	/**
	 * Create a test entity persisted and available for lookup.
	 * @return
	 */
	abstract T createEntity();
	
	/**
	 * Serialize the entity into a JSON string. This should call out
	 * to a {@link toJSON()} method in the entity or delegate to 
	 * Jackson Annotations.
	 * @param testEntity entity to serialize
	 * @return
	 */
	abstract String serializeEntityToJSON(T testEntity);
	
	/**
	 * Get the uuid of the entity. This should call out to the {@link #getUuid()}
	 * method in the entity.
	 * @param testEntity
	 * @return
	 */
	abstract String getEntityUuid(T testEntity);
	
	
}
