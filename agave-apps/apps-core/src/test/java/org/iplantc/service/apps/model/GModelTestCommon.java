package org.iplantc.service.apps.model;

import org.iplantc.service.apps.model.JSONTestDataUtil;
import org.json.JSONObject;
import org.json.JSONException;
import org.testng.Assert;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 4/4/12
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class GModelTestCommon {
    public JSONObject jsonTreeInputs = null;
    public JSONObject jsonTreeParameters = null;
    public JSONObject jsonTreeOutputs = null;
    public JSONObject jsonTreeSoftwareBase = null;
    public JSONTestDataUtil jtd = JSONTestDataUtil.getInstance();


    public void setUp() throws Exception {
        jsonTreeInputs = jtd.getTestDataObject("src/test/resources/software/test_specific/baseInputsTestFragment.json");
        jsonTreeOutputs = jtd.getTestDataObject("src/test/resources/software/test_specific/baseOutputsTestFragment.json");
        jsonTreeParameters = jtd.getTestDataObject("src/test/resources/software/test_specific/baseParametersTestFragment.json");
        jsonTreeSoftwareBase =  jtd.getTestDataObject("src/test/resources/software/wca-iplant-condor.tacc.utexas.edu.json");

    }


    /**
     * This is a generic method for testing iplantc.service.model business objects for fromJSON() method calls
     *
     * @param object                A SoftwareInput, ..Output, ..Parameter etc object with a fromJSON method
     * @param jsonTree              The json object for test input
     * @param name                  Name of the json field to change
     * @param changeValue           The object to set the named field to
     * @param message               The message associated with this test
     * @param expectExceptionThrown The boolean value that states if an exception is expected to be thrown
     * @throws Exception
     */
    public void commonSoftwareFromJSON(Object object, JSONObject jsonTree, String name, Object changeValue, String message, boolean expectExceptionThrown) {
        boolean exceptionOccurred  = false;
        String exceptionMsg = message;

        try{
            Method m = object.getClass().getMethod("fromJSON",JSONObject.class);
            m.invoke(null,jsonTree);
        }catch (Exception cause) {
            exceptionOccurred = true;
            Throwable reason = cause.getCause();
            String causeName = reason.getClass().getName();
            exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\" field  "
                    + name + "\" = \"" + changeValue + "\"\n" + causeName +"  "+reason.getMessage();
        }
        Assert.assertEquals(exceptionOccurred,expectExceptionThrown,exceptionMsg);
    }


    protected JSONObject updateTestData(JSONObject jsonTree, String attribute, Object newValue)
    throws JSONException
    {
        if (newValue == null)
            jsonTree.put(attribute,  (String)null);
        else if (newValue instanceof Collection) {
            jsonTree.put(attribute, newValue);
        }
        else if (newValue instanceof String) {
            jsonTree.put(attribute, newValue);
        }
        else if (newValue instanceof Integer) {
            jsonTree.put(attribute, ( (Integer) newValue ).intValue());
        }
        else if (newValue instanceof Float) {
            jsonTree.put(attribute, ( (Float) newValue ).floatValue());
        }
        else if (newValue instanceof Object) {
            jsonTree.put(attribute, new JSONObject(newValue));
        }
        else {
            jsonTree.put(attribute, newValue);
        }

        return jsonTree;
    }

}
