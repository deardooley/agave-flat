package org.iplantc.service.jobs.dao.utils;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

/** Result class for JobDao.lockJob() methods that packages multiple 
 * fields from jobs table.  The constructor expect a 2 element array,
 * the first element a status string and the second the epoch number.
 * 
 * Instances of this class are constructed only if its fields can be
 * successfully assigned.  Any failure causes the constructor to
 * throw an exception.
 * 
 * @author rcardone
 */
public final class JobLockResult
{
    // Logging.
    private static final Logger _log = Logger.getLogger(JobLockResult.class);
    
    // Fields.
    public final JobStatusType status;
    public final int           epoch;
    
    /** Verify and assign this object's fields given raw database result
     * array of [status, epoch].
     * 
     * @param row a two element row returned from the 
     *        database consisting of fields (status, epoch)
     * @throws Exception on unexpected input or failed assignment       
     */
    public JobLockResult(Object[] row)
     throws Exception
    {
        // Do we have any results?
        if (row == null) {
            String msg = "Unable to find job to lock.";
            _log.error(msg);
            throw new JobException(msg);
        }
        // Are the results valid?
        if (row.length < 2) {
            String msg = "Invalid job lock results received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Assign the status from a string.
        try {status = JobStatusType.valueOf((String)row[0]);}
        catch (Exception e) {
            String msg = "Unable to convert " + row[0] + " into a JobStatusType";
            _log.error(msg, e);
            throw e;
        }
        
        // Assign the epoch.
        epoch = (Integer) row[1];
    }
}
