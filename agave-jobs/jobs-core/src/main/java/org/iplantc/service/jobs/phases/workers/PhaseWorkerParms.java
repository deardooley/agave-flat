package org.iplantc.service.jobs.phases.workers;

import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler;

import com.rabbitmq.client.Connection;

/** Simple data structure used to pass parameters  
 * to a new PhaseWorker object for construction.
 * 
 * @author rcardone
 */
public class PhaseWorkerParms 
{
    public ThreadGroup threadGroup;
    public String threadName;
    public Connection connection;
    public AbstractPhaseScheduler scheduler; 
    public String tenantId;
    public String queueName; 
    public int threadNum; 
}
