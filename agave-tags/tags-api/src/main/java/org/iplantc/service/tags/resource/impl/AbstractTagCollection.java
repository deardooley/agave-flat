package org.iplantc.service.tags.resource.impl;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.notification.search.NotificationSearchFilter;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Reference;

/**
 * @author dooley
 *
 */
public class AbstractTagCollection extends AbstractTagResource {

    /**
     * 
     */
    public AbstractTagCollection() {}
    
    /**
     * Parses url query looking for a search string
     * @return
     */
    protected Map<SearchTerm, Object> getQueryParameters() 
    {
        Request currentRequest = Request.getCurrent();
        Reference ref = currentRequest.getOriginalRef();
        Form form = ref.getQueryAsForm();
        if (form != null && !form.isEmpty()) {
            return new NotificationSearchFilter().filterCriteria(form.getValuesMap());
        } else {
            return new HashMap<SearchTerm, Object>();
        }
    }

}