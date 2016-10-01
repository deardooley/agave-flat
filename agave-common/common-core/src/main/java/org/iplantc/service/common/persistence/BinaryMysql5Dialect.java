package org.iplantc.service.common.persistence;

import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * Allows autogeneration of tables with utf8 support and 
 * @author dooley
 *
 */
public class BinaryMysql5Dialect extends MySQL5InnoDBDialect {
	
	public BinaryMysql5Dialect() {
		super();
        
		/**
         * Function to evaluate regexp in MySQL
         */
		registerFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1 REGEXP ?2"));
    }
	
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8";
    }
}
