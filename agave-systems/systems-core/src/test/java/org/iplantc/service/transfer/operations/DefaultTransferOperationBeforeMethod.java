/**
 * 
 */
package org.iplantc.service.transfer.operations;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;

/**
 * Uses native {@link RemoteDataClient} operations to clean up after 
 * each {@link BaseRemoteDataClientOperationTest} subclass method
 * @author dooley
 *
 */
public class DefaultTransferOperationBeforeMethod implements TransferOperationBeforeMethod {

    /**
     * 
     */
    public DefaultTransferOperationBeforeMethod() {}

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.TransferOperationBeforeMethod#beforeMethod(org.iplantc.service.transfer.RemoteDataClient)
     */
    @Override
    public void setup(RemoteDataClient client) 
    throws Exception 
    {   
      try
      {   
          // auth client and ensure test directory is present
          client.authenticate();
          client.delete("");
      }
      catch (FileNotFoundException e) {
          // if it's not there, that's fine
      }
      catch (IOException | RemoteDataException e) {
          throw e;
      }
      
      try {   
          if (!client.mkdirs("")) {
              Assert.fail("System home directory " + client.resolvePath("") + " exists, but is not a directory.");
          }
      } 
      catch (IOException | RemoteDataException e) {
          throw e;
      }
      catch (Exception e) {
          Assert.fail("Failed to create home directory " + (client == null ? "" : client.resolvePath("")) + " before test method.", e);
      }
    }
}
