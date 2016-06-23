/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import javax.ws.rs.Path;

import org.iplantc.service.apps.model.SoftwareEvent;

/**
 * Returns a collection of history records for a {@link SoftwareEvent}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{softwareId}/history")
public class StandaloneSoftwareHistoryCollectionImpl extends SoftwareHistoryCollectionImpl {
    
    
}
