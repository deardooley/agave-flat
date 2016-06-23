/**
 * 
 */
package org.iplantc.service.apps.authorizer;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;

/**
 * @author dooley
 *
 */
public class SoftwareRoleChecker extends Authorizer {

    /**
     * 
     */
    public SoftwareRoleChecker() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param identifier
     */
    public SoftwareRoleChecker(String identifier) {
        super(identifier);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.restlet.security.Authorizer#authorize(org.restlet.Request, org.restlet.Response)
     */
    @Override
    protected boolean authorize(Request arg0, Response arg1) {
        // TODO Auto-generated method stub
        return false;
    }

}
