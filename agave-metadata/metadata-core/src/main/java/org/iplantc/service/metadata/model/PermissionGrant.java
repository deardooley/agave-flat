/**
 * 
 */
package org.iplantc.service.metadata.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.model.validation.constraints.CheckAtLeastOneNotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Bean to handle permissions grants to a metadata item.
 * @author dooley
 *
 */
@CheckAtLeastOneNotNull(fieldNames={"username","group","role"})
public class PermissionGrant {
    
    /**
     * User granted special access
     */
    @JsonInclude(Include.NON_NULL)
    private String username;
    
    /**
     * Group granted special access
     */
    @JsonInclude(Include.NON_NULL)
    private String group;
    
    /**
     * Role granted special access
     */
    @JsonInclude(Include.NON_NULL)
    private String role;
    
    /**
     * User can read
     */
    @JsonInclude(Include.NON_NULL)
    private boolean read;
    
    /**
     * User can write  
     */
    @JsonInclude(Include.NON_NULL)
    private boolean write;
    
    /**
     * User can delete
     */
    @JsonInclude(Include.NON_NULL)
    private boolean delete;
    
    /**
     * User is owner
     */
    @JsonInclude(Include.NON_NULL)
    private boolean own;
    
    /**
     * User can create new acl
     */
    @JsonInclude(Include.NON_NULL)
    private boolean create_acl;
    
    /**
     * User can view acl
     */
    @JsonInclude(Include.NON_NULL)
    private boolean read_acl;
    
    /**
     * User can change existing acl
     */
    @JsonInclude(Include.NON_NULL)
    private boolean write_acl;
      
//    external roles per metadata entity                
//    tenant_id:meta_uuid | role[,role] | username 

//    external roles per files
//    tenant_id:system_id:absolute_path | role[,role] | username
    /**
     * User can delete acl
     */
    @JsonInclude(Include.NON_NULL)
    private boolean delete_acl;
    
    
    /**
     * User can delete
     */
    @NotNull
    private String uuid;
    
    /**
     * Timestamp of last time the permission was updated
     */
    @NotNull
    private Date lastUpdated;
    
    /**
     * Timestamp permission was created
     */
    @NotNull
    private Date created;
    
    public PermissionGrant() {
        lastUpdated = new Date();
        created = new Date();
        uuid = new AgaveUUID(UUIDType.PERMISSION).toString();
    }

    /**
     * @return the username
     */
    public synchronized String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public synchronized void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the group
     */
    public synchronized String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public synchronized void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the role
     */
    public synchronized String getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public synchronized void setRole(String role) {
        this.role = role;
    }

    /**
     * @return the readable
     */
    public synchronized boolean isReadable() {
        return read;
    }

    /**
     * @param readable the readable to set
     */
    public synchronized void setReadable(boolean readable) {
        this.read = readable;
    }

    /**
     * @return the writeable
     */
    public synchronized boolean isWriteable() {
        return write;
    }

    /**
     * @param writeable the writeable to set
     */
    public synchronized void setWriteable(boolean writeable) {
        this.write = writeable;
    }

    /**
     * @return the deletable
     */
    public synchronized boolean isDeletable() {
        return delete;
    }

    /**
     * @param deletable the deletable to set
     */
    public synchronized void setDeletable(boolean deletable) {
        this.delete = deletable;
    }

    /**
     * @return the uuid
     */
    public synchronized String getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public synchronized void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the lastUpdated
     */
    public synchronized Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public synchronized void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return the created
     */
    public synchronized Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public synchronized void setCreated(Date created) {
        this.created = created;
    }
    
}