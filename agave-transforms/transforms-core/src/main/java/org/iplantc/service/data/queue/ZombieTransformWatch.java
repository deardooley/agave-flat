package org.iplantc.service.data.queue;

import static org.iplantc.service.io.model.enumerations.TransformTaskStatus.TRANSFORMING_FAILED;
import static org.iplantc.service.io.model.enumerations.TransformTaskStatus.TRANSFORMING_QUEUED;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.data.dao.DecodingTaskDao;
import org.iplantc.service.data.exceptions.TransformPersistenceException;
import org.iplantc.service.data.model.DecodingTask;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.QueueTask;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This is a reaper task designed to clean up any {@link QueueTask} 
 * who has an unresponsive transfer for more than 15 minutes 
 * or an intermediate status for more than an hour.  
 * 
 * @author dooley
 */
@DisallowConcurrentExecution
public class ZombieTransformWatch implements org.quartz.Job 
{
	private static final Logger	log	= Logger.getLogger(ZombieTransformWatch.class);
	
	public ZombieTransformWatch() {}

	public void execute(JobExecutionContext context)
			throws JobExecutionException
	{
		try 
		{
			doExecute();
		}
		catch(JobExecutionException e) {
			throw e;
		}
		catch (Throwable e) 
		{
			log.error("Unexpected error during reaping of zombie jobs", e);
		}
	}
	
	public void doExecute() throws JobExecutionException
	{
		try
		{
			if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
				log.debug("Queue draining has been enabled. Skipping zombie reaping task." );
				return;
			}
			
			List<Long> zombieIds = DecodingTaskDao.findZombieTransforms(TenancyHelper.getDedicatedTenantIdForThisService());
			
			for (Long id: zombieIds) 
            {
				DecodingTask decodingTask = DecodingTaskDao.getById(id);
				
				if (decodingTask == null) continue;
				
				LogicalFile logicalFile = decodingTask.getLogicalFile();
				
				// this is a new thread and thus has no tenant info loaded. we set it up
				// here so things like app and system lookups will stay local to the 
				// tenant
				TenancyHelper.setCurrentTenantId(decodingTask.getLogicalFile().getTenantId());
				TenancyHelper.setCurrentEndUser(decodingTask.getLogicalFile().getOwner());
				
				try 
				{	
					if (decodingTask.getStorageSystem() == null) 
                	{
        				decodingTask.setStatus(TRANSFORMING_FAILED);
        				decodingTask.setLastUpdated(new Date());
        				DecodingTaskDao.persist(decodingTask);
        				
        				LogicalFileDao.updateTransferStatus(logicalFile, 
        						FileEventType.TRANSFORMING_FAILED.name(), 
        						"Transform task for this file item was "
								+ "found in a zombie state. Task will be rolled back "
								+ "to the previous state and transformation will resume.",
								logicalFile.getOwner());
                	}
					else
					{
						LogicalFileDao.updateTransferStatus(logicalFile, 
								FileEventType.TRANSFORMING_FAILED.name(), 
        						"Transform task for this file item was "
								+ "found in a zombie state. Task will be rolled back "
								+ "to the previous state and transformation will resume.",
								logicalFile.getOwner());
						
						decodingTask.setStatus(TRANSFORMING_FAILED);
						decodingTask.setLastUpdated(new Date());
						DecodingTaskDao.persist(decodingTask);
						
						decodingTask.setStatus(TRANSFORMING_QUEUED);
        				decodingTask.setLastUpdated(new Date());
        				DecodingTaskDao.persist(decodingTask);
					}
                	
            		
		        }
				catch (UnresolvableObjectException e) {
					log.debug("Just avoided a job archive race condition from decoding task reaper");
				}
				catch (StaleObjectStateException e) {
					log.debug("Just avoided a job archive race condition from decoding task reaper");
				}
				catch (Throwable e)
				{
					log.error("Failed to roll back transform of decoding task " + 
							decodingTask.getId(), e);
				} 
            }
		}
		catch (TransformPersistenceException e) {
			log.error("Failed to retrieve decoding task information from db", e);
		}
		catch (Throwable e)
		{
			log.error("Failed to resolve one or more zombie decoding tasks. "
					+ "Reaping will resume shortly.", e);
		} 
	}		
}
