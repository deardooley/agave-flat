package org.iplantc.service.jobs.model;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.iplantc.service.common.util.AgaveStringUtils;
import org.iplantc.service.jobs.model.enumerations.JobInterruptType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** This class is the in-memory model object for a record from job_interrupts table.
 * 
 * The choice to not define this class as a Hibernate entity is intentional.
 * See the class comment in JobQueue for a discussion.  
 *  
 * @author rcardone
 */
public final class JobInterrupt 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobInterrupt.class);
    
    // Expiration period.  This duration is the interrupt's time-to-live.
    public static final int JOB_INTERRUPT_TTL_SECONDS = 3600; // 1 hour
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Database fields.
    private long                id;             // Unique database sequence number
    private String              jobUuid;        // Unique job id not based on database id
    private String              tenantId;       // Tenant associated with job
    private int                 epoch;          // The job's current epoch
    private JobInterruptType    interruptType;  // Interrupt type
    private Date                created;        // Time interrupt was defined
    private Date                expiresAt;      // Time interrupt will expire

    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Internal use only constructor used to populate a job interrupt 
     * retrieved from the database.
     */
    public JobInterrupt(){}
    
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Used by client code to create a new job interrupt object for insertion
     * into the database.
     * 
     * @param jobUuid the job uuid to be interrupted
     * @param tenantId the tenant id of the job
     * @param interruptType the type of interrupt
     */
    public JobInterrupt(String jobUuid, String tenantId, JobInterruptType interruptType,
                        int epoch)
    {
        this.jobUuid = jobUuid;
        this.tenantId = tenantId;
        this.interruptType = interruptType;
        this.epoch = epoch;
    }
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getJobUuid() {
        return jobUuid;
    }
    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    public int getEpoch()
    {
        return epoch;
    }
    public void setEpoch(int epoch)
    {
        this.epoch = epoch;
    }
    public JobInterruptType getInterruptType()
    {
        return interruptType;
    }
    public void setInterruptType(JobInterruptType interruptType)
    {
        this.interruptType = interruptType;
    }
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    public Date getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
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
            .put("jobUuid", jobUuid)
            .put("tenantId", tenantId)
            .put("epoch", epoch)
            .put("interruptType", interruptType.name())
            .put("created", new DateTime(created).toString())
            .put("expiresAt", new DateTime(expiresAt).toString());
        
        return json.toString();
    }
    
    /* ----------------------------------------------------------- */
    /* toString:                                                   */
    /* ----------------------------------------------------------- */
    @Override
    public String toString(){return AgaveStringUtils.toString(this);}
}
