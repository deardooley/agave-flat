package org.iplantc.service.apps.model;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.util.ServiceUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 3/30/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
@Test(groups={"unit"})
public class GSoftwareInputTest extends GModelTestCommon {
    SoftwareInput input = new SoftwareInput();

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
    }

    @DataProvider(name = "dataTestInputs")
    public Object[][] createData1() {
        Object[][] mydata = jtd.dataTestInputs;
        return mydata;
    }

    @DataProvider(name = "dataTestInputsValue")
    public Object[][] createData2() {
        Object[][] mydata = jtd.dataTestInputsValue;
        return mydata;
    }

    @DataProvider(name = "dataTestInputsDetails")
    public Object[][] createData3() {
        Object[][] mydata = jtd.dataTestInputsDetails;
        return mydata;
    }

    @DataProvider(name = "dataTestInputsSemantics")
    public Object[][] createData4() {
        Object[][] mydata = jtd.dataTestInputsSemantics;
        return mydata;
    }

    /**
     * Tests the validation of the top level element of SoftwareInputs
     * @param name              the json label to test
     * @param changeValue       set the json label to this value for test
     * @param message           the message associated with this test data
     * @param exceptionThrown   boolean to indicate if an exception should result
     * @throws Exception        can be a JSONException or SoftwareException
     */
    @Test (groups="model", dataProvider="dataTestInputs")
    public void commonSoftwareInputFromJSON(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.put(name,changeValue);
        super.commonSoftwareFromJSON(input,jsonTree,name,changeValue,message,exceptionThrown);
    }

    /**
     * Tests the validation of the value element of SoftwareInputs
     * @param name              the json label to test
     * @param changeValue       set the json label to this value for test
     * @param message           the message associated with this test data
     * @param exceptionThrown   boolean to indicate if an exception should result
     * @throws Exception        can be a JSONException or SoftwareException
     */
    @Test (groups="model", dataProvider="dataTestInputsValue")
    public void inputsValueFromJSON(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("value").put(name,changeValue);
        jsonTree.getJSONObject("semantics").put("minCardinality", StringUtils.equals(name, "required") || StringUtils.equals(name, "visible") ? 1 : 0);
        
        super.commonSoftwareFromJSON(input,jsonTree,name,changeValue,message,exceptionThrown);
    }

    /**
     * Tests the validation of the details element of SoftwareInputs
     * @param name              the json label to test
     * @param changeValue       set the json label to this value for test
     * @param message           the message associated with this test data
     * @param exceptionThrown   boolean to indicate if an exception should result
     * @throws Exception        can be a JSONException or SoftwareException
     */
    @Test (groups="model", dataProvider="dataTestInputsDetails")
    public void inputsDetailsFromJSON(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("details").put(name,changeValue);
        super.commonSoftwareFromJSON(input,jsonTree,name,changeValue,message,exceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestInputsSemantics")
    public void inputsSemanticsFromJSON(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("semantics").put(name,changeValue);
        jsonTree.getJSONObject("value").put("required",false);
        jsonTree.getJSONObject("value").put("visible",true);
        super.commonSoftwareFromJSON(input,jsonTree,name,changeValue,message,exceptionThrown);
    }

    @DataProvider(name="inputsTypeEnforcedOnDefaultValueProvider")
    public Object[][] inputsTypeEnforcedOnDefaultValueProvider() throws JSONException
    {
    	Object positiveInteger = new Integer(5);
    	Object negativeInteger = new Integer(5);
    	Object positiveDecimal = new Float(5.5);
    	Object negativeDecimal = new Float(-5.5);
    	Object nonEmptyString = "hello";
    	Object emptyString = "";
    	Object jsonObject = new JSONObject();
    	Object emptyArray = new JSONArray();
    	Object objectArray = new JSONArray(Arrays.asList(jsonObject));
    	Object stringArray = new JSONArray(Arrays.asList("hello"));
    	Object trueValue = Boolean.TRUE;
    	Object falseValue = Boolean.FALSE;
    	
    	return new Object[][] {
    			{ positiveInteger, "Setting input.value.default to positive integer should not be allowed", true },
    			{ negativeInteger, "Setting input.value.default to negative integer should not be allowed", true },
    			{ jsonObject, "Setting input.value.default to object should not be allowed", true },
    			{ objectArray, "Setting input.value.default to array of objects should not be allowed", true },
    			{ trueValue, "Setting input.value.default to true should not be allowed", true },
    			{ falseValue, "Setting input.value.default to false should not be allowed", true },
    			{ positiveDecimal, "Setting input.value.default to positive decimal should not be allowed", true },
    			{ negativeDecimal, "Setting input.value.default to negative decimal should not be allowed", true },
    			{ stringArray, "Setting input.value.default to array of objects should not be allowed", false },
    			{ emptyArray, "Setting input.value.default to empty array should be allowed", false },
    			{ (String)null, "Setting input.value.default to null should be allowed", false },
    			{ emptyString, "Setting input.value.default to empty string should be allowed", false },
    			{ nonEmptyString, "Setting input.value.default to string should be allowed", false },
    	};
    }
    
    @Test (groups="model", dataProvider="inputsTypeEnforcedOnDefaultValueProvider")
    public void inputsTypeEnforcedOnDefaultValue(Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("value").put("validator",(String)null);
        jsonTree.getJSONObject("value").put("default", changeValue);
        super.commonSoftwareFromJSON(input, jsonTree, "default", changeValue, message, exceptionThrown);
    }
    
    @DataProvider(name="inputsValidatorEnforcedOnDefaultValueProvider")
    public Object[][] inputsValidatorEnforcedOnDefaultValueProvider() throws JSONException
    {
    	return new Object[][] {
    		{  ".*\\.pdf$", "something.png", "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", "something.pdf2", "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", "pdf", "Default value not matching regex should throw exception", true },
    		
    		{  (String)null, "", "Empty default value should pass matching regex", false },
    		{  "", "", "Empty default value should pass empty regex", false },
    		{  "\\s*", "", "Empty default value should pass matching regex", false },
    		{  ".*\\.pdf$", "", "Empty default value should pass matching regex", true },
    		{  ".*\\.pdf$", null, "Null default value should pass despite regex", false },
    		{  ".*\\.pdf$", "something.pdf", "Default value matching regex should pass", false },
    		{  ".*\\.pdf$", ".pdf", "Default value matching regex should pass", false },
    		{  ".*\\.pdf$", "something.png.pdf", "Default value matching regex should pass", false },
    	};
    }
   
    @Test (groups="model", dataProvider="inputsValidatorEnforcedOnDefaultValueProvider")
    public void inputsValidatorEnforcedOnDefaultValue(String validator, String defaultValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("value").put("validator",validator);
        jsonTree.getJSONObject("value").put("default", defaultValue);
        super.commonSoftwareFromJSON(input, jsonTree, "default", defaultValue, message, exceptionThrown);
    }
    
    @DataProvider(name="inputsValidatorEnforcedOnDefaultValueArrayProvider")
    public Object[][] inputsValidatorEnforcedOnDefaultValueArrayProvider() throws JSONException
    {
    	String emptyString = "";
    	String nullString = null;
    	
    	String validFileExtension = "pdf";
    	String badFileExtension = "png";
    	String basename = "something";
    	
    	String badFileExtenstion = basename + "." + badFileExtension;
    	String badFileExtensionSuffix = basename + "." + validFileExtension + "2";
    	String badFileExtensionPrefix = basename + ".df";
    	
    	String matchingFilename = basename + "." + validFileExtension;
    	String matchingExtension = "." + validFileExtension;
    	String matchingFilenameWithMultipleExtensions = badFileExtenstion + matchingExtension;
    			
    	
    	return new Object[][] {
    		// Single value arrays
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtenstion)), "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionSuffix)), "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionPrefix)), "Default value not matching regex should throw exception", true },
    		
    		{  emptyString, new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex matches", false },
    		{  "\\s*", new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex matches", false },
    		{  nullString, new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex is null", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(emptyString)), "Empty default value should throw exception when it does not match", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(nullString)), "Null default value should pass despite regex", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList("something.pdf")), "Default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(".pdf")), "Default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList("something.png.pdf")), "Default value matching regex should pass", false },
    		
    		// Mixed value arrays
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtenstion, badFileExtenstion)), "both values in default value array not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionSuffix, badFileExtensionSuffix)), "both values in default value array not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionPrefix, badFileExtensionPrefix)), "both values in default value array not matching regex should throw exception", true },
    		
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList("something.png", matchingFilename)), "Any entry in default value array not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList("something.pdf2", matchingFilename)), "Any entry in default value array not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList("pdf", matchingFilename)), "Any entry in default value array not matching regex should throw exception", true },
    		
    		{  emptyString, new JSONArray(Arrays.asList(emptyString, emptyString)), "all empty values in default value array should pass when regex matches", false },
    		{  "\\s*", new JSONArray(Arrays.asList(emptyString, emptyString)), "all empty values in default value array should pass when regex matches", false },
    		{  nullString, new JSONArray(Arrays.asList(emptyString, emptyString)), "all empty values in default value array should pass when regex is null", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(emptyString, emptyString)), "all empty values in default value array should throw exception when regex does not match", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(nullString, nullString)), "all null values in default value array should pass despite regex", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingFilename, matchingFilename)), "both values in default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingExtension, matchingExtension)), "both values in default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingFilenameWithMultipleExtensions, matchingFilenameWithMultipleExtensions)), "both values in default value matching regex should pass", false },
    	};
    }
   
    @Test (groups="model", dataProvider="inputsValidatorEnforcedOnDefaultValueArrayProvider")
    public void inputsValidatorEnforcedOnDefaultArrayValue(String validator, JSONArray defaultValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("value").put("validator",validator);
        jsonTree.getJSONObject("value").put("default", defaultValue);
        super.commonSoftwareFromJSON(input, jsonTree, "default", defaultValue, message, exceptionThrown);
    }
    
    @DataProvider(name="inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider")
    public Object[][] inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider() throws JSONException
    {
    	String emptyString = "";
    	String nullString = null;
    	
    	String foofile = "foo.pdf";
    	String barfile = "bar.pdf";
    	
    	return new Object[][] {
    		// Single value retains in array
			{  nullString, new String[]{}, "null value should be reduced to an empty default value array" },
			{  emptyString, new String[]{emptyString}, "single empty string should be reduced to a single empty string in the resulting default value array" },
			{  foofile, new String[]{foofile}, "single string should be reduced to a single string in the resulting default value array" },
			
			// Single value array retains in array
			{  new JSONArray(Arrays.asList(nullString)), new String[]{}, "null value array should be reduced to an empty default value array" },
			{  new JSONArray(Arrays.asList(emptyString)), new String[]{emptyString}, "single empty string array should be retained in the resulting default value array" },
			{  new JSONArray(Arrays.asList(foofile)), new String[]{foofile}, "single string array should be retained in the resulting default value array" },
			
			// Redundant values get filtered to single value
			{  new JSONArray(Arrays.asList(nullString, nullString)), new String[]{}, "multiple null strings should be reduced to an empty default value array" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), new String[]{emptyString}, "null strings should be removed from the default value array" },
    		{  new JSONArray(Arrays.asList(nullString, foofile)), new String[]{foofile}, "null strings should be removed and valid value retained in the default value array" },
    		{  new JSONArray(Arrays.asList(nullString, barfile)), new String[]{barfile}, "null strings should be removed and valid value retained in the default value array" },
    		{  new JSONArray(Arrays.asList(nullString, nullString, nullString)), new String[]{}, "multiple null strings should be reduced to an empty default value array" },
    		{  new JSONArray(Arrays.asList(nullString, nullString, emptyString)), new String[]{emptyString}, "null strings should be removed and valid value retained in the default value array" },
    		{  new JSONArray(Arrays.asList(nullString, nullString, foofile)), new String[]{foofile}, "null strings should be removed and valid value retained in the default value array" },
    		{  new JSONArray(Arrays.asList(nullString, nullString, barfile)), new String[]{barfile}, "null strings should be removed and valid value retained in the default value array" },
    		
    		
    		{  new JSONArray(Arrays.asList(emptyString, emptyString)), new String[]{emptyString}, "multiple empty strings should be reduced to a single empty string in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, foofile)), new String[]{emptyString, foofile}, "unique entries should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, barfile)), new String[]{emptyString, barfile}, "unique entries should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString)), new String[]{foofile, emptyString}, "unique entries should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(barfile, emptyString)), new String[]{barfile, emptyString}, "unique entries should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString, barfile)), new String[]{foofile, emptyString, barfile}, "unique entries should be retained in the default value array" },
    		
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, emptyString)), new String[]{emptyString}, "redundant empty strings should be reduced to a single empty string in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, foofile)), new String[]{emptyString, foofile}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, barfile)), new String[]{emptyString, barfile}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, foofile, emptyString)), new String[]{emptyString, foofile}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(emptyString, barfile, emptyString)), new String[]{emptyString, barfile}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString, emptyString)), new String[]{foofile, emptyString}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(barfile, emptyString, emptyString)), new String[]{barfile, emptyString}, "multiple empty strings should be removed and unique values should be retained in the default value array" },
    		
    		
    		{  new JSONArray(Arrays.asList(foofile, foofile)), new String[]{foofile}, "redundant strings should be reduced to a single string in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, foofile, foofile)), new String[]{foofile}, "redundant strings should be reduced to a single string in the default value array" },
    		{  new JSONArray(Arrays.asList(barfile, foofile, foofile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, barfile, foofile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, foofile, barfile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
    		
    		
    		{  new JSONArray(Arrays.asList(barfile, barfile)), new String[]{barfile}, "redundant strings should be reduced to a single string in the default value array" },
    		{  new JSONArray(Arrays.asList(barfile, barfile, barfile)), new String[]{barfile}, "redundant strings should be reduced to a single string in the default value array" },
    		{  new JSONArray(Arrays.asList(foofile, barfile, barfile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
        	{  new JSONArray(Arrays.asList(barfile, foofile, barfile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
    		{  new JSONArray(Arrays.asList(barfile, barfile, foofile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the default value array" },
    	};
    }
   
    @Test (groups="model", dataProvider="inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider")
    public void inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider(Object defaultValue, String[] expectedValues, String message) 
    throws Exception 
    {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("value").put("default", defaultValue);
        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
        
        String[] defaultValuesArray = ServiceUtils.getStringValuesFromJsonArray(input.getDefaultValueAsJsonArray(), false);
        
        Assert.assertEquals(expectedValues.length, defaultValuesArray.length, 
        		"The number of expected values after parsing the input did not match the actual values after parsing.");
        
        for(String expectedValue: expectedValues) 
        {
        	Assert.assertTrue(Arrays.asList(defaultValuesArray).contains(expectedValue), "expected value " + 
        			expectedValue + " was not found in the default value array after parsing the input");
        }
    }
    
    @DataProvider(name="inputDefaultValueArrayRetainsOrderingProvider")
    public Object[][] inputDefaultValueArrayRetainsOrderingProviderProvider() throws JSONException
    {
    	String emptyString = "";
    	
    	String foofile = "foo.pdf";
    	String barfile = "bar.pdf";
    	String scatfile = "scat.pdf";
    	
    	return new Object[][] {
    		{  new JSONArray(Arrays.asList(emptyString, emptyString)), new String[]{emptyString} },
    		{  new JSONArray(Arrays.asList(emptyString, foofile)), new String[]{emptyString, foofile} },
    		{  new JSONArray(Arrays.asList(emptyString, barfile)), new String[]{emptyString, barfile} },
    		{  new JSONArray(Arrays.asList(foofile, emptyString)), new String[]{foofile, emptyString} },
    		{  new JSONArray(Arrays.asList(barfile, emptyString)), new String[]{barfile, emptyString} },
    		
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, foofile)), new String[]{emptyString, foofile} },
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, barfile)), new String[]{emptyString, barfile} },
    		{  new JSONArray(Arrays.asList(emptyString, foofile, emptyString)), new String[]{emptyString, foofile} },
    		{  new JSONArray(Arrays.asList(emptyString, barfile, emptyString)), new String[]{emptyString, barfile} },
    		{  new JSONArray(Arrays.asList(foofile, emptyString, emptyString)), new String[]{foofile, emptyString} },
    		{  new JSONArray(Arrays.asList(barfile, emptyString, emptyString)), new String[]{barfile, emptyString} },
    		
    		{  new JSONArray(Arrays.asList(barfile, foofile, foofile)), new String[]{barfile, foofile} },
    		{  new JSONArray(Arrays.asList(foofile, barfile, foofile)), new String[]{foofile, barfile} },
    		{  new JSONArray(Arrays.asList(foofile, foofile, barfile)), new String[]{foofile, barfile} },
    		
    		{  new JSONArray(Arrays.asList(foofile, barfile, barfile)), new String[]{foofile, barfile} },
        	{  new JSONArray(Arrays.asList(barfile, foofile, barfile)), new String[]{barfile, foofile} },
    		{  new JSONArray(Arrays.asList(barfile, barfile, foofile)), new String[]{barfile, foofile} },
    		
    		{  new JSONArray(Arrays.asList(foofile, barfile, scatfile)), new String[]{foofile, barfile, scatfile} },
    		{  new JSONArray(Arrays.asList(foofile, scatfile, barfile)), new String[]{foofile, scatfile, barfile} },
    		{  new JSONArray(Arrays.asList(barfile, scatfile, foofile)), new String[]{barfile, scatfile, foofile} },
    		{  new JSONArray(Arrays.asList(barfile, foofile, scatfile)), new String[]{barfile, foofile, scatfile} },
    		{  new JSONArray(Arrays.asList(scatfile, barfile, foofile)), new String[]{scatfile, barfile, foofile} },
    		{  new JSONArray(Arrays.asList(scatfile, foofile, barfile)), new String[]{scatfile, foofile, barfile} },
    	};
    }
    
    @Test (groups="model", dataProvider="inputDefaultValueArrayRetainsOrderingProvider")
    public void inputDefaultValueArrayRetainsOrdering(JSONArray defaultValue, String[] expectedValues) 
    throws Exception 
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
	        jsonTree.getJSONObject("value").put("default", defaultValue);
	        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
	        
	        String[] defaultValuesArray = ServiceUtils.getStringValuesFromJsonArray(input.getDefaultValueAsJsonArray(), false);
	        
	        for(int i=0; i<expectedValues.length; i++) 
	        {
	        	Assert.assertEquals(defaultValuesArray[i], expectedValues[i], "order was not preserved " +  
	        			defaultValuesArray[i] + " was expected, but " + expectedValues[i] + " was found.");
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		Assert.fail(e.getMessage());
    	}
    }
    
    @DataProvider(name="inputsRequiredAndVisibleRespectCardinalityProvider")
    public Object[][] inputsRequiredAndVisibleRespectCardinalityProvider() throws JSONException
    {
    	return new Object[][] {
			// { required, visible, minCardinality, shouldThrowException, message }
			{ true, true, 0, true, "required inputs must have minCardinality > 0"},
			{ true, false, 0, true, "hidden inputs must have minCardinality > 0"},
			{ false, true, 0, false, "visible, optional inputs can have minCardinality = 0"},
			{ false, false, 0, true, "hidden inputs must have minCardinality > 0"}
    	};
    }
    
    @Test (groups="model", dataProvider="inputsRequiredAndVisibleRespectCardinalityProvider")
    public void inputRequiredAndVisibleRespectCardinality(boolean required, boolean visible, int minCardinality, boolean shouldThrowException, String message)
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
	        jsonTree.getJSONObject("value").put("required", required);
	        jsonTree.getJSONObject("value").put("visible", visible);
	        jsonTree.getJSONObject("semantics").put("minCardinality", minCardinality);
	        jsonTree.getJSONObject("semantics").put("maxCardinality", -1);
	        jsonTree.getJSONObject("value").put("default", "foo");
	        Assert.assertNotNull(SoftwareInput.fromJSON(jsonTree), "Null input returned from SoftwareInput.fromJSON");
	        
    	} catch (Exception e) {
    		Assert.assertTrue(shouldThrowException, message);
    	}
    }
    
    
    @DataProvider(name="inputOntologyArrayRetainsOrderingProvider")
    public Object[][] inputOntologyArrayRetainsOrderingProvider() throws JSONException
    {
    	String foofile = "http://sswap.org/foo.xml";
    	String barfile = "http://sswap.org/bar.xml";
    	String scatfile = "http://sswap.org/scat.xml";
    	
    	return new Object[][] {
    		{  new JSONArray(Arrays.asList(barfile, foofile, foofile)), new String[]{barfile, foofile} },
    		{  new JSONArray(Arrays.asList(foofile, barfile, foofile)), new String[]{foofile, barfile} },
    		{  new JSONArray(Arrays.asList(foofile, foofile, barfile)), new String[]{foofile, barfile} },
    		
    		{  new JSONArray(Arrays.asList(foofile, barfile, barfile)), new String[]{foofile, barfile} },
        	{  new JSONArray(Arrays.asList(barfile, foofile, barfile)), new String[]{barfile, foofile} },
    		{  new JSONArray(Arrays.asList(barfile, barfile, foofile)), new String[]{barfile, foofile} },
    		
    		{  new JSONArray(Arrays.asList(foofile, barfile, scatfile)), new String[]{foofile, barfile, scatfile} },
    		{  new JSONArray(Arrays.asList(foofile, scatfile, barfile)), new String[]{foofile, scatfile, barfile} },
    		{  new JSONArray(Arrays.asList(barfile, scatfile, foofile)), new String[]{barfile, scatfile, foofile} },
    		{  new JSONArray(Arrays.asList(barfile, foofile, scatfile)), new String[]{barfile, foofile, scatfile} },
    		{  new JSONArray(Arrays.asList(scatfile, barfile, foofile)), new String[]{scatfile, barfile, foofile} },
    		{  new JSONArray(Arrays.asList(scatfile, foofile, barfile)), new String[]{scatfile, foofile, barfile} },
    	};
    }
    
    @Test (groups="model", dataProvider="inputOntologyArrayRetainsOrderingProvider")
    public void inputOntologyArrayRetainsOrdering(JSONArray defaultValue, String[] expectedValues) 
    throws Exception 
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
	        jsonTree.getJSONObject("semantics").put("ontology", defaultValue);
	        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
	        
	        List<String> ontologies = input.getOntologyAsList();
	        
	        for(int i=0; i<expectedValues.length; i++) 
	        {
	        	Assert.assertEquals(ontologies.get(i), expectedValues[i], "order was not preserved " +  
	        			ontologies.get(i) + " was expected, but " + expectedValues[i] + " was found.");
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		Assert.fail(e.getMessage());
    	}
    }
    
    @DataProvider(name="inputsFiltersRedundantValuesFromOntologyProvider")
    public Object[][] inputsFiltersRedundantValuesFromOntologyProvider() throws JSONException
    {
    	String emptyString = "";
    	String nullString = null;
    	
    	String foofile = "http://sswap.org/foo.xml";
    	String barfile = "http://sswap.org/bar.xml";
    	
    	return new Object[][] {
    		// Single value array retains in array
			{  new JSONArray(Arrays.asList(emptyString)), new String[]{}, "single empty string array should be reduced to an empty ontology list" },
			{  new JSONArray(Arrays.asList(foofile)), new String[]{foofile}, "single string array should be retained in the resulting ontology list" },
			
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), new String[]{}, "multiple empty strings should be reduced to an empty ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, foofile)), new String[]{foofile}, "unique entries should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, barfile)), new String[]{barfile}, "unique entries should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString)), new String[]{foofile}, "unique entries should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(barfile, emptyString)), new String[]{barfile}, "unique entries should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString, barfile)), new String[]{foofile, barfile}, "unique entries should be retained in the ontology list" },
    		
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, emptyString)), new String[]{}, "redundant empty strings should be reduced to a single empty string in the ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, foofile)), new String[]{foofile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, emptyString, barfile)), new String[]{barfile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, foofile, emptyString)), new String[]{foofile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(emptyString, barfile, emptyString)), new String[]{barfile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, emptyString, emptyString)), new String[]{foofile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(barfile, emptyString, emptyString)), new String[]{barfile}, "multiple empty strings should be removed and unique values should be retained in the ontology list" },
    		
    		{  new JSONArray(Arrays.asList(foofile, foofile)), new String[]{foofile}, "redundant strings should be reduced to a single string in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, foofile, foofile)), new String[]{foofile}, "redundant strings should be reduced to a single string in the ontology list" },
    		{  new JSONArray(Arrays.asList(barfile, foofile, foofile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, barfile, foofile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, foofile, barfile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
    		
    		{  new JSONArray(Arrays.asList(barfile, barfile)), new String[]{barfile}, "redundant strings should be reduced to a single string in the ontology list" },
    		{  new JSONArray(Arrays.asList(barfile, barfile, barfile)), new String[]{barfile}, "redundant strings should be reduced to a single string in the ontology list" },
    		{  new JSONArray(Arrays.asList(foofile, barfile, barfile)), new String[]{foofile, barfile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
        	{  new JSONArray(Arrays.asList(barfile, foofile, barfile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
    		{  new JSONArray(Arrays.asList(barfile, barfile, foofile)), new String[]{barfile, foofile}, "redundant strings should be reduced to a single string and unique values should be retained in the ontology list" },
    	};
    }
   
    @Test (groups="model", dataProvider="inputsFiltersRedundantValuesFromOntologyProvider")
    public void inputsFiltersRedundantValuesFromOntology(Object defaultValue, String[] expectedValues, String message) 
    throws Exception 
    {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("semantics").put("ontology", defaultValue);
        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
        
        List<String> ontologies = input.getOntologyAsList();
        
        Assert.assertEquals(expectedValues.length, ontologies.size(), 
        		"The number of expected values after parsing the input did not match the actual values after parsing.");
        
        for(String expectedValue: expectedValues) 
        {
        	Assert.assertTrue(ontologies.contains(expectedValue), "expected value " + 
        			expectedValue + " was not found in the default value array after parsing the input");
        }
    }
    
    @Test (groups="model", dataProvider="inputOntologyArrayRetainsOrderingProvider")
    public void inputFileTypesArrayRetainsOrdering(JSONArray defaultValue, String[] expectedValues) 
    throws Exception 
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
	        jsonTree.getJSONObject("semantics").put("fileTypes", defaultValue);
	        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
	        
	        List<String> filesTypes = input.getFileTypesAsList();
	        
	        for(int i=0; i<expectedValues.length; i++) 
	        {
	        	Assert.assertEquals(filesTypes.get(i), expectedValues[i], "order was not preserved " +  
	        			filesTypes.get(i) + " was expected, but " + expectedValues[i] + " was found.");
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test (groups="model", dataProvider="inputsFiltersRedundantValuesFromOntologyProvider")
    public void inputsFiltersRedundantValuesFromFileTypes(Object defaultValue, String[] expectedValues, String message) 
    throws Exception 
    {
        JSONObject jsonTree = new JSONObject(jsonTreeInputs.toString());
        jsonTree.getJSONObject("semantics").put("fileTypes", defaultValue);
        SoftwareInput input = SoftwareInput.fromJSON(jsonTree);
        
        List<String> filesTypes = input.getFileTypesAsList();
        
        Assert.assertEquals(expectedValues.length, filesTypes.size(), 
        		"The number of expected values after parsing the input did not match the actual values after parsing.");
        
        for(String expectedValue: expectedValues) 
        {
        	Assert.assertTrue(filesTypes.contains(expectedValue), "expected value " + 
        			expectedValue + " was not found in the default value array after parsing the input");
        }
    }
    
    @Test(groups="model")
    public void inputCloneCopiesEverything() 
    throws Exception 
    {
        try {
            // create a software object for the oriinputameter's reference
            Software software = new Software();
            software.setId(new Long(1));
            
            // set fields to positive values to ensure we do not get false positives during clone.
            SoftwareInput input = new SoftwareInput();
            input.setId(new Long(1));
            input.setArgument("fooargument");
            input.setEnquote(true);
            input.setFileTypes("foo,bar,bat");
            input.setKey("fookey");
            input.setLabel("foolabel");
            input.setMaxCardinality(12);
            input.setMinCardinality(1);
            input.setOntology(new JSONArray().put("xs:string").put("xs:foo").toString());
            input.setOrder(6);
            input.setRepeatArgument(true);
            input.setRequired(false);
            input.setShowArgument(true);;
            input.setSoftware(software);
            input.setValidator(".*");
            input.setVisible(false);
            
            software.addInput(input);
            
            // create a new software object to assign during the clone
            Software clonedSoftware = new Software();
            clonedSoftware.setId(new Long(2));
            
            // run the clone
            SoftwareInput clonedParameter = input.clone(clonedSoftware);
            
            // validate every field was copied
            Assert.assertEquals(clonedParameter.getArgument(), input.getArgument(), "Argument was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getDefaultValueAsJsonArray().toString(), input.getDefaultValueAsJsonArray().toString(), "defaultValue attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getDescription(), input.getDescription(), "description attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.isEnquote(), input.isEnquote(), "enquote attribute was not cloned from one software input to another");
            Assert.assertNull(clonedParameter.getId(), "id attribute should not be cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getKey(), input.getKey(), "key attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getLabel(), input.getLabel(), "lable attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getMaxCardinality(), input.getMaxCardinality(), "maxCardinality attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getMinCardinality(), input.getMinCardinality(), "minCardinality attribute was not cloned from one software input to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedParameter.getOntologyAsList(), input.getOntologyAsList()), "ontology attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getOrder(), input.getOrder(), "order attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.isRepeatArgument(), input.isRepeatArgument(), "repeatArgument attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.isRequired(), input.isRequired(), "required attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.isShowArgument(), input.isShowArgument(), "showArgument attribute was not cloned from one software input to another");
            Assert.assertNotNull(clonedParameter.getSoftware(), "Sofware reference was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getSoftware().getId(), clonedSoftware.getId(), "Wrong software reference was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.isVisible(), input.isVisible(), "visible attribute was not cloned from one software input to another");
            Assert.assertEquals(clonedParameter.getValidator(), input.getValidator(), "Argument was not cloned from one software input to another");
            
            // do a sanity check by serializing the json as well
            Assert.assertEquals(input.toJSON(), clonedParameter.toJSON(), "cloned input serialized to different value than original input.");
            
        } catch (Exception e) {
            Assert.fail("Cloning software input should not throw exception",e);
        }
    }
    
    @AfterClass
    public void tearDown() throws Exception {

    }

}
