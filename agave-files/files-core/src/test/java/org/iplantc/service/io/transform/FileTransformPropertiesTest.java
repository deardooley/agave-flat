package org.iplantc.service.io.transform;

import java.util.List;

import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.exceptions.TransformException;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;

public class FileTransformPropertiesTest extends BaseTestCase {

	private FileTransformProperties props;
	
	@BeforeMethod
	protected void setUp() throws Exception {
		props = new FileTransformProperties();
		//Settings.TRANSFORMS_FOLDER_PATH = testFileAnalysisDirectory.getAbsolutePath();
	}
	
	//@Test
	public void testTranformXML() throws Exception 
	{	
		FileTransform defaultTransform = new FileTransform("2raw", "1.0",
				"Test transform leaving files unchanged.", 
				"*");
		
		//FileTransformFilter filter = new FileTransformFilter("2clear", "2pass through filter", "");
		FilterChain decoder = new FilterChain("2raw", "1.0", "2pass through filter chain");
		defaultTransform.getDecoders().add(decoder);
		defaultTransform.getEncodingChain().getFilters().add(new FileTransformFilter("2clear", "3pass through  encoding filter", ""));
		
		props.addTransform(defaultTransform);
		props.saveTransforms();
	}

	//@Test
	public void testGetTransforms() {
		try {
			List<FileTransform> transforms = props.getTransforms();
			AssertJUnit.assertNotNull("Transforms may be empty, but not null", transforms);
		} catch (TransformException e) {
			Assert.fail("Reading transform file should not throw an exception");
		}
	}

	//@Test
	public void testGetTransformNames() {
		try {
			List<FileTransform> transforms = props.getTransforms();
			List<String> names = props.getTransformIds();
			
			for(FileTransform transform: transforms) {
				System.out.println(transform.getName());
				if (transform.isEnabled()) 
					AssertJUnit.assertTrue("Transform names list is missing the name " + 
						transform.getName(), 
						names.contains(transform.getName()));
			}
			
			for(String name: names) {
				boolean found = false;
				for(FileTransform transform: transforms) {
					if (transform.getName().equals(name)) {
						found = true;
						break;
					}
				}
				AssertJUnit.assertTrue("Transforms list is missing a transform by the name of " + name, found);
			}
			
		} catch (TransformException e) {
			Assert.fail("Reading transform names should not throw an exception");
		}
	}

	//@Test
	public void testGetTransformNull() {
		try {
			AssertJUnit.assertNull("Null transform name should throw exception", 
						props.getTransform(null));
			
		} catch (TransformException e) {
			// Null transform name should throw exception
		}
	}
	
	//@Test
	public void testGetTransformEmpty() {
		try {
			AssertJUnit.assertNull("Null transform name should throw exception", 
						props.getTransform(""));
		} catch (TransformException e) {
			// Null transform name should throw exception
		}
	}
	
	//@Test
	public void testGetTransform() {
		try {
			List<FileTransform> transforms = props.getTransforms();
			
			for (FileTransform transform: transforms) {
				System.out.println(transform.getId() + " " + transform.isEnabled());
				if (transform.isEnabled())
					AssertJUnit.assertNotNull("Failed to retrieve transform " + transform.getId(), 
						props.getTransform(transform.getId()));
			}
			
		} catch (TransformException e) {
			Assert.fail("Reading transform file should not throw an exception");
		}
	} 

}
