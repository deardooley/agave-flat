package org.iplantc.service.apps.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 4/5/12
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
@Test(groups={"unit"})
public class GSoftwareParameterTest extends GModelTestCommon{
    SoftwareParameter parameter = new SoftwareParameter();

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

    }

    @DataProvider(name = "dataTestParameter")
    public Object[][] createData1() {
        Object[][] mydata = jtd.dataTestParameter;
        return mydata;
    }

    @DataProvider(name = "dataTestParameterValue")
    public Object[][] createData2() {
        Object[][] mydata = jtd.dataTestParameterValue;
        return mydata;
    }

    @DataProvider(name = "dataTestParameterDetails")
    public Object[][] createData3() {
        Object[][] mydata = jtd.dataTestInputsDetails;
        return mydata;
    }

    @DataProvider(name = "dataTestParameterSemanticsOntology")
    public Object[][] createData4() {
        Object[][] mydata = jtd.dataTestParameterSemanticsOntology;
        return mydata;
    }

    //@Test (groups="model", dataProvider="dataTestParameter")
    public void parameterFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.put(name, changeValue);

        super.commonSoftwareFromJSON(parameter,jsonTree,name,changeValue,message,expectExceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestParameterValue")
    public void parameterValueFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("value").put(name,changeValue);
        jsonTree.getJSONObject("semantics").put("minCardinality", StringUtils.equals(name, "required") || StringUtils.equals(name, "visible") ? 1 : 0);
        
        super.commonSoftwareFromJSON(parameter,jsonTree, name,changeValue,message,expectExceptionThrown);
    }
    
    @DataProvider(name="parameterRequiredAndVisibleRespectCardinalityProvider")
    public Object[][] parameterRequiredAndVisibleRespectCardinalityProvider() throws JSONException
    {
    	return new Object[][] {
			// { required, visible, minCardinality, shouldThrowException, message }
			{ true, true, 0, true, "required parameters must have minCardinality > 0"},
			{ true, false, 0, true, "hidden parameters must have minCardinality > 0"},
			{ false, true, 0, false, "visible, optional parameters can have minCardinality = 0"},
			{ false, false, 0, true, "hidden parameters must have minCardinality > 0"}
    	};
    }
    
    @Test (groups="model", dataProvider="parameterRequiredAndVisibleRespectCardinalityProvider")
    public void parameterRequiredAndVisibleRespectCardinality(boolean required, boolean visible, int minCardinality, boolean shouldThrowException, String message)
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
	        jsonTree.getJSONObject("value").put("required", required);
	        jsonTree.getJSONObject("value").put("visible", visible);
	        jsonTree.getJSONObject("semantics").put("minCardinality", minCardinality);
	        jsonTree.getJSONObject("semantics").put("maxCardinality", -1);
	        jsonTree.getJSONObject("value").put("default", "foo");
	        Assert.assertNotNull(SoftwareParameter.fromJSON(jsonTree), "Null parameter returned from SoftwareParameter.fromJSON");
	        
    	} catch (Exception e) {
    		Assert.assertTrue(shouldThrowException, message);
    	}
    }
    
    @DataProvider(name="parameterValueEnumFromJSONProvider")
    public Object[][] parameterValueEnumFromJSONProvider() throws Exception 
    {
    	JSONObject alpha = new JSONObject();
    	alpha.put("alpha", "alpha");
    	JSONObject beta = new JSONObject();
    	beta.put("beta", "beta");
    	JSONObject gamma = new JSONObject();
    	gamma.put("gamma", "gamma");
    	JSONObject delta = new JSONObject();
    	delta.put("delta1", "delta1");
    	delta.put("delta2", "delta2");
    	
    	return new Object[][] {
			{"","cannot set input.value.enum_value to empty string",true},
            {new JSONObject(),"cannot set input.value.enum_value to empty object",true},
            {new JSONArray(new JSONObject[] {alpha, beta, new JSONObject()}),"cannot set input.value.enum_value to array with empty object",true},
            {Arrays.asList(alpha, beta, delta),"cannot set input.value.enum_value to array with object with more than one attribute",true},
            {Arrays.asList(new JSONObject(),new JSONObject(),new JSONArray()),"cannot set input.value.enum_value to array with arraylist in it",true},
			{Arrays.asList("gamma", "beta", new JSONArray()),"cannot set input.value.enum_value to array of string with arraylist in it",true},
            {Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"cannot set input.value.enum_value to array of empty objects",true},
            {null,"cannot set input.value.enum_value to null",true},
			{"abracadabra","cannot set input.value.enum_value to string",true},
			{new Integer(25),"cannot set input.value.enum_value to 25",true},
			{Arrays.asList(alpha, beta, gamma),"should allow seting input.value.enum_value to array of json objects",false},
			{Arrays.asList(alpha, beta, "gamma"),"should allow seting input.value.enum_value to array of single attribute objects and strings",false},
			{Arrays.asList(alpha, "beta", "gamma"),"should allow seting input.value.enum_value to array of single attribute objects and strings",false},
			{Arrays.asList("alpha", "beta", "gamma"),"should allow seting input.value.enum_value to array of single attribute strings",false},
    	};
    }
    
    @Test (groups="model", dataProvider="parameterValueEnumFromJSONProvider")
    public void parameterValueEnumFromJSON(Object enumValue, String message, boolean expectExceptionThrown) 
    throws Exception 
    {   
    	JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        
        jsonTree.getJSONObject("value").remove("type");
        jsonTree.getJSONObject("value").put("type", SoftwareParameterType.enumeration.name());
        
        jsonTree.getJSONObject("value").remove("validator");
        jsonTree.getJSONObject("value").remove("default");
        jsonTree.getJSONObject("value").put("default", "alpha");
       
        jsonTree.getJSONObject("value").remove("enum_values");
        jsonTree.getJSONObject("value").remove("enumValues");
        jsonTree.getJSONObject("value").put("enumValues", enumValue);
         
        super.commonSoftwareFromJSON(parameter, jsonTree, "enum_value", enumValue, message, expectExceptionThrown);
    }
    
    @DataProvider(name="parameterValueEnumDefaultFromJSONProvider")
    public Object[][] parameterValueEnumDefaultFromJSONProvider() throws Exception
    {
    	return new Object[][] {
    		{"", "empty string default value should throw exception", true},
    		{new JSONArray(Arrays.asList("")), "empty string default value should throw exception", true},
    		{new Long(25), "numeric default value should throw exception", true},
    		{new JSONArray(Arrays.asList(new Long(25))), "numeric default value should throw exception", true},
    		{Boolean.TRUE, "boolean true default value should throw exception", true},
    		{Boolean.FALSE, "boolean false default value should throw exception", true},
    		{new JSONArray(Arrays.asList(Boolean.TRUE)), "array of boolean true default value should throw exception", true},
    		{new JSONArray(Arrays.asList(Boolean.FALSE)), "array of boolean false default value should throw exception", true},
    		{new JSONObject(), "empty object for default value should throw exception", true},
    		{"alpha", "single value enum values should not throw exception", false},
    		{"beta", "single value enum values should not throw exception", false},
    		{"gamma", "single value enum values should not throw exception", false},
    		{"zed", "single value other than one of enum values should throw exception", true},
    		{new JSONArray(new String[] { "zed" }), "array with value other than one of enum values should throw exception", true},
    		{new JSONArray(new String[] { "alpha" }), "array with one of enum values should pass with enum", false},
    		{new JSONArray(), "empty jsonarray default value should pass with enum", false},
    		{new JSONArray(Arrays.asList((String)null)), "array with null value should throw exception", true},
    	};
    }
    
    @Test (groups="model", dataProvider="parameterValueEnumDefaultFromJSONProvider")
    public void parameterValueEnumDefaultFromJSON(Object changeValue, String message, boolean expectExceptionThrown) 
    throws Exception 
    {
    	JSONObject alpha = new JSONObject();
    	alpha.put("alpha", "alpha");
    	JSONObject beta = new JSONObject();
    	beta.put("beta", "beta");
    	JSONObject gamma = new JSONObject();
    	gamma.put("gamma", "gamma");
    	
    	JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        
        jsonTree.getJSONObject("value").remove("type");
        jsonTree.getJSONObject("value").put("type", SoftwareParameterType.enumeration.name());
        
        jsonTree.getJSONObject("value").remove("validator");
        jsonTree.getJSONObject("value").remove("enum_values");
        jsonTree.getJSONObject("value").remove("enumValues");
        jsonTree.getJSONObject("value").put("enumValues", new JSONArray(new JSONObject[] { alpha, beta, gamma }));
        
        jsonTree.getJSONObject("value").remove("default");
        jsonTree.getJSONObject("value").put("default", changeValue);
        
        super.commonSoftwareFromJSON(parameter, jsonTree, "default", changeValue, message, expectExceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestParameterDetails")
    public void parameterDetailsFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("details").put(name,changeValue);
        super.commonSoftwareFromJSON(parameter,jsonTree, name,changeValue,message,expectExceptionThrown);
    }
    
    @DataProvider(name="parameterTypeEnforcedOnDefaultValueProvider")
    public Object[][] parameterTypeEnforcedOnDefaultValueProvider() throws JSONException
    {
    	Object positiveInteger = new Integer(5);
    	Object negativeInteger = new Integer(5);
    	Object positiveDecimal = new Float(5.5);
    	Object negativeDecimal = new Float(-5.5);
    	Object nonEmptyString = "hello";
    	Object emptyString = "";
    	Object trueValue = Boolean.TRUE;
    	Object falseValue = Boolean.FALSE;
    	Object jsonObject = new JSONObject();
    	Object emptyArray = new JSONArray();
    	Object numberArray = new JSONArray(Arrays.asList(positiveInteger));
    	Object stringArray = new JSONArray(Arrays.asList(nonEmptyString));
    	Object booleanArray = new JSONArray(Arrays.asList(trueValue));
    	Object objectArray = new JSONArray(Arrays.asList(jsonObject));
    	
    	List<Object> allObjects = Arrays.asList(positiveInteger, negativeInteger, positiveDecimal, negativeDecimal, 
    			trueValue, falseValue, nonEmptyString, emptyString, jsonObject, emptyArray, objectArray, numberArray, stringArray, booleanArray);
    	
    	Map<SoftwareParameterType, List<Object>> map = new HashMap<SoftwareParameterType, List<Object>>();
    	map.put(SoftwareParameterType.number, Arrays.asList(positiveInteger, negativeInteger, positiveDecimal, negativeDecimal,emptyArray, numberArray));
    	map.put(SoftwareParameterType.flag, Arrays.asList(trueValue, falseValue,emptyArray, booleanArray));
    	map.put(SoftwareParameterType.bool, Arrays.asList(trueValue, falseValue,emptyArray, booleanArray));
    	map.put(SoftwareParameterType.string, Arrays.asList(nonEmptyString, emptyString,emptyArray,stringArray));
    	
    	List<Object[]> testData = new ArrayList<Object[]>();
    	for (SoftwareParameterType type: map.keySet())
    	{
    		for (Object object: allObjects)
        	{
    			if (map.get(type).contains(object)) 
    			{
	        		testData.add(new Object[] {
	    				type, 
	    				object, 
	    				"Setting input.value.default to " + object.toString() + " with input.value.type = number should be allowed",
	    				false });
    			} 
    			else 
    			{
    				testData.add(new Object[] {
    	    				type, 
    	    				object, 
    	    				"Setting input.value.default to " + object.toString() + " with input.value.type = number should  not be allowed",
    	    				true });
    			}
        	}
    		
    		testData.add(new Object[] {
    				type, 
    				(String)null, 
    				"Setting input.value.default to null with input.value.type = number should be allowed",
    				false });
    	}
    	
    	return testData.toArray(new Object[56][4]);
    }
    
    @Test (groups="model", dataProvider="parameterTypeEnforcedOnDefaultValueProvider")
    public void parameterTypeEnforcedOnDefaultValue(SoftwareParameterType type, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("value").put("type",type);
        jsonTree.getJSONObject("value").put("validator",(String)null);
        jsonTree.getJSONObject("value").put("default", changeValue);
        super.commonSoftwareFromJSON(parameter, jsonTree, "default", changeValue, message, exceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestParameterSemanticsOntology")
    public void parameterSemanticsFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("semantics").put(name,changeValue);
        jsonTree.getJSONObject("value").put("required",false);
        jsonTree.getJSONObject("value").put("visible",true);
        super.commonSoftwareFromJSON(parameter,jsonTree,name,changeValue,message,expectExceptionThrown);
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
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("value").put("validator",validator);
        jsonTree.getJSONObject("value").put("default", defaultValue);
        super.commonSoftwareFromJSON(parameter, jsonTree, "default", defaultValue, message, exceptionThrown);
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
    		
    		// null checks should pass since parameter has default of 0
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(nullString)), "Null default value should pass despite regex", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(nullString, nullString)), "all null values in default value array should pass despite regex", false },
    			
    		// Single value arrays
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtenstion)), "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionSuffix)), "Default value not matching regex should throw exception", true },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(badFileExtensionPrefix)), "Default value not matching regex should throw exception", true },
    		
    		{  emptyString, new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex matches", false },
    		{  "\\s*", new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex matches", false },
    		{  nullString, new JSONArray(Arrays.asList(emptyString)), "Empty default value should pass when regex is null", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(emptyString)), "Empty default value should throw exception when it does not match", true },
    		
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
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingFilename, matchingFilename)), "both values in default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingExtension, matchingExtension)), "both values in default value matching regex should pass", false },
    		{  ".*\\.pdf$", new JSONArray(Arrays.asList(matchingFilenameWithMultipleExtensions, matchingFilenameWithMultipleExtensions)), "both values in default value matching regex should pass", false },
    	};
    }
   
    @Test (groups="model", dataProvider="inputsValidatorEnforcedOnDefaultValueArrayProvider")
    public void inputsValidatorEnforcedOnDefaultArrayValue(String validator, JSONArray defaultValue, String message, boolean exceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("semantics").put("maxCardinality", -1);
        jsonTree.getJSONObject("value").put("validator",validator);
        jsonTree.getJSONObject("value").put("default", defaultValue);
        super.commonSoftwareFromJSON(parameter, jsonTree, "default", defaultValue, message, exceptionThrown);
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
   
   // @Test (groups="model", dataProvider="inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider")
    public void inputsValidatorFiltersRedundantValuesFromDefaultValueArrayProvider(Object defaultValue, String[] expectedValues, String message) 
    throws Exception 
    {
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("semantics").put("maxCardinality", -1);
        jsonTree.getJSONObject("value").put("default", defaultValue);
        SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonTree);
        
        String[] defaultValuesArray = ServiceUtils.getStringValuesFromJsonArray(parameter.getDefaultValueAsJsonArray(), false);
        
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
    
    //@Test (groups="model", dataProvider="inputDefaultValueArrayRetainsOrderingProvider")
    public void inputDefaultValueArrayRetainsOrdering(JSONArray defaultValue, String[] expectedValues) 
    throws Exception 
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
	        jsonTree.getJSONObject("semantics").put("maxCardinality", -1);
	        jsonTree.getJSONObject("value").put("default", defaultValue);
	        SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonTree);
	        
	        String[] defaultValuesArray = ServiceUtils.getStringValuesFromJsonArray(parameter.getDefaultValueAsJsonArray(), false);
	        
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
	        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
	        jsonTree.getJSONObject("semantics").put("ontology", defaultValue);
	        SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonTree);
	        
	        List<String> ontologies = parameter.getOntologyAsList();
	        
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
        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
        jsonTree.getJSONObject("semantics").put("ontology", defaultValue);
        SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonTree);
        
        List<String> ontologies = parameter.getOntologyAsList();
        
        Assert.assertEquals(expectedValues.length, ontologies.size(), 
        		"The number of expected values after parsing the input did not match the actual values after parsing.");
        
        for(String expectedValue: expectedValues) 
        {
        	Assert.assertTrue(ontologies.contains(expectedValue), "expected value " + 
        			expectedValue + " was not found in the default value array after parsing the input");
        }
    }
    
    @DataProvider(name="parametersDefaultValuesHonorCardinalityProvider")
    public Object[][] parametersDefaultValuesHonorCardinalityProvider() throws JSONException
    {
    	String emptyString = "";
    	String nullString = null;
    	
    	String foo = "foo";
    	
    	return new Object[][] {
    		// Single value against minCardinality
			{  nullString, 0, -1, false, "null string should be allowed as a default value when minCardinality is zero" },
			{  emptyString, 0, -1, false, "empty string should be allowed as a default value when minCardinality is zero" },
			{  foo, 0, -1, false, "string should be as a default value allowed when minCardinality is zero" },
			
			{  nullString, 1, -1, true, "null default value should throw exception when minCardinality is 1" },
			{  emptyString, 1, -1, false, "empty string should be allowed as a default value when minCardinality is 1" },
			{  foo, 1, -1, false, "string should be as a default value allowed when minCardinality is 1" },
			
			{  nullString, 2, -1, true, "null default value should throw exception when minCardinality is 2" },
			{  emptyString, 2, -1, true, "empty string should throw exception when minCardinality is 2" },
			{  foo, 2, -1, true, "string should throw exception when minCardinality is 2" },
			
			// Single value against maxCardinality
			{  nullString, 0, 0, true, "maxCardinality should not be zero" },
			{  nullString, 0, 1, false, "null string should be allowed as a default value when maxCardinality is 1" },
			{  emptyString, 0, 1, false, "empty string should be allowed as a default value when maxCardinality is 1" },
			{  foo, 0, 1, false, "string should be as a default value allowed when maxCardinality is 1" },
			
			{  nullString, 0, 2, false, "null string should be allowed as a default value when maxCardinality is 2" },
			{  emptyString, 0, 2, false, "empty string should be allowed as a default value when maxCardinality is 2" },
			{  foo, 0, 2, false, "string should be as a default value allowed when maxCardinality is 2" },
			
			{  foo, 0, 0, true, "maxCardinality should throw an exception if set to zero" },
			
			// Single value array against minCardinality
			{  new JSONArray(Arrays.asList(nullString)), 0, -1, false, "array with single null string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(emptyString)), 0, -1, false, "array with single empty string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(foo)), 0, -1, false, "array with single string should be as a default value allowed when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(foo)), 1, 0, true, "min cardinality should throw an exception if less than min cardinality" },
			
			{  new JSONArray(Arrays.asList(nullString)), 1, -1, true, "array with single null default value should throw exception when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString)), 1, -1, false, "array with single empty string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(foo)), 1, -1, false, "array with single string should be as a default value allowed when minCardinality is 1" },
			
			{  new JSONArray(Arrays.asList(nullString)), 2, -1, true, "array with single null default value should throw exception when minCardinality is 2" },
			{  new JSONArray(Arrays.asList(emptyString)), 2, -1, true, "array with single empty string should throw exception when minCardinality is 2" },
			{  new JSONArray(Arrays.asList(foo)), 2, -1, true, "array with single string should throw exception when minCardinality is 2" },
			
			// Single value array against maxCardinality
			{  new JSONArray(Arrays.asList(nullString)), 0, 0, true, "array with single null value should throw exception when maxCardinality is zero" },
			{  new JSONArray(Arrays.asList(nullString)), 0, 1, false, "array with single null string should be allowed as a default value when maxCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString)), 0, 1, false, "array with single empty string should be allowed as a default value when maxCardinality is 1" },
			{  new JSONArray(Arrays.asList(foo)), 0, 1, false, "array with single string should be as a default value allowed when maxCardinality is 1" },
			
			{  new JSONArray(Arrays.asList(nullString)), 0, 2, false, "array with single null array should be allowed as a default value when maxCardinality is 2" },
			{  new JSONArray(Arrays.asList(emptyString)), 0, 2, false, "array with single empty string should be allowed as a default value when maxCardinality is 2" },
			{  new JSONArray(Arrays.asList(foo)), 0, 2, false, "array with single string should be as a default value allowed when maxCardinality is 2" },
			
			// Multiple value array against minCardinality 0
			{  new JSONArray(Arrays.asList(nullString, nullString)), 0, -1, false, "array with double null string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), 0, -1, false, "array with null and empty string string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(nullString, foo)), 0, -1, false, "array with null and single string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), 0, -1, false, "array with double empty string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(emptyString, foo)), 0, -1, false, "array with empty and string should be allowed as a default value when minCardinality is zero" },
			{  new JSONArray(Arrays.asList(foo, foo)), 0, -1, false, "array with double string should be allowed as a default value when minCardinality is zero" },
			
			
			// Multiple value array against minCardinality 1
			{  new JSONArray(Arrays.asList(nullString, nullString)), 1, -1, true, "array with double null string should fail when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), 1, -1, false, "array with null and empty string string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(nullString, foo)), 1, -1, false, "array with null and single string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), 1, -1, false, "array with double empty string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString, foo)), 1, -1, false, "array with empty and string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(foo, foo)), 1, -1, false, "array with double string should be allowed as a default value when minCardinality is 1" },
			
			// Multiple value array against minCardinality 3
			{  new JSONArray(Arrays.asList(nullString, nullString)), 3, -1, true, "array with double null string should fail when minCardinality is 3" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), 3, -1, true, "array with null and empty string string should fail when minCardinality is 3" },
			{  new JSONArray(Arrays.asList(nullString, foo)), 3, -1, true, "array with null and single string should fail when minCardinality is 3" },
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), 3, -1, true, "array with double empty string should fail when minCardinality is 3" },
			{  new JSONArray(Arrays.asList(emptyString, foo)), 3, -1, true, "array with empty and string should fail when minCardinality is 3" },
			{  new JSONArray(Arrays.asList(foo, foo)), 3, -1, true, "array with double string should fail when minCardinality is 3" },
			
			// Multiple value array against min and max Cardinality 1
			{  new JSONArray(Arrays.asList(nullString, nullString)), 1, 1, true, "array with double null string should fail when minCardinality and maxCardinality are 1" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), 1, 1, false, "array with null and empty string string should be allowed when minCardinality and maxCardinality are 1" },
			{  new JSONArray(Arrays.asList(nullString, foo)), 1, 1, false, "array with null and single string should be allowed when minCardinality and maxCardinality are 1" },
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), 1, 1, true, "array with double empty string should fail when minCardinality and maxCardinality are 1" },
			{  new JSONArray(Arrays.asList(emptyString, foo)), 1, 1, true, "array with empty and string should fail when minCardinality and maxCardinality are 1" },
			{  new JSONArray(Arrays.asList(foo, foo)), 1, 1, true, "array with double string should fail when minCardinality and maxCardinality are 1" },
			
			// Multiple value array against min and max Cardinality 3
			{  new JSONArray(Arrays.asList(nullString, nullString)), 3, 3, true, "array with double null string should fail when minCardinality and maxCardinality are 3" },
			{  new JSONArray(Arrays.asList(nullString, emptyString)), 3, 3, true, "array with null and empty string string should fail when minCardinality and maxCardinality are 3" },
			{  new JSONArray(Arrays.asList(nullString, foo)), 3, 3, true, "array with null and single string should fail when minCardinality and maxCardinality are 3" },
			{  new JSONArray(Arrays.asList(emptyString, emptyString)), 3, 3, true, "array with double empty string should fail when minCardinality and maxCardinality are 3" },
			{  new JSONArray(Arrays.asList(emptyString, foo)), 3, 3, true, "array with empty and string should fail when minCardinality and maxCardinality are 3" },
			{  new JSONArray(Arrays.asList(foo, foo)), 3, 3, true, "array with double string should fail when minCardinality and maxCardinality are 3" },
			
			
			{  new JSONArray(Arrays.asList(nullString)), 1, -1, true, "array with single null default value should throw exception when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString)), 1, -1, false, "array with single empty string should be allowed as a default value when minCardinality is 1" },
			{  new JSONArray(Arrays.asList(foo)), 1, -1, false, "array with single string should be as a default value allowed when minCardinality is 1" },
			
			{  new JSONArray(Arrays.asList(nullString)), 2, -1, true, "array with single null default value should throw exception when minCardinality is 2" },
			{  new JSONArray(Arrays.asList(emptyString)), 2, -1, true, "array with single empty string should throw exception when minCardinality is 2" },
			{  new JSONArray(Arrays.asList(foo)), 2, -1, true, "array with single string should throw exception when minCardinality is 2" },
			
			// Single value array against maxCardinality 
			{  new JSONArray(Arrays.asList(nullString)), 0, 0, true, "array with single null value should throw exception when maxCardinality is zero" },
			{  new JSONArray(Arrays.asList(nullString)), 0, 1, false, "array with single null string should be allowed as a default value when maxCardinality is 1" },
			{  new JSONArray(Arrays.asList(emptyString)), 0, 1, false, "array with single empty string should be allowed as a default value when maxCardinality is 1" },
			{  new JSONArray(Arrays.asList(foo)), 0, 1, false, "array with single string should be as a default value allowed when maxCardinality is 1" },
			
			{  new JSONArray(Arrays.asList(nullString)), 0, 2, false, "array with single null array should be allowed as a default value when maxCardinality is 2" },
			{  new JSONArray(Arrays.asList(emptyString)), 0, 2, false, "array with single empty string should be allowed as a default value when maxCardinality is 2" },
			{  new JSONArray(Arrays.asList(foo)), 0, 2, false, "array with single string should be as a default value allowed when maxCardinality is 2" },
			
			
    	};
    }
    
    @Test (groups="model", dataProvider="parametersDefaultValuesHonorCardinalityProvider")
    public void parametersDefaultValuesHonorCardinality(Object defaultValue, int minCardinality, int maxCardinality, boolean shouldThrowException, String message) 
    throws Exception 
    {
    	try {
	        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
	        jsonTree.getJSONObject("semantics").put("maxCardinality", new Integer(maxCardinality));
	        jsonTree.getJSONObject("semantics").put("minCardinality", new Integer(minCardinality));
	        jsonTree.getJSONObject("value").put("default", defaultValue);
	        Assert.assertNotNull(SoftwareParameter.fromJSON(jsonTree), message);
    	} catch (Exception e) {
    		e.printStackTrace();
    		Assert.assertTrue(shouldThrowException, message);
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}
    }
    
    @Test(groups="model")
    public void softwareParameterEnumeratedValueCloneCopiesEverything() 
    throws Exception 
    {
        SoftwareParameter param = new SoftwareParameter();
        param.setId(new Long(1));
        
        SoftwareParameterEnumeratedValue enumValue = new SoftwareParameterEnumeratedValue("alpha", "The alpha parameter", param);
        
        SoftwareParameterEnumeratedValue clonedEnumValue = enumValue.clone();
        
        Assert.assertEquals(clonedEnumValue.getLabel(), enumValue.getLabel(), "label attribute was not cloned from one software parameter enum value to another");
        Assert.assertEquals(clonedEnumValue.getValue(), enumValue.getValue(), "value attribute was not cloned from one software parameter enum value  to another");
        Assert.assertNull(clonedEnumValue.getSoftwareParameter(), "softwareParameter should not be cloned from one software parameter enum value to another");
    }
    
    @Test(groups="model")
    public void parameterCloneCopiesEverything() 
    throws Exception 
    {
        try {
            // create a software object for the original parameter's reference
            Software software = new Software();
            software.setId(new Long(1));
            
            // set fields to positive values to ensure we do not get false positives during clone.
            SoftwareParameter param = new SoftwareParameter();
            param.setId(new Long(1));
            param.setArgument("fooargument");
            param.setEnquote(true);
            param.addEnumValue(new SoftwareParameterEnumeratedValue("alpha", "The alpha parameter", param));
            param.addEnumValue(new SoftwareParameterEnumeratedValue("beta", "The beta parameter", param));
            param.addEnumValue(new SoftwareParameterEnumeratedValue("gamma", "The gamma parameter", param));
            param.setKey("fookey");
            param.setLabel("foolabel");
            param.setMaxCardinality(12);
            param.setMinCardinality(1);
            param.setOntology(new JSONArray().put("xs:string").put("xs:foo").toString());
            param.setOrder(6);
            param.setRepeatArgument(true);
            param.setRequired(false);
            param.setShowArgument(true);;
            param.setSoftware(software);
            param.setType(SoftwareParameterType.enumeration);
            param.setValidator(".*");
            param.setVisible(false);
            
            software.addParameter(param);
            
            // create a new software object to assign during the clone
            Software clonedSoftware = new Software();
            clonedSoftware.setId(new Long(2));
            
            // run the clone
            SoftwareParameter clonedParameter = param.clone(clonedSoftware);
            
            // validate every field was copied
            Assert.assertEquals(clonedParameter.getArgument(), param.getArgument(), "Argument was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getDefaultValueAsJsonArray().toString(), param.getDefaultValueAsJsonArray().toString(), "defaultValue attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getDescription(), param.getDescription(), "description attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.isEnquote(), param.isEnquote(), "enquote attribute was not cloned from one software parameter to another");
            Assert.assertNull(clonedParameter.getId(), "id attribute should not be cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getKey(), param.getKey(), "key attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getLabel(), param.getLabel(), "lable attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getMaxCardinality(), param.getMaxCardinality(), "maxCardinality attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getMinCardinality(), param.getMinCardinality(), "minCardinality attribute was not cloned from one software parameter to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedParameter.getOntologyAsList(), param.getOntologyAsList()), "ontology attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getOrder(), param.getOrder(), "order attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.isRepeatArgument(), param.isRepeatArgument(), "repeatArgument attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.isRequired(), param.isRequired(), "required attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.isShowArgument(), param.isShowArgument(), "showArgument attribute was not cloned from one software parameter to another");
            Assert.assertNotNull(clonedParameter.getSoftware(), "Sofware reference was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getSoftware().getId(), clonedSoftware.getId(), "Wrong software reference was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getType(), param.getType(), "parameter type attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.isVisible(), param.isVisible(), "visible attribute was not cloned from one software parameter to another");
            Assert.assertEquals(clonedParameter.getValidator(), param.getValidator(), "Argument was not cloned from one software parameter to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedParameter.getEnumValues(), param.getEnumValues()), "enumValues attribute was not cloned from one software parameter to another"); ;
            for (SoftwareParameterEnumeratedValue enumValue: clonedParameter.getEnumValues()) {
                Assert.assertNotNull(enumValue.getSoftwareParameter(), "software parameter association should be present in all software parameter enum values after clone");
                Assert.assertEquals(enumValue.getSoftwareParameter(), clonedParameter, "enum value should reference the cloned software parameter, not the original after cloneing a software parameter");
            }
            // do a sanity check by serializing the json as well
            Assert.assertEquals(param.toJSON(), clonedParameter.toJSON(), "cloned parameter serialized to different value than original parameter.");
            
        } catch (Exception e) {
            Assert.fail("Cloning software parameter should not throw exception",e);
        }
    }
    
//    @Test (groups="model", dataProvider="inputsFiltersRedundantValuesFromOntologyProvider")
//    public void inputsFiltersRedundantValuesFromFileTypes(Object defaultValue, String[] expectedValues, String message) 
//    throws Exception 
//    {
//        JSONObject jsonTree = new JSONObject(jsonTreeParameters.toString());
//        jsonTree.getJSONObject("semantics").put("fileTypes", defaultValue);
//        SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonTree);
//        
//        List<String> filesTypes = parameter.getFileTypesAsList();
//        
//        Assert.assertEquals(expectedValues.length, filesTypes.size(), 
//        		"The number of expected values after parsing the input did not match the actual values after parsing.");
//        
//        for(String expectedValue: expectedValues) 
//        {
//        	Assert.assertTrue(filesTypes.contains(expectedValue), "expected value " + 
//        			expectedValue + " was not found in the default value array after parsing the input");
//        }
//    }

}
