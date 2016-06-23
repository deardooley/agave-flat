package org.iplantc.service.systems.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class to hold the load information for a specific {@link BatchQueue}
 * @author dooley
 *
 */
public class BatchQueueLoad {
    
    @JsonIgnore
    private String name;
    private long active = 0;
    private long backlogged = 0;
    private long pending = 0;
    private long paused = 0;
    private long processingInputs = 0;
    private long stagingInputs = 0;
    private long staging = 0;
    private long submitting = 0;
    private long queued = 0;
    private long running = 0;
    private long cleaningUp = 0;
    private long archiving = 0;
    
    @JsonIgnore
    private Date created = new Date();
    
    public BatchQueueLoad() {}
    
    public BatchQueueLoad(String name) {
        this.name = name;
    }

    public BatchQueueLoad(String queueName, long pending, long staging, long queued,
            long submitting, long running, long cleaningUp, long archiving) {
        super();
        this.name = queueName;
        this.pending = pending;
        this.staging = staging;
        this.queued = queued;
        this.submitting = submitting;
        this.running = running;
        this.cleaningUp = cleaningUp;
        this.archiving = archiving;
    }
    
    /**
     * @return the queueName
     */
    public synchronized String getName() {
        return name;
    }

    /**
     * @param queueName the queueName to set
     */
    public synchronized void setName(String queueName) {
        this.name = queueName;
    }

    /**
     * @return the pending
     */
    public synchronized long getPending() {
        return pending;
    }

    /**
     * @param pending the pending to set
     */
    public synchronized void setPending(long pending) {
        this.pending = pending;
    }

    /**
     * @return the staging
     */
    public synchronized long getStaging() {
        return staging;
    }

    /**
     * @param staging the staging to set
     */
    public synchronized void setStaging(long staging) {
        this.staging = staging;
    }

    /**
     * @return the queued
     */
    public synchronized long getQueued() {
        return queued;
    }

    /**
     * @param queued the queued to set
     */
    public synchronized void setQueued(long queued) {
        this.queued = queued;
    }

    /**
     * @return the submitting
     */
    public synchronized long getSubmitting() {
        return submitting;
    }

    /**
     * @param submitting the submitting to set
     */
    public synchronized void setSubmitting(long submitting) {
        this.submitting = submitting;
    }

    /**
     * @return the running
     */
    public synchronized long getRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public synchronized void setRunning(long running) {
        this.running = running;
    }

    /**
     * @return the cleaningUp
     */
    public synchronized long getCleaningUp() {
        return cleaningUp;
    }

    /**
     * @param cleaningUp the cleaningUp to set
     */
    public synchronized void setCleaningUp(long cleaningUp) {
        this.cleaningUp = cleaningUp;
    }

    /**
     * @return the archiving
     */
    public synchronized long getArchiving() {
        return archiving;
    }

    /**
     * @param archiving the archiving to set
     */
    public synchronized void setArchiving(long archiving) {
        this.archiving = archiving;
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
     * @return the active
     */
    public synchronized long getActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public synchronized void setActive(long active) {
        this.active = active;
    }

    /**
     * @return the backlogged
     */
    public synchronized long getBacklogged() {
        return backlogged;
    }

    /**
     * @param backlogged the backlogged to set
     */
    public synchronized void setBacklogged(long backlogged) {
        this.backlogged = backlogged;
    }

    /**
     * @return the paused
     */
    public synchronized long getPaused() {
        return paused;
    }

    /**
     * @param paused the paused to set
     */
    public synchronized void setPaused(long paused) {
        this.paused = paused;
    }

    /**
     * @return the processingInputs
     */
    public synchronized long getProcessingInputs() {
        return processingInputs;
    }

    /**
     * @param processingInputs the processingInputs to set
     */
    public synchronized void setProcessingInputs(long processingInputs) {
        this.processingInputs = processingInputs;
    }

    /**
     * @return the stagingInputs
     */
    public synchronized long getStagingInputs() {
        return stagingInputs;
    }

    /**
     * @param stagingInputs the stagingInputs to set
     */
    public synchronized void setStagingInputs(long stagingInputs) {
        this.stagingInputs = stagingInputs;
    }
}
