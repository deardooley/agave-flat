package org.iplantc.service.common.persistence;

import org.hibernate.dialect.MySQL5InnoDBDialect;

/**
 * Allows autogeneration of tables with utf8 support and 
 * @author dooley
 *
 */
public class BinaryMysql5Dialect extends MySQL5InnoDBDialect {
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8";
    }
}
