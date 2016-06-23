package org.iplantc.service.profile.model;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

public class LdapProfile extends Profile {

	public LdapProfile(Attributes attrs) throws NamingException {
		this.username = (String) attrs.get("uid").get(0);
	}
}
