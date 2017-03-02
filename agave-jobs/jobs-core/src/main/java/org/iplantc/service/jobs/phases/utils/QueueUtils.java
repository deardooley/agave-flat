package org.iplantc.service.jobs.phases.utils;

import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This class provides utilities that both queue readers and writers can share. 
 * 
 * @author rcardone
 */
public final class QueueUtils
{
    /* ---------------------------------------------------------------------- */
    /* getTopicQueueName:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Construct the name of the phase-specific topic queue.  
     * 
     * @param phase the target phase
     * @return the topic queue name used by the specified phase
     */
    public static String getTopicQueueName(JobPhaseType phase)
    {
        return QueueConstants.TOPIC_QUEUE_PREFIX + "." + phase.name();
    }
}
