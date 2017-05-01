package org.iplantc.service.apps.model;


import org.apache.commons.collections.CollectionUtils;
import org.iplantc.service.apps.model.SoftwareOutput;
import org.json.JSONArray;
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
public class GSoftwareOutputTest extends GModelTestCommon{
    SoftwareOutput output = new SoftwareOutput();

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

    }

    @DataProvider(name = "dataTestOutput")
    public Object[][] createData1() {
        Object[][] mydata = jtd.dataTestOutput ;
        return mydata;
    }

    @DataProvider(name = "dataTestOutputValue")
    public Object[][] createData2() {
        Object[][] mydata = jtd.dataTestOutputValue;
        return mydata;
    }

    @DataProvider(name = "dataTestOutputDetails")
    public Object[][] createData3() {
        Object[][] mydata = jtd.dataTestOutputDetails;
        return mydata;
    }

    @DataProvider(name = "dataTestOutputSemantics")
    public Object[][] createData4() {
        Object[][] mydata = jtd.dataTestOutputSemantics;
        return mydata;
    }

    @Test (dataProvider="dataTestOutput")
    public void outputFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeOutputs.toString());
        jsonTree.put(name,changeValue);
        super.commonSoftwareFromJSON(output,jsonTree, name,changeValue,message,expectExceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestOutputValue")
    public void outputValueFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeOutputs.toString());
        jsonTree.getJSONObject("value").put(name,changeValue);
        super.commonSoftwareFromJSON(output,jsonTree,name,changeValue,message,expectExceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestOutputDetails")
    public void outputDetailsFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeOutputs.toString());
        jsonTree.getJSONObject("details").put(name,changeValue);
        super.commonSoftwareFromJSON(output,jsonTree, name,changeValue,message,expectExceptionThrown);
    }

    @Test (groups="model", dataProvider="dataTestOutputSemantics")
    public void outputSemanticsFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) throws Exception {
        JSONObject jsonTree = new JSONObject(jsonTreeOutputs.toString());
        jsonTree.getJSONObject("semantics").put(name,changeValue);
        super.commonSoftwareFromJSON(output,jsonTree,name,changeValue,message,expectExceptionThrown);
    }
    
    @Test(groups="model")
    public void outputCloneCopiesEverything() 
    throws Exception 
    {
        try {
            // create a software object for the original output parameter's reference
            Software software = new Software();
            software.setId(new Long(1));
            
            // set fields to positive values to ensure we do not get false positives during clone.
            SoftwareInput output = new SoftwareInput();
            output.setId(new Long(1));
            output.setArgument("fooargument");
            output.setEnquote(true);
            output.setFileTypes("foo,bar,bat");
            output.setKey("fookey");
            output.setLabel("foolabel");
            output.setMaxCardinality(12);
            output.setMinCardinality(1);
            output.setOntology(new JSONArray().put("xs:string").put("xs:foo").toString());
            output.setOrder(6);
            output.setRepeatArgument(true);
            output.setRequired(false);
            output.setShowArgument(true);;
            output.setSoftware(software);
            output.setValidator(".*");
            output.setVisible(false);
            
            software.addInput(output);
            
            // create a new software object to assign during the clone
            Software clonedSoftware = new Software();
            clonedSoftware.setId(new Long(2));
            
            // run the clone
            SoftwareInput clonedParameter = output.clone(clonedSoftware);
            
            // validate every field was copied
            Assert.assertEquals(clonedParameter.getArgument(), output.getArgument(), "Argument was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getDefaultValueAsJsonArray().toString(), output.getDefaultValueAsJsonArray().toString(), "defaultValue attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getDescription(), output.getDescription(), "description attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.isEnquote(), output.isEnquote(), "enquote attribute was not cloned from one software output to another");
            Assert.assertNull(clonedParameter.getId(), "id attribute should not be cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getKey(), output.getKey(), "key attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getLabel(), output.getLabel(), "lable attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getMaxCardinality(), output.getMaxCardinality(), "maxCardinality attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getMinCardinality(), output.getMinCardinality(), "minCardinality attribute was not cloned from one software output to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedParameter.getOntologyAsList(), output.getOntologyAsList()), "ontology attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getOrder(), output.getOrder(), "order attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.isRepeatArgument(), output.isRepeatArgument(), "repeatArgument attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.isRequired(), output.isRequired(), "required attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.isShowArgument(), output.isShowArgument(), "showArgument attribute was not cloned from one software output to another");
            Assert.assertNotNull(clonedParameter.getSoftware(), "Sofware reference was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getSoftware().getId(), clonedSoftware.getId(), "Wrong software reference was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.isVisible(), output.isVisible(), "visible attribute was not cloned from one software output to another");
            Assert.assertEquals(clonedParameter.getValidator(), output.getValidator(), "Argument was not cloned from one software output to another");
            
            // do a sanity check by serializing the json as well
            Assert.assertEquals(output.toJSON(), clonedParameter.toJSON(), "cloned output serialized to different value than original output.");
            
        } catch (Exception e) {
            Assert.fail("Cloning software output should not throw exception",e);
        }
    }

}
