package org.iplantc.service.io.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;

/**
 * Contains constants and test data for unit testing.
 *  
 */
public class JSONTestDataUtil {
	public static String TEST_SOFTWARE_FOLDER = "software/";
	public static String TEST_SOFTWARE_FILE = TEST_SOFTWARE_FOLDER + "head-lonestar.tacc.teragrid.org.json";
	public static String TEST_SYSTEM_FOLDER = "systems/";
	public static String TEST_SOFTWARE_SYSTEM_FILE = TEST_SOFTWARE_FOLDER + "system-software.json";
	public static String TEST_EXECUTION_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "execution/execute.example.com.json";
//	public static String TEST_STORAGE_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "storage/storage.example.com.json";
	public static String TEST_STORAGE_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "storage/sftp.example.com.json";
	public static String TEST_IRODS_STORAGE_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "storage/data.iplantcollaborative.org.json";
	public static String TEST_AUTHENTICATION_SYSTEM_FILE = "authentication-system.json";
    
	public static JSONTestDataUtil jsonTestData;
	public static String TEST_OWNER = "api_sample_user";
	public static String TEST_SHARED_OWNER = "ipctestshare";
	public static String TEST_PUBLIC_OWNER = "guest";
	
    /**
     *
     */
    protected String[] data = {"name","parallelism","version","author","helpURI","datePublished",
            "label","shortDescription","longDescription","tags","ontology",
            "executionHost","executionType","deploymentPath","templatePath","executablePath",
            "testPath","checkpointable","modules","inputs","parameters","outputs"};



/*
    private Object[][] softwareFields(){
        JSONTestDataUtil.concatAll()
    }
*/

    /**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException 
     * @throws JsonProcessingException 
     */
    public JSONObject getTestDataObject(String file) throws JSONException, IOException
    {
    	InputStream in = null;
    	try 
    	{
            in = JSONTestDataUtil.class.getClassLoader().getResourceAsStream(file);
    		String json = IOUtils.toString(in, "UTF-8");
    		
	    	return new JSONObject(json);
    	} 
    	finally {
    		if (in != null) try { in.close(); } catch (Exception e) {}
    	}
    }

    /**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException
     * @throws JsonProcessingException
     */
    public JSONObject getTestDataObject(File pfile) throws JSONException, IOException
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(pfile.getAbsoluteFile());
            String json = IOUtils.toString(in, "UTF-8");

            return new JSONObject(json);
        }
        finally {
            try { in.close(); } catch (Exception e) {}
        }
    }

    /**
     * Used in conjunction with jackson data mapping
     * @param obj pojo to be mapped
     * @return  a POJONode after mapping.
     */
    public POJONode makeJsonObj(Object obj)
    {
    	ObjectMapper mapper = new ObjectMapper();
    	return (POJONode)mapper.getNodeFactory().pojoNode(obj);
    }


    protected String arbitraryArray = "{\"one\", \"two\", \"three\", \"four\", \"five\", \"six\", \"seven\", \"eight\"}";

    protected String emptyJSONFile, malformedJSONFile_1, malformedJSONFile_2 = "{}";

    protected Object[][] dataTestSoftwareFieldsEmpty = {
            {"name","","set name to empty string",true},
            {"parallelism","","set parallelism to empty string",true},
            {"version","","set version to empty string",true},
            {"helpURI","","set helpURI to empty string",false},
            {"label","","set label to empty string",false},
            {"shortDescription","","set shortDescription to empty string",false},
            {"longDescription","","set longDescription to empty string",false},
            {"tags","","set tags to empty string",false},
            {"ontology","","set ontology to empty string",false},
            {"executionHost","","set executionHost to empty string",true},
            {"executionType","","set executionType to empty string",true},
            {"deploymentPath","","set deploymentPath to empty string",true},
            {"templatePath","","set templatePath to empty string",true},
            {"testPath","","set testPath to empty string",true},             // requires a string larger than empty
            {"checkpointable","","set checkpointable to empty string",true},
            {"modules","","set modules to empty string",false},
            {"inputs","","set inputs to empty string",true},
            {"parameters","","set parameters to empty string",true},
            {"outputs","","set outputs to empty string",true}
    };

    protected Object[][] dataTestSoftwareFieldsNull = {
            {"name",null,"set name to null",true},
            {"parallelism",null,"set parallelism to null",true},
            {"version",null,"set version to null",true},
            {"helpURI",null,"set helpURI to null",false},
            {"label",null,"set label to null",false},
            {"shortDescription",null,"set shortDescription to null",false},
            {"longDescription",null,"set longDescription to null",false},
            {"tags",null,"set tags to null",false},
            {"ontology",null,"set ontology to null",false},
            {"executionHost",null,"set executionHost to null",true},
            {"executionType",null,"set executionType to null",true},
            {"deploymentPath",null,"set deploymentPath to null",true},
            {"templatePath",null,"set templatePath to null",true},
            {"testPath",null,"set testPath to null",true},             // requires a string larger than empty
            {"checkpointable",null,"set checkpointable to null",false},
            {"modules",null,"set modules to null",false},
            {"inputs",null,"set inputs to null",false},
            {"parameters",null,"set parameters to null",false},
            {"outputs",null,"set outputs to null",false}
    };
    
    protected Object[][] dataTestSoftwareFieldsEmptyObject = {
            {"name",new Object(),"set name to null",true},
            {"parallelism",new Object(),"set parallelism to null",true},
            {"version",new Object(),"set version to null",true},
            {"helpURI",new Object(),"set helpURI to null",true},
            {"label",new Object(),"set label to null",true},
            {"shortDescription",new Object(),"set shortDescription to null",true},
            {"longDescription",new Object(),"set longDescription to null",true},
            {"tags",new Object(),"set tags to null",true},
            {"ontology",new Object(),"set ontology to null",true},
            {"executionHost",new Object(),"set executionHost to null",true},
            {"executionType",new Object(),"set executionType to null",true},
            {"deploymentPath",new Object(),"set deploymentPath to null",true},
            {"templatePath",new Object(),"set templatePath to null",true},
            {"testPath",new Object(),"set testPath to null",true},             // requires a string larger than empty
            {"checkpointable",new Object(),"set checkpointable to null",true},
            {"modules",new Object(),"set modules to null",true},
            {"inputs",new Object(),"set inputs to null",true},
            {"parameters",new Object(),"set parameters to null",true},
            {"outputs",new Object(),"set outputs to null",true}
    };
    
    protected Object[][] dataTestSoftwareFieldsArray = {
            {"name",Arrays.asList(new Object()),"set name to array with empty object",true},
            {"parallelism",Arrays.asList(new Object()),"set parallelism to array with empty object",true},
            {"version",Arrays.asList(new Object()),"set version to array with empty object",true},
            {"helpURI",Arrays.asList(new Object()),"set helpURI to array with empty object",true},
            {"label",Arrays.asList(new Object()),"set label to array with empty object",true},
            {"shortDescription",Arrays.asList(new Object()),"set shortDescription to array with empty object",true},
            {"longDescription",Arrays.asList(new Object()),"set longDescription to array with empty object",true},
            {"tags",Arrays.asList(new Object()),"set tags to array with empty object",true},
            {"ontology",Arrays.asList(new Object()),"set ontology to array with empty object",true},
            {"executionHost",Arrays.asList(new Object()),"set executionHost to array with empty object",true},
            {"executionType",Arrays.asList(new Object()),"set executionType to array with empty object",true},
            {"deploymentPath",Arrays.asList(new Object()),"set deploymentPath to array with empty object",true},
            {"templatePath",Arrays.asList(new Object()),"set templatePath to array with empty object",true},
            {"testPath",Arrays.asList(new Object()),"set testPath to array with empty object",true},             // requires a string larger than empty
            {"checkpointable",Arrays.asList(new Object()),"set checkpointable to array with empty object",true},
            {"modules",Arrays.asList(new Object()),"set modules to array with empty object",true},
            {"inputs",Arrays.asList(new Object()),"set inputs to array with empty object",true},
            {"parameters",Arrays.asList(new Object()),"set parameters to array with empty object",true},
            {"outputs",Arrays.asList(new Object()),"set outputs to array with empty object",true}
    };
    
    protected Object[][] dataTestSoftwareFieldsInvalid = {
            {"parallelism",Boolean.TRUE,"set parallelism to TRUE",true},
            {"parallelism",Boolean.FALSE,"set parallelism to FALSE",true},
            {"parallelism",new Integer(4),"set parallelism to 4",true},
            {"parallelism","concurrent","set parallelism to invalid string",true},
            {"version","1-1-1","set version to invalid dash notation, 1-1-1",true},
            {"version","1-1-1","set version to invalid underscore notation, 1_1_1",true},
            {"version","a.b.c","set version to invalid alpha characters, a.b.c",true},
            {"version","1-1-1","set version to invalid mixed underscore notation, 1.1_1",true},
            {"version","1-1-1","set version to invalid mixed dash notation, 1.1-1",true},
            {"helpURI","ut website","set helpURI to invalid url",true},
            {"tags",Arrays.asList("abcd","abcd"),"set tags to array with duplicate entries",true},
            {"ontology",Arrays.asList("abcd","abcd"),"set ontology to array with duplicate entries",true},
            {"executionHost",Boolean.TRUE,"set executionHost to TRUE",true},
            {"executionHost",Boolean.FALSE,"set executionHost to FALSE",true},
            {"executionHost",new Integer(4),"set executionHost to 4",true},
            {"executionHost","ranch","set executionHost to invalid system",true},
            {"executionType",Boolean.TRUE,"set executionType to TRUE",true},
            {"executionType",Boolean.FALSE,"set executionType to FALSE",true},
            {"executionType",new Integer(4),"set executionType to 4",true},
            {"executionType","ranch","set executionType to invalid system",true},
            {"checkpointable",new Integer(1),"set checkpointable to 1",true},
            {"checkpointable","yes","set checkpointable to yes",true},
            {"checkpointable","no","set checkpointable to no",true},
            {"modules",Arrays.asList("abcd","abcd"),"set modules to array with duplicate modules",true},
    };
    
    protected Object[][] dataTestSoftwareFieldsValid = {
    		{"name","abcd","set name to valid string",false},
    		{"parallelism","PARALLEL","set parallelism to PARALLEL",false},
    		{"parallelism","parallel","set parallelism to parallel",false},
    		{"parallelism","PTHREAD","set parallelism to PTHREAD",false},
    		{"parallelism","pthread","set parallelism to pthread",false},
    		{"parallelism","SERIAL","set parallelism to SERIAL",false},
    		{"parallelism","serial","set parallelism to serial",false},
    		{"version","1.1.0","set version to valid version '1.1.0'",false},
            {"helpURI","http://example.com","set helpURI to valid uri 'http://example.com'",false},
            {"label","my label","set label to valid string 'my label'",false},
            {"shortDescription","short description","set shortDescription to valid string 'short description'",false},
            {"longDescription","long description","set longDescription to valid string 'long description'",false},
            {"tags",Arrays.asList("abcd","efgh","ijkl"),"set tags to valid array of string",false},
            {"ontology",Arrays.asList("abcd","efgh","ijkl"),"set ontology to valid array of strings",false},
            {"executionHost","lonestar4.tacc.teragrid.org","set executionHost to 'lonestar4.tacc.teragrid.org'",false},
            {"executionType",ExecutionType.ATMOSPHERE.name(),"set executionType to " + ExecutionType.ATMOSPHERE.name(),false},
            {"executionType",ExecutionType.ATMOSPHERE.name().toLowerCase(),"set executionType to " + ExecutionType.ATMOSPHERE.name().toLowerCase(),false},
            {"executionType",ExecutionType.CLI.name(),"set executionType to " + ExecutionType.CLI.name(),false},
            {"executionType",ExecutionType.CLI.name().toLowerCase(),"set executionType to " + ExecutionType.CLI.name().toLowerCase(),false},
            {"executionType",ExecutionType.CONDOR.name(),"set executionType to " + ExecutionType.CONDOR.name(),false},
            {"executionType",ExecutionType.CONDOR.name().toLowerCase(),"set executionType to " + ExecutionType.CONDOR.name().toLowerCase(),false},
            {"executionType",ExecutionType.HPC.name(),"set executionType to " + ExecutionType.HPC.name(),false},
            {"executionType",ExecutionType.HPC.name().toLowerCase(),"set executionType to " + ExecutionType.HPC.name().toLowerCase(),false},
            {"deploymentPath","/path/to/app","set deploymentPath to '/path/to/app'",false},
            {"templatePath","path/to/wrapper","set templatePath to 'path/to/wrapper'",false},
            {"testPath","path/to/test","set testPath to 'path/to/test'",false},             // requires a string larger than empty
            {"checkpointable",Boolean.TRUE,"set checkpointable to TRUE",false},
            {"checkpointable",Boolean.TRUE,"set checkpointable to FALSE",false},
            {"modules",Arrays.asList("abcd","efgh","ijkl"),"set modules to valid array of strings",false},
    };
    
    

//    // Empty arrays test for JSONArray objects in json message
//    protected Object[][] dataTest3 = {
//            {"tags",new Object(),"set tags to an empty array",true},
//            {"tags",arbitraryArray,"set tags to an array of values",true},
//            {"ontology",new Object(),"set tags to an empty array",true},
//            {"ontology",arbitraryArray,"set ontology to an array of values",true},
//            {"modules",new Object(),"set tags to an empty array",true},
//            {"modules",arbitraryArray,"set modules to an array of values",true}
//    };

    
    /************************************************************************
     * 				Software Input Test Data
     ************************************************************************/

    /*
        The values of each individual array element are
        'json label','value to set on the json label','the message for the test','if this test data should throw an exception'
     */
            // test data with a null value as the change will not work with these Object arrays the corresponding array
            // element just disappears array[1] = null just doesn't exist.
    public Object[][] dataTestInputs = {
            {"id","","set input.id to empty string",true},
            {"id",null,"set input.id to null",true},
            {"value",new Object(),"set input.value to empty object",true},
            {"value",new ArrayList<String>(),"set input.value to empty array",true},
			{"value","","set input.value to empty string",true},
			{"value","abcd","set input.value to random string",true},
            {"value",null,"set input.value to null",true},
            {"details",new Object(),"set input.details to empty object",true},
			{"details",new ArrayList<String>(),"set input.details to empty array",true},
			{"details","","set input.details to empty string",true},
			{"details","abcd","set input.details to random string",true},
            {"details",null,"set input.details to null",true},
			{"semantics",new Object(),"set input.semantics to empty object",true},
			{"semantics",new ArrayList<String>(),"set input.semantics to empty array",true},
			{"semantics","","set input.semantics to empty string",true},
			{"semantics","abcd","set input.semantics to random string",true},
            {"semantics",null,"set input.semantics to null",true},
    };

    public Object[][] dataTestInputsValue = {
            {"default","","set input.value.default to empty string",false},
            {"default",new Object(),"set input.value.default to empty object",false},
            {"default",Arrays.asList(new Object(),new Object(),new Object()),"set input.value.default to array of empty objects",false},
            {"default", null,"set value.default to null",false},
			{"default","abracadabra","set value.default to null on hidden input",false},
			{"default",new Integer(25),"set input.value.default to 25 on hidden input",false},
            {"validator","","set input.value.validator to empty string",false},
            {"validator",new Object(),"set input.value.validator to empty object",false},
            {"validator",Arrays.asList(new Object(),new Object(),new Object()),"set input.value.validator to array of empty objects",true},
            {"validator",new String("/(foo"),"set validator to bad regex string",false},
            {"validator",Arrays.asList(new String("/(foo"),new String("/(foo2"),new String("/(foou")),"set input.value.validator to array of bad regex strings",false},
            {"validator","\\d+","set validator to valid regex string",false},
            {"validator",Arrays.asList(new String("^(\\/)?(\\/[a-z_\\-\\s0-9\\.]+)+"), new String(".pdf"), new String("[A-Z][a-z]{1,}")),"set input.value.validator to array of valid regex strings",false},
            {"validator",null,"set input.value.validator to null",false},
            {"required","","set input.value.required to empty string",true},
            {"required",Boolean.TRUE,"set input.value.required to true",false},
            {"required",Boolean.FALSE,"set input.value.required to false",false},
            {"required",null,"set input.value.required to null",false},
            {"visible","","set input.value.visible to empty string",true},
            {"visible",Boolean.TRUE,"set input.value.visible to true",false},
            {"visible",Boolean.FALSE,"set input.value.visible to false",false},
            {"visible",null,"set input.value.visible to null",false},
    };

	public Object[][] dataTestInputsDetails = {
            {"label","","set input.details.label to empty string",false},
            {"label",new Object(),"set input.details.label to empty object",false},
            {"label",Arrays.asList(new Object()),"set input.details.label to list of empty objects",false},
            {"label",null,"set input.details.label to null",false},
            {"label","Sample label","set input.details.label to valid string",false},
            {"description","","set input.details.description to empty string",false},
            {"description",new Object(),"set input.details.description to empty object",false},
            {"description",Arrays.asList(new Object()),"set input.details.description to list of empty objects",false},
            {"description",null,"set input.details.description to null",false},
            {"description","Sample description","set input.details.description to valid string",false},
    };
	
    public Object[][] dataTestHiddenInputsValue = {
			{"default","","set input.value.default to empty string on hidden input",true},
			{"default",null,"set input.value.default to null on hidden input",true},
			{"default","abracadabra","set input.value.default to random string on hidden input",false},
			{"default",new Integer(25),"set input.value.default to null on hidden input",true},
	};

	public Object[][] dataTestInputsSemantics = {
			{"ontology","","set input.semantics.ontology to empty object",true},
			{"ontology",new Object(),"set input.semantics.ontology to empty object",true},
            {"ontology",Arrays.asList(new Object()),"set input.semantics.ontology to empty list of objects",true},
            {"ontology","abcdef","set input.semantics.ontology to valid string",true},
            {"ontology",Arrays.asList("abcdef","abcdef","abcdef"),"set input.semantics.ontology to valid list of strings",true},
            {"ontology",null,"set input.semantics.ontology to null",true},
            {"minCardinality","","set input.semantics.minCardinality to empty string",true},
            {"minCardinality",null,"set input.semantics.minCardinality to null",false},
			{"minCardinality","abc","set input.semantics.minCardinality to non-numeric",true},
			{"minCardinality","1","set input.semantics.minCardinality to numeric string",false},
			{"minCardinality",new Integer(1),"set input.semantics.minCardinality to non-numeric",false},
			{"maxCardinality","","set input.semantics.maxCardinality to empty string",false},
            {"maxCardinality",null,"set input.semantics.maxCardinality to null",false},
			{"maxCardinality","abc","set input.semantics.maxCardinality to non-numeric",false},
			{"maxCardinality","1","set input.semantics.maxCardinality to numeric string",false},
			{"maxCardinality",new Integer(1),"set input.semantics.maxCardinality to non-numeric",false},
			{"fileTypes",new Object(),"set input.semantics.fileTypes to empty object",true},
            {"fileTypes",null,"set input.semantics.fileTypes to null",false},
            {"fileTypes","","set input.semantics.fileTypes to empty string",true},
            {"fileTypes","abcdf","set input.semantics.fileTypes to valid string",true},
            {"fileTypes",Arrays.asList("abcdef","abcdef","abcdef"),"set input.semantics.fileTypes to list of strings",true},
            {"fileTypes",Arrays.asList(new Object()),"set input.semantics.fileTypes to list of objects",true},
    };

	/************************************************************************
     * 				Software Argument Test Data
     ************************************************************************/

    protected Object[][] softwareArgumentDefaultValueData = {
            {"","set SoftwareArgument defaultValue to empty string",true},
            {null,"set SoftwareArgument defaultValue to null",true},
            {1,"set SoftwareArgument defaultValue to int 1",true},
            {"test","set SoftwareArgument defaultValue to string test",true},
            {new Object(),"set SoftwareArgument defaultValue to empty object",true},
            {Arrays.asList(new Object()),"set SoftwareArgument defaultValue to empty list of objects",true},
    };

    protected Object[][] softwareArgumentIdData = {
            {"","set default to empty string",true},
            {null,"set default to null",true},
            {"test","set default to string test",false},
            {Integer.valueOf(25),"set default to 25",false},
    };
	
	
	protected Object[][] softwareArgumentValueData = {
            {"","set value to empty string",true},
            {null,"set default to null",true},
            {"test","set default to string test",true},
            {Integer.valueOf(25),"set default to 25",true},
    };
	
    protected Object[][] softwareArgumentSemanticsData = {
			{"","set semantics to empty string",true},
			{new Object(),"set semantics to empty object",false},
            {Arrays.asList(new Object()),"set semantics.ontology to empty list of objects",true},
            {"abcdef","set semantics to valid string",true},
            {Arrays.asList("abcdef","abcdef","abcdef"),"set semantics to valid list of strings",true},
            {null,"set semantics to null",false},
           
	};
    
    /************************************************************************
     * 				Software Parameter Test Data
     ************************************************************************/

    public Object[][] dataTestParameter = {
            {"id","","set parameter.id to empty string",true},
            {"id",null,"set parameter.id to null",true},
            {"value",new Object(),"set parameter.value to empty object",true},
            {"value",new ArrayList<String>(),"set parameter.value to empty array",true},
			{"value","","set parameter.value to empty string",true},
			{"value","abcd","set parameter.value to random string",true},
            {"value",null,"set parameter.value to null",true},
            {"details",new Object(),"set parameter.details to empty object",true},
			{"details",new ArrayList<String>(),"set parameter.details to empty array",true},
			{"details","","set parameter.details to empty string",true},
			{"details","abcd","set parameter.details to random string",true},
            {"details",null,"set parameter.details to null",true},
			{"semantics",new Object(),"set parameter.semantics to empty object",true},
			{"semantics",new ArrayList<String>(),"set parameter.semantics to empty array",true},
			{"semantics","","set parameter.semantics to empty string",true},
			{"semantics","abcd","set parameter.semantics to random string",true},
            {"semantics",null,"set parameter.semantics to null",true},
    };

    public Object[][] dataTestParameterValue = {
            {"default","","set parameter.value.default to empty string",false},
            {"default",new Object(),"set parameter.value.default to empty object",false}, //just changed this
            {"default",Arrays.asList(new Object(),new Object(),new Object()),"set parameter.value.default to array of empty objects",false},
            {"default", null,"set value.default to null",false},
			{"validator","","set parameter.value.validator to empty string",false},
            {"validator",new Object(),"set parameter.value.validator to empty object",false},
            {"validator",Arrays.asList(new Object(),new Object(),new Object()),"set parameter.value.validator to array of empty objects",true},
            {"validator",new String("/(foo"),"set validator to bad regex string",false},
            {"validator",Arrays.asList(new String("/(foo"),new String("/(foo2"),new String("/(foou")),"set parameter.value.validator to array of bad regex strings",false},
            {"validator","\\d+","set validator to valid regex string",false},
            {"validator",Arrays.asList(new String("\\d+"), new String(".pdf"), new String("[A-Z][a-z]{1,}")),"set parameter.value.validator to array of valid regex strings",false},
            {"validator",null,"set parameter.value.validator to null",false},
            {"required","","set parameter.value.required to empty string",true},
            {"required",Boolean.TRUE,"set parameter.value.required to true",false},
            {"required",Boolean.FALSE,"set parameter.value.required to false",false},
            {"required",null,"set parameter.value.required to null",false},
            {"visible","","set parameter.value.visible to empty string",true},
            {"visible",Boolean.TRUE,"set parameter.value.visible to true",false},
            {"visible",Boolean.FALSE,"set parameter.value.visible to false",false},
            {"visible",null,"set parameter.value.visible to null",false},
    };

	public Object[][] dataTestParameterDetails = {
            {"label","","set parameter.details.label to empty string",true},
            {"label",new Object(),"set parameter.details.label to empty object",false},
            {"label",Arrays.asList(new Object()),"set parameter.details.label to list of empty objects",false},
            {"label",null,"set parameter.details.label to null",false},
            {"label","Sample label","set parameter.details.label to valid string",false},
            {"description","","set parameter.details.description to empty string",false},
            {"description",new Object(),"set parameter.details.description to empty object",false},
            {"description",Arrays.asList(new Object()),"set parameter.details.description to list of empty objects",false},
            {"description",null,"set parameter.details.description to null",false},
            {"description","Sample description","set parameter.details.description to valid string",false},
    };
	
    public Object[][] dataTestHiddenParameterValue = {
			{"default","","set parameter.value.default to empty string on hidden input",true},
			{"default",null,"set parameter.value.default to null on hidden input",true},
			{"default","abracadabra","set parameter.value.default to random string on hidden input",false},
			{"default",new Integer(25),"set parameter.value.default to null on hidden input",true},
	};

	public Object[][] dataTestParameterSemanticsOntology = {
			{"ontology","","set parameter.semantics.ontology to empty object",true},
			{"ontology",new Object(),"set parameter.semantics.ontology to empty object",true},
            {"ontology",Arrays.asList(new Object()),"set parameter.semantics.ontology to empty list of objects",true},
            {"ontology","abcdef","set parameter.semantics.ontology to valid string",true},
            {"ontology",Arrays.asList("abcdef","abcdef","abcdef"),"set parameter.semantics.ontology to valid list of strings",true},
            {"ontology",null,"set parameter.semantics.ontology to null",false},
	};
	public Object[][] dataTestParameterSemanticsFileTypes = {
			{"fileTypes",new Object(),"set parameter.semantics.fileTypes to empty array",true},
            {"fileTypes",null,"set parameter.semantics.fileTypes to null",true},
            {"fileTypes","","set parameter.semantics.fileTypes to empty string",true},
            {"fileTypes","abcdf","set parameter.semantics.fileTypes to valid string",true},
            {"fileTypes",Arrays.asList("abcdef","abcdef","abcdef"),"set parameter.semantics.fileTypes to list of strings",true},
            {"fileTypes",Arrays.asList(new Object()),"set parameter.semantics.fileTypes to list of objects",true},
    };

    public Object[][] dataTestParameterSemanticsMaxCardinality  = {
			{"maxCardinality","","set parameter.semantics.maxCardinality to empty string",true},
            {"maxCardinality",null,"set parameter.semantics.maxCardinality to null",true},
			{"maxCardinality","abc","set parameter.semantics.maxCardinality to non-numeric",true},
			{"maxCardinality","1","set parameter.semantics.maxCardinality to numeric string",true},
			{"maxCardinality",new Integer(1),"set parameter.semantics.maxCardinality to non-numeric",true},
	};

    public Object[][] dataTestParameterSemanticsMinCardinality  = {
			{"minCardinality","","set parameter.semantics.minCardinality to empty string",true},
            {"minCardinality",null,"set parameter.semantics.minCardinality to null",true},
			{"minCardinality","abc","set parameter.semantics.minCardinality to non-numeric",true},
			{"minCardinality","1","set parameter.semantics.minCardinality to numeric string",true},
			{"minCardinality",new Integer(1),"set parameter.semantics.minCardinality to non-numeric",true},
	};
    
    /************************************************************************
     * 				Software Output Test Data
     ************************************************************************/

    public Object[][] dataTestOutput = {
            {"id","","set output.id to empty string",true},
            {"id",null,"set output.id to null",true},
            {"value","{}","set output.value to empty array",true},
            {"value",null,"set output.value to null",true},
            {"details","{}","set output.details to empty array",true},
            {"details",null,"set output.details to null",true},
            {"semantics","{}","set output.semantics to empty array",true},
            {"semantics",null,"set output.semantics to null",true},
    };

    public  Object[][] dataTestOutputValue = {
            {"default","","set output.value.default to empty string",false},
            {"default",null,"set output.value.default to null",false},
            {"validator","","set output.value.validator to empty string",false},
            {"validator",null,"set output.value.validator to null",false},
            {"required","","set output.value.required to empty string",false},
            {"required",null,"set output.value.required to null",false},
            {"required","abcd","set output.value.required to \"abcd\"",false},
            {"required","true","set output.value.required to \"true\"",false},
            {"required","false","set output.value.required to \"false\"",false},
            {"required",true,"set output.value.required to true",false},
            {"required",false,"set output.value.required to false",false},
    };

    public Object[][] dataTestOutputDetails = {
            {"label","","set output.details.label to empty string",false},
            {"label",null,"set output.details.label to null",false},
            {"label","Sample label","set output.details.label to null",false},
            {"description","","set output.details.description to empty string",false},
            {"description",null,"set output.details.description to null",false},
            {"description","Sample description.","set output.details.description to null",false},
            {"visible","","set output.details.visible to empty string",false},
            {"visible",null,"set output.details.visible to null",false},
            {"visible",true,"set output.details.visible to true",false},
            {"visible",false,"set output.details.visible to false",false},
            {"visible","alpha","set output.details.visible to 'alpha'",false},
            {"visible",new Integer(1),"set output.details.visible to 1",false},
    };

    public Object[][] dataTestOutputSemantics = {
            {"ontology","{}","set ontology to empty array",true},
            {"ontology",null,"set ontology to null",false},
            {"minCardinality","","set minCardinality to empty string",true},
            {"minCardinality",null,"set minCardinality to null",false},
            {"maxCardinality","","set maxCardinality to empty string",true},
            {"maxCardinality",null,"set maxCardinality to null",false},
            {"fileTypes","{}","set fileTypes to empty array",true},
            {"fileTypes",null,"set fileTypes to null",false},
            {"fileTypes",arbitraryArray,"set fileTypes to arbitrary values",true},
    };


    private JSONTestDataUtil()
    {
    	init();
    }
    
    public static JSONTestDataUtil getInstance() 
    {
    	if (jsonTestData == null) {
    		jsonTestData = new JSONTestDataUtil();
    	}
    	
    	return jsonTestData;
    }
    
    private void init() {}
    
    public static <T> T[] concatAll(T[] first, T[]... rest) {
    	int totalLength = first.length;
    	for (T[] array : rest) {
    		totalLength += array.length;
    	}
    	T[] result = Arrays.copyOf(first, totalLength);
    	int offset = first.length;
    	for (T[] array : rest) {
    		System.arraycopy(array, 0, result, offset, array.length);
    		offset += array.length;
    	}
    	return result;
	}

/**
 *
 * @return ArrayList of json text strings each in an array, needed as input to the fromJSON method
 */
    public Object[][] genJsonTestMessages() {
        // String name, String changeValue, String message, String expected
        List<Object[]> holder = new ArrayList<Object[]>();

        Collections.addAll(holder, dataTestSoftwareFieldsValid);
        Collections.addAll(holder, dataTestSoftwareFieldsInvalid);
        Collections.addAll(holder, dataTestSoftwareFieldsEmpty);
        Collections.addAll(holder, dataTestSoftwareFieldsEmptyObject);
        Collections.addAll(holder, dataTestSoftwareFieldsNull);
        Collections.addAll(holder, dataTestSoftwareFieldsArray);

        Object[][] result = holder.toArray(new Object[holder.size()][]);
        return result;
    }


}




/*
            // create a new base JSONObject message from base text
            def jsonMsgObjTemp = getBaseJsonObjFromText()
            // apply the change (index 1) to the label(index 0) in  base json message
            jsonMsgObjTemp.put(valueSet{0},valueSet{1})
            // swap out the current valueSet{2} value with the new changed json message
            valueSet{2} = jsonMsgObjTemp
*/

/*
this.dataTest1.each { valueSet ->
    // create a new JsonTestDataObject from valueSet
    def testObject = new JsonTestDataObject(valueSet)
    // turn the array of values into a JsonTestDataObject
    valueSet = testObject
}
*/

//
//    protected Object[][] dataTestParameterStringTypesValue = {
//    		{SoftwareArgumentValueType.string,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",true},
//    		{SoftwareArgumentValueType.string,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",true},
//    		{SoftwareArgumentValueType.string,"default","abracadabra","set value.default with value.type string to valid string",false},
//		    {SoftwareArgumentValueType.string,"default","","set value.default with value.type string to empty string",false},
//		    {SoftwareArgumentValueType.string,"default",null,"set value.default with value.type string to valid string",false},
//		    {SoftwareArgumentValueType.string,"default",new Integer(4),"set value.default with value.type string to integer",true},
//		    {SoftwareArgumentValueType.string,"default",new Float(4.0),"set value.default with value.type string to float",true}
//    };
//
//    protected Object[][] dataTestParameterFileTypesValue = {
//    		{SoftwareArgumentValueType.file,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",true},
//    		{SoftwareArgumentValueType.file,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",true},
//    		{SoftwareArgumentValueType.file,"default","abracadabra","set value.default with value.type file to valid string",false},
//		    {SoftwareArgumentValueType.file,"default","","set value.default with value.type file to empty string",false},
//		    {SoftwareArgumentValueType.file,"default",null,"set value.default with value.type file to valid string",false},
//		    {SoftwareArgumentValueType.file,"default",new Integer(4),"set value.default with value.type file to integer",true},
//		    {SoftwareArgumentValueType.file,"default",new Float(4.0),"set value.default with value.type file to float",true},
//    };
//
//    protected Object[][] dataTestParameterBoolTypesValue = {
//    		{SoftwareArgumentValueType.bool,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",false},
//    		{SoftwareArgumentValueType.bool,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",false},
//    		{SoftwareArgumentValueType.bool,"default","","set value.default with value.type bool to empty string",false},
//		    {SoftwareArgumentValueType.bool,"default",null,"set value.default with value.type bool to valid string",false},
//		    {SoftwareArgumentValueType.bool,"default","abracadabra","set value.default with value.type bool to valid string",true},
//		    {SoftwareArgumentValueType.bool,"default",new Integer(4),"set value.default with value.type bool to integer",true},
//		    {SoftwareArgumentValueType.bool,"default",new Float(4.0),"set value.default with value.type bool to float",true},
//    };
//
//    protected Object[][] dataTestParameterNumericTypesValue = {
//    		{SoftwareArgumentValueType.number,"default",Boolean.TRUE,"set value.default with value.type number to boolean true",true},
//    		{SoftwareArgumentValueType.number,"default",Boolean.FALSE,"set value.default with value.type number to boolean false",true},
//    		{SoftwareArgumentValueType.number,"default","","set value.default with value.type number to empty string",false},
//		    {SoftwareArgumentValueType.number,"default",null,"set value.default with value.type number to valid string",false},
//		    {SoftwareArgumentValueType.number,"default","abracadabra","set value.default with value.type number to valid string",true},
//		    {SoftwareArgumentValueType.number,"default",new Integer(4),"set value.default with value.type number to integer",false},
//		    {SoftwareArgumentValueType.number,"default",new Float(4.0),"set value.default with value.type number to float",false},
//    };
//
//    protected Object[][] dataTestParameterIntegerTypesValue = {
//    		{SoftwareArgumentValueType.integer,"default",Boolean.TRUE,"set value.default with value.type integer to boolean true",true},
//    		{SoftwareArgumentValueType.integer,"default",Boolean.FALSE,"set value.default with value.type integer to boolean false",true},
//    		{SoftwareArgumentValueType.integer,"default","","set value.default with value.type integer to empty string",false},
//		    {SoftwareArgumentValueType.integer,"default",null,"set value.default with value.type integer to valid string",false},
//		    {SoftwareArgumentValueType.integer,"default","abracadabra","set value.default with value.type integer to valid string",true},
//		    {SoftwareArgumentValueType.integer,"default",new Integer(4),"set value.default with value.type integer to integer",false},
//		    {SoftwareArgumentValueType.integer,"default",new Float(4.0),"set value.default with value.type integer to float",true},
//    };
//
//    protected Object[][] dataTestParameterDecimalTypesValue = {
//    		{SoftwareArgumentValueType.decimal,"default",Boolean.TRUE,"set value.default with value.type decimal to boolean true",true},
//    		{SoftwareArgumentValueType.decimal,"default",Boolean.FALSE,"set value.default with value.type decimal to boolean false",true},
//    		{SoftwareArgumentValueType.decimal,"default","","set value.default with value.type decimal to empty string",false},
//		    {SoftwareArgumentValueType.decimal,"default",null,"set value.default with value.type decimal to valid string",false},
//		    {SoftwareArgumentValueType.decimal,"default","abracadabra","set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.decimal,"default",new Integer(4),"set value.default with value.type decimal to integer",true},
//		    {SoftwareArgumentValueType.decimal,"default",new Float(4.0),"set value.default with value.type decimal to float",false},
//    };
//
//    protected Object[][] dataTestParameterFlagTypesValue = {
//    		{SoftwareArgumentValueType.flag,"default",Boolean.TRUE,"set value.default with value.type decimal to boolean true",true},
//    		{SoftwareArgumentValueType.flag,"default",Boolean.FALSE,"set value.default with value.type decimal to boolean false",true},
//    		{SoftwareArgumentValueType.flag,"default","","set value.default with value.type decimal to empty string",true},
//		    {SoftwareArgumentValueType.flag,"default",null,"set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.flag,"default","abracadabra","set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.flag,"default",new Integer(4),"set value.default with value.type decimal to integer",true},
//		    {SoftwareArgumentValueType.flag,"default",new Float(4.0),"set value.default with value.type decimal to float",true},
//    };
//

/************************************************************************
 * 				Software Output Test Data
 ************************************************************************/


//    protected Object[][] dataTestOutput = {
//            {"id","","set output.id to empty string",true},
//            {"id",null,"set output.id to null",true},
//            {"value","{}","set output.value to empty array",true},
//            {"value",null,"set output.value to null",true},
//            {"details","{}","set output.details to empty array",true},
//            {"details",null,"set output.details to null",true},
//            {"semantics","{}","set output.semantics to empty array",true},
//            {"semantics",null,"set output.semantics to null",true},
//    };
//
//    protected  Object[][] dataTestOutputValue = {
//            {"default","","set output.value.default to empty string",true},
//            {"default",null,"set output.value.default to null",true},
//            {"validator","","set output.value.validator to empty string",true},
//            {"validator",null,"set output.value.validator to null",true},
//            {"required","","set output.value.required to empty string",true},
//            {"required",null,"set output.value.required to null",true},
//            {"required","abcd","set output.value.required to \"abcd\"",true},
//            {"required","true","set output.value.required to \"true\"",true},
//            {"required","false","set output.value.required to \"false\"",true},
//            {"required",true,"set output.value.required to true",false},
//            {"required",false,"set output.value.required to false",false},
//    };
//
//    protected Object[][] dataTestOutputDetails = {
//            {"label","","set output.details.label to empty string",true},
//            {"label",null,"set output.details.label to null",true},
//            {"label","Sample label","set output.details.label to null",true},
//            {"description","","set output.details.description to empty string",true},
//            {"description",null,"set output.details.description to null",true},
//            {"description","Sample description.","set output.details.description to null",true},
//            {"visible","","set output.details.visible to empty string",true},
//            {"visible",null,"set output.details.visible to null",true},
//            {"visible",true,"set output.details.visible to true",true},
//            {"visible",false,"set output.details.visible to false",true},
//            {"visible","alpha","set output.details.visible to 'alpha'",true},
//            {"visible",new Integer(1),"set output.details.visible to 1",true},
//    };
//
//    protected Object[][] dataTestOutputSemantics = {
//            {"ontology","{}","set ontology to empty array",true},
//            {"ontology",null,"set ontology to null",true},
//            {"minCardinality","","set minCardinality to empty string",true},
//            {"minCardinality",null,"set minCardinality to null",true},
//            {"maxCardinality","","set maxCardinality to empty string",true},
//            {"maxCardinality",null,"set maxCardinality to null",true},
//            {"fileTypes","{}","set fileTypes to empty array",true},
//            {"fileTypes",null,"set fileTypes to null",true},
//            {"fileTypes",arbitraryArray,"set fileTypes to arbitrary values",true},
//    };
