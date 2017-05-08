/**
 * 
 */
package org.iplantc.service.systems.migrations;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.dao.BatchQueueDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.testng.annotations.Test;

/**
 * Migration class to assign valid agavea uuid to all entities of the given class.
 * 
 * @author dooley
 *
 */
@Test(groups={"broken", "integration"})
public class BatchQueueUUIDBackfill {

    /**
     * 
     */
    public BatchQueueUUIDBackfill() {
        
    }
    
    public void main(String[] args) {
        JndiSetup helper = new JndiSetup();
        helper.init();
        
        BatchQueueDao dao = new BatchQueueDao();
        try {
            for(BatchQueue q: dao.getAll()) {
                if (StringUtils.isEmpty(q.getUuid())) {
                    q.setUuid(new AgaveUUID(UUIDType.BATCH_QUEUE).toString());
                    dao.persist(q);
                }
            }
        }
        catch (Throwable t) {
            System.out.append("Failed to updated batch queues");
            t.printStackTrace();
        }
    }

}
