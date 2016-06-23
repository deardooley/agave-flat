/*Copyright (c) 2004,University of Illinois at Urbana-Champaign.  All rights reserved.
 * 
 * Created on May 30, 2006
 * 
 * Developed by: CCT, Center for Computation and Technology, 
 * 				NCSA, University of Illinois at Urbana-Champaign
 * 				OSC, Ohio Supercomputing Center
 * 				TACC, Texas Advanced Computing Center
 * 				UKy, University of Kentucky
 * 
 * https://www.gridchem.org/
 * 
 * Permission is hereby granted, free of charge, to any person 
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal with the Software without 
 * restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom 
 * the Software is furnished to do so, subject to the following conditions:
 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimers.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimers in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the names of Chemistry and Computational Biology Group , NCSA, 
 *    University of Illinois at Urbana-Champaign, nor the names of its contributors 
 *    may be used to endorse or promote products derived from this Software without 
 *    specific prior written permission.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  
 * IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS WITH THE SOFTWARE.
 */

package org.iplantc.service.apps.model.enumerations;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.util.ReflectHelper;

/**
 * A generic UserType that handles String-based JDK 5.0 Enums.
 * 
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class StringEnumUserType implements EnhancedUserType, ParameterizedType {

	@SuppressWarnings("rawtypes")
	private Class<Enum>	enumClass;

	public void setParameterValues(Properties parameters)
	{
		String enumClassName = parameters.getProperty("enumClassname");
		try
		{
			// log.debug("Finding class for name " + enumClassName);
			enumClass = ReflectHelper.classForName(enumClassName);
		}
		catch (ClassNotFoundException cnfe)
		{
			throw new HibernateException("Enum class not found", cnfe);
		}
	}

	public Class<?> returnedClass()
	{
		return enumClass;
	}

	@SuppressWarnings("deprecation")
	public int[] sqlTypes()
	{
		return new int[] { Hibernate.STRING.sqlType() };
	}

	public boolean isMutable()
	{
		return false;
	}

	public Object deepCopy(Object value)
	{
		return value;
	}

	@SuppressWarnings("rawtypes")
	public Serializable disassemble(Object value)
	{
		return (Enum) value;
	}

	public Object replace(Object original, Object target, Object owner)
	{
		return original;
	}

	public Object assemble(Serializable cached, Object owner)
	{
		return cached;
	}

	public boolean equals(Object x, Object y)
	{
		return x == y;
	}

	public int hashCode(Object x)
	{
		return x.hashCode();
	}

	public Object fromXMLString(String xmlValue)
	{
		return Enum.valueOf(enumClass, xmlValue);
	}

	@SuppressWarnings("rawtypes")
	public String objectToSQLString(Object value)
	{
		return '\'' + ( (Enum) value ).name() + '\'';
	}

	@SuppressWarnings("rawtypes")
	public String toXMLString(Object value)
	{
		return ( (Enum) value ).name();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws SQLException
	{
		String name = rs.getString(names[0]);
		return rs.wasNull() ? null : Enum.valueOf(enumClass, name);
	}

	@SuppressWarnings({ "deprecation", "rawtypes" })
	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws SQLException
	{
		if (value == null)
		{
			st.setNull(index, Hibernate.STRING.sqlType());
		}
		else
		{
			st.setString(index, ( (Enum) value ).name());
		}
	}

}
