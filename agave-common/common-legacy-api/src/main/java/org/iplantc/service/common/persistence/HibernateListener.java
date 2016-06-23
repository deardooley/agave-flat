package org.iplantc.service.common.persistence;

import java.lang.reflect.Field;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.mysql.jdbc.AbandonedConnectionCleanupThread;

import java.sql.Driver;

/**
 * Adds a listener to the servlet closing the session factory down after use.
 * This should prevent the permgen exceptions caused by orphaned threadlocal
 * variables in {@link HibernateUtil}. Add the following to your web.xml to enable this
 * filter:
 * 
 * <listener>
 * <listener-class>org.iplantc.service.common.persistence.HibernateListener
 * </listener-class> </listener>
 * 
 * @author dooley
 * 
 */
public class HibernateListener implements ServletContextListener 
{	
	private static final Logger log = Logger.getLogger(HibernateListener.class);

	public void contextInitialized(ServletContextEvent event) {
		// HibernateUtil.getSessionFactory(); // Just call the static
		// initializer of that class
	}

	public void contextDestroyed(ServletContextEvent event) {
		// HibernateUtil.getSessionFactory().close(); // Free all resources
//		cleanupThreadLocals(null, "cglib", Thread.currentThread().getContextClassLoader());
		
//		Enumeration<Driver> drivers = DriverManager.getDrivers();
//        Driver d = null;
//        while(drivers.hasMoreElements()) {
//            try {
//                d = drivers.nextElement();
//                DriverManager.deregisterDriver(d);
//                log.warn(String.format("Driver %s deregistered", d));
//            } catch (SQLException ex) {
//                log.warn(String.format("Error deregistering driver %s", d), ex);
//            }
//        }
//        try {
//        	AbandonedConnectionCleanupThread.shutdown();
//        } catch (InterruptedException e) {
//            log.warn("SEVERE problem cleaning up: " + e.getMessage());
//            e.printStackTrace();
//        }
        
	}
	

	@SuppressWarnings("rawtypes")
	public static void cleanupThreadLocals(Thread thread, final String classFilter, final ClassLoader classLoader) 
	{
//		if (classLoader != null) 
//		{
//			System.out.println("@@ cleanupThreadLocals for classLoader="
//					+ classLoader.getClass().getName() + "@"
//					+ Integer.toHexString(classLoader.hashCode()));
//		}
//		
//		Thread[] threadList;
//		if (thread != null) {
//			threadList = new Thread[1];
//			threadList[0] = thread;
//		} else {
//			// Every thread
//			threadList = new Thread[Thread.activeCount()];
//			Thread.enumerate(threadList);
//		}
//
//		for (int iThreadList = 0; iThreadList < threadList.length; iThreadList++) 
//		{
//			Thread t = threadList[iThreadList];
//
//			Field field;
//			try 
//			{
//				Class c;
//				if (t instanceof java.lang.Thread) 
//				{
//					c = t.getClass();
//					while ((c != null) && (c != java.lang.Thread.class))
//						c = c.getSuperclass();
//					
//					if (c != null) 
//					{
//						field = c.getDeclaredField("threadLocals");
//						field.setAccessible(true);
//
//						Object threadLocals = field.get(t);
//						if (threadLocals != null) {
//							Field entries = threadLocals.getClass()
//									.getDeclaredField("table");
//							entries.setAccessible(true);
//							Object entryList[] = (Object[]) entries
//									.get(threadLocals);
//							for (int iEntry = 0; iEntry < entryList.length; iEntry++) 
//							{
//								if (entryList[iEntry] != null) 
//								{
//									Field fValue = entryList[iEntry].getClass()
//											.getDeclaredField("value");
//									
//									if (fValue != null) 
//									{
//										fValue.setAccessible(true);
//										Object value = fValue
//												.get(entryList[iEntry]);
//										if (value != null) 
//										{
//											boolean flag = true;
//											System.out
//													.println("found entry: value="
//															+ value.getClass()
//																	.getName()
//															+ "@"
//															+ Integer
//																	.toHexString(value
//																			.hashCode()));
//											if ((classFilter != null)
//													&& (value
//															.getClass()
//															.getName()
//															.indexOf(
//																	classFilter) == -1))
//												flag = false;
//											if ((classLoader != null)
//													&& (value.getClass()
//															.getClassLoader() != classLoader))
//												flag = false;
//
//											if (flag) {
//												System.out
//														.println("@@ Entry for "
//																+ value.getClass()
//																		.getName()
//																+ "@"
//																+ Integer
//																		.toHexString(value
//																				.hashCode())
//																+ " cleared.");
//												entryList[iEntry] = null;
//											}
//										}
//									}
//								}
//							}
//						} else
//							System.out.println("@@ no threadLocals for '"
//									+ t.getName() + "'");
//					}
//				}
//			} catch (IllegalAccessException ex) {
//				log.error(ex);
//			} catch (SecurityException ex) {
//				log.error(ex);
//			} catch (NoSuchFieldException ex) {
//				log.error(ex);
//				Field fields[] = t.getClass().getDeclaredFields();
//				System.out.println("Fields available for "
//						+ t.getClass().getName() + ": ");
//				for (int ii = 0; ii < fields.length; ii++)
//					System.out.println(fields[ii].getName());
//				System.out.println();
//			}
//		}
	}
}