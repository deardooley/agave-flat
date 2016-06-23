/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iplantc.service.apps.model.enumerations;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.type.IntegerType;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class GenericEnumUserType implements UserType, ParameterizedType {

	@SuppressWarnings("rawtypes")
	private Class<? extends Enum>	enumClass;
	private Class<?>				identifierType;
	private Method					identifierMethod;
	private Method					valueOfMethod;
	private static final String		defaultIdentifierMethodName	= "name";
	private static final String		defaultValueOfMethodName	= "valueOf";
	private IntegerType				type;
	private int[]					sqlTypes;

	public void setParameterValues(Properties parameters)
	{
		String enumClassName = parameters.getProperty("enumClass");
		try
		{
			enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
		}
		catch (ClassNotFoundException exception)
		{
			throw new HibernateException("Enum class not found", exception);
		}

		String identifierMethodName = parameters.getProperty(
				"identifierMethod", defaultIdentifierMethodName);

		try
		{
			identifierMethod = enumClass.getMethod(identifierMethodName,
					new Class[0]);
			identifierType = identifierMethod.getReturnType();
		}
		catch (Exception exception)
		{
			throw new HibernateException("Failed to optain identifier method",
					exception);
		}

		TypeResolver tr = new TypeResolver();
		type = (IntegerType) tr.basic(identifierType.getName());
		if (type == null) { throw new HibernateException(
				"Unsupported identifier type " + identifierType.getName()); }
		sqlTypes = new int[] { type.sqlType() };

		String valueOfMethodName = parameters.getProperty("valueOfMethod",
				defaultValueOfMethodName);

		try
		{
			valueOfMethod = enumClass.getMethod(valueOfMethodName,
					new Class[] { identifierType });
		}
		catch (Exception exception)
		{
			throw new HibernateException("Failed to optain valueOf method",
					exception);
		}
	}

	public Class<?> returnedClass()
	{
		return enumClass;
	}

	@SuppressWarnings("deprecation")
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException
	{
		Object identifier = type.get(rs, names[0]);
		try
		{
			return valueOfMethod.invoke(enumClass, new Object[] { identifier });
		}
		catch (Exception exception)
		{
			throw new HibernateException("Exception while"
					+ " invoking valueOfMethod of enumeration class: ",
					exception);
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException
	{
		try
		{
			Object identifier = value != null ? identifierMethod.invoke(value,
					new Object[0]) : null;
			st.setObject(index, identifier);
		}
		catch (Exception exception)
		{
			throw new HibernateException("Exception while"
					+ " invoking identifierMethod of enumeration class: ",
					exception);

		}
	}

	public int[] sqlTypes()
	{
		return sqlTypes;
		// There was a logical bug within the set-up phase of any user type
		// I reported the issue and it got instantly solved (Thanks again
		// Garvin!)
		// But it might still exist in your Hibernate version. So if you are
		// facing any null-pointer exceptions, use the return statement below.
		// Note: INTEGER works even for String based mappings...
		// return new int [] {Types.INTEGER};
	}

	public Object assemble(Serializable cached, Object owner)
			throws HibernateException
	{
		return cached;
	}

	public Object deepCopy(Object value) throws HibernateException
	{
		return value;
	}

	public Serializable disassemble(Object value) throws HibernateException
	{
		return (Serializable) value;
	}

	public boolean equals(Object x, Object y) throws HibernateException
	{
		return x == y;
	}

	public int hashCode(Object x) throws HibernateException
	{
		return x.hashCode();
	}

	public boolean isMutable()
	{
		return false;
	}

	public Object replace(Object original, Object target, Object owner)
			throws HibernateException
	{
		return original;
	}
}