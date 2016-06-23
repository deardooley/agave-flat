/**
 * 
 */
package org.iplantc.service.notification.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Response from the remote target on a {@link NotificationAttempt}
 * @author dooley
 *
 */
@Embeddable
public class NotificationAttemptResponse {

	@Column(name = "`response_code`", nullable = false)
	private int code = 0;
	
	@Column(name = "`response_message`", nullable = true)
	private String message;
	
	/**
	 * @param code
	 * @param message
	 */
	public NotificationAttemptResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public NotificationAttemptResponse() {}
	
	
}
