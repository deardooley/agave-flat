/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class EntityEventPersistenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9061537896123798539L;

	/**
	 * 
	 */
	public EntityEventPersistenceException() {
	}

	/**
	 * @param paramString
	 */
	public EntityEventPersistenceException(String paramString) {
		super(paramString);
	}

	/**
	 * @param paramThrowable
	 */
	public EntityEventPersistenceException(Throwable paramThrowable) {
		super(paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 */
	public EntityEventPersistenceException(String paramString,
			Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 * @param paramBoolean1
	 * @param paramBoolean2
	 */
	public EntityEventPersistenceException(String paramString,
			Throwable paramThrowable, boolean paramBoolean1,
			boolean paramBoolean2) {
		super(paramString, paramThrowable, paramBoolean1, paramBoolean2);
	}

}
