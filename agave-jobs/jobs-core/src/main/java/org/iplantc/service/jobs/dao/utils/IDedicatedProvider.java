package org.iplantc.service.jobs.dao.utils;

/** This interface allows production and test implementations of the 
 * dedicated configuration retrieval methods.  See DedicatedConfig for
 * details.
 * 
 * @author rcardone
 */
public interface IDedicatedProvider
{
    String   getDedicatedTenantIdForThisService();
    String[] getDedicatedUsernamesFromServiceProperties();
    String[] getDedicatedSystemIdsFromServiceProperties();
}
