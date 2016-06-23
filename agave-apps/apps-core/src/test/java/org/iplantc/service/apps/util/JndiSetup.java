package org.iplantc.service.apps.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;


/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 7/18/13
 * Time: 8:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class JndiSetup 
{
    static Properties props = new Properties();

    public static void doSetup(String ds_name)
    {
        try {
            props.load(new FileInputStream("target/test-classes/jdbc.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.memory.MemoryContextFactory");
        System.setProperty("org.osjava.sj.jndi.shared", "true");

        InitialContext ic = null;
        try {
            ic = new InitialContext();
            ic.createSubcontext("java:/comp/env/jdbc");
            // Construct DataSource
            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(props.getProperty("url"));
            ds.setDatabaseName(props.getProperty("dbname"));
            ds.setUser(props.getProperty("user"));
            ds.setPassword(props.getProperty("password"));

            // Put datasource in JNDI context
            ic.bind("java:/comp/env/jdbc/" + props.getProperty("ds_name"), ds);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        JndiSetup.doSetup("iplant_io");

//        DataSource ds;
//        Statement stmnt;
//        InitialContext ic = null;
//
//        try {
//            ic = new InitialContext();
//            ds = (DataSource)ic.lookup("java:/comp/env/jdbc/iplant_io");
//            Connection connection = ds.getConnection();
//            stmnt = connection.createStatement();
//            boolean b = stmnt.execute("select * from `iplant-api`.`jobs`;");
//            assert b;
//        } catch (NamingException e) {
//            e.printStackTrace();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }
}
