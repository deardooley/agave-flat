package org.iplantc.service.common.dao;

import org.hibernate.Session;

public abstract class AbstractDao {
	
	/**
	 * Singlular location to get a session for the current thread. Enables
	 * a single place to apply filters that apply entity wide such as tenancy,
	 * pagination, etc.
	 * 
	 * @return
	 */
	abstract protected Session getSession();
}

