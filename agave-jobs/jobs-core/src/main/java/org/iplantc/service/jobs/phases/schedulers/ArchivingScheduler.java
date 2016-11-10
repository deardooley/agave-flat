package org.iplantc.service.jobs.phases.schedulers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

/** Concrete job phase scheduler
 * 
 * @author rcardone
 */
public final class ArchivingScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(ArchivingScheduler.class);

    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public ArchivingScheduler() throws JobException
    {
        super(JobPhaseType.ARCHIVING);
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseTriggerStatus:                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    protected List<JobStatusType> getPhaseTriggerStatuses()
    {
        ArrayList<JobStatusType> list = new ArrayList<>();
        list.add(JobStatusType.ARCHIVING);
        list.add(JobStatusType.CLEANING_UP);
        return list;
    }
}
