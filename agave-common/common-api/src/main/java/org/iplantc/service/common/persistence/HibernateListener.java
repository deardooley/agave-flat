package org.iplantc.service.common.persistence;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.iplantc.service.common.persistence.HibernateUtil;

/**
 * Adds a listener to the servlet closing the session factory down after use.
 * This should prevent the permgen exceptions caused by orphaned threadlocal
 * variables in hibernateutil. Add the following to your web.xml to enable
 * this filter:
 * 
 * <listener>  
 * 	<listener-class>org.iplantc.service.common.persistence.HibernateListener</listener-class>  
 * </listener> 
 * 
 * @author dooley
 *
 */
public class HibernateListener implements ServletContextListener {  
  
    public void contextInitialized(ServletContextEvent event) {  
        HibernateUtil.getSessionFactory(); // Just call the static initializer of that class      
    }  
  
    public void contextDestroyed(ServletContextEvent event) {  
        HibernateUtil.getSessionFactory().close(); // Free all resources  
    }  
}  