package org.iplantc.service.common.persistence;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.Entity;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.FilterDef;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.iplantc.service.common.persistence.time.CdtTimestampType;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 * Basic Hibernate helper class, handles SessionFactory, Session and Transaction.
 * <p>
 * Uses a static initializer for the initial SessionFactory creation
 * and holds Session and Transactions in thread local variables. All
 * exceptions are wrapped in an unchecked PersistenceException.
 *
 * @author christian@hibernate.org
 */
public class HibernateUtil {

	private static Logger log = Logger.getLogger(HibernateUtil.class);

	private static Configuration configuration;
	private static SessionFactory sessionFactory;
	private static Queue<String> filters = new ConcurrentLinkedQueue<String>();
	private static final ThreadLocal<Session> threadSession = new ThreadLocal<Session>();
	private static final ThreadLocal<Transaction> threadTransaction = new ThreadLocal<Transaction>();
	private static final ThreadLocal<Interceptor> threadInterceptor = new ThreadLocal<Interceptor>();

	// Create the initial SessionFactory from the default configuration files
	static {
		try {
			Reflections reflections = new Reflections(new ConfigurationBuilder()
				.filterInputsBy(new FilterBuilder().includePackage("org.iplantc.service"))
				.addUrls(ClasspathHelper.forPackage("org.iplantc.service"))
				.addUrls(ClasspathHelper.forClass(Entity.class))
				.setScanners(new ResourcesScanner(), 
				             new TypeAnnotationsScanner(), 
				             new SubTypesScanner()));
			
			configuration = new Configuration().configure(
					HibernateUtil.class.getClassLoader().getResource("hibernate.cfg.xml"));
			Set<Class<?>> jpaEntities = reflections.getTypesAnnotatedWith(Entity.class);
			for (Class<?> clazz: jpaEntities) {
				configuration.addAnnotatedClass(clazz);
			}
			configuration.setNamingStrategy(new ImprovedNamingStrategy());
			
			configuration.addSqlFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1 REGEXP ?2"));
			
			// adding to properly handle conversion of dates to/from the db in UTC
			configuration.registerTypeOverride(new CdtTimestampType());
			
			// uncomment to see hql of all schema export statments.
//			new SchemaExport(configuration).create(true, false);
			sessionFactory = configuration.buildSessionFactory();
			
		} catch (Throwable ex) {
			// We have to catch Throwable, otherwise we will miss
			// NoClassDefFoundError and other subclasses of Error
			log.error("Building SessionFactory failed.", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 * Returns the SessionFactory used for this static class.
	 *
	 * @return SessionFactory
	 */
	public static SessionFactory getSessionFactory() 
	{
		return sessionFactory;
	}

	/**
	 * Returns the original Hibernate configuration.
	 *
	 * @return Configuration
	 */
	public static Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Rebuild the SessionFactory with the static Configuration.
	 *
	 */
	 public static void rebuildSessionFactory()
		throws PersistenceException {
		synchronized(sessionFactory) {
			try {
				sessionFactory = getConfiguration().buildSessionFactory();
			} catch (Exception ex) {
				throw new PersistenceException(ex);
			}
		}
	 }

	/**
	 * Rebuild the SessionFactory with the given Hibernate Configuration.
	 *
	 * @param cfg
	 */
	public static void rebuildSessionFactory(Configuration cfg)
	throws PersistenceException 
	{
		synchronized(sessionFactory) {
			try {
				Reflections reflections = new Reflections(new ConfigurationBuilder()
					.filterInputsBy(new FilterBuilder().includePackage("org.iplantc.service"))
					.addUrls(ClasspathHelper.forPackage("org.iplantc.service"))
					.addUrls(ClasspathHelper.forClass(Entity.class))
					.setScanners(new ResourcesScanner(), 
				             new TypeAnnotationsScanner(), 
				             new SubTypesScanner()));
				
				Set<Class<?>> jpaEntities = reflections.getTypesAnnotatedWith(Entity.class);
				for (Class<?> clazz: jpaEntities) {
					cfg.addAnnotatedClass(clazz);
				}
				
				cfg.setNamingStrategy(new ImprovedNamingStrategy());
				
				sessionFactory = cfg.buildSessionFactory();
				configuration = cfg;
			} catch (Exception ex) {
				throw new PersistenceException(ex);
			}
		}
	 }

	/**
	 * Retrieves the current Session local to the thread.
	 * <p/>
	 * If no Session is open, opens a new Session for the running thread.
	 *
	 * @return Session
	 */
	public static Session getSession()
		throws PersistenceException {
		Session s = (Session) threadSession.get();
        try {
			if (s == null || !s.isOpen()) {
				//log.debug("Opening new Session for this thread.");
				if (getInterceptor() != null) {
//				    log.debug("Opening session...");
//					log.debug("Using interceptor: " + getInterceptor().getClass());
					s = getSessionFactory().openSession(getInterceptor());
				} else {
					s = getSessionFactory().openSession();
//					log.debug("Opening session...");
				}
				threadSession.set(s);
            } else if (s.isConnected() == false) {
//                log.debug("Reusing existing session...");
                reconnect(s);
            }
//            s.enableFilter("filterDeletedJobs");
//          s.enableFilter("limitJobs");
		} catch (HibernateException ex) {
		    throw new PersistenceException(ex);
		}
		return s;
	}

	/**
	 * Closes the Session local to the thread.
	 */
	public static void closeSession()
		throws PersistenceException {
        try {
			Session s = (Session) threadSession.get();
			threadSession.set(null);
			if (s != null && s.isOpen()) {
				//log.debug("Closing Session of this thread.");
				s.close();
			}
		} catch (HibernateException ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Start a new database transaction.
	 */
	public static void beginTransaction()
		throws PersistenceException {
		Transaction tx = (Transaction) threadTransaction.get();
		try {
			if (tx == null) {
//				log.debug("Starting new database transaction in this thread.");
				tx = getSession().beginTransaction();
				threadTransaction.set(tx);
			}
		} catch (HibernateException ex) {
			log.error("failed to begin transaction",ex);
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Commit the database transaction.
	 */
	public static void commitTransaction()
		throws PersistenceException {
		Transaction tx = (Transaction) threadTransaction.get();
		try {
			if ( tx != null && !tx.wasCommitted()
							&& !tx.wasRolledBack() ) {
//				log.debug("Committing database transaction of this thread.");
				tx.commit();
			}
			threadTransaction.set(null);
		} catch (HibernateException ex) {
			rollbackTransaction();
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Commit the database transaction.
	 */
	public static void rollbackTransaction()
		throws PersistenceException {
		Transaction tx = (Transaction) threadTransaction.get();
		try {
			threadTransaction.set(null);
			if ( tx != null && !tx.wasCommitted() && !tx.wasRolledBack() ) {
//				log.debug("Trying to rollback database transaction of this thread.");
				tx.rollback();
			}
		} catch (HibernateException ex) {
			throw new PersistenceException(ex);
		} finally {
			closeSession();
		}
	}

	/**
	 * Reconnects a Hibernate Session to the current Thread.
	 *
	 * @param session The Hibernate Session to be reconnected.
	 */
	@SuppressWarnings("deprecation")
	public static void reconnect(Session session)
		throws PersistenceException {
		try {
			session.reconnect();
			threadSession.set(session);
		} catch (HibernateException ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Disconnect and return Session from current Thread.
	 *
	 * @return Session the disconnected Session
	 */
	public static Session disconnectSession()
		throws PersistenceException {

		Session session = getSession();
		try {
			threadSession.set(null);
			if (session.isConnected() && session.isOpen())
				session.disconnect();
		} catch (HibernateException ex) {
			throw new PersistenceException(ex);
		}
		return session;
	}

	/**
	 * Register a Hibernate interceptor with the current thread.
	 * <p>
	 * Every Session opened is opened with this interceptor after
	 * registration. Has no effect if the current Session of the
	 * thread is already open, effective on next close()/getSession().
	 */
	public static void registerInterceptor(Interceptor interceptor) {
		threadInterceptor.set(interceptor);
	}

	private static Interceptor getInterceptor() {
		Interceptor interceptor =
			(Interceptor) threadInterceptor.get();
		return interceptor;
	}

	public static void flush() {
		beginTransaction();
		
		Session session = getSession();
		try {
//			session.getTransaction().commit();
			session.flush();
		} catch (HibernateException ex) {
			throw new PersistenceException("Failed to flush session. Previous transaction may not show up immediately");
		}
	}
	
	public static void disableAllFilters() {
	    Reflections reflections = new Reflections(new ConfigurationBuilder()
            .filterInputsBy(new FilterBuilder().includePackage("org.iplantc.service"))
            .addUrls(ClasspathHelper.forPackage("org.iplantc.service"))
            .addUrls(ClasspathHelper.forClass(FilterDef.class))
            .setScanners(new TypeAnnotationsScanner(), 
                     new SubTypesScanner()));
	    
	    if (filters.isEmpty()) {
	        Set<Class<?>> filterEntities = reflections.getTypesAnnotatedWith(FilterDef.class);
    	    for (Class<?> clazz: filterEntities) {
                filters.add(clazz.getAnnotation(FilterDef.class).name());
            }
	    }
	    
        Session session = getSession();
        for (String filterName: filters) {
//            log.debug("Disabling filter " + clazz.getAnnotation(FilterDef.class).name());
            session.disableFilter(filterName);
//            ParamDef[] params = clazz.getAnnotation(FilterDef.class).parameters();
//            for (ParamDef param: params) {
//                session.disableFilter(param.name());
//            }
        } 
	}

}