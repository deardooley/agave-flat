/**
 * 
 */
package org.iplantc.service.jobs.managers;


/**
 * Generic quota check interface.
 * 
 * @author dooley
 *
 */
public interface QuotaCheck {

	public void check() throws Exception;
}
