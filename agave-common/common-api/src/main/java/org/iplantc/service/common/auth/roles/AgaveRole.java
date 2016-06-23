/**
 * 
 */
package org.iplantc.service.common.auth.roles;

import org.restlet.Application;
import org.restlet.security.Role;

/**
 * @author dooley
 *
 */
public class AgaveRole extends Role {
    
    public static final AgaveRole ROLE_SERVICE = new AgaveRole("SERVICE");
    public static final AgaveRole ROLE_OWN = new AgaveRole("OWN");
    public static final AgaveRole ROLE_ADMIN = new AgaveRole("ADMIN");
    public static final AgaveRole ROLE_SHARE = new AgaveRole("SHARE");
    public static final AgaveRole ROLE_EDIT = new AgaveRole("EDIT");
    public static final AgaveRole ROLE_VIEW = new AgaveRole("VIEW");
    public static final AgaveRole ROLE_GUEST = new AgaveRole("GUEST");
    public static final AgaveRole ROLE_NONE = new AgaveRole("NONE");
    
    /**
     * 
     */
    public AgaveRole() {
        super(Application.getCurrent(), "NONE");
    }

    /**
     * @param application
     * @param name
     */
    public AgaveRole(String name) {
        super(Application.getCurrent(), name);
    }

    /**
     * @param application
     * @param name
     * @param description
     */
    public AgaveRole(String name, String description) {
        super(Application.getCurrent(), name, description);
    }

}
