/**
 * 
 */
package org.iplantc.service.common.migration;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.iplantc.service.common.migration.utils.BackfillUtil;
import org.iplantc.service.common.migration.utils.ColumnUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;

/**
 * Backfills valid {@link AgaveUUID} into each row of the jobevents and transfertasks tables
 * and sets the columns as unique indexes.
 * 
 * @author dooley
 *
 */
public class V2_1_7_1__Backfill_jobevents_transferevents_fileevents_uuid implements JdbcMigration, MigrationChecksumProvider {

    @Override
    public Integer getChecksum() {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("SHA1");
            m.update(getClass().getSimpleName().getBytes());
            
            return new BigInteger(1, m.digest()).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new FlywayException("Unable to determine checksum for migration class");
        }
    }

    public void migrate(Connection connection) throws Exception {
    	connection.setAutoCommit(false);
    	
    	System.out.println("Starting to backfill jobevents table.");
    	BackfillUtil.backfillAgaveUUID(connection, "jobevents", UUIDType.JOB_EVENT);
    	System.out.println("Finished backfilling jobevents table.");
    	
    	
    	System.out.println("Starting to backfill transfertasks table.");
//    	BackfillUtil.backfillAgaveUUID(connection, "transfertasks", UUIDType.TRANSFER);
    	System.out.println("Finished backfilling transfertasks table.");
    	
    	System.out.println("Started adding jobevents uuid index.");
//    	ColumnUtil.addUniqueIndex(connection, "jobevents");
    	System.out.println("Finished adding jobevents uuid index.");
    	
//        ColumnUtil.addUniqueIndex(connection, "transfertasks");
        
    }
}
