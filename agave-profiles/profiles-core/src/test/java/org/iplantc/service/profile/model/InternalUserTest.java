package org.iplantc.service.profile.model;

import java.util.Arrays;

import org.iplantc.service.profile.ModelTestCommon;
import org.iplantc.service.profile.TestDataHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests deserialization from JSON into an InternalUser object
 * 
 * @author dooley
 *
 */
@Test(groups={"unit"})
public class InternalUserTest extends ModelTestCommon{

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = dataHelper.getTestDataObject(TestDataHelper.TEST_INTERNAL_USER_FILE);
	}

    @DataProvider(name = "usernameProvider")
    public Object[][] usernameProvider() 
    {
    	return new Object[][] {
    			{ "username", null, "username cannot be null", true },
    			{ "username", "", "username cannot be empty", true },
    			{ "username", new Object(), "username cannot be object", true },
    			{ "username", Arrays.asList("Harry"), "username cannot be array", true },
    			{ "username", "test name", "username cannot contain spaces", true },
    			{ "username", "test~name", "username cannot contain ~ characters", true },
    			{ "username", "test`name", "username cannot contain ` characters", true },
    			{ "username", "test!name", "username cannot contain ! characters", true },
    			{ "username", "test@name", "username cannot contain @ characters", true },
    			{ "username", "test#name", "username cannot contain # characters", true },
    			{ "username", "test$name", "username cannot contain $ characters", true },
    			{ "username", "test%name", "username cannot contain % characters", true },
    			{ "username", "test^name", "username cannot contain ^ characters", true },
    			{ "username", "test&name", "username cannot contain & characters", true },
    			{ "username", "test*name", "username cannot contain * characters", true },
    			{ "username", "test(name", "username cannot contain ( characters", true },
    			{ "username", "test)name", "username cannot contain ) characters", true },
    			{ "username", "test_name", "username cannot contain _ characters", true },
    			{ "username", "test+name", "username cannot contain + characters", true },
    			{ "username", "test=name", "username cannot contain = characters", true },
    			{ "username", "test{name", "username cannot contain { characters", true },
    			{ "username", "test}name", "username cannot contain } characters", true },
    			{ "username", "test|name", "username cannot contain | characters", true },
    			{ "username", "test\\name", "username cannot contain \\ characters", true },
    			{ "username", "test\nname", "username cannot contain carrage return characters", true },
    			{ "username", "test\tname", "username cannot contain tab characters", true },
    			{ "username", "test:name", "username cannot contain : characters", true },
    			{ "username", "test;name", "username cannot contain ; characters", true },
    			{ "username", "test'name", "username cannot contain ' characters", true },
    			{ "username", "test\"name", "username cannot contain \" characters", true },
    			{ "username", "test,name", "username cannot contain , characters", true },
    			{ "username", "test?name", "username cannot contain ? characters", true },
    			{ "username", "test/name", "username cannot contain / characters", true },
    			{ "username", "test<name", "username cannot contain < characters", true },
    			{ "username", "test>name", "username cannot contain > characters", true },
    			{ "username", "test-name", "username can contain dashes", false },
    			{ "username", "test.name", "username cannot contain periods", false },
    			{ "username", "testname", "username cannot contain all chars", false },
    			{ "username", "1234567890", "username cannot contain all numbers", false },
    			{ "username", "test0name", "username cannot contain alpha", false },
    			
    	};
    }

    @Test (groups={"model","internaluser"}, dataProvider="usernameProvider")
    public void storageSystemIdValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
        super.commonInternalUserFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "internalUserAttributes")
    public Object[][] internalUserAttributes() 
    {
    	return new Object[][] {
    			{ "email", null, "email cannot be null", true },
    			{ "email", "", "email cannot be empty", true },
    			{ "email", new Object(), "email cannot be object", true },
    			{ "email", Arrays.asList("Harry"), "email cannot be array", true },
    			{ "email", "test name", "email can be an invalid email address", true },
    			{ "email", "joe@example.com", "email can be a valid email address", false },

    			{ "firstName", null, "firstName can be null", false },
    			{ "firstName", "", "firstName cannot be empty", true },
    			{ "firstName", new Object(), "firstName cannot be object", true },
    			{ "firstName", Arrays.asList("Harry"), "firstName cannot be array", true },
    			{ "firstName", "test site", "firstName can be a valid string", false },
    			
    			{ "lastName", null, "lastName can be null", false },
    			{ "lastName", "", "lastName cannot be empty", true },
    			{ "lastName", new Object(), "lastName cannot be object", true },
    			{ "lastName", Arrays.asList("Harry"), "lastName cannot be array", true },
    			{ "lastName", "test site", "lastName can be a valid string", false },
    			
    			{ "position", null, "position can be null", false },
    			{ "position", "", "position cannot be empty", true },
    			{ "position", new Object(), "position cannot be object", true },
    			{ "position", Arrays.asList("Harry"), "position cannot be array", true },
    			{ "position", "test site", "position can be a valid string", false },
    			
    			{ "institution", null, "institution can be null", false },
    			{ "institution", "", "institution cannot be empty", true },
    			{ "institution", new Object(), "institution cannot be object", true },
    			{ "institution", Arrays.asList("Harry"), "institution cannot be array", true },
    			{ "institution", "test site", "institution can be a valid string", false },
    			
    			{ "phone", null, "phone can be null", false },
    			{ "phone", "", "phone cannot be empty", true },
    			{ "phone", new Object(), "phone cannot be object", true },
    			{ "phone", Arrays.asList("Harry"), "phone cannot be array", true },
    			{ "phone", "test site", "phone cannot be an invalid phone format", true },
    			{ "phone", "123456789", "phone cannot be an invalid phone format", true },
    			{ "phone", "123-456-7890", "phone can be a valid phone format", false },
    			
    			{ "fax", null, "fax can be null", false },
    			{ "fax", "", "fax cannot be empty", true },
    			{ "fax", new Object(), "fax cannot be object", true },
    			{ "fax", Arrays.asList("Harry"), "fax cannot be array", true },
    			{ "fax", "test site", "fax cannot be an invalid fax format", true },
    			{ "fax", "123456789", "fax cannot be an invalid fax format", true },
    			{ "fax", "1234567890", "fax can be a valid fax format", false },
    			{ "fax", "123 4567890", "fax can be a valid fax format", false },
    			{ "fax", "123456 7890", "fax can be a valid fax format", false },
    			{ "fax", "123 456 7890", "fax can be a valid fax format", false },
    			{ "fax", "123 456-7890", "fax can be a valid fax format", false },
    			{ "fax", "123-456 7890", "fax can be a valid fax format", false },
    			{ "fax", "123-456-7890", "fax can be a valid fax format", false },
    			{ "fax", "(123) 456-7890", "fax can be a valid fax format", false },
    			{ "fax", "(123) 456 7890", "fax can be a valid fax format", false },
    			{ "fax", "(123) 4567890", "fax can be a valid fax format", false },
    			{ "fax", "(123)4567890", "fax can be a valid fax format", false },
    			{ "fax", "(123)456 7890", "fax can be a valid fax format", false },
    			{ "fax", "(123)-456-7890", "fax can be a valid fax format", false },
    			
    			{ "fax", "123 456 7890", "fax can be a valid fax format", false },
    			
    			{ "researchArea", null, "researchArea can be null", false },
    			{ "researchArea", "", "researchArea cannot be empty", true },
    			{ "researchArea", new Object(), "researchArea cannot be object", true },
    			{ "researchArea", Arrays.asList("Harry"), "researchArea cannot be array", true },
    			{ "researchArea", "test site", "researchArea can be a valid string", false },
    			
    			{ "department", null, "department can be null", false },
    			{ "department", "", "department cannot be empty", true },
    			{ "department", new Object(), "department cannot be object", true },
    			{ "department", Arrays.asList("Harry"), "department cannot be array", true },
    			{ "department", "test site", "department can be a valid string", false },
    			
    			{ "city", null, "city can be null", false },
    			{ "city", "", "city cannot be empty", true },
    			{ "city", new Object(), "city cannot be object", true },
    			{ "city", Arrays.asList("Harry"), "city cannot be array", true },
    			{ "city", "test site", "city can be a valid string", false },
    			
    			{ "state", null, "state can be null", false },
    			{ "state", "", "state cannot be empty", true },
    			{ "state", new Object(), "state cannot be object", true },
    			{ "state", Arrays.asList("Harry"), "state cannot be array", true },
    			{ "state", "test site", "state can be a valid string", false },
    			
    			{ "country", null, "country can be null", false },
    			{ "country", "", "country cannot be empty", true },
    			{ "country", new Object(), "country cannot be object", true },
    			{ "country", Arrays.asList("Harry"), "country cannot be array", true },
    			{ "country", "test site", "country can be a valid string", false },
    			
//    			{ "fundingAgencies", null, "fundingAgencies can be null", false },
//    			{ "fundingAgencies", "", "fundingAgencies cannot be empty", true },
//    			{ "fundingAgencies", new Object(), "fundingAgencies cannot be object", true },
//    			{ "fundingAgencies", "test site", "fundingAgencies cannot be a string", true },
//    			
    			{ "gender", null, "gender can be null", false },
    			{ "gender", "", "gender cannot be empty", true },
    			{ "gender", new Object(), "gender cannot be object", true },
    			{ "gender", Arrays.asList("Harry"), "gender cannot be array", true },
    			{ "gender", "type", "gender cannot be an invalid system type", true },
    			{ "gender", "FEMALE", "gender can be FEMALE", false },
    			{ "gender", "MALE", "gender can be MALE", false },
    			{ "gender", "male", "type is case insensitive", false },
    			
    	};
    }

    @Test (groups={"model","internaluser"}, dataProvider="internalUserAttributes")
    public void storageSystemNameValidationTest(String name, Object changeValue, String message, boolean exceptionThrown)
    throws Exception 
    {
        super.commonInternalUserFromJSON(name,changeValue,message,exceptionThrown);
    }
}
