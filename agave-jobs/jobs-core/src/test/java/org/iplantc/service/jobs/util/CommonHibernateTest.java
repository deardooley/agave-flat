package org.iplantc.service.jobs.util;

import java.io.IOException;

import org.iplantc.service.common.persistence.HibernateUtil;

public class CommonHibernateTest {

    public static void initdb() throws IOException {
       
    }

    public static void closeSession(){
        HibernateUtil.flush();
        HibernateUtil.closeSession();
    }

    public static void closeSessionFactory(){
        closeSession();
        HibernateUtil.getSessionFactory().close();
    }
}