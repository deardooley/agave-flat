package org.iplantc.service.profile.resource.impl;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("profiles/{username}/users")
@Produces("application/json")
public class StandaloneInternalUserResourceImpl extends InternalUserResourceImpl
{
	
}
