package org.iplantc.service.realtime.resource.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.iplantc.service.realtime.WebApiApplication;
import org.restlet.util.Series;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;
import org.restlet.data.Reference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Preference;
import org.restlet.representation.Representation;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.security.Role;

import java.util.logging.Level;

/**
 * Defines common behaviour of server resources.
 */
public abstract class AbstractServerResource extends ServerResource {

    /**
     * Throws a {@link ResourceException} if the current user has not sufficient
     * permissions.
     * 
     * @param allowedGroups
     *            The list of allowed groups.
     * @param deniedGroups
     *            The list of denied groups.
     * @throws ResourceException
     */
    protected void checkGroups(String[] allowedGroups, String[] deniedGroups)
            throws ResourceException {
        if (!checkGroups(allowedGroups, deniedGroups, getRequest().getClientInfo().getRoles())) {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
        }
    }

    /**
     * Indicates if the given list of {@link Role} matches the list of allowed
     * and denied roles.
     * 
     * @param allowedRoles
     *            The list of allowed roles.
     * @param deniedRoles
     *            The list of denied roles.
     * @param userRoles
     *            The list of roles to check.
     * @return True if the list of roles match allowed and denied roles.
     */
    private boolean checkGroups(String[] allowedGroups, String[] deniedGroups, List<Role> userRoles) {
        boolean allowed = false;
		for (int i = 0; !allowed && i < allowedGroups.length; i++) {
		    allowed = "anyone".equals(allowedGroups[i]) || hasRole(userRoles, allowedGroups[i]);
        }
        for (int i = 0; allowed && i < deniedGroups.length; i++) {
            allowed = !("anyone".equals(deniedGroups[i]) || hasRole(userRoles, deniedGroups[i]));
        }
        return allowed;
    }

    /**
     * Indicates if the given role is in the list of roles.
     * @param roles
     *            The list of roles.
     * @param roleName
     *            The name of the role to look for.
     * @return True if the list of roles contains the given role.
     */
    protected boolean hasRole(List<Role> roles, String roleName) {
        for (Role role : roles) {
            if (role.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    protected String getRootUri() {
        Reference resourceRef = getRequest().getResourceRef();
        return resourceRef.toString();
    }

    protected String getCurrentSecurityIdentifier() {
        return getClientInfo().getUser().getIdentifier();
    }

    @Override
    public WebApiApplication getApplication() {
        return (WebApiApplication) super.getApplication();
    }
}