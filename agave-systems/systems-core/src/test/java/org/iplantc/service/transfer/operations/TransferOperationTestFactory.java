/**
 * 
 */
package org.iplantc.service.transfer.operations;

import java.util.LinkedHashMap;
import java.util.List;

import org.iplantc.service.systems.model.RemoteSystem;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.annotations.Factory;

/**
 * Factory class to aggregate all test cases needed for
 * 
 * @author dooley
 *
 */
public abstract class TransferOperationTestFactory implements IMethodInterceptor {
    
    /**
     * Factory method to generate test cases for a single file protocol.
     * @return
     */
    @Factory
    public Object[] createInstances() {
        
        String systemJsonFilePath = getSystemJsonFilePath();
        DefaultTransferOperationAfterMethod teardown = new DefaultTransferOperationAfterMethod();
        DefaultTransferOperationBeforeMethod setup = new DefaultTransferOperationBeforeMethod();
        ForbiddenPathProvider forbiddenPathProvider = getForbiddenPathProvider();
        
        LinkedHashMap<String, Object> tests = new LinkedHashMap<String, Object>();
        
        tests.put("IsPermissionMirroringRequiredOperationTest", new IsPermissionMirroringRequiredOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("IsThirdPartyTransferSupportedOperationTest", new IsThirdPartyTransferSupportedOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("AuthenticateOperationTest", new AuthenticateOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("MkdirOperationTest", new MkdirOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("MkdirsOperationTest", new MkdirsOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("DoesExistOperationTest", new DoesExistOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("DeleteOperationTest", new DeleteOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("IsDirectoryOperationTest", new IsDirectoryOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("IsFileOperationTest", new IsFileOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("LengthOperationTest", new LengthOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("ListingOperationTest", new ListingOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("ChecksumOperationTest", new ChecksumOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("PutDirectoryOperationTest", new PutDirectoryOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("PutFileOperationTest", new PutFileOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("GetDirectoryOperationTest", new GetDirectoryOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("GetFileOperationTest", new GetFileOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("GetInputStreamOperationTest", new GetInputStreamOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("GetOutputStreamOperationTest", new GetOutputStreamOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("GetURLForPathOperationTest", new GetURLForPathOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("DoRenameOperationTest", new DoRenameOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("CopyDirectoryOperationTest", new CopyDirectoryOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("CopyFileOperationTest", new CopyFileOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
        tests.put("SyncOperationTest", new SyncOperationTest(systemJsonFilePath, teardown, setup, forbiddenPathProvider));
    
        LinkedHashMap<String, Object> adjustedTests = applyProtocolTestFilter(tests);
        
        return adjustedTests.values().toArray();
    }
    
    /**
     * Filter applied to the default list of factory tests common to all protocols.
     * You may override this method to alter the list of tests run for a given 
     * protocol.
     *  
     * @param tests
     * @return
     */
    protected LinkedHashMap<String, Object> applyProtocolTestFilter(LinkedHashMap<String, Object> tests) {
        return tests;
    }
    
    /**
     * Returns the path to the file containing the json description of the system to be 
     * used in the tests.
     * 
     * @return path to json description of {@link RemoteSystem}
     */
    public abstract String getSystemJsonFilePath();
    
    /**
     * Returns {@link ForbiddenPathProvider} to provide the appropriate forbidden paths 
     * for this protocol's transfer operations. 
     * @return {@link ForbiddenPathProvider} 
     */
    public abstract ForbiddenPathProvider getForbiddenPathProvider();

    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        return methods;
    }
}
