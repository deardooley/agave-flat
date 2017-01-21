/**
 * 
 */
package org.iplantc.service.jobs.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.search.JobEventSearchFilter;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.joda.time.DateTime;

/**
 * Model class for interacting with job events. JobEvents are
 * not persisted as mapped entities in the Job class due to the
 * potentially large number.
 * 
 * @author dooley
 * 
 */
public class JobEventDao {
	
	private static final Logger log = Logger.getLogger(JobEventDao.class);
	
	protected static Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        session.enableFilter("jobEventTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/**
	 * Returns the job event with the given id.
	 * 
	 * @param eventId
	 * @return
	 * @throws JobException
	 */
	public static JobEvent getById(Long eventId)
	throws JobException
	{

		if (!ServiceUtils.isValid(eventId))
			throw new JobException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			JobEvent event = (JobEvent)session.get(JobEvent.class, eventId);
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns all job job events for the job with the given id.
	 * 
	 * @param jobId
	 * @return
	 * @throws JobException
	 */
	public static List<JobEvent> getByJobId(Long jobId)
	throws JobException
	{
		return JobEventDao.getByJobId(jobId, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * @param jobId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<JobEvent> getByJobId(Long jobId, int limit, int offset)
	throws JobException
	{
		return getByJobId(jobId, limit, offset, null, null);
	}
	
	/**
	 * @param jobId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<JobEvent> getByJobId(Long jobId, int limit, int offset, AgaveResourceResultOrdering order, SearchTerm orderBy)
	throws JobException
	{
		if (order == null) {
			order = AgaveResourceResultOrdering.ASCENDING;
		}
		String sortField = "id";
		if (orderBy != null) {
			sortField = String.format(orderBy.getMappedField(), orderBy.getPrefix());
		}
		
	
		if (!ServiceUtils.isValid(jobId))
			throw new JobException("Job id cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "FROM JobEvent e \n"
					+ "WHERE e.job.id = :jobid \n"
					+ "ORDER BY " + sortField + " " +  order.toString() + " \n";
			List<JobEvent> events = session.createQuery(hql)
					.setLong("jobid", jobId)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets the job events for the specified job id and job status
	 * 
	 * @param jobId
	 * @param status
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<JobEvent> getByJobIdAndStatus(Long jobId, JobStatusType status) 
	throws JobException
	{
		if (status == null)
			throw new JobException("status cannot be null");
		
		if (!ServiceUtils.isValid(jobId))
			throw new JobException("job id cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "select * from jobevents where job_id = :jobid and status = :status order by created asc";
			List<JobEvent> events = session.createSQLQuery(hql)
					.addEntity(JobEvent.class)
					.setString("status", status.name())
					.setLong("jobid", jobId)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new job permission. Upates existing ones.
	 * @param pem
	 * @throws JobException
	 */
	public static void persist(JobEvent event) throws JobException
	{
		if (event == null)
			throw new JobException("JobEvent cannot be null");

		try
		{
//			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			log.debug(String.format("Job Event[%d] %s vs %s vs %s", event.getId(), f.format(event.getCreated()), new DateTime().toString(), f.format(new Date())));
			Session session = getSession();
			session.saveOrUpdate(event);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to save job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give job permission.
	 * 
	 * @param event
	 * @throws JobException
	 */
	public static void delete(JobEvent event) throws JobException
	{
		if (event == null)
			throw new JobException("JobEvent cannot be null");

		try
		{
			Session session = getSession();
			session.delete(event);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to delete job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all job events for the job with given id
	 * 
	 * @param jobId
	 * @throws JobException
	 */
	public static void deleteByJobId(Long jobId) throws JobException
	{
		if (jobId == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from JobEvent where job.id = :jobid";
			session.createQuery(hql)
					.setLong("jobid", jobId)
					.executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to delete events for job " + jobId, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Searches for {@link JobEvent} by the given user who matches the given set of 
	 * parameters. Permissions are honored in this query.
	 *
	 * @param jobid
	 * @param searchCriteria
	 * @param offset
	 * @param limit
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<JobEvent> findMatching(Long jobId,
			Map<SearchTerm, Object> searchCriteria,
			int offset, int limit, AgaveResourceResultOrdering order, SearchTerm orderBy) throws JobException
	{
		if (order == null) {
			order = AgaveResourceResultOrdering.ASCENDING;
		}
		
		if (orderBy == null) {
			orderBy = new JobEventSearchFilter().filterAttributeName("id");
		}
		
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "SELECT * \n" +
					" FROM jobevents e \n" +
					" WHERE e.job_id = :jobid \n" +
					"       AND e.tenant_id = :tenantid \n "; 
			
			for (SearchTerm searchTerm: searchCriteria.keySet()) 
			{
				hql += "\n       AND " + searchTerm.getExpression();
			}
			
			hql +=	"\n ORDER BY " + String.format(orderBy.getMappedField(), orderBy.getPrefix()) + " " +  order.toString() + " \n";
			
			String q = hql;
			//log.debug(q);
			SQLQuery query = (SQLQuery)session.createSQLQuery(hql);
			query.setLong("jobid", jobId)
				 .setString("tenantid", TenancyHelper.getCurrentTenantId());
			
			q = StringUtils.replace(q, ":jobid", String.valueOf(jobId));
			q = StringUtils.replace(q, ":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
			
			for (SearchTerm searchTerm: searchCriteria.keySet()) 
			{
			    if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
			        List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
			        for(int i=0;i<formattedDates.size(); i++) {
			            query.setDate(searchTerm.getSearchField()+i, new DateTime(formattedDates.get(i)).toDate());
			            q = StringUtils.replace(q, ":" + searchTerm.getSearchField() + i, "'" + formattedDates.get(i) + "'");
			        }
			    }
			    else if (searchTerm.getOperator().isSetOperator()) 
				{
					query.setParameterList(searchTerm.getSearchField(), (List<Object>)searchCriteria.get(searchTerm));
					q = StringUtils.replace(q, ":" + searchTerm.getSearchField(), "('" + StringUtils.join((List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)), "','") + "')" );
				}
			    else 
				{
					query.setParameter(searchTerm.getSearchField(), 
							searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)));
					q = StringUtils.replace(q, ":" + searchTerm.getSearchField(), "'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'");
				}
			    
			}
			
			log.debug(q);
			
			List<JobEvent> events = query
					.addEntity(JobEvent.class)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.setCacheable(false)
					.setCacheMode(CacheMode.IGNORE)
					.list();

			session.flush();
			
			return events;

		}
		catch (Throwable ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

}
