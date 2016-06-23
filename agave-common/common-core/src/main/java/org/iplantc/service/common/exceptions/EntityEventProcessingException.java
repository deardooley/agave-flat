/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class EntityEventProcessingException extends Exception {


	/**
	 * 
	 */
	private static final long serialVersionUID = -2887295266636954858L;

	/**
	 * 
	 */
	public EntityEventProcessingException() {
	}

	/**
	 * @param paramString
	 */
	public EntityEventProcessingException(String paramString) {
		super(paramString);
	}

	/**
	 * @param paramThrowable
	 */
	public EntityEventProcessingException(Throwable paramThrowable) {
		super(paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 */
	public EntityEventProcessingException(String paramString,
			Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 * @param paramBoolean1
	 * @param paramBoolean2
	 */
	public EntityEventProcessingException(String paramString,
			Throwable paramThrowable, boolean paramBoolean1,
			boolean paramBoolean2) {
		super(paramString, paramThrowable, paramBoolean1, paramBoolean2);
	}
}
