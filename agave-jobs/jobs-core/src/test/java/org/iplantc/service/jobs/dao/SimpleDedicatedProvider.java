package org.iplantc.service.jobs.dao;

import org.iplantc.service.jobs.dao.utils.IDedicatedProvider;

/** This class allows test programs to create and modify dedicated configuration 
 * information.  See DedicatedConfig for details.
 *  
 * @author rcardone
 */
public final class SimpleDedicatedProvider
 implements IDedicatedProvider
{
    // Fields set directly by test programs.
    public String   tenantId;
    public String[] userNames;
    public String[] systemIds;
    
    @Override
    public String getDedicatedTenantIdForThisService(){return tenantId;}

    @Override
    public String[] getDedicatedUsernamesFromServiceProperties(){return userNames;}

    @Override
    public String[] getDedicatedSystemIdsFromServiceProperties(){return systemIds;}
}
