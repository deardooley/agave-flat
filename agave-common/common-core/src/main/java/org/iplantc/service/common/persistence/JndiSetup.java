package org.iplantc.service.common.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;


/**
 * Utility class to setup a jndi connection when there is not one otherwise
 * specified.
 * 
 * @author sterry1
 */
public class JndiSetup 
{
	private static final Logger log = Logger.getLogger(JndiSetup.class);
    static Properties props = new Properties();

    public static void init()
    {
    	InputStream jdbcStream = null;
        try {
        	jdbcStream = JndiSetup.class.getClassLoader().getResourceAsStream("jdbc.properties");
            props.load(jdbcStream);
        } catch (IOException e) {
            log.warn("Unable to load jdbc properties file.", e);
        } finally {
        	if (jdbcStream != null) try {jdbcStream.close();} catch (Exception e){}
        }

        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.memory.MemoryContextFactory");
        System.setProperty("org.osjava.sj.jndi.shared", "true");

        InitialContext ic = null;
        try {
            ic = new InitialContext();
            ic.createSubcontext("java:comp/env/jdbc");
            // Construct DataSource
            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(props.getProperty("url"));
            ds.setDatabaseName(props.getProperty("dbname"));
            ds.setUser(props.getProperty("user"));
            ds.setPassword(props.getProperty("password"));

            // Put datasource in JNDI context
            ic.bind("java:comp/env/jdbc/" + props.getProperty("ds_name"), ds);
        } 
        catch (NamingException e) {
            e.printStackTrace();
        }
    }   
    
    public static void close() {
    	InitialContext ic = null;
        try {
            ic = new InitialContext();
            ic.createSubcontext("java:comp/env/jdbc");
            
        } catch (NamingException e) {
            e.printStackTrace();
        }
        finally {
        	try {ic.close();} catch (Exception e){}
        }
    }
}
