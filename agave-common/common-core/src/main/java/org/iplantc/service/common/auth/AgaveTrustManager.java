/**
 * 
 */
package org.iplantc.service.common.auth;

import org.globus.myproxy.MyTrustManager;

/**
 * @author dooley
 *
 */
public class AgaveTrustManager extends MyTrustManager {
	
	private String trustrootLocation;

	public AgaveTrustManager(String trustrootLocation) {
		this.setTrustrootLocation(trustrootLocation);
	}

	public String getTrustrootLocation() {
		return trustrootLocation;
	}

	public void setTrustrootLocation(String trustrootLocation) {
		this.trustrootLocation = trustrootLocation;
	}

}
