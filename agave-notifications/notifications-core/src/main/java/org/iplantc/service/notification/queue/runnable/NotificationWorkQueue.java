/**
 * 
 */
package org.iplantc.service.notification.queue.runnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Entrypoint for starting standalone work queue to process 
 * notification messages. Each instance of this class is configured 
 * with the properties of a Quartz properties file passed into 
 * the {@link #getInstance(String)} method. In this way, the 
 * work queue will effectively represent a singleton of the underlying 
 * Quartz scheduler. 
 * 
 * @author dooley
 *
 */
public class NotificationWorkQueue {
	
	private Scheduler scheduler;
	private Properties properties = null;
	
	private static NotificationWorkQueue workQueue;
	
	private static final Logger log = Logger
			.getLogger(NotificationWorkQueue.class);
	/**
	 * 
	 */
	private NotificationWorkQueue() {
		
	}
	
	/**
	 * Returns a new instance of a work queue configured with the 
	 * quartz properties file passed in.
	 * 
	 * @param quartzPropertiesFile
	 * @return
	 * @throws NotificationException
	 */
	public static NotificationWorkQueue getInstance(String quartzPropertiesFile) 
	throws NotificationException 
	{
		if (workQueue == null) {
			workQueue = new NotificationWorkQueue();
			workQueue.initJNDI();
		}
		
		workQueue.initScheduler(quartzPropertiesFile);
		
		return workQueue;
	}
	
	/**
	 * Starts the work queue and, if necessary, the Quartz scheduler. Note that this will 
	 * only start the scheduler named in the properties file used to initialized this 
	 * work queue. Other schedulers will remain untouched.
	 *  
	 * @throws NotificationException
	 */
	public void start() throws NotificationException {
		
		try {
			if (getScheduler() != null) { 
				if (!getScheduler().isStarted() || !getScheduler().isInStandbyMode()) {
					getScheduler().start();
				} else {
					log.debug("Skipping startup as scheduler is already running.");
				}
			}
			else {
				throw new NotificationException("Failed to start existing work queue. "
						+ "No scheduler has been initialized");
			}
		}
		catch (SchedulerException e) {
			throw new NotificationException("Failed to start existing work queue.", e);
		}
		
	}
	
	/**
	 * Stops the work queue and, if necessary, the Quartz scheduler. Note that this will 
	 * only stop the scheduler named in the properties file used to initialized this 
	 * work queue. Other schedulers will remain untouched.
	 * @throws NotificationException
	 */
	public void stop() throws NotificationException {
		
		try {
			if (getScheduler() != null) {
				getScheduler().shutdown(false);
			}
			else {
				throw new NotificationException("Failed to start existing work queue. "
						+ "No scheduler has been initialized");
			}
		}
		catch (SchedulerException e) {
			throw new NotificationException("Failed to stop existing work queue.", e);
		}
		finally {
			JndiSetup.close();
		}
		
	}
	
	/**
	 * Starts up a JNDI connection for database interaction.
	 */
	private void initJNDI() {
		JndiSetup.init();
	}
	
	/**
	 * Create and initialize a Quartz scheduler to handle the work processing
	 * using the properties defined in the named {@code propertiesFile}.
	 * 
	 * @param propertiesFile
	 * @throws NotificationException
	 */
	private void initScheduler(String propertiesFile) 
	throws NotificationException 
	{
		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = null;
	    try {
	    	Properties props = loadPropertiesFromDisk(propertiesFile);
	    	props.setProperty("org.quartz.threadPool.threadCount", String.valueOf(Settings.MAX_NOTIFICATION_TASKS + 1));
	    	
	    	setProperties(props);
	    	
	        schedulerFactory.initialize(props);
	        
	        scheduler = schedulerFactory.getScheduler(props.getProperty("org.quartz.scheduler.instanceName"));
		    
		    setScheduler(scheduler);
	    }
	    catch (IOException e) {
	    	throw new NotificationException("Failed to load properties from " + propertiesFile, e);
	    }
	    catch (SchedulerException e) {
	        throw new NotificationException("Failed to initialize NewNotificationWorkQueue scheduler", e);
	    }
	}
	
	/**
	 * Read propprties from disk.
	 * @param propertiesFile
	 * @return
	 * @throws IOException
	 */
	private Properties loadPropertiesFromDisk(String propertiesFile) 
	throws IOException 
	{
		InputStream in = null;
	    Properties properties = null;
	    try {
	    	
	    	if (new File(propertiesFile).exists()) {
	    		in = new FileInputStream(propertiesFile);
	    	} 
	    	else {
	    		in = getClass().getClassLoader().getResourceAsStream(propertiesFile);
	    	}
	        
	    	properties = new Properties();
	        
	        properties.load(in);
	        
	        return properties;
	    } 
	    finally {
	        try { in.close(); } catch (Exception e) {}
	    }
	}
	
	public static void main(String[] args) {
		NotificationWorkQueue queue = null;
		try {
			queue = NotificationWorkQueue.getInstance("quartz-newNotificationWorkQueue.properties");
			queue.start();
		}
		catch (Throwable e) {
			log.error("Unexpected termination shutting down the work queue", e);
		}
		finally {
			try {
				queue.stop();
			} catch (NotificationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return the scheduler
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
