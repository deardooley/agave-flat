package org.iplantc.service.common.auth;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;

public class LDAPClient {
	private static Logger log = Logger.getLogger(LDAPClient.class);
	private String username;
	private String pass;

	public LDAPClient(String username, String pass) {
		this.username = username;
		this.pass = pass;
	}

	private DirContext getDirContext() {
		System.setProperty("javax.net.ssl.keyStore", Settings.KEYSTORE_PATH);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		System.setProperty("javax.net.ssl.trustStore", Settings.TRUSTSTORE_PATH);
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, Settings.IPLANT_LDAP_URL
				+ Settings.IPLANT_LDAP_BASE_DN);

		// Specify SSL
		env.put(Context.SECURITY_PROTOCOL, "ssl");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "uid=" + username + ","
				+ Settings.IPLANT_LDAP_BASE_DN);
		env.put(Context.SECURITY_CREDENTIALS, pass);

		try {
			return new InitialDirContext(env);
		} catch (NamingException e) {
			log.error(e.getMessage(), e.getCause());
		}
		return null;
	}

	public boolean login() {
		DirContext ctx = getDirContext();
		if (ctx == null)
			return false;
		else
			try {
				ctx.close();
			} catch (Exception e) {
			}
		return true;
	}

	public Attributes getAttributes() {
		return getAttributes(username);
	}

	public Attributes getAttributes(String user) {
		DirContext ctx = getDirContext();

		/*
		 * fail to connect to LDAP
		 */
		if (ctx == null)
			return null;

		NamingEnumeration<SearchResult> results = null;
		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			results = ctx.search("", "uid=" + user, controls);
			//return attribute if found
			return results.hasMore()?results.next().getAttributes():null;
		} catch (Exception e) {
			log.error(e.getMessage(), e.getCause());			
		} finally {

			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
				}
			}
			if (ctx != null) {
				try {
					ctx.close();
				} catch (Exception e) {
				}
			}
		}
		return null;
	}

	public List<Attributes> searchDirectory(String attr, String searchTerm) {
		DirContext ctx = getDirContext();
		List<Attributes> results = null;

		/*
		 * fail to connect to LDAP
		 */
		if (ctx == null)
			return null;

		NamingEnumeration<SearchResult> answers = null;
		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			String filter = attr + "=*" + searchTerm + "*";
			answers = ctx.search("", filter, controls);
			while (answers.hasMore()) {
				// initial result only has search result
				if (results == null)
					results = new ArrayList<Attributes>();
				results.add(answers.next().getAttributes());
			}
			return results;
		} catch (Exception e) {
			log.error(e.getMessage(), e.getCause());
		} finally {

			if (answers != null) {
				try {
					answers.close();
				} catch (Exception e) {
				}
			}
			if (ctx != null) {
				try {
					ctx.close();
				} catch (Exception e) {
				}
			}
		}
		return null;
	}
}
