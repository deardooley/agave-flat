/**
 * 
 */
package org.iplantc.service.systems.search;

import java.util.Date;

import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

/**
 * Simple bean to handle polymorphic search results since Hibernate
 * cannot return instances of a RemoteSystem.
 * 
 * @author dooley
 *
 */
public class SystemSearchResult {
    
    protected Long                  id;
    protected String                name;
    protected String                systemId;
    protected String                description;
    protected String                owner;
    protected Date                  lastUpdated = new Date();
    protected Date                  created = new Date();
    protected SystemStatusType      status = SystemStatusType.UP;
    protected RemoteSystemType      type;
    protected boolean               globalDefault = false;
    protected int                   revision = 1;
    protected boolean               publiclyAvailable = false;
    protected boolean               available = true;   
    protected String                uuid;
    protected String                tenantId;
    
    public SystemSearchResult() {}

    /**
     * @return the id
     */
    public synchronized Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public synchronized void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public synchronized String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public synchronized void setName(String name) {
        this.name = name;
    }

    /**
     * @return the systemId
     */
    public synchronized String getSystemId() {
        return systemId;
    }

    /**
     * @param systemId the systemId to set
     */
    public synchronized void setSystemId(String systemId) {
        this.systemId = systemId;
    }

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

    /**
     * @return the owner
     */
    public synchronized String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public synchronized void setOwner(String owner) {
        this.owner = owner;
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

    /**
     * @return the status
     */
    public synchronized SystemStatusType getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public synchronized void setStatus(SystemStatusType status) {
        this.status = status;
    }

    /**
     * @return the type
     */
    public synchronized RemoteSystemType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public synchronized void setType(RemoteSystemType type) {
        this.type = type;
    }

    /**
     * @return the globalDefault
     */
    public synchronized boolean isGlobalDefault() {
        return globalDefault;
    }

    /**
     * @param globalDefault the globalDefault to set
     */
    public synchronized void setGlobalDefault(boolean globalDefault) {
        this.globalDefault = globalDefault;
    }

    /**
     * @return the revision
     */
    public synchronized int getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to set
     */
    public synchronized void setRevision(int revision) {
        this.revision = revision;
    }

    /**
     * @return the publiclyAvailable
     */
    public synchronized boolean isPubliclyAvailable() {
        return publiclyAvailable;
    }

    /**
     * @param publiclyAvailable the publiclyAvailable to set
     */
    public synchronized void setPubliclyAvailable(boolean publiclyAvailable) {
        this.publiclyAvailable = publiclyAvailable;
    }

    /**
     * @return the available
     */
    public synchronized boolean isAvailable() {
        return available;
    }

    /**
     * @param available the available to set
     */
    public synchronized void setAvailable(boolean available) {
        this.available = available;
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
     * @return the tenantId
     */
    public synchronized String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId the tenantId to set
     */
    public synchronized void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

   
}
