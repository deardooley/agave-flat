package org.iplantc.service.common.uuid;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups={"unit"})
public class AgaveUUIDTest {
    private static final Logger log = Logger.getLogger(AgaveUUIDTest.class);
    
    @Test
    public void getUniqueId() throws InterruptedException {
        int count = 200000;
        HashSet<String> uuids = new HashSet<String>();
        for (int i = 0; i < count; i++) {
            AgaveUUID uuid = new AgaveUUID(UUIDType.FILE);
            String suuid = uuid.toString();
            Assert.assertTrue(uuids.add(suuid), "Duplicate UUID " + suuid
                    + " was created.");
            if (i%10000 == 0) {
            	log.debug("["+i+"] UUID generated");
            }
        }
        log.debug(count + " uuid generated without conflict");
    }
}
