package org.iplantc.service.notification.providers.realtime.model;

import java.util.List;

/**
 * Bean for serializing the channel messages properly for sending 
 * to pushpin.
 * 
 * @author dooley
 *
 */
public class RealtimeMessageItems {
    private List<ChannelMessage> items;
    
    public RealtimeMessageItems(List<ChannelMessage> items) {
        this.setItems(items);
    }

    /**
     * @return the items
     */
    public List<ChannelMessage> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(List<ChannelMessage> items) {
        this.items = items;
    }
}
