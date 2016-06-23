package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;


/**
 * This abstract class handles . Concrete
 * classes (one per api resource) will implement the details of generating subject
 * lines for emails, email bodies, and processing macros.
 * 
 * @author dooley
 *
 */
public abstract class AbstractEventFilter implements EventFilter
{
	private static final Logger	log	= Logger.getLogger(AbstractEventFilter.class);

	protected Notification notification;
	protected NotificationDao dao = new NotificationDao();
	protected String event;
	protected String owner;
	protected AgaveUUID associatedUuid;
	protected int responseCode = -1;
	private String customNotificationMessageContextData = null;
	
//	private String attemptUuid;
    
	public AbstractEventFilter(AgaveUUID associatedUuid, Notification notification, String event, String owner) {
		this.associatedUuid = associatedUuid;
		this.notification = notification;
		if (StringUtils.isEmpty(event)) {
			this.event = notification.getEvent();
		} else {
			this.event = event;
		}
		this.owner = owner;
	}
	
	public AbstractEventFilter(AgaveUUID associatedUuid, Notification notification, String event, String owner, String customNotificationMessageContextData) {
        this.associatedUuid = associatedUuid;
        this.notification = notification;
        if (StringUtils.isEmpty(event)) {
            this.event = notification.getEvent();
        } else {
            this.event = event;
        }
        this.owner = owner;
        this.setCustomNotificationMessageContextData(customNotificationMessageContextData);
    }
	
//	@Override
//	public void fire() throws NotificationException
//	{
//		log.debug("Processing webhook for notification: " + notification.getCallbackUrl());
//		
//		NotificationAttempt attempt = NotificationAttemptFactory.getInstance(notification);
//		
//		NotificationException trappedException = null;
//		try 
//		{
//			
//			// forward message based on destination. Email is detected by URL regex
//			if (ServiceUtils.isValidEmailAddress(notification.getCallbackUrl())) {
//				ObjectMapper mapper = new ObjectMapper();
//				ObjectNode json = mapper.createObjectNode().put("subject", getEmailSubject()).put("message", getEmailBody());
//				
//				attempt = new NotificationAttempt(this.notification, 
//						this.event, json.toString(), new Timestamp(System.currentTimeMillis()), 1);
//				
//			// realtime push messages are detected based on a prefix match of <pre>{@link Tenant#baseUrl()}/realtime</pre>
//			} else if (ServiceUtils.isValidRealtimeChannel(notification.getCallbackUrl(), notification.getTenantId())) {
//			    attempt = new NotificationAttempt(this.notification, 
//						this.event, json.toString(), new Timestamp(System.currentTimeMillis()), 1);
//				
//				pushRealtimeMessage();
//			
// 			// SMS is detected by phone number regex match.
// 			// uncomment to enable sms notifications via twilio
// 			//} else if (ServiceUtils.isValidPhoneNumber(notification.getCallbackUrl())) {
// 			//	responseStatus = smsCallback();
//			
//			// otherwise we assume it's a standard http webhook and forward accordingly.
//			} else {
//				postCallback();
//			}
//		} catch (NotificationException e) {
//			log.error("[" + attemptUuid + "] " + e.getMessage());
//			trappedException = e;
//		}
//		
//		notification.setAttempts(notification.getAttempts() + 1);
//		notification.setResponseCode(this.responseCode);
//		notification.setLastSent(new Date());
//		notification.setSuccess(this.responseCode >= 200 && this.responseCode < 300);
//		if (notification.isSuccess())
//		{
//			// if the notification succeeded, but it will be reused, then 
//			// reset the attempts so the next has a fresh opportunity to 
//			// succeed
//			if (notification.isPersistent()) {
//				notification.setAttempts(0);
//			}
//			// otherwise, set the terminated marker to remove it.
//			else {
//				notification.setTerminated(true);
//			}
//		}
//		// notification failed
//		else 
//		{
//			// if it failed and we're over the retry limit, terminate the notification 
//			if (notification.getAttempts() > Settings.MAX_NOTIFICATION_RETRIES) {
//				notification.setTerminated(true);
//			} 
//			// otherwise, let it retry
//			else {
//				// dodged a bullet
//			}
//		}
//		
//		// save and return status
//		try { dao.persist(notification); } catch (Exception e) {}
//		
//		if (trappedException == null) {
//			return notification.isSuccess();
//		} else {
//			throw trappedException;
//		}
//	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getNotification()
	 */
	@Override
	public Notification getNotification() {
		return notification;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setNotification(org.iplantc.service.notification.model.Notification)
	 */
	@Override
	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getDao()
	 */
	@Override
	public NotificationDao getDao() {
		return dao;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setDao(org.iplantc.service.notification.dao.NotificationDao)
	 */
	@Override
	public void setDao(NotificationDao dao) {
		this.dao = dao;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getEvent()
	 */
	@Override
	public String getEvent() {
		return event;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setEvent(java.lang.String)
	 */
	@Override
	public void setEvent(String event) {
		this.event = event;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getOwner()
	 */
	@Override
	public String getOwner() {
		return owner;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setOwner(java.lang.String)
	 */
	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getAssociatedUuid()
	 */
	@Override
	public AgaveUUID getAssociatedUuid() {
		return associatedUuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setAssociatedUuid(org.iplantc.service.common.uuid.AgaveUUID)
	 */
	@Override
	public void setAssociatedUuid(AgaveUUID associatedUuid) {
		this.associatedUuid = associatedUuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getResponseCode()
	 */
	@Override
	public int getResponseCode() {
		return responseCode;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setResponseCode(int)
	 */
	@Override
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	/**
	 * @return the log
	 */
	public static Logger getLog() {
		return log;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> getJobRow(String table, String uuid) throws NotificationException 
	{
		try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            Map<String, Object> row = (Map<String, Object>)session
            		.createSQLQuery("select * from " + table + " where uuid = :uuid")
            		.setString("uuid", uuid.toString())
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();

            session.flush();
            
            if (row == null)
                throw new UUIDException("No such uuid present");
            
            return row;
        }
        catch (Throwable ex) 
        {
			throw new NotificationException(ex);
		}  
        finally 
        {
        	try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
	 */
	@Override
	public String getHtmlEmailBody() {
	    return String.format("<div><pre>%s</pre></div>", getHtmlEmailBody());
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#getCustomNotificationMessageContextData()
	 */
    @Override
	public String getCustomNotificationMessageContextData() {
        return customNotificationMessageContextData;
    }

    /* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent2#setCustomNotificationMessageContextData(java.lang.String)
	 */
    @Override
	public void setCustomNotificationMessageContextData(String customNotificationMessageContextData) {
        this.customNotificationMessageContextData = customNotificationMessageContextData;
    }
}
