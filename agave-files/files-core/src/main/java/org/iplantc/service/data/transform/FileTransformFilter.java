/**
 * 
 */
package org.iplantc.service.data.transform;

import org.json.JSONString;

/**
 * @author dooley
 *
 */
public class FileTransformFilter implements JSONString {

	private String name;
	private String handle;
	private String description;
	private boolean useOriginalFile = true;
	private boolean enabled = true;
	
	public FileTransformFilter() {}
	
	public FileTransformFilter(String name, String description, String handle) {
		setName(name);
		setHandle(handle);
		setDescription(description);
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
	 * @param handle the handle to set
	 */
	public void setHandle(String handle) {
		this.handle = handle;
	}

	/**
	 * @return the handle
	 */
	public String getHandle() {
		return handle;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param useOriginalFile the useOriginalFile to set
	 */
	public void setUseOriginalFile(boolean useOriginalFile) {
		this.useOriginalFile = useOriginalFile;
	}

	/**
	 * @return the useOriginalFile
	 */
	public boolean isUseOriginalFile() {
		return useOriginalFile;
	}

	public String toString() {
		return name + (isEnabled()? "" : "[disabled]") + " : " + handle + " : " + description;
	}
	
	public boolean equals(Object o) {
		if (o instanceof FileTransformFilter) {
			return (name.equals(((FileTransformFilter)o).getName()));
		}
		
		return false;
	}

	@Override
	public String toJSONString() {
		// TODO Auto-generated method stub
		return null;
	}
}