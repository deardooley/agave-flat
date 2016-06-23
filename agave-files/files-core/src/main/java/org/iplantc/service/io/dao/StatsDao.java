package org.iplantc.service.io.dao;

import java.math.BigInteger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.io.util.ServiceUtils;

/**
 * Data access for internal stats counter
 * 
 * @deprecated
 * @author dooley
 *
 */
public class StatsDao 
{
	public static void increment(String stat) 
	{
		if (!ServiceUtils.isValid(stat)) {
			throw new HibernateException("Invalid statistic");
		} 
		
		// if the stat isn't already there, add it with inital value 1
		if (!exists(stat)) {
			persist(stat);
			return;
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession(); 
			session.createSQLQuery("update Stats set counter = counter + 1 where name = :name")
				.setString("name", stat).executeUpdate();
			session.flush();
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw e;
		} 
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}  
	}
	
	private static boolean exists(String stat) {
		long count = getCount(stat);
		return (count > -1);
		
	}
	
	public static long getCount(String stat) 
	{
		if (!ServiceUtils.isValid(stat)) {
			throw new HibernateException("Invalid statistic");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			BigInteger count = (BigInteger) session.createSQLQuery("select (counter) from Stats where name = :name")
				.setString("name", stat).uniqueResult();
		
			session.flush();
			
			if (count == null) {
				return -1;
			} else {
				return count.longValue();
			}
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw e;
		}
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
	
	private static void persist(String stat) 
	{
		if (!ServiceUtils.isValid(stat)) {
			throw new HibernateException("Null file cannot be committed.");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String sql = "insert into Stats (name, counter) values ('" + stat + "', 1)";
			session.createSQLQuery(sql).executeUpdate();
			
			session.flush();
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw e;
		}
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
}
