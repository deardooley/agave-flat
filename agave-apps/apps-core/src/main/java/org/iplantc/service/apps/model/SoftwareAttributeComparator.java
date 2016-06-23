/**
 * 
 */
package org.iplantc.service.apps.model;

import java.util.Comparator;

/**
 * Interface to support consistent ordering of software attribute
 * collections via the order parameter.
 * @author dooley
 *
 */
public class SoftwareAttributeComparator<T extends SoftwareAttribute<T>> implements Comparator<T>
{
	@Override
	public int compare(T a, T b)
	{
		return a.compareTo(b);
	}
}
