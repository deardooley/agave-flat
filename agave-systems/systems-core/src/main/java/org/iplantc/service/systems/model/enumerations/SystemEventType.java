package org.iplantc.service.systems.model.enumerations;

public enum SystemEventType {
    CREATED("This system was created"),
    UPDATED("This system was updated"),
    DELETED("This system was deleted"),
    ENABLED("This system was enabled"),
    DISABLED("This system was disabled"),
    ERASED("This system was erased and the system id free up for reuse"),
    UNSET_PUBLIC_DEFAULT("The system was unset as the default system of this type for the tenant."),
    SET_PUBLIC_DEFAULT("The system was unset as the default system of this type for the tenant."),
    
    CLEAR_CREDENTIALS("All authentication credentials were removed for this system."),
    UPDATE_DEFAULT_CREDENTIAL("The default authentication credentials for this system were updated."),
    UPDATE_CREDENTIAL("One or more authentication credentials for this system were updated."),
    REMOVE_CREDENTIAL("One or more authentication credentials for this system were removed."),
    REMOVE_DEFAULT_CREDENTIAL("The default authentication credentials for this system were removed."),
    UNSET_USER_DEFAULT("One or more users unset this system as their default system of this type."),
    SET_USER_DEFAULT("One or more users unset this system as their default system of this type."),
    
    
    ROLES_GRANT("A new user role was granted on this system"),
    ROLES_REVOKE("A user role was revoked on this sytem"),
    QUEUE_CREATE("A new batch queue was added to this system"),
    QUEUE_UPDATE("A batch queue was updated on this system"),
    QUEUE_DELETE("A batch queue was deleted from this system"),
    
    
    CREDENTIAL_CREATION("A new credential was pulled for this sytem"),
    CREDENTIAL_REFRESH("A credential was updated on this sytem"),
    CREDENTIAL_EXPIRATION("A credential expired on this system"),
    AUTH_CONFIG_UPDATE("The authentication configuration for this system was updated"),
    AUTH_CONFIG_DELETE("The authentication configuration for this system was removed"),
    AUTH_CONFIG_CREATE("A new authentication configuration for this system was added"),
    
    
    APP_CREATE("A new app was registered on this system"),
    APP_UPDATE("An app registred on this system was updated"),
    APP_DELETE("An app registered on this system was deleted"),
    JOB_SUBMIT("A job request was made to this system"),
    JOB_STAGE("A job began staging input data to this system"),
    JOB_QUEUE("A job was placed into a batch queue on this system"),
    JOB_COMPLETE("A job completed execution on this system"),
    STATUS_UPDATE("The status of this system was changed"),
    PUBLISH("This system was published into the public scope"),
    UNPUBLISH("This system was removed from the public scope");
    
    private String description;
    
    /**
     * @return the description
     */
    public synchronized String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public synchronized void setDescription(String description) {
        this.description = description;
    }

    private SystemEventType(String description) {
        this.description = description;
    }
}
