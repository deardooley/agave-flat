package org.iplantc.service.notification.providers.realtime.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean for serializing the channel messages properly for sending 
 * to pushpin.
 * 
 * @author dooley
 *
 */
public class RealtimeMessageItems {
    private List<ChannelMessage> items = new ArrayList<ChannelMessage>();
    
    public RealtimeMessageItems(List<ChannelMessage> items) {
        this.setItems(items);
    }

    /**
     * @return the items
     */
    public List<ChannelMessage> getItems() {
    	if (items == null) {
        	items = new ArrayList<ChannelMessage>();
        }
    	return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(List<ChannelMessage> items) {
    	getItems().clear();
    	if (items != null) {
    		getItems().addAll(items);
    	}
    }
}
