package org.iplantc.service.profile.dao;

import org.iplantc.service.profile.model.enumeration.ProfileType;


public class ProfileDAOFactory {
	
	public ProfileDAO getProfileDAO(ProfileType type) {
		
		if (type.equals(ProfileType.TRELLIS)) {
			return new TrellisProfileDAO();
		} else if (type.equals(ProfileType.DB)) {
			return new DatabaseProfileDAO();
		} else {
			return new LDAPProfileDAO();
		}
		
	}
}
