package org.iplantc.service.common.resource;

import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.restlet.resource.ResourceException;

public interface Sortable<T extends AgaveResourceSearchFilter> {

	/**
	 * Parses the result ordering from the query string.
	 * @return the result of {@link AgaveResourceResultOrdering#getCaseInsensitiveValue(String)} 
	 * if {@link order} was present in the query string, {@link AgaveResourceResultOrdering#ASCENDING} otherwise.
	 * @see {@link #getSortOrder(AgaveResourceResultOrdering)}
	 * @throws ResourceException if an invalid value was assigned to the {@link order} query parameter. 
	 */
	public abstract AgaveResourceResultOrdering getSortOrder()
			throws ResourceException;

	/**
	 * Parses the result ordering from the query string.
	 * @param defaultOrder the default ordering. {@link AgaveResourceResultOrdering#ASCENDING} if null
	 * @return the result of {@link AgaveResourceResultOrdering#getCaseInsensitiveValue(String)} 
	 * if {@link order} was present in the query string, {@link defaultOrder} otherwise.
	 * @throws ResourceException if an invalid value was assigned to the {@link order} query parameter. 
	 */
	public abstract AgaveResourceResultOrdering getSortOrder(
			AgaveResourceResultOrdering defaultOrder)
			throws ResourceException;

	/**
	 * Parses the {@link orderBy} url query parameter to find the user-specified term by which to order the 
	 * results.
	 * @param defaultValue the default {@link SearchTerm}.
	 * @return a {@link SearchTerm} mapping to a valid {@link AgaveResourceSearchFilter#getSearchTermMappings()}
	 * key if {@link orderBy} was present in the query string, null otherwise.
	 * @throws ResourceException if an invalid search value was assigned to the {@link orderBy} query parameter. 
	 */
	public abstract SearchTerm getSortOrderSearchTerm()
			throws ResourceException;
	

    /**
	 * Get the {@link AgaveResourceSearchFilter} instance for the given resource type.
	 * 
	 * @return
	 */
	public abstract T getAgaveResourceSearchFilter();

}