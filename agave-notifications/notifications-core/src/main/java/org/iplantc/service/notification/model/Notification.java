package org.iplantc.service.notification.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.validator.constraints.NotEmpty;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.model.CaseInsensitiveEnumDeserializer;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.FailedNotificationAttemptQueue;
import org.iplantc.service.notification.dao.NotificationAttemptDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.constraints.ValidAssociatedUuid;
import org.iplantc.service.notification.model.constraints.ValidCallbackUrl;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;

/**
 * A {@link Notification} represents a subscription target to which one or more {@link #event}s is published.
 * An {@link #event} can be any platform or user-defined occurrence sent from anywhere in Agave. An {@link #event} 
 * can also be a wildcard, {@code *}, which signifies any and all events. An event only makes sense within the
 * context of the API resources from which it springs. The resource(s) for which a {@link Notification} should 
 * listen for occurences of the given {@link #event} are defined by the {@link #associatedUuid} field. The 
 * {@link #associatedUuid} field can be any valid {@link AgaveUUID}. Wildcard {@link #associatedUuid} are also 
 * supported provided the {@link #callbackUrl} is a {@link RealtimeChannel} endpoint or the subscriber is an administrator. 
 * 
 * A {@link Notification} is not concerned with the information being published, it simply handles the content
 * negotiation and forwarding of event messages from the notification queue to the user-defined {@link #callbackUrl}. 
 * Currently notifications can be sent via SMS, email, webhook, and streaming interfaces. 
 * 
 * Every {@link Notification} has a {@link NotificationPolicy} associated with it. The {@link NotificationPolicy}
 * dictates the desired retry and failure policy in situations where communication with the target resource fails. 
 * Every attempt to publish an event message to a subscribed {@link #callbackUrl} is recorded by a {@NotificationAttempt}. 
 * Successful {@NotificationAttempt}s are not stored. Failed {@NotificationAttempt}s are retried, and if eventually 
 * unable to satisfy the {@NotificationPolicy}, are stored in a fixed size queue for user retrieval at a later date. 
 * It is important to note that every individual failed {@NotificationAttempt} is not kept, only a single entry 
 * signifying the overall failed {@NotificationAttempt}.
 * 
 *   
 * @author dooley
 *
 */
@Entity
@Table(name = "notifications")
@FilterDef(name="notificationTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="notificationTenantFilter", condition="tenant_id=:tenantId"))
@ValidAssociatedUuid
public class Notification
{
	/**
	 * Primary key for this entity
	 */
	@Id
	@GeneratedValue
	@Column(name = "`id`", unique = true, nullable = false)
	@JsonIgnore
	private Long id;
	
	/**
	 * UUID of this notification
	 */
	@Column(name = "uuid", nullable = false, length = 64, unique = true)
	@JsonProperty("id")
	@Size(min=3,
		  max=64,
		  message = "Invalid uuid value. uuid must be between {min} and {max} characters long.")
	private String uuid;
	
	/**
	 * UUID of the resource to which this notification applies
	 */
	@Column(name = "associated_uuid", nullable = false, length = 64)
	@Size(min=1,
			max=128, 
			message = "Invalid associatedUuid value. associatedUuid must be between {min} and {max} characters long.")
	private String associatedUuid;
	
	/**
	 * Creator of this notification
	 */
	@Column(name = "owner", nullable = false, length = 32)
	@Size(min=3,max=32, message="Invalid notification owner. Usernames must be between {min} and {max} characters.")
	private String owner;
	
	/**
	 * Textual name of the event that was thrown
	 */
	@Column(name = "`notification_event`", nullable = false, length = 32)
	@JsonProperty("event")
	@Size(min=1, max=128, message="Invalid notification event. Notification events must be between {min} and {max} characters.")
	private String event;
	
	/**
	 * The target url to which to send event notifications 
	 */
	@Column(name = "callback_url", nullable=false, length = 1024)
	@ValidCallbackUrl
	@JsonProperty("url")
	@NotEmpty(message = "Invalid notification URL value. Notification URL are an email address, "
			+ "phone number, web address or wildcard to which the API will send notification of the event.")
	@Size(min=4,max=1024, message="Invalid notification URL. Notification URL must be between {min} and {max} characters.")
	private String callbackUrl;
	
	/**
	 * Should the notification persist after its initial use
	 */
	@Column(name = "is_persistent", columnDefinition = "TINYINT(1)")
	private boolean persistent = false;
	
	/**
	 * Flag for indicating the notifiation has been deleted
	 */
	@JsonIgnore
	@Column(name = "is_visible", columnDefinition = "TINYINT(1)")
	private boolean visible = true; 
	
	/**
	 * Current status of this notification.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 12)
	@JsonIgnore
	private NotificationStatusType status = NotificationStatusType.ACTIVE;
	
	/**
	 * {@link NotificationPolicy} assigned to this notification. The default
	 * policy is {@code null} which will retry a failed {@link NotificationAttempt} 
	 * {@link Settings#MAX_NOTIFICATION_RETRIES} times. 
	 */
	@Embedded
	@Valid
//	@JsonManagedReference("policy")
//	@JsonSerialize(as=NotificationPolicy.class)
	@JsonProperty("policy")
	private NotificationPolicy policy = new NotificationPolicy();
	
	/**
	 * The tenant in which this notification was created.
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
//	@ValidTenant
	@JsonIgnore
	private String tenantId;
	
	/**
	 * The last time this entity was updated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	@NotNull(message="Invalid notification lastUpdated. Notification lastUpdated cannot be null.")
	private Date lastUpdated;	
	
	/**
	 * The creation date of this entity 
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	@NotNull(message="Invalid notification created. Notification created cannot be null.")
	private Date created;
	
	public Notification() {
		this.uuid = new AgaveUUID(UUIDType.NOTIFICATION).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.created = new Date();
		this.lastUpdated = new Date();
	}

	public Notification(String eventName, String callbackUrl) throws NotificationException
	{
		this();
		setEvent(eventName);
		setCallbackUrl(callbackUrl);
	}

	public Notification(String associatedUuid, String owner, String eventName, String callbackUrl, boolean persistent)
	throws NotificationException
	{
		this(eventName, callbackUrl);
		setAssociatedUuid(associatedUuid);
		setOwner(owner);
	}

	/**
	 * @return the id
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param nonce the uuid to set
	 */
	public void setUuid(String uuid) throws NotificationException
	{
		if (StringUtils.length(uuid) > 64) {
			throw new NotificationException("Invalid notification uuid. " +
					"Notification associatedUuid must be less than 64 characters.");
		}
		this.uuid = uuid;
	}

	/**
	 * @return the owner
	 */
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) throws NotificationException
	{
		if (StringUtils.length(owner) > 32) {
			throw new NotificationException("Invalid notification owner. " +
					"Notification owner must be less than 32 characters.");
		}
		this.owner = owner;
	}

	/**
	 * @return the uuid
	 */
	public String getAssociatedUuid()
	{
		return associatedUuid;
	}

	/**
	 * @param nonce the uuid to set
	 */
	public void setAssociatedUuid(String associatedUuid) throws NotificationException
	{
		associatedUuid = StringUtils.trimToEmpty(associatedUuid);

		if (StringUtils.isEmpty(associatedUuid)) {
			throw new NotificationException("Invalid notification event. " +
					"Notification associatedUuid cannot be empty.");
		}
		else if (StringUtils.length(associatedUuid) > 64) {
			throw new NotificationException("Invalid notification associatedUuid. " +
					"Notification associatedUuid must be less than 64 characters.");
		}
		this.associatedUuid = associatedUuid;
	}

	/**
	 * @return the event
	 */
	public String getEvent()
	{
		return event;
	}

	/**
	 * @param event the notificationEvent to set
	 */
	public void setEvent(String event) throws NotificationException
	{
		event = StringUtils.trimToEmpty(event);

		if (StringUtils.isEmpty(event)) {
			throw new NotificationException("Invalid notification event. " +
					"Notification events cannot be null.");
		}
		if (StringUtils.length(event) > 32) {
			throw new NotificationException("Invalid notification event. " +
					"Notification events must be less than 32 characters.");
		}
		this.event = event;
	}

	/**
	 * @return the callbackUrl
	 */
	public String getCallbackUrl()
	{
		return callbackUrl;
	}

	/**
	 * @param callbackUrl the callbackUrl to set
	 * @throws NotificationException
	 */
	public void setCallbackUrl(String callbackUrl) throws NotificationException
	{
		callbackUrl = StringUtils.trimToEmpty(callbackUrl);
		
		this.callbackUrl = callbackUrl;
		
//		if (StringUtils.isEmpty(callbackUrl)) {
//			throw new NotificationException("Invalid callback url. " +
//					"Callbacks cannot be null.");
//		}
//		if (StringUtils.length(callbackUrl) > 1024) {
//			throw new NotificationException("Invalid callback url. " +
//					"Callbacks must be less than 1024 characters.");
//		}
//		try
//		{
//			if (ServiceUtils.isEmailAddress(callbackUrl)) {
//				this.callbackUrl = callbackUrl;
//			}
//			else if (ServiceUtils.isValidPhoneNumber(callbackUrl)) {
//				if (Settings.SMS_PROVIDER == SmsProviderType.TWILIO) {
//					this.callbackUrl = callbackUrl;
//				} else {
//					throw new NotificationException("Invalid callback address. SMS notifications are not enabled for this tenant.");
//				}
//			}
//			else
//			{
//				URI url = new URI(callbackUrl.replaceAll("\\$", "%24").replaceAll("\\{", "%7B").replaceAll("\\}", "%7B"));
//				if (StringUtils.isEmpty(url.getHost()) ||
//						StringUtils.equals(url.getHost(), "localhost") ||
//						url.getHost().startsWith("127.") ||
//						url.getHost().startsWith("255.")) 
//				{
//					throw new NotificationException("Invalid callback host. Please specify a publicly resolvable hostname or ip address.");
//				}
//				else if (StringUtils.isEmpty(url.getScheme()) || !Arrays.asList("http","https","agave").contains(url.getScheme().toLowerCase())) {
//				
//					throw new NotificationException("Invalid callback protocol. Please specify either http, https, or agave");
//				}
//				else {
//					this.callbackUrl = callbackUrl;
//				}
//			}
//		}
//		catch (NotificationException e) {
//			throw e;
//		}
//		catch (Throwable e) {
//			throw new NotificationException("Invalid callback url. " +
//					"Callbacks should be either email addresses or a valid URL.");
//		}
	}

	/**
	 * @return the status
	 */
	public NotificationStatusType getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(NotificationStatusType status) {
		this.status = status;
	}

	/**
	 * @return the policy
	 */
	public NotificationPolicy getPolicy() {
		return policy;
	}

	/**
	 * @param policy the policy to set
	 */
	public void setPolicy(NotificationPolicy policy) {
		this.policy = policy;
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @param visible the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * @return the persistent
	 */
	public boolean isPersistent()
	{
		return persistent;
	}

	/**
	 * @param persistent the persistent to set
	 */
	public void setPersistent(boolean persistent)
	{
		this.persistent = persistent;
	}
	
	/**
	 * @return the tenantId
	 */
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
//	/**
//	 * Deserialization method translating {@link JsonNode} 
//	 * into a validated {@link Notification} object.
//	 * 
//	 * @param jsonNotification the json representation of a notification request
//	 * @return validated {@link Notification} object
//	 * @throws NotificationException
//	 */
//	public static Notification fromJSON(JsonNode jsonNotification) throws NotificationException {
//		try {
//			return fromJSON(new JSONObject(jsonNotification.toString()));
//		} catch (JSONException e) {
//			throw new NotificationException("Failed to parse notification object", e);
//		}
//	}

	public static Notification fromJSON(JsonNode json) 
	throws NotificationException
	{
		SimpleModule enumModule = new SimpleModule();
        enumModule.setDeserializers(new CaseInsensitiveEnumDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        
        mapper.registerModule(enumModule);
		
		try {
			Notification notification = mapper.readValue(json.toString(), Notification.class);
			
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	        Validator validator = factory.getValidator();
	        
        	Set<ConstraintViolation<Notification>> violations = validator.validate(notification);
        	if (violations.isEmpty()) {
        		return notification;
        	} else {
        		throw new NotificationException(violations.iterator().next().getMessage()); 
        	}
        } catch (NotificationException e) {
        	throw e;
        } catch (UnrecognizedPropertyException e) {
        	
        	String nestedField = "";
        	if (e.getReferringClass() == NotificationPolicy.class) {
        		nestedField = "policy.";
        	}
        	
        	String message = "Unknown property " + nestedField + e.getPropertyName();
        	
        	String recommendation = null;
        	String propName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, e.getPropertyName().toLowerCase());
        	int closestLevenshteinMatch = Integer.MAX_VALUE;
        	for (Object prop: e.getKnownPropertyIds()) {
        		int currentLevenshteinMatch = StringUtils.getLevenshteinDistance(prop.toString().toLowerCase(), propName);
        		
        		if (StringUtils.startsWith(prop.toString().toLowerCase(), propName)) {
        			currentLevenshteinMatch = 0;
        		}
        		
        		if (currentLevenshteinMatch < 6) {
        			if (currentLevenshteinMatch == closestLevenshteinMatch) {
        				if (recommendation == null) {
        					recommendation = ". Did you mean " + nestedField + prop.toString();
        				}
        				else {
        					recommendation += ", " + nestedField + prop.toString();
        				}
        				closestLevenshteinMatch = currentLevenshteinMatch;
        			}
        			else if (currentLevenshteinMatch < closestLevenshteinMatch) {
        				recommendation = ". Did you mean " + nestedField + prop.toString();
        				closestLevenshteinMatch = currentLevenshteinMatch;
        			}
        		}
        	}
        	
        	if (recommendation == null) {
        		recommendation = ". Valid values are: " + nestedField + StringUtils.join(e.getKnownPropertyIds(), ", "+nestedField);
        	}
        	else {
        		recommendation += "?";
        	}
        	
        	throw new NotificationException(message + recommendation);
        }
		catch (JsonMappingException e) {
			String message = "Invalid value for ";
			for (Reference ref: e.getPath()) {
				message += ref.getFieldName() + ".";
			}
			 
			throw new NotificationException(message);
		}
		catch (Exception e) {
        	throw new NotificationException("Unexpected error while validating notification.", e); 
        }
        
//		try
//		{
//			if (json.has("url")) {
//				try {
//					notification.setCallbackUrl(json.get("url").asText());
//				} catch (NotificationException e) {
//					throw new NotificationException(e.getMessage() + " This will be a an email address, "
//							+ "phone number, or web address to which the API will send notification of the event.");
//				}
//			} else {
//				throw new NotificationException("Please specify a valid url " +
//						"value for the notification.url field. This will be the email address, "
//							+ "phone number, or web address to which the API will send notification of the event.");
//			}
//
//			if (json.has("associatedUuid") && !json.get("associatedUuid").isNull()) {
//			
//				String associatedUuid = json.get("associatedUuid").textValue();
//				
//				// check for wildcard subscriptions
//				if (StringUtils.equalsIgnoreCase(associatedUuid, "*")) {
//					// websockets are good for the firehose
//					if (ServiceUtils.isValidRealtimeChannel(notification.getCallbackUrl(), notification.getTenantId()) || 
//							AuthorizationHelper.isTenantAdmin(TenancyHelper.getCurrentEndUser())) {
//						notification.setAssociatedUuid("*");
//					}
//					else {
//						throw new NotificationException("Invalid associatedUuid. " +
//		    					"Wildcard notification subscriptions are reserved for administrators.");
//					}
//				}
//				else if (StringUtils.isEmpty(associatedUuid)) {
//					throw new NotificationException("No associated entity provided. " +
//	    					"Please provide an associatedUuid for which this notification should apply.");
//				}
//				else {
//					AgaveUUID agaveUUID = new AgaveUUID(associatedUuid);
//		    		if (StringUtils.isEmpty(agaveUUID.getObjectReference())) {
//		    			throw new NotificationException("Invalid associatedUuid. The provided uuid was not recognized"
//		    					+ "as a valid resource uuid type. Please provide an associatedUuid for which this notification should apply.");
//		    		}
//		    		else {
//		    			notification.setAssociatedUuid(associatedUuid);
//		    		}
//				}
//			} else {
//				throw new NotificationException("No associated entity provided. " +
//    					"Please provide an associatedUuid for which this notification should apply.");
//			}
//			
//			if (json.has("event") && !json.get("event").isNull()) {
//				if (json.get("event").isValueNode()) {
//					notification.setEvent(json.get("event").textValue());
//				} else {
//					throw new NotificationException("Invalid event. Please specify the name of a valid event "
//							+ "for the associatedUuid you specified.");
//				}
//			} else {
//				throw new NotificationException("Invalid event. Please specify the name of a valid event "
//						+ "for the associatedUuid you specified.");
//			}
//			
//			if (json.has("persistent")) {
//				if (json.get("persistent").isBoolean()) {
//					notification.setPersistent(json.get("persistent").asBoolean());
//				} else {
//					throw new NotificationException("persistent should be a boolean value indicating " +
//	    					"whether the subscription should expire after the first notification is sent.");
//				}
//			} 
//			// if persistent was not specified, imply it if they subscribed to
//			// wildcard events.
//			else if (StringUtils.equals("*", notification.getEvent())) {
//				notification.setPersistent(true);
//			// no wildcard, no persistent field, default to false
//			} else {
//				notification.setPersistent(false);
//			}
//			
//			// check for a notification policy
//			if (json.has("policy") && !json.get("policy").isNull()) {
//				ObjectMapper mapper = new ObjectMapper().
//				NotificationPolicy policy = mapper.treeToValue(json, Notification.class);
//				notification.setPolicy(policy);
//			} else {
//				notification.setPolicy(new NotificationPolicy());
//			}
//			
//			notificatinoP
//		}
//		catch (NotificationPolicyException e) {
//			throw e;
//		}
//		catch (NotificationException e) {
//			throw e;
//		}
//		catch (MalformedURLException e) {
//			throw new NotificationException("Invalid url. Please specify a valid callback url " +
//						"for the notification.url field. This will be the url " +
//						"to which the API will POST a when the trigger fires.", e);
//		}
//		catch (JSONException e) {
//			throw new NotificationException("Failed to parse notification object", e);
//		}
//		catch (UUIDException e) {
//			throw new NotificationException(e.getMessage(), e);
//		}
//		catch (Exception e) {
//			throw new NotificationException("Failed to parse notification object: " + e.getMessage(), e);
//		}
//
//		return notification;
	}

	
	@SuppressWarnings("deprecation")
	public String toJSON() throws NotificationException  
	{
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			ObjectNode json = mapper.createObjectNode()
				.put("id", getUuid())
				.put("owner", getOwner())
				.put("url", getCallbackUrl())
				.put("associatedUuid", getAssociatedUuid())
				.put("event", getEvent());
				
			NotificationAttempt lastAttempt = FailedNotificationAttemptQueue.getInstance().next(getUuid());// new NotificationAttemptDao().getLastAttemptForNotificationId(getUuid());
			if (lastAttempt != null) {
				if (lastAttempt.getResponse().getCode() == 0 ) {
					json.putNull("responseCode");
				}
				else {
					json.put("responseCode", lastAttempt.getResponse().getCode());
				}
				
				json.put("attempts", lastAttempt.getAttemptNumber() )
					.put("success", lastAttempt.isSuccess())
					.put("lastSent", new DateTime(lastAttempt.getEndTime()).toString());
			} 
			else {
				json.putNull("responseCode")
					.put("attempts", 0)
					.putNull("lastSent")
					.put("success", false);
			}
			json.put("persistent", isPersistent())
				.put("status", getStatus().name())
				.put("lastUpdated", new DateTime(lastUpdated).toString())
				.put("created", new DateTime(created).toString())
				.put("policy", mapper.valueToTree(getPolicy()));
				
				
			ObjectNode linksObject = mapper.createObjectNode();
			
			linksObject.put("self", (ObjectNode)mapper.createObjectNode()
								.put("href", 
										TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + getUuid()));
			
//			linksObject.put("history", (ObjectNode)mapper.createObjectNode()
//								.put("href", 
//										TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + getUuid() + "/history"));

			linksObject.put("attempts", (ObjectNode)mapper.createObjectNode()
								.put("href", 
										TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + getUuid() + "/attempts"));

			linksObject.put("owner", (ObjectNode)mapper.createObjectNode()
								.put("href", 
										TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner()));
			
			if (!StringUtils.contains("*", getAssociatedUuid()) && !StringUtils.isEmpty(this.associatedUuid)) {
	        	try {
	        		AgaveUUID agaveUuid = new AgaveUUID(associatedUuid);
		        	UUIDType type = agaveUuid.getResourceType();
		        	if (type != null) {
		        		linksObject.put(type.name().toLowerCase(), (ObjectNode)mapper.createObjectNode()
								.put("href", 
										TenancyHelper.resolveURLToCurrentTenant(agaveUuid.getObjectReference())));
		        	}
	        	}
	        	catch (UUIDException e) {}
	        }
			
			json.put("_links", linksObject);
		    
			return json.toString();
		}
		catch (Exception e)
		{
			throw new NotificationException("Error producing JSON output for notification", e);
		}

	}
	
	
	public String toString() {
//		return getId() + "";
		return associatedUuid + " - " + event + " " + callbackUrl;
	}
	
	@Transient
	@JsonIgnore
	public boolean isSuccess() {
		return getStatus() == NotificationStatusType.COMPLETE;
	}
}
