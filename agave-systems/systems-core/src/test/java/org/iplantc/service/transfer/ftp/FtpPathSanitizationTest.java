/**
 * 
 */
package org.iplantc.service.transfer.ftp;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathSanitizationTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"ftp","sanitization"})
public class FtpPathSanitizationTest extends AbstractPathSanitizationTest {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.AbstractPathSanitizationTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "ftp.example.com.json");
    }

}
