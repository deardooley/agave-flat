package org.iplantc.service.common.search;

import org.apache.commons.lang.StringUtils;

/**
 * Enumeration to handle the sort direction of search results.
 * 
 * @author dooley
 *
 */
public enum AgaveResourceResultOrdering {
	ASCENDING,
	ASC,
	ASCEND,
	DESCENDING,
	DESC,
	DESCEND;
	
	public boolean isAscending() {
		return this == ASCENDING || this == ASC || this == ASCEND;
	}
	
	public boolean isDescending() {
		return !isAscending();
	}
	
	/**
	 * Prints SQL equivalent of this enum ordering.
	 * 
	 * @return "ASC" if {@link #isAscending()}, "DESC" otherwise
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		return isAscending() ? "ASC" : "DESC";
	}
	
	/**
	 * Case-insensitive implementation of {@link #getValue(String)}.
	 * 
	 * @param val
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static AgaveResourceResultOrdering getCaseInsensitiveValue(String val) 
	throws IllegalArgumentException 
	{
		return AgaveResourceResultOrdering.valueOf(StringUtils.upperCase(val));
	}
}
