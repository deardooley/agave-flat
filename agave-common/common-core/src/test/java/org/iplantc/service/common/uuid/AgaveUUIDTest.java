package org.iplantc.service.common.uuid;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AgaveUUIDTest {
    private static final Logger log = Logger.getLogger(AgaveUUIDTest.class);
    
    @Test
    public void getUniqueId() throws InterruptedException {
        int count = 200000;
        List<String> uuids = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            AgaveUUID uuid = new AgaveUUID(UUIDType.FILE);
            String suuid = uuid.toString();
            Assert.assertFalse(uuids.contains(suuid), "Duplicate UUID " + suuid
                    + " was created.");
            // Thread.currentThread().sleep(10);
            uuids.add(suuid);
            System.out.println(suuid);
            log.debug(suuid);
        }
    }
}
