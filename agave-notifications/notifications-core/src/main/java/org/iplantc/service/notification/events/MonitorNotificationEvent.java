/**
 * 
 */
package org.iplantc.service.notification.events;

import java.math.BigInteger;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class MonitorNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(MonitorNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public MonitorNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
	    String body =   "ID: ${UUID}\n" +
                "Owner: ${OWNER}\n" +
                "Monitored system: ${TARGET}\n" +
                "Check result: ${LAST_RESULT}\n" +
                "Atomatically update system status: ${UPDATE_SYSTEM_STATUS}\n" +
                "Check Frequency: ${FREQUENCY}\n" +
                "Is check active: ${ACTIVE}\n" +
                "Last successful check: ${LAST_SUCCESS}\n" +
                "Next check: ${NEXT_CHECK}\n" +
                "Last updated: ${LAST_UPDATED}\n" + 
                "Created: ${CREATED}\n";        
        
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "The following monitor was created: \n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "The following monitor was deleted.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "activated")) {
			body = "The following monitor was reactivated.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "deactivated")) {
			body = "The following monitor was deactivated.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "passed")) {
			body = "The following monitor check passed.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "failed")) {
			body = "The following monitor check failed.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "unknown")) {
			body = "The following monitor check result was indeterminate.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "result_change")) {
			body = "The following monitor changed status.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
			body = "The following monitor changed status.\n\n" + body;
		}
		else
		{
			body = "The following monitor received a(n) " + event +  
					" event. The current monitor description is now: \n\n" + body;
		}
		
		return resolveMacros(body, false);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = "<p><strong>ID: ${UUID}<br>" +
                "<strong>Owner:</strong> ${OWNER}<br>" +
                "<strong>Monitored system:</strong> ${TARGET}<br>" +
                "<strong>Check result:</strong> ${LAST_RESULT}<br>" +
                "<strong>Atomatically update system status:</strong> ${UPDATE_SYSTEM_STATUS}<br>" +
                "<strong>Check Frequency:</strong> ${FREQUENCY}<br>" +
                "<strong>Is check active:</strong> ${ACTIVE}<br>" +
                "<strong>Last successful check:</strong> ${LAST_SUCCESS}<br>" +
                "<strong>Next check:</strong> ${NEXT_CHECK}<br>" +
                "<strong>Last updated:</strong> ${LAST_UPDATED}<br>" + 
                "<strong>Created:</strong> ${CREATED}</p>";      
        
        if (StringUtils.equalsIgnoreCase(event, "created")) {
            body = "<p>The following monitor was created: </p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
            body = "<p>The following monitor was deleted.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "activated")) {
            body = "<p>The following monitor was reactivated.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "deactivated")) {
            body = "<p>The following monitor was deactivated.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "passed")) {
            body = "<p>The following monitor check passed.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "failed")) {
            body = "<p>The following monitor check failed.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "unknown")) {
            body = "<p>The following monitor check result was indeterminate.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "result_change")) {
            body = "<p>The following monitor changed status.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
            body = "<p>The following monitor changed status.</p><br>" + body;
        }
        else
        {
            body = "<p>The following monitor received a(n) " + event +  
                    " event. The current monitor description is now: </p><br>" + body;
        }
        
        return resolveMacros(body, false);
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			return resolveMacros("New monitor created for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			return resolveMacros("Monitor deleted for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "activated")) {
			return resolveMacros("Monitor reactivated for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "deactivated")) {
			return resolveMacros("Monitor deactivated for ${TARGET}", false);
		} 
		else if (StringUtils.equalsIgnoreCase(event, "passed")) {
			return resolveMacros("Monitor check passed for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "failed")) {
			return resolveMacros("Monitor check failed for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "unknown")) {
			return resolveMacros("Monitor check unknown for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "result_change")) {
			return resolveMacros("Monitor changed status for ${TARGET}", false);
		}
		else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
			return resolveMacros("Monitor changed status for ${TARGET}", false);
		}
		else {
			return resolveMacros("Monitor received a(n) ${EVENT} event", false);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow("monitors", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${OWNER}", notification.getOwner());
			String systemId = jobFieldMap.get("system").toString();
			body = StringUtils.replace(body, "${SYSTEM}", systemId);
			body = StringUtils.replace(body, "${SYSTEM_ID}", systemId);
			body = StringUtils.replace(body, "${TARGET}", systemId);
			
			String currentlyActive = "false";
            if (jobFieldMap.get("is_active") instanceof Byte) {
                if ((Byte)jobFieldMap.get("is_active") == 1) {
                    currentlyActive = "true";
                }
            }
            else if ((Integer)jobFieldMap.get("is_active") == 1) {
                currentlyActive = "true";
            }
            body = StringUtils.replace(body, "${ACTIVE}", currentlyActive);
            
            
            String updateSystemStatus = "false";
            if (jobFieldMap.get("update_system_status") instanceof Byte) {
                if ((Byte)jobFieldMap.get("update_system_status") == 1) {
                    updateSystemStatus = "true";
                }
            }
            else if ((Integer)jobFieldMap.get("update_system_status") == 1) {
                updateSystemStatus = "true";
            }
            body = StringUtils.replace(body, "${UPDATE_SYSTEM_STATUS}", updateSystemStatus);
            
			String internalUsername = (String)jobFieldMap.get("internal_username");
			if (internalUsername == null) {
				internalUsername = "";
			}
			
			body = StringUtils.replace(body, "${INTERNAL_USERNAME}", internalUsername);
			body = StringUtils.replace(body, "${FREQUENCY}", jobFieldMap.get("frequency").toString() + " minutes");
			body = StringUtils.replace(body, "${LAST_UPDATED}", new DateTime(jobFieldMap.get("last_updated")).toString());
            body = StringUtils.replace(body, "${NEXT_CHECK}", new DateTime(jobFieldMap.get("next_update_time")).toString());
            body = StringUtils.replace(body, "${CREATED}", new DateTime(jobFieldMap.get("created")).toString());
            
			Object lastSuccess = jobFieldMap.get("last_success");
			if (lastSuccess == null) {
				body = StringUtils.replace(body, "${LAST_SUCCESS}", "");
				body = StringUtils.replace(body, "${LAST_RESULT}", "failure");
			} else { 
			    if (new DateTime(lastSuccess).plusMinutes(1).isAfter(new DateTime(jobFieldMap.get("last_updated")).getMillis())){
			        body = StringUtils.replace(body, "${LAST_RESULT}", "success");
			    } else {
			        body = StringUtils.replace(body, "${LAST_RESULT}", "failure");
			    }
				body = StringUtils.replace(body, "${LAST_SUCCESS}", new DateTime(lastSuccess).toString());
			}
			
			boolean resolvedCheck = false;
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
                    if (json != null && json.isObject() && json.has("lastCheck") && json.get("lastCheck").isObject()) {
                        JsonNode jsonLastCheck = json.get("lastCheck");
                        body = StringUtils.replace(body, "${LAST_CHECK_ID}", jsonLastCheck.hasNonNull("id") ? jsonLastCheck.get("id").asText() : "");
                        body = StringUtils.replace(body, "${LAST_MESSAGE}", jsonLastCheck.hasNonNull("message") ? jsonLastCheck.get("message").asText() : "");
                        body = StringUtils.replace(body, "${TYPE}", jsonLastCheck.hasNonNull("type") ? jsonLastCheck.get("type").asText() : "");
                        resolvedCheck = true;
                    } 
                } catch (Exception e) {
                    log.error("Failed to resolve monitor check info from message JSON", e);
                }
            } 
			
			if (!resolvedCheck) 
			{
			    body = StringUtils.replace(body, "${LAST_CHECK_ID}", "");
			    body = StringUtils.replace(body, "${LAST_MESSAGE}", "");
			    body = StringUtils.replace(body, "${TYPE}", "");
			}
			
			return body;
		}
		catch (Exception e) {
		    log.error("Failed to create notification body", e);
			return "Monitor " + associatedUuid.toString() + " received a(n) " + event +  
					" event.\n\n";
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> getJobRow(String table, String uuid) throws NotificationException 
	{
		try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            String sql = "SELECT * from monitors m where m.uuid = :uuid";
            
            Map<String, Object> monitorRow = (Map<String, Object>)session
            		.createSQLQuery(sql)
            		.setString("uuid", uuid.toString())
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();
            
            if (monitorRow == null || monitorRow.isEmpty())
                throw new UUIDException("No monitor found matching " + uuid );
            
            Map<String, Object> systemRow = (Map<String, Object>)session
            		.createSQLQuery("SELECT s.system_id from systems s where s.id = :systemId and s.tenant_id = :tenantId")
            		.setLong("systemId", ((BigInteger)monitorRow.get("system")).longValue())
            		.setString("tenantId", (String)monitorRow.get("tenant_id"))
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();
            
            monitorRow.put("system", systemRow.get("system_id"));
            
            session.flush();
            
            return monitorRow;
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

}
