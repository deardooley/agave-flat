/**
 * 
 */
package org.iplantc.service.common.auth;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.iplantc.service.common.Settings;

/**
 * @author dooley
 * 
 */
public class LDAPClient {

	private String	username;
	private String	pass;

	public LDAPClient(String username, String pass)
	{
		this.username = username;
		this.pass = pass;
	}

	public boolean login()
	{

		return getUserAttributesFromLDAP() != null;

	}

	public String getUserEmail()
	{
		Attributes attributes = getUserAttributesFromLDAP();
		return attributes.get("mail").toString();
	}
	
	public String getUserEmail(String uid)
	{
		Attributes attributes = getUserAttributesFromLDAP(uid);
		if (attributes == null) {
			return null;
		} else {
			return attributes.get("mail").toString();
		}
	}
	
	public String getUserFullName()
	{
		Attributes attributes = getUserAttributesFromLDAP();
		return attributes.get("name").toString();
	}

	public String getUserDn()
	{
		return "uid=" + username + "," + Settings.IPLANT_LDAP_BASE_DN;
	}

	public Attributes getUserAttributesFromLDAP() 
	{
		return getUserAttributesFromLDAP(username); 
	}
	
	public Attributes getUserAttributesFromLDAP(String uid)
	{
		System.setProperty("javax.net.ssl.keyStore", Settings.KEYSTORE_PATH);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");

		System
				.setProperty("javax.net.ssl.trustStore",
						Settings.TRUSTSTORE_PATH);
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, Settings.IPLANT_LDAP_URL + "/" 
				+ Settings.IPLANT_LDAP_BASE_DN);

		// Specify SSL
		// env.put(Context.SECURITY_PROTOCOL, "ssl");

		// Authenticate as S. User and password "mysecret"
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "uid=" + uid + ","
				+ Settings.IPLANT_LDAP_BASE_DN);
		env.put(Context.SECURITY_CREDENTIALS, pass);

		DirContext ctx;
		try
		{
			ctx = new InitialDirContext(env);
		}
		catch (NamingException e)
		{
			throw new RuntimeException(e);
		}

		// LinkedList<String> list = new LinkedList<String>();
		NamingEnumeration<SearchResult> results = null;
		try
		{
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			results = ctx.search("", "uid=" + uid, controls);
			return results.next().getAttributes();
			// while (results.hasMore()) {
			// SearchResult searchResult = (SearchResult) results.next();
			// // Attributes attributes = searchResult.getAttributes();
			// return searchResult.getAttributes();
			// // Attribute attr = attributes.get("cn");
			// // String cn = (String) attr.get();
			// // list.add(cn);
			// }
			// } catch (NameNotFoundException e) {
			// e.printStackTrace();
			// // The base context was not found.
			// // Just clean up and exit.
			// } catch (NamingException e) {
			// throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{

			if (results != null)
			{
				try
				{
					results.close();
				}
				catch (Exception e)
				{
					// Never mind this.
				}
			}
			if (ctx != null)
			{
				try
				{
					ctx.close();
				}
				catch (Exception e)
				{
					// Never mind this.
				}
			}
		}
	}
}
