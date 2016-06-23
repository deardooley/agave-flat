/**
 * 
 */
package org.iplantc.service.data.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.exceptions.InvalidFileTransformFilterException;
import org.json.JSONString;
import org.json.JSONStringer;

/**
 * @author dooley
 *
 */
public class FilterChain implements JSONString {
	
	// This is a lexically-unique key that identifies the data format. Name should be one of the supported formats
	private String name;
	// String in version format, 0.0.0..., that represents the version of the named format. 
	// The specific version for the conversion target must be defined
	private String version = "0.0";
	// Human-readable description. Keep it short. For exposure to external users
	private String description;
	// Anywhere from 1-n filters. These are evaluated in order and provide for export behavior
	private List<FileTransformFilter> filters;

	public FilterChain() {
		setFilters(new ArrayList<FileTransformFilter>());
	}
	
	public FilterChain(String name, String version, String description) {
		setName(name);
		setVersion(version);
		setDescription(description);
		setFilters(new ArrayList<FileTransformFilter>());
	}
	
	public FilterChain(String name, String version, String description, List<FileTransformFilter> filters) {
		setName(name);
		setVersion(version);
		setDescription(description);
		this.getFilters().addAll(filters);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return StringUtils.isEmpty(version) ? "0" : version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param filters the filters to set
	 */
	public void setFilters(List<FileTransformFilter> filters) {
		this.filters = filters;
	}

	/**
	 * @return the filters
	 */
	public List<FileTransformFilter> getFilters() {
		if (filters == null) {
			filters = new ArrayList<FileTransformFilter>();
		}
		return filters;
	}

	public String getId() {
		return getName() + "-" + getVersion();
	}
	
	public FileTransformFilter getFirstFilter() {
		for (FileTransformFilter filter: getFilters()) {
			if (filter.isEnabled()) return filter;
		}
		return null;
	}
	
	public FileTransformFilter getLastFilter() {
		for (int i=getFilters().size()-1; i>=0; i--) {
			FileTransformFilter filter = getFilters().get(i);
			if (filter.isEnabled()) return filter;
		}
		return null;
	}
	
	public FileTransformFilter getNextFilter(String name) throws InvalidFileTransformFilterException {
		for (FileTransformFilter filter: getFilters()) {
			if (filter.getName().equalsIgnoreCase(name)) {
				int handlerIndex = getFilters().indexOf(filter);
				if ((handlerIndex + 1) == getFilters().size()) {
					// this  is the last filter
					return null;
				} else {
					return getFilters().get(handlerIndex + 1);
				}
			}
		}
		
		throw new InvalidFileTransformFilterException("Transform " + name + " does not contain a filter with name " + name);
	}
	
	public FileTransformFilter getPreviousFilter(String name) throws InvalidFileTransformFilterException {
		for (int i=getFilters().size()-1; i>=0; i--) {
			FileTransformFilter filter = getFilters().get(i);
			if (filter.getName().equalsIgnoreCase(name)) {
				if ((i - 1) < 0) {
					// this  is the first filter
					return null;
				} else {
					return getFilters().get(i - 1);
				}
			}
		}
		
		throw new InvalidFileTransformFilterException("Transform " + name + " does not contain a filter with name " + name);
	}
	
	public FileTransformFilter getFilter(String name) {
		for (FileTransformFilter filter: getFilters()) {
			if (filter.getName().equalsIgnoreCase(name)) {
				return filter;
			}
		}
		return null;
	}
	
	public boolean equals(Object o) {
//		if (o instanceof FilterChain) {
//			if (((FilterChain)o).getFilters().size() != getFilters().size()) return false;
//			
//			for(FileTransformFilter filter: ((FilterChain)o).getFilters()) {
//				if (getFilters().contains(filter)) return false;
//			}
//			
//			return true;
//		}
		
//		return false;
		
		if (o instanceof FilterChain) {
			return (((FilterChain)o).name.equals(name) && 
					((FilterChain)o).version.equals(version));
		}
		
		return false;
	}

	@Override
	public String toJSONString() {
		String json = "";
		try {
			json = new JSONStringer()
				.object()
					.key("name").value(getId())
					.key("description").value(getDescription())
				.endObject().toString();
			
		} catch (Exception e) {
			return "{\"name\": \"" + getId() + "\"}";
		}
		return json;
	}
}
