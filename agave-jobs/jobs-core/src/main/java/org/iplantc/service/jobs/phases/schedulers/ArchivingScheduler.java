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
    
    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;

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
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseTriggerStatus:                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    protected List<JobStatusType> getPhaseTriggerStatuses()
    {
        if (_phaseTriggerStatuses == null)
        {
            _phaseTriggerStatuses = new ArrayList<>(2);
            _phaseTriggerStatuses.add(JobStatusType.ARCHIVING);
            _phaseTriggerStatuses.add(JobStatusType.CLEANING_UP);
        }
        return _phaseTriggerStatuses;
    }
    
    /* ---------------------------------------------------------------------- */
    /* allowsRepublishing:                                                    */
    /* ---------------------------------------------------------------------- */
    @Override
    public boolean allowsRepublishing(){return false;}
}
