/**
 * 
 */
package org.iplantc.service.systems;

import org.iplantc.service.common.restlet.AgaveApplication;
import org.iplantc.service.systems.resources.BatchQueueManagementResource;
import org.iplantc.service.systems.resources.BatchQueueResource;
import org.iplantc.service.systems.resources.SystemCredentialResource;
import org.iplantc.service.systems.resources.SystemHistoryResource;
import org.iplantc.service.systems.resources.SystemManagementResource;
import org.iplantc.service.systems.resources.SystemRoleResource;
import org.iplantc.service.systems.resources.SystemsDocumentationResource;
import org.restlet.Router;

/**
 * @author dooley
 * 
 */
public class SystemsApplication extends AgaveApplication 
{
	@Override
	protected void mapServiceEndpoints(Router router)
	{

		router.attach(getStandalonePrefix() + "/usage", SystemsDocumentationResource.class);
		router.attach(getStandalonePrefix() + "/usage/", SystemsDocumentationResource.class);
		
		// Define the resource for the static usage page
		if (!Settings.SLAVE_MODE)
		{
			secureEndpoint(router, "", SystemManagementResource.class); 
			secureEndpoint(router, "/", SystemManagementResource.class); 
			secureEndpoint(router, "/{systemid}", SystemManagementResource.class); 
			secureEndpoint(router, "/{systemid}/", SystemManagementResource.class); 
			secureEndpoint(router, "/{systemid}/history", SystemHistoryResource.class); 
            secureEndpoint(router, "/{systemid}/history/", SystemHistoryResource.class);
            secureEndpoint(router, "/{systemid}/queues", BatchQueueResource.class); 
            secureEndpoint(router, "/{systemid}/queues/", BatchQueueResource.class);
            secureEndpoint(router, "/{systemid}/queues/{queueid}", BatchQueueManagementResource.class); 
            secureEndpoint(router, "/{systemid}/queues/{queueid}/", BatchQueueManagementResource.class); 
            secureEndpoint(router, "/{systemid}/roles", SystemRoleResource.class); 
			secureEndpoint(router, "/{systemid}/roles/", SystemRoleResource.class); 
			secureEndpoint(router, "/{systemid}/roles/{user}", SystemRoleResource.class); 
			secureEndpoint(router, "/{systemid}/roles/{user}/", SystemRoleResource.class); 
			secureEndpoint(router, "/{systemid}/credentials", SystemCredentialResource.class); 
			secureEndpoint(router, "/{systemid}/credentials/", SystemCredentialResource.class); 
			secureEndpoint(router, "/{systemid}/credentials/{user}", SystemCredentialResource.class); 
			secureEndpoint(router, "/{systemid}/credentials/{user}/", SystemCredentialResource.class); 
			secureEndpoint(router, "/{systemid}/credentials/{user}/{type}", SystemCredentialResource.class); 
			secureEndpoint(router, "/{systemid}/credentials/{user}/{type}/", SystemCredentialResource.class); 
		}
	}
    
    @Override
	protected String getStandalonePrefix() {
		return !isStandaloneMode() ? "" : "/systems";
	}
    
}