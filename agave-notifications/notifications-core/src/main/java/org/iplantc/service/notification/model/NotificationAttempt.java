/**
 * 
 */
package org.iplantc.service.notification.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A scheduled attempt to send a {@link Notification} message to a webhook 
 * endpoint about a specific event. Because a {@link Notification} can represents  
 * a subscription to receive information about one or more events, a 
 * {@link NotificationAttempt} represents an attempt to satisfy the subscription
 * for a specific occurrence of an event.
 * 
 * @author dooley
 *
 */
//@Entity
//@Table(name = "notificationattempts")
//@FilterDef(name="notificationAttemptTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
//@Filters(@Filter(name="notificationAttemptTenantFilter", condition="tenant_id=:tenantId"))
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationAttempt {

	@Id
	@GeneratedValue
	@JsonIgnore
	private String id;
	
	@Embedded
	private NotificationAttemptResponse response;
	
	@Column(name = "`start_time`", nullable = false)
	private Date startTime = null;
	
	@Column(name = "`end_time`", nullable = false)
	private Date endTime = null;
	
	@Column(name = "`scheduled_time`", nullable = false)
	@JsonIgnore
	private Date scheduledTime = null;
	
	@Column(name = "attempt_number", nullable = false)
	private int attemptNumber = 0;
	
	@Column(name = "owner", nullable = false, length = 32)
	private String owner;
	
	@Column(name = "`notification_id`", nullable = false, length = 64)
	private String notificationId;
	
	@Column(name = "`associated_uuid`", nullable = false, length = 64)
	private String associatedUuid;
	
	@Column(name = "`callback_url`", nullable = false, length = 32)
	@JsonProperty("url")
	private String callbackUrl;
	
	@Enumerated(EnumType.STRING)
//	@Column(name = "`callback_type`", nullable = false, length = 32)
//	@JsonProperty("provider")
	@JsonIgnore
	private NotificationCallbackProviderType provider;
	
	@Column(name = "`event_name`", nullable = false, length = 32)
	@JsonProperty("event")
	private String eventName;
	
	@Column(name = "`content`", nullable = false, length = 32)
	private String content;
	
	@Column(name = "`uuid`", unique = true, nullable = false)
	@JsonProperty("id")
	private String uuid;
	
	@Column(name = "tenant_id", nullable=false, length = 128)
	@JsonIgnore
	private String tenantId;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	private Date created;
	
	/**
	 * No-args constructor
	 */
	public NotificationAttempt() {
		this.uuid = new AgaveUUID(UUIDType.NOTIFICATION_DELIVERY).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.created = new Date();
		this.response = new NotificationAttemptResponse();
	}
	
	/**
	 * Creates a new  {@link NotificationAttempt} from an existing {@link Notification} at 
	 * a {@code scheduledTime}. A {@link Notification} represents a subscription for one or 
	 * more events, this represents a notification for a specific event.
	 */
	public NotificationAttempt(String notificationId, String callbackUrl, 
			String owner, String associatedUuid, 
			String eventName, String content, Timestamp scheduledTime) {
		this();
		setNotificationId(notificationId);
		setCallbackUrl(callbackUrl);
		setOwner(owner);
		setAssociatedUuid(associatedUuid);
//		setProvider(protocol);
		setAttemptNumber(attemptNumber);
		setEventName(eventName);
		setContent(content);
		setScheduledTime(scheduledTime);
	}

	/**
	 * @return the id
	 */
	@JsonIgnore
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	@JsonIgnore
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the response
	 */
	public NotificationAttemptResponse getResponse() {
		return response == null ? new NotificationAttemptResponse() : response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(NotificationAttemptResponse response) {
		this.response = response;
	}

	/**
	 * @return the fireTime
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * @param fireTime the fireTime to set
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the scheduledTime
	 */
	public Date getScheduledTime() {
		return scheduledTime;
	}

	/**
	 * @param scheduledTime the scheduledTime to set
	 */
	public void setScheduledTime(Date scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	/**
	 * @return the attemptNumber
	 */
	public int getAttemptNumber() {
		return attemptNumber;
	}

	/**
	 * @param attemptNumber the attemptNumber to set
	 */
	public void setAttemptNumber(int attemptNumber) {
		this.attemptNumber = attemptNumber;
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the notificationId
	 */
	public String getNotificationId() {
		return notificationId;
	}

	/**
	 * @param notificationId the notificationId to set
	 */
	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}

	/**
	 * @return the associatedUuid
	 */
	public String getAssociatedUuid() {
		return associatedUuid;
	}

	/**
	 * @param associatedUuid the associatedUuid to set
	 */
	public void setAssociatedUuid(String associatedUuid) {
		this.associatedUuid = associatedUuid;
	}

	/**
	 * @return the callbackUrl
	 */
	public String getCallbackUrl() {
		return callbackUrl;
	}

	/**
	 * @param callbackUrl the callbackUrl to set
	 */
	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	/**
	 * @return the provider
	 */
	public NotificationCallbackProviderType getProvider() {
		return provider;
	}

	/**
	 * @param provider the provider to set
	 */
	public void setProvider(NotificationCallbackProviderType provider) {
		this.provider = provider;
	}

	/**
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * @param eventName the eventName to set
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the tenantId
	 */
	public String getTenantId() {
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}
	
	/**
	 * Convenience method for parsing the {@link NotificationAttemptResponse#getCode()} and 
	 * determining success based on the value.
	 * @return true if the code is in the 200's
	 */
	@Transient
	@JsonIgnore
	public boolean isSuccess() {
		return getResponse().getCode() >= 200 && getResponse().getCode() < 300;
	}
}
