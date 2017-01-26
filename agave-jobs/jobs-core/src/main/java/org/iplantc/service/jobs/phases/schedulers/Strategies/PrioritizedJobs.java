package org.iplantc.service.jobs.phases.schedulers.Strategies;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.model.Job;

/** Concrete strategies use this class to store their prioritized
 * tenants, users, and jobs.
 * 
 * @author rcardone
 */
public final class PrioritizedJobs
  implements Iterable<Job>
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(PrioritizedJobs.class);
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Input strategies.
    private final ITenantStrategy _tenantStrategy;
    private final IUserStrategy   _userStrategy;
    private final IJobStrategy    _jobStrategy;
    
    // Input jobs.
    private final List<Job>       _unprioritizedJobs;
    
    // Prioritized list that provides root
    // nodes for ordered navigation.
    private List<String>          _tenants;
    
    // Maps from prioritized list elements.
    //  key = tenant name, value = users
    private HashMap<String,List<String>> _tenantToUsersMap = new HashMap<>();
    //  key = user name, value = prioritized job list
    private HashMap<String,List<Job>>    _userToJobsMap = new HashMap<>();
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public PrioritizedJobs(ITenantStrategy tenantStrategy,
                           IUserStrategy   userStrategy,
                           IJobStrategy    jobStrategy,
                           List<Job>       unprioritizedJobs)
    {
        // Check input.
        if (tenantStrategy == null    ||
            userStrategy == null      || 
            jobStrategy  == null      ||
            unprioritizedJobs == null ||
            unprioritizedJobs.isEmpty())
        {
            String msg = "Invalid initialization parameter for job prioritization.";
            _log.error(msg);
            throw new RuntimeException(msg);
        }
        
        // Initialize input fields.
        _tenantStrategy    = tenantStrategy;
        _userStrategy      = userStrategy;
        _jobStrategy       = jobStrategy;
        _unprioritizedJobs = unprioritizedJobs;
        
        // Prioritize the jobs according to the supplied strategies.
        prioritize();
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* iterator:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Create a new iterator that will returns jobs in priority order. */
    @Override
    public Iterator<Job> iterator()
    {
        return new JobIterator();
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* prioritize:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Prioritization means populating the tenants list and related maps. This
     * method calls the strategy object methods to order tenant, user and job
     * lists.  We assume that a non-empty list of each type will exist after
     * this method executes since the unprioritizedJobs list is non-empty.
     */
    private void prioritize()
    {
        // Get the tenants list in priority order.
        _tenants = _tenantStrategy.prioritizeTenants(_unprioritizedJobs);
        
        // Populate the tenant-to-users map.
        for (String tenantId : _tenants) {
            List<String> users = _userStrategy.prioritizeUsers(tenantId, _unprioritizedJobs);
            _tenantToUsersMap.put(tenantId, users);
            
            // Populate the user-to-jobs map.
            for (String user : users) {
                List<Job> jobs = _jobStrategy.prioritizeJobs(tenantId, user, _unprioritizedJobs);
                _userToJobsMap.put(user, jobs);
            }
        }
    }
    
    /* ********************************************************************** */
    /*                            JobIterator Class                           */
    /* ********************************************************************** */
    private class JobIterator
     implements Iterator<Job>
    {
        // ------------------ Fields.
        private ListIterator<String> _itTenants;
        private ListIterator<String> _itUsers;
        private ListIterator<Job>    _itJobs;
        
        // ------------------ Constructor
        private JobIterator()
        {
            // Get the first user for the first tenant.
            _itTenants = _tenants.listIterator();
            if (_itTenants.hasNext()) {
                List<String> users = _tenantToUsersMap.get(_itTenants.next());
                _itUsers = users.listIterator();
            }
            
            // Sanity check.
            if (_itUsers == null) {
                // Something is very wrong here.
                String msg = "Unable to get initial user iterator.";
                _log.error(msg);
                clearState();
                return;
            }
            
            // Get the iterator for the first job for the first user.
            if (_itUsers.hasNext()) {
                List<Job> jobs = _userToJobsMap.get(_itUsers.next());
                _itJobs = jobs.listIterator();
            }
            
            // Sanity check.
            if (_itJobs == null) {
                // Something is very wrong here.
                String msg = "Unable to get initial job iterator.";
                _log.error(msg);
                clearState();
                return;
            }
        }

        // ------------------ Public Methods
        @Override
        public boolean hasNext()
        {
            // Is this iterator used up?
            if (isDone()) return false;
            
            // See if the current job iterator still has jobs.
            if (_itJobs.hasNext()) return true;
            
            // Get a new iterator.
            refreshItJobs();
            if (_itJobs == null) return false;
             else return _itJobs.hasNext();
        }

        @Override
        public Job next()
        {
            // Is this iterator used up?
            if (isDone()) {
                String msg = "No more jobs, use hasNext() before calling next().";
                _log.error(msg);
                throw new NoSuchElementException(msg);
            }
            
            // Be forgiving and attempt to use an new iterator 
            // if the current jobs iterator is exhausted.  This
            // will only happen if hasNext() was called first.
            try {return _itJobs.next();}
                catch (NoSuchElementException e) {
                    // We try again with a refreshed iterator.
                    if (hasNext()) return _itJobs.next();
                      else throw e;
                }
        }

        @Override
        public void remove()
        {
            String msg = "JobIterator does not implement the remove method.";
            throw new UnsupportedOperationException(msg);
        }
        
        // ------------------ Public Methods
        /** Advance to the next user's job iterator.  The the tenant
         * has no more users, then advance to the next tenant.  If there
         * are no more tenants, were done. 
         */
        private void refreshItJobs()
        {
            // Discard the current iterator.
            _itJobs = null;
            
            // Try to get the next user for the current tenant.
            String user = null;
            if (_itUsers.hasNext()) {
                user = _itUsers.next();
            }
            else {
                // Try to get the next user for the next tenant. 
                refreshItUsers();
                if (_itUsers != null) user = _itUsers.next();
            }
            
            // There's nothing to do if we didn't obtain a user.
            if (user == null) return;
            
            // Assign an iterator for the new user's job list.
            // If the user-to-job mapping returns null, the
            // jobs iterator remains null.
            List<Job> jobs = _userToJobsMap.get(user);
            if (jobs != null) _itJobs = jobs.listIterator();
        }
        
        /** Advance to the tenant's users.  If there is no next
         * tenant, then this iterator is put into the done state.
         */
        private void refreshItUsers()
        {
            // Discard the current iterator.
            _itUsers = null;
            
            // Determine if there is a next tenant.
            if (!_itTenants.hasNext()) {
                // This iterator is done.
                clearState();
                return;
            }
            
            // Get the users iterator for the new tenant.
            String tenant = _itTenants.next();
            List<String> users = _tenantToUsersMap.get(tenant);
            if (users != null) _itUsers = users.listIterator();
        }
        
        /** Set this object to the done state by nulling out
         * all iterator fields. 
         */
        private void clearState() 
        {
            // Wipe out the state data.
            _itTenants = null;
            _itUsers   = null;
            _itJobs    = null;
        }
        
        /** Detect whether we are in a done state.  A done state
         * means that no more jobs will be delivered by this iterator.
         * 
         * @return true if the iterator is done, false otherwise.
         */
        private boolean isDone()
        {
            // We're done when there are no more jobs.
            return _itJobs == null;
        }
    }
}
