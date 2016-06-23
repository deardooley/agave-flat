package org.iplantc.service.io.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

@Entity
@Table(name = "logicalfilenotifications")
public class LogicalFileNotification {

	private Long id;
	private LogicalFile logicalFile;
	private String status;
	private String uri;
	private boolean stillPending = true;
	private Date lastSent;
	private Date created = new Date();

	public LogicalFileNotification() {}

	public LogicalFileNotification(LogicalFile logicalFile, String status, String uri, boolean persistent) {
		this.logicalFile = logicalFile;
		this.status = status;
		this.uri = uri;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "`id`", unique = true, nullable = false)
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
	 * @return the job
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "logicalfile_id")
	public LogicalFile getLogicalFile()
	{
		return logicalFile;
	}

	/**
	 * @param job the job to set
	 */
	public void setLogicalFile(LogicalFile logicalFile)
	{
		this.logicalFile = logicalFile;
	}

	/**
	 * @return the status
	 */
	@Column(name = "`status`", nullable = false, length = 32)
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	/**
	 * @return the uri
	 */
	@Column(name = "callback", length = 1024)
	public String getUri()
	{
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri)
	{
		this.uri = uri;
	}

	/**
	 * @return the stillPending
	 */
	@Column(name = "still_pending", columnDefinition = "TINYINT(1)")
	public boolean isStillPending()
	{
		return stillPending;
	}

	/**
	 * @param stillPending the stillPending to set
	 */
	public void setStillPending(boolean stillPending)
	{
		this.stillPending = stillPending;
	}

	/**
	 * @return the lastSent
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_sent", nullable = false, length = 19)
	public Date getLastSent()
	{
		return lastSent;
	}

	/**
	 * @param lastSent the lastSent to set
	 */
	public void setLastSent(Date lastSent)
	{
		this.lastSent = lastSent;
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

	public LogicalFileNotification fromJSON(JSONObject jsonNotification) throws LogicalFileException
	{
		LogicalFileNotification notification = new LogicalFileNotification();

		try
		{
			if (ServiceUtils.isNonEmptyString(jsonNotification, "url")) {
				URI uri = new URI(jsonNotification.getString("url"));
				notification.setUri(uri.toString());
			} else {
				throw new LogicalFileException("Please specify a valid url " +
						"value for the 'notification.url' field. This will be the url " +
						"to which the API will POST a notification event.");
			}

			if (ServiceUtils.isNonEmptyString(jsonNotification, "status")) {
				try {
					StagingTaskStatus trigger = StagingTaskStatus.valueOf(jsonNotification.getString("status").toUpperCase());
					notification.setStatus(trigger.name());
				} catch (IllegalArgumentException e) {
					TransformTaskStatus trigger = TransformTaskStatus.valueOf(jsonNotification.getString("status").toUpperCase());
					notification.setStatus(trigger.name());
				}

			} else {
				throw new LogicalFileException("Please specify a valid file activity status " +
						"value for the 'notification.status' field. A notification will " +
						"be sent when the file activity reaches this status.");
			}
		}
		catch (LogicalFileException e) {
			throw e;
		}
		catch (IllegalArgumentException e) {
			throw new LogicalFileException("Invalid file activity status. Please specify a valid file status " +
					"value for the 'notification.status' field. A notification will " +
					"be sent when the file activity reaches this status.");
		}
		catch (URISyntaxException e) {
			throw new LogicalFileException("Invalid url. Please specify a valid url " +
						"value for the 'notification.url' field. This will be the url " +
						"to which the API will POST a notification event.", e);
		}
		catch (JSONException e) {
			throw new LogicalFileException("Failed to parse 'notification' object", e);
		}
		catch (Exception e) {
			throw new LogicalFileException("Failed to parse 'notification' object: " + e.getMessage(), e);
		}

		return notification;
	}

	public String toJSON()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{
			js.object()
				.key("url").value(this.uri)
				.key("trigger").value(status.toLowerCase())
			.endObject();
			output = js.toString();
		}
		catch (Exception e)
		{
			System.out.println("Error producing JSON output.");
		}

		return output;

	}
	public String toString() {
		return logicalFile + " " + status + " " + uri;
	}

}
