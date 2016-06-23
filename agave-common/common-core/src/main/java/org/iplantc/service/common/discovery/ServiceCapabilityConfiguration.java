/**
 * 
 */
package org.iplantc.service.common.discovery;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.discovery.providers.sql.DiscoverableService;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads and maintains the capabilities of a worker or API. Configurations
 * are read first from disk, and second from a <code>CONFIGURATIONS</code> environment
 * variable.
 * 
 * @author dooley
 *
 */
public class ServiceCapabilityConfiguration
{
	private Logger log = Logger.getLogger(ServiceCapabilityConfiguration.class);
	
	private Set<ServiceCapability> localCapabilities = new HashSet<ServiceCapability>();
	
	private static ServiceCapabilityConfiguration serviceDiscoveryConfiguration;
	
	public static ServiceCapabilityConfiguration getInstance() {
		if (serviceDiscoveryConfiguration == null) {
			serviceDiscoveryConfiguration = new ServiceCapabilityConfiguration();
			serviceDiscoveryConfiguration.loadLocalConfiguration();
		}
		
		return serviceDiscoveryConfiguration;
	}
	
	private ServiceCapabilityConfiguration() {}
	
	private void loadLocalConfiguration() 
	{
		readCapabilitiesFromDisk();
		readCapabilitiesFromEnvironment();
	}
	
	/**
	 * Reads in a JSON Array of serialized {@link DiscoverableService}s from a file named
	 * capabilities.json. If that is not found, it looks for a capabilities.txt file
	 * which contains  
	 */
	private void readCapabilitiesFromDisk()
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json;
		try 
		{
			// read in json list of capabilities
			json = mapper.readTree(ServiceDiscoveryFactory.class.getClassLoader().getResourceAsStream("capabilities.json"));
			if (json != null)
			{
				// is there an array of json capabilities?
				if (json.isArray()) 
				{
					JsonNode jsonService = null;
					for (Iterator<JsonNode> iter = json.iterator(); iter.hasNext(); jsonService = iter.next()) {
						getLocalCapabilities().add(mapper.treeToValue(jsonService, ServiceCapability.class));
					}	
				} 
				else // nope, just one 
				{
					getLocalCapabilities().add(mapper.treeToValue(json, ServiceCapability.class));
				}
			}
			else // no json capability file, so look for a txt file 
			{
				InputStream in = null;
				try {
					
					in = ServiceDiscoveryFactory.class.getClassLoader().getResourceAsStream("capabilities.txt");
					if (in != null) {
						List<String> lines = IOUtils.readLines(in, "UTF-8");
						for (String line: lines) {
							line = StringUtils.trim(line);
							if (StringUtils.isNotEmpty(line) && !StringUtils.startsWith(line, "#")) {
								getLocalCapabilities().add(ServiceCapabilityFactory.getInstance(line));
							}
						}
					}
				}
				finally {
					try { in.close(); } catch (Exception e) {}
				}
			} 
		} catch (Exception e) {
			log.error("Failed to read service discovery file from disk. Checking environment...");
		}
	}
	
	/**
	 * Reads in a JSON Array of serialized {@link DiscoverableService}s from a file named
	 * capabilities.json 
	 */
	private void readCapabilitiesFromEnvironment() {
		Map<String, String> env = System.getenv();
		for(String key: env.keySet()) {
			if (key.toLowerCase().equals("capabilities")) {
				for(String capability: env.get(key).split(",")) 
				{
					getLocalCapabilities().add(ServiceCapabilityFactory.getInstance(capability));
				}
			}
		}
	}

	/**
	 * @return the localCapabilities
	 */
	public Set<ServiceCapability> getLocalCapabilities()
	{
		return localCapabilities;
	}

	/**
	 * @param localCapabilities the localCapabilities to set
	 */
	public void setLocalCapabilities(Set<ServiceCapability> localCapabilities)
	{
		this.localCapabilities = localCapabilities;
	}
}
