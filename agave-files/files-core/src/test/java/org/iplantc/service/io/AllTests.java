package org.iplantc.service.io;

import org.testng.TestListenerAdapter;
import org.testng.TestNG;
/**
 * Run all unit tests for TGFM middlewae service.
 *
 * @author Rion Dooley <dooley [at] cct [dot] lsu [dot] edu>
 */
public class AllTests {
	

//	public static Test suite() {
//
////		TestSuite suite = new TestSuite();
//		
////		// Persistence tests
////		suite.addTest( QueueTaskDaoTest.suite() );
////		suite.addTest( LogicalFileDaoTest.suite() );
////		
////		// Remote client tests
////		suite.addTest( FtpClientTest.suite() );
////		suite.addTest( HttpClientTest.suite() );
////		suite.addTest( HttpsClientTest.suite() );
////		suite.addTest( S3ClientTest.suite() );
////		suite.addTest( GridFtpClientTest.suite() );
////		suite.addTest( SftpClientTest.suite() );
////        
////		// StagingQueue tests
////		suite.addTest( StagingJobTest.suite() );
////		
//////		 TransformQueue tests
////		suite.addTest( TransformJobTest.suite() );
//		
//		// Transform analysis tests
////		suite.addTest( FileTransformPropertiesTest.suite() );
////		suite.addTest( FileAnalyzerTest.suite() );
//		
//		return suite;
//	}

	public static void main(String args[]) {
		TestListenerAdapter tla = new TestListenerAdapter();
		TestNG testng = new TestNG();
		testng.setTestClasses(new Class[] { AllTests.class });
		testng.addListener(tla);
		testng.run();
		
//		TestRunner.run( suite() );
	}
}
