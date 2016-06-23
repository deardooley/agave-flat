package org.iplantc.service.io.resources;

import java.util.List;

import org.iplantc.service.io.FilesApplication;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.security.Role;

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
    public FilesApplication getApplication() {
        return (FilesApplication) super.getApplication();
    }
}