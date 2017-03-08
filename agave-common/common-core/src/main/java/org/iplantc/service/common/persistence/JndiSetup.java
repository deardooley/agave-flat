package org.iplantc.service.common.persistence;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;


/**
 * Utility class to setup a jndi connection when there is not one otherwise
 * specified.
 * 
 * @author sterry1
 */
public class JndiSetup 
{
    static Properties props = new Properties();

    public static void init()
    {
        try {
            props.load(JndiSetup.class.getClassLoader().getResourceAsStream("jdbc.properties"));
        } catch (IOException e) {
            e.printStackTrace();
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
