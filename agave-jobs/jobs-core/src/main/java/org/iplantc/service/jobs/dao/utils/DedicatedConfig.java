package org.iplantc.service.jobs.dao.utils;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;

/** This singleton class provides the dedicated tenant, system and user configuration
 * information for an instance of the jobs service.  All requests for that information 
 * are funneled through this class, which allows it to be initialized with different
 * data for testing.  
 * 
 * JobDao methods access the singleton instance of this class when to filter
 * the results of various queries.  In production, the default configuration provider 
 * retrieves this information from settings files installed on the server.  In 
 * test environments, this class's singleton instance can be initialized with
 * any provider that meets testing needs.
 * 
 * @author rcardone
 */
public final class DedicatedConfig
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Singleton instance.
    private static DedicatedConfig   _instance;
    
    // The actual dedicated configuration provider.
    private final IDedicatedProvider _dedicatedProvider;

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getInstance:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This method returns the singleton instance of this class, initializing
     * it with the production configuration provider if the singleton has not
     * yet been created.  JobDao methods only use this method to access the 
     * singleton.
     * 
     * @return the singleton instance of this class
     */
    public static synchronized DedicatedConfig getInstance()
    {
        // Initialize the default production provider.
        if (_instance == null) {
            _instance = new DedicatedConfig();
        }
        return _instance;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getInstance:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This method returns the singleton instance of this class, initializing
     * it with the specified provider if the singleton has not yet been created.  
     * If test programs call this method before any JobDao methods execute, then
     * JobDao will execute with the configuration parameters returned from the
     * specified provider. 
     * 
     * @param the non-null custion provider used for testing
     * @return the singleton instance of this class
     */
    public static synchronized DedicatedConfig getInstance(IDedicatedProvider provider)
    {
        // The provider must not be null.
        if (_instance == null) {
            _instance = new DedicatedConfig(provider);
        }
        return _instance;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getDedicatedProvider:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Get the provider assigned to the singleton instance of this class.
     * 
     * @return the dedicated configuration provider used to construct this class
     */
    public IDedicatedProvider getDedicatedProvider(){return _dedicatedProvider;}
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** The production constructor initialized with the default configuration
     * provider.  This provider retrieves configuration information from server
     * settings files.
     */
    private DedicatedConfig()
    {
        _dedicatedProvider = new DefaultDedicatedConfig();
    }
    
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This constructor is used only for testing.  It allows the caller to 
     * specify a custom provider that can dispense any configuration data needed
     * for testing.
     * 
     * @param provider a custom, non-null configuration provider used for testing only
     */
    private DedicatedConfig(IDedicatedProvider provider)
    {
        if (provider == null) 
            throw new NullPointerException("Null dedicated provider received.");
        _dedicatedProvider = provider;
    }
    
    /* ********************************************************************** */
    /*                     DefaultDedicatedConfig Class                       */
    /* ********************************************************************** */
    /** This is the default production implementation of the dedicated provider
     * interface.  Once the singleton of the enclosing class is initialized with
     * an instance of this class, all JobDao methods that reference the 
     * dedicated configuration will retrieve that data from here. 
     */
    private static final class DefaultDedicatedConfig
     implements IDedicatedProvider
    {
        // Limit construction to this file.
        private DefaultDedicatedConfig(){}
        
        @Override
        public String getDedicatedTenantIdForThisService()
        {
            return TenancyHelper.getDedicatedTenantIdForThisService();
        }

        @Override
        public String[] getDedicatedUsernamesFromServiceProperties()
        {
            return Settings.getDedicatedUsernamesFromServiceProperties();
        }

        @Override
        public String[] getDedicatedSystemIdsFromServiceProperties()
        {
            return Settings.getDedicatedSystemIdsFromServiceProperties();
        }
    }
}
