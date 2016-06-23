package org.iplantc.service.monitor.managers;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.iplantc.service.monitor.dao.MonitorCheckDao;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.events.MonitorEventProcessor;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.model.enumeration.MonitorCheckType;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.monitor.model.enumeration.MonitorStatusType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.joda.time.DateTime;

/**
 * Helper class to queue up all the registered monitors for a given resource event.
 * 
 * @author dooley
 *
 */
public class MonitorManager 
{
	private static final Logger log = Logger.getLogger(MonitorManager.class);
	
	private MonitorEventProcessor eventProcessor;
	
	public MonitorManager() {
		this.eventProcessor = new MonitorEventProcessor();
	}
	
	/**
	 * Sends off all the monitors for a particular event and UUID for processing.
	 * 
	 * @param associatedUuid
	 * @param monitorEvent
	 * @param affectedUser
	 * @return
	 */
	public MonitorCheck check(Monitor monitor, String createdBy)
	{
		MonitorDao dao = new MonitorDao();
		if (!monitor.getSystem().isAvailable())
		{
			try {
				monitor.setActive(false);
				monitor.setLastUpdated(new Date());
				dao.persist(monitor);
				log.debug("Disabled monitor " + monitor.getUuid() + " on system " + 
						monitor.getSystem().getSystemId() + " due to system being inactive.");
			} catch (MonitorException e) {
				log.error("Failed to disable monitor " + monitor.getUuid() + " after system was made unavialable", e);
			} finally {
			    eventProcessor.processContentEvent(monitor, MonitorEventType.DISABLED, monitor.getOwner());
			}    
			return null;
		}
		else
		{
			MonitorCheck check = doStorageCheck(monitor, createdBy); 
			
			if (monitor.getSystem() instanceof ExecutionSystem) {
				MonitorCheck loginCheck = doLoginCheck(monitor, createdBy);
				if (check.getResult() != MonitorStatusType.FAILED) {
					check = loginCheck;
				}
			}
			if (check.getResult().equals(MonitorStatusType.PASSED)) {
				monitor.setLastSuccess(new Date(check.getCreated().getTime()));
				try { 
					dao.persist(monitor); 
				} catch (Exception e) {
					log.error("Check for monitor " + monitor.getUuid() + 
							" succeeded, but failed to updated the monitor's last success date.");
				}
			}
			
			return check;
		}
	}
	
	/**
	 * Performs a check on connectivity to a storage system.
	 * 
	 * @param monitor the monitor to check
	 * @param createdBy user who kicked off the check
	 * @return
	 */
	@SuppressWarnings("unused")
    private MonitorCheck doStorageCheck(Monitor monitor, String createdBy)
	{
		MonitorCheckDao checkDao = new MonitorCheckDao();
		MonitorCheck currentCheck = new MonitorCheck(monitor, MonitorStatusType.UNKNOWN, null, MonitorCheckType.STORAGE);
		
		RemoteDataClient monitoredSystemRemoteDataClient = null;
		MonitorCheck lastCheck = null;
		RemoteSystem system = null;
		try 
		{	
			lastCheck = checkDao.getLastMonitorCheck(monitor.getId());
			system = monitor.getSystem();
			
			monitoredSystemRemoteDataClient = system.getRemoteDataClient(monitor.getInternalUsername());
			log.debug("[" + Thread.currentThread().getName() + "] Checking data connectivity to " + system.getSystemId() + 
					"(" +  system.getStorageConfig().getProtocol() + ":" + monitoredSystemRemoteDataClient.getHost() + ")");
			
			monitoredSystemRemoteDataClient.authenticate();
			monitoredSystemRemoteDataClient.doesExist("");
			
			currentCheck.setResult(MonitorStatusType.PASSED);
			currentCheck.setCreated(new Date());
//			monitor.setLastSuccess(currentCheck.getCreated());
			log.debug("Data connectivity succeeded to " + system.getSystemId() + 
					"(" +  system.getStorageConfig().getProtocol() + ":" + monitoredSystemRemoteDataClient.getHost() + ")");
		}
		catch (IOException e) 
		{
			if (system != null && system.getStorageConfig() != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
					"(" +  system.getStorageConfig().getProtocol() + ":" + monitoredSystemRemoteDataClient.getHost() + ")" + 
					". " + e.getMessage());
			} else if (system != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
						". " + e.getMessage());
			} 
			else {
				log.debug("Data connectivity failed for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage(e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (RemoteDataException e) 
		{
			if (system != null && system.getStorageConfig() != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
					"(" +  system.getStorageConfig().getProtocol() + ":" + monitoredSystemRemoteDataClient.getHost() + ")" + 
					". " + e.getMessage());
			} else if (system != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
						". " + e.getMessage());
			} else {
				log.debug("Data connectivity failed for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage(e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (RemoteCredentialException e) 
		{
			if (system != null) {
				log.debug("Failed to retrieve an authentication credential for " + system.getSystemId() + 
					" when running storage check. " +  e.getMessage());
			} else {
				log.debug("Failed to retrieve an authentication credential for "
						+ " when performing storage check for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage("Authentication failed for " + system.getSystemId() + 
					". " + e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (Exception e)
		{
			if (system != null && system.getStorageConfig() != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
					"(" +  system.getStorageConfig().getProtocol() + ":" + monitoredSystemRemoteDataClient.getHost() + ")" + 
					". " + e.getMessage());
			} else if (system != null) {
				log.debug("Data connectivity failed to " + system.getSystemId() + 
						". " + e.getMessage());
			} else {
				log.debug("Data connectivity failed for monitor " + monitor.getId());
			}
			
			currentCheck.setMessage("Failed to perform storage monitoring check on " + 
					monitor.getSystem().getSystemId() + "\n" + e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		finally {
			try { monitoredSystemRemoteDataClient.disconnect(); } catch (Exception e) {}
		}
		
		try 
		{ 
			currentCheck.setMonitor(monitor);
			 
			checkDao.persist(currentCheck); 
			
			eventProcessor.processCheckEvent(monitor, 
					lastCheck, 
					currentCheck, 
					createdBy);
		} 
		catch (Exception e) 
		{
			log.error("Failed to persist storage monitor check " + currentCheck.getUuid() + " for monitor " + monitor.getUuid(), e);
		}
		finally 
		{	
			try {
				monitor.setLastUpdated(new Date());
				new MonitorDao().persist(monitor);
			} catch (Exception e){}
			
		}
		
		return currentCheck;
	}
	
	/**
	 * Performs a check on connectivity to a login system.
	 * 
	 * @param monitor the monitor to check
	 * @param createdBy user who kicked off the check
	 * @return
	 */
	@SuppressWarnings("unused")
	private MonitorCheck doLoginCheck(Monitor monitor, String createdBy)
	{
		MonitorCheckDao checkDao = new MonitorCheckDao();
		
		MonitorCheck currentCheck = new MonitorCheck(monitor, MonitorStatusType.UNKNOWN, null, MonitorCheckType.LOGIN);
		
		RemoteSubmissionClient submissionClient = null;
		MonitorCheck lastCheck = null;
		ExecutionSystem system = null;
		try 
		{	
			lastCheck = checkDao.getLastMonitorCheck(monitor.getId());
			
			system = (ExecutionSystem)monitor.getSystem();
			
			log.debug("Checking login connectivity to " + system.getSystemId() + 
					"(" +  system.getLoginConfig().getProtocol() + ":" + 
					system.getLoginConfig().getHost() + ")");
			
			submissionClient = ((ExecutionSystem)monitor.getSystem()).getRemoteSubmissionClient(monitor.getInternalUsername());
			
			if (submissionClient.canAuthentication()) {
    			currentCheck.setResult(MonitorStatusType.PASSED);
    			currentCheck.setCreated(new Date());
    //			monitor.setLastSuccess(currentCheck.getCreated());
    			
    			log.debug("Login authentication succeeded to " + system.getSystemId() + 
    					"(" +  system.getLoginConfig().getProtocol() + ":" + 
    					system.getLoginConfig().getHost() + ")");
			} 
			else 
			{
			    currentCheck.setResult(MonitorStatusType.FAILED);
                currentCheck.setCreated(new Date());
                currentCheck.setMessage("Failed to authenticate to system " + system.getSystemId() + " at " + 
                        system.getLoginConfig().getProtocol() + ":" + system.getLoginConfig().getHost());
    //          monitor.setLastSuccess(currentCheck.getCreated());
                
                log.debug("Login authentication failed to " + system.getSystemId() + 
                        "(" +  system.getLoginConfig().getProtocol() + ":" + 
                        system.getLoginConfig().getHost() + ")");
			}
		}
		catch (IOException e) 
		{
			if (system != null && system.getStorageConfig() != null) {
				log.debug("Login connectivity failed to " + system.getSystemId() + 
					"(" +  system.getLoginConfig().getProtocol() + ":" + system.getLoginConfig().getHost() + ")" + 
					". " + e.getMessage());
			} else if (system != null) {
				log.debug("Login connectivity failed to " + system.getSystemId() + 
						". " + e.getMessage());
			} else {
				log.debug("Login connectivity failed for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage(e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (RemoteDataException e) 
		{
			if (system != null && system.getStorageConfig() != null) {
				log.debug("Login connectivity failed to " + system.getSystemId() + 
					"(" +  system.getLoginConfig().getProtocol() + ":" + system.getLoginConfig().getHost() + ")" + 
					". " + e.getMessage());
			} else if (system != null) {
				log.debug("Login connectivity failed to " + system.getSystemId() + 
						". " + e.getMessage());
			} else {
				log.debug("Login connectivity failed for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage(e.getMessage());
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (RemoteCredentialException e) 
		{
			if (system != null) {
				log.debug("Failed to retrieve an authentication credential for " + system.getSystemId() + 
					" when running login check. " +  e.getMessage());
			} else {
				log.debug("Failed to retrieve an authentication credential for "
						+ " when performing login check for monitor " + monitor.getId());
			}
			
			// something went wrong. The system storage is not accessible.
			currentCheck.setMessage("Authentication failed for " + system.getSystemId() + 
					". " + e.getMessage());
			
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		catch (Exception e)
		{
			log.debug("Login connectivity failed to " + system.getSystemId() + 
					"(" +  system.getLoginConfig().getProtocol() + ":" + 
					system.getLoginConfig().getHost() + ")");
			
			currentCheck.setMessage("Failed to perform login monitoring check on " + 
					system.getSystemId() + "\n" + e.getMessage());
			
			currentCheck.setResult(MonitorStatusType.FAILED);
		}
		finally {
			try { submissionClient.close(); } catch (Exception e) {}
		}
		
		try 
		{ 
			currentCheck.setMonitor(monitor);
			 
			checkDao.persist(currentCheck); 
			
			eventProcessor.processCheckEvent(monitor, 
					lastCheck,
					currentCheck, 
					createdBy);
			
//			NotificationManager.process(monitor.getUuid(), currentCheck.getResult().name(), monitor.getOwner(), monitor.toJSON());
			
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectNode jsonMonitor = (ObjectNode)mapper.readTree(monitor.toJSON());
//			jsonMonitor.put("lastCheck", mapper.createObjectNode()
//			        .put("id", currentCheck.getUuid())
//			        .put("type", currentCheck.getCheckType().name())
//			        .put("message", currentCheck.getMessage())
//			        .put("created", new DateTime(currentCheck.getCreated()).toString()));
//			
//			if (lastCheck != null && currentCheck.getResult() != lastCheck.getResult()) {
//				eventProcessor.processCheckEvent(monitor, 
//						currentCheck, 
//						MonitorEventType.RESULT_CHANGE, 
//						monitor.getOwner());
//				NotificationManager.process(monitor.getUuid(), MonitorEventType.RESULT_CHANGE, monitor.getOwner(), jsonMonitor.toString());
//			}
//			
//			if (currentCheck.getResult().getSystemStatus() != system.getStatus())
//			{
//				eventProcessor.processCheckEvent(monitor, 
//						currentCheck, 
//						MonitorEventType.STATUS_CHANGE, 
//						monitor.getOwner());
//				
//				//NotificationManager.process(monitor.getUuid(), MonitorEventType.STATUS_CHANGE, monitor.getOwner(), jsonMonitor.toString());
//				
//				if (monitor.isUpdateSystemStatus()) {
//					system = (ExecutionSystem)monitor.getSystem();
////					system.setStatus(currentCheck.getResult().getSystemStatus());
////					system.setLastUpdated(new Date());
//					
//					SystemManager systemManager = new SystemManager();
//					try {
//						systemManager.updateSystemStatus(monitor.getSystem(), currentCheck.getResult().getSystemStatus(), monitor.getOwner());
//					}
//					catch (Throwable e) {
//						log.error("Failed to update system status change event after monitor check " + 
//								currentCheck.getUuid() + " on monitor " + monitor.getUuid(), e);
//					}
//					
//					eventProcessor.processCheckEvent(monitor, 
//							currentCheck, 
//							MonitorEventType.RESULT_CHANGE, 
//							monitor.getOwner());
//					
////					NotificationManager.process(system.getUuid(), MonitorEventType.STATUS_CHANGE, monitor.getOwner(), system.toJSON());
//				}
//			}
		} 
		catch (Exception e) {
			log.error("Failed to persist login monitor check " + currentCheck.getUuid() + " for monitor " + monitor.getUuid(), e);
		}
		finally 
		{	
			try {
				monitor.setLastUpdated(new Date());
				new MonitorDao().persist(monitor);
			} catch (Exception e){}
			
		}
		
		return currentCheck;
	}
	
	/**
	 * Reschedules a {@link MonitorCheck} for the next avaialble time based on the
	 * frequency set at registration.
	 * 
	 * @param monitor
	 * @throws MonitorException
	 */
	public void resetNextUpdateTime(Monitor monitor) throws MonitorException
	{
		MonitorDao dao = new MonitorDao();
		dao.refresh(monitor);
		monitor.setLastUpdated(new Date());
		monitor.setNextUpdateTime(new DateTime().plusMinutes(monitor.getFrequency()).toDate());
		dao.persist(monitor);
	}
}
