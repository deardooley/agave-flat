/**
 * 
 */
package org.iplantc.service.apps.model;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

/**
 * Interface to support consistent ordering of software attribute
 * collections via the order parameter.
 * @author dooley
 *
 */
public class SoftwarePermissionComparator implements Comparator<SoftwarePermission>
{
	@Override
	public int compare(SoftwarePermission a, SoftwarePermission b)
	{
		if (StringUtils.equals(a.getUsername(), b.getUsername())) {
			if (StringUtils.equals(a.getPermission().name(), b.getPermission().name())) {
				return a.getLastUpdated().compareTo(b.getLastUpdated());
			} else {
				return a.getPermission().compareTo(b.getPermission());
			}
		} else {
			return a.getUsername().compareTo(b.getUsername());
		}
	}

}
