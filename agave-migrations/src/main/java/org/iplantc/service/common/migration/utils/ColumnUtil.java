/**
 * 
 */
package org.iplantc.service.common.migration.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Helper class to perform alternations on database columns.
 * 
 * @author dooley
 *
 */
public class ColumnUtil {
    
    /**
     * Makes the {@code uuid} column a unique index on the given table.
     *  
     * @param connection the current database connectino passed in by Flyway
     * @param tableName the table to udpate
     * @throws Exception
     */
    public static void addUniqueIndex(Connection connection, String tableName) throws Exception{
        PreparedStatement stmt = connection.prepareStatement("ALTER TABLE " + tableName + " ADD UNIQUE INDEX (`uuid`)");
        try {
            stmt.execute(); 
        } finally {
            stmt.close();
        }
    }
}
