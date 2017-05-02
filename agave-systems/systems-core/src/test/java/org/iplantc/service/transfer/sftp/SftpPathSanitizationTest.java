/**
 * 
 */
package org.iplantc.service.transfer.sftp;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathSanitizationTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"sftp","sanitization","broken","integration"})
public class SftpPathSanitizationTest extends AbstractPathSanitizationTest {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.AbstractPathSanitizationTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-password.example.com.json");
    }

}
