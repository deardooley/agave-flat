/**
 * 
 */
package org.iplantc.service.common.resource;

import java.util.Map;

import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;

/**
 * @author dooley
 * @param <T>
 *
 */
public interface Searchable<T extends AgaveResourceSearchFilter> extends Sortable<T> {

	/**
	 * Parses url query parameters for valid search terms
	 * @return
	 */
	public abstract Map<SearchTerm, Object> getQueryParameters();
}
