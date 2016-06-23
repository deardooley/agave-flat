/**
 * 
 */
package org.iplantc.service.systems.model.enumerations;

/**
 * Types of systems one may register
 * @author dooley
 *
 */
public enum RemoteSystemType
{
	EXECUTION, STORAGE, AUTH;

	@Override
	public String toString() {
		return name();
	}
}
