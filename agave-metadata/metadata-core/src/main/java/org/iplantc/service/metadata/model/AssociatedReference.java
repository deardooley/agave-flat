package org.iplantc.service.metadata.model;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AssociatedReference {
    private AgaveUUID uuid;
    public String url;
    
    public AssociatedReference(AgaveUUID uuid, String url) {
        this.setUuid(uuid);
        this.url = url;
    }

    /**
     * @return the url
     */
    public synchronized String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public synchronized void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the uuid
     */
    public AgaveUUID getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(AgaveUUID uuid) {
        this.uuid = uuid;
    }
}
