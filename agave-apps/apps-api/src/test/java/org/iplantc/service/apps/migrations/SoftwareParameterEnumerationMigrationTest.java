package org.iplantc.service.apps.migrations;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.SoftwareParameterEnumeratedValue;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This is a helper test that serves as a migration tool and validator 
 * for {@link SoftwareParameter} records of type {@link SoftwareParameterType#enumeration}.
 * Prior to the 2.1.2 release, enumerated values were stored as serialized
 * JSON arrays in the validator field. Now they are stored as 
 * {@link SoftwareParameterEnumeratedValue} objects with a proper one-to-many
 * relationship to {@link SoftwareParameter}.
 * 
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class SoftwareParameterEnumerationMigrationTest 
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(SoftwareParameterEnumerationMigrationTest.class);
	
	@BeforeClass
	public void beforeClass() {
		Assert.assertEquals(Settings.IPLANT_APPS_SERVICE, Settings.IPLANT_APPS_SERVICE);
	}
	
	/**
	 * Verifies that all {@link Software} records serialize properly.
	 * 
	 * @throws Exception
	 */
	@Test
    public void softwareToJsonTest() throws Exception
    {
		for (Software software: SoftwareDao.getAll())
		{
			try { 
				software.toJSON();
			} catch (Exception e) {
				System.out.println("Unable to serialized " + software.getUniqueName() + " to json");
			}
		}
    }
	
	/**
	 * Migrates {@link SoftwareParameter} records from storing enumerated values
	 * in the {@link SoftwareParameter#validator} field to storing them as a 
	 * {@link java.util.Collection} of {@link SoftwareParameterEnumeratedValue} objects.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
//	@Test
    public void parameterEnumeratedValidMigrationTest() throws Exception
    {	
		ObjectMapper mapper = new ObjectMapper();
		Session session = null;
    	try {
    		HibernateUtil.beginTransaction();
    		session = HibernateUtil.getSession();
    		
    		List<Long> ids = session.createQuery("select s.id from Software s join s.parameters p where p.type = :type and s.name like :name")
    			.setString("type", "enumeration")
    			.setString("name", "%")
    			.list();
    		HibernateUtil.commitTransaction();
    		
    		JsonNode enumeratedValuesArray = null;
    		for (Long softwareId: ids) 
    		{
    			Software software = SoftwareDao.get(softwareId);
    			
    			boolean changed = false;
    			
	    		for(SoftwareParameter param: software.getParameters()) 
	    		{
	    			if (param.getType() == SoftwareParameterType.enumeration) 
	    			{
	    				if (StringUtils.isEmpty(param.getValidator())) {
	    					System.out.println("Null validator for " + software.getUniqueName() + " parameter " + param.getKey());
	    					continue;
	    				}
	    				
	    				changed = true;
		    			
	    				enumeratedValuesArray = mapper.readTree(param.getValidator());
		    			if (enumeratedValuesArray.isArray()) 
		    			{	
		    				for(Iterator<JsonNode> iter = ((ArrayNode)enumeratedValuesArray).iterator(); iter.hasNext();) {
		    					JsonNode child = iter.next();
		    					if (child.isObject()) {
		    						String value = child.fieldNames().next();
		    						String label = child.get(value).asText();
		    						param.addEnumValue(new SoftwareParameterEnumeratedValue(value, label, param));
		    					} else {
		    						param.addEnumValue(new SoftwareParameterEnumeratedValue(child.asText(), child.asText(), param));
		    					}
		    				}
		    				
		    				System.out.println(String.format("Updating %s parameter %s:\n\tfrom: %s\n\tto: %s", 
		    						software.getUniqueName(),
		    						param.getKey(),
		    						param.getValidator(),
		    						mapper.valueToTree(param.getEnumValues()).toString()));
		    				
		    				param.setValidator(null);
		    				System.out.println("Saving migrated enumerated values " + 
			    					param.getKey());
		    			}
	    			}
	    		}
	    		
	    		if (changed) 
	    		{
		    		try {
		    			software.setLastUpdated(new Date());
		    			software.toJSON();
		    			System.out.println("Saving migrated enumerated values " + 
		    					software.getUniqueName());
		    			SoftwareDao.persist(software);
		    		} catch (Exception e) {
		    			Assert.fail("Unable to serialize software " + software.getUniqueName() + 
		    					" to json after parameter migration", e);
		    		}
	    		}
    		}
    	}
    	catch (Throwable e) {
    		try {
    			HibernateUtil.rollbackTransaction();
    		}
    		catch (Exception e1) {
    			throw e1;
    		}
    		throw e;
    	}
    	finally {
    		try {session.close(); } catch (Exception e) {}
    	}
    }
}
