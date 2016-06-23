/**
 * 
 */
package org.iplantc.service.apps.model;

import java.util.Comparator;
import java.util.Date;

/**
 * Generic interface to enforce sortability, etc on software attributes.
 * 
 * @author dooley
 *
 */
public interface SoftwareAttribute<T> extends Comparable<T>, Comparator<T>
{
	public Integer getOrder();
	
	public Date getCreated();
	
	public Long getId();
	
	public String getKey();
}
