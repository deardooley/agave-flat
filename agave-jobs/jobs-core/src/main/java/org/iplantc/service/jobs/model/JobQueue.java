package org.iplantc.service.jobs.model;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.AgaveStringUtils;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** This class is the in-memory model object for a record from job_queues table.
 * 
 * The choice to not define this class as a Hibernate entity is intentional.  
 * There are arguments for and against using Hibernate.  The main reasons
 * this class does not use Hibernate are to keep complete control over 
 * (1) the scope and timing of transactions, (2) locking behavior, and (3) the
 * precision by which field updates can be performed.  In addition, the 
 * programming model is simplified by removing caching, lazy evaluation, and 
 * other complex facilities that Hibernate offers.  On the downside, we have to 
 * write and maintain some boilerplate code that Hibernate would otherwise handle
 * and we still have to coexist with Hibernate.
 *  
 * @author rcardone
 */
public class JobQueue {

    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobQueue.class);
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Database fields.
    private long            id;             // Unique database sequence number
    private String          uuid;           // Unique id not based on database id
    private String          name;           // Queue name
    private String          tenantId;       // Tenant associated with queue
    private JobPhaseType    phase;          // Job processing phase of queue
    private int             priority;       // Priority used for queue selection
    private int             numWorkers;     // Default # of threads dedicated to queue
    private int             maxMessages;    // Maximum # of messages allowed in queue
    private String          filter;         // SQL-like filter for matching jobs to queues
    private Date            created;        // Time queue was defined
    private Date            lastUpdated;    // Time queue definition was last updated

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    public JobPhaseType getPhase() {
        return phase;
    }
    public void setPhase(JobPhaseType phase) {
        this.phase = phase;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public int getNumWorkers() {
        return numWorkers;
    }
    public void setNumWorkers(int numWorkers) {
        this.numWorkers = numWorkers;
    }
    public int getMaxMessages() {
        return maxMessages;
    }
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }
    public String getFilter() {
        return filter;
    }
    public void setFilter(String filter) {
        this.filter = filter;
    }
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    public Date getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* toJson:                                                                */
    /* ---------------------------------------------------------------------- */
    /** Create the json representation of this object.
     * 
     * @return a json string.
     * @throws JsonProcessingException
     * @throws IOException
     */
    @JsonValue
    public String toJSON() throws JsonProcessingException, IOException
    {
        // Dump all fields into a json object.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode()
            .put("id", id)
            .put("uuid", uuid)
            .put("name", name)
            .put("tenantId", tenantId)
            .put("phase", phase.name())
            .put("priority", priority)
            .put("numWorkers", numWorkers)
            .put("maxMessages", maxMessages)
            .put("filter", filter)
            .put("created", new DateTime(created).toString())
            .put("lastUpdated", new DateTime(lastUpdated).toString());
        
        // Add the hyperlinks.
        json.set("_links", getHypermedia());
        
        return json.toString();
    }
    
    /* ----------------------------------------------------------- */
    /* toString:                                                   */
    /* ----------------------------------------------------------- */
    @Override
    public String toString(){return AgaveStringUtils.toString(this);}
    
    /* ********************************************************************** */
    /*                             Private Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getHypermedia:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Create the links to include in json representation.
     * 
     * @return the object containing the links
     */
    private ObjectNode getHypermedia()
    {
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode linksObject = mapper.createObjectNode();
        linksObject.replace("self", (ObjectNode)mapper.createObjectNode()
            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, getTenantId()) + uuid));

        return linksObject;
    }
}
