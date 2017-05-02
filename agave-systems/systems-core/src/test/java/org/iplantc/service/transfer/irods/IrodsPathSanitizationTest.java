/**
 * 
 */
package org.iplantc.service.transfer.irods;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathSanitizationTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"irods","sanitization", "broken","integration"})
public class IrodsPathSanitizationTest extends AbstractPathSanitizationTest {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.AbstractPathSanitizationTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods.example.com.json");
    }

}
