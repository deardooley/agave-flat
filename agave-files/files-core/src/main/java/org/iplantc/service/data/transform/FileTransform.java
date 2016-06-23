/**
 * 
 */
package org.iplantc.service.data.transform;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.io.Settings;
import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * @author dooley
 *
 */
public class FileTransform implements JSONString {
	
	// Decoders specifies a list of decodingChains
	private List<FilterChain> decoders = new ArrayList<FilterChain>();
	// The encodingChain defines what to do with a file at intake time.
	private FilterChain encodingChain = new FilterChain();
	// This is a lexically-unique key that identifies the data format
	private String name;
	// String in version format, 0.0.0..., that represents the version of the named format. 0.0 if none is specified.
	private String version = "0.0";
	// Placeholder for auto-detection based on file extension
	private String fileExtensions;
	// Regex to allow autodetect of the data format
	private String pattern;
	// Human-readable name for the format
	private String description;
	// A link to the spec or a description. Note that this is a URI so it could be an internal link
	private String descriptionURI;
	// A comma-separated list of additional semantic tags for this format. Used to facilitate search but not to define the namespace
	private String tags;
	
	// is this transform currently available
	private boolean enabled = false;
	
	public FileTransform() {}
	
	public FileTransform(String name, String version, String description, String pattern) {
		setName(name);
		setVersion(version);
		setDescription(getDescription());
		setPattern(pattern);
	}
	
	public FileTransform(String name, String version, String description, String pattern, FilterChain encodingChain, List<FilterChain> decoders) {
		setName(name);
		setVersion(version);
		setDescription(getDescription());
		setPattern(pattern);
		setEncodingChain(encodingChain);
		setDecoders(decoders);
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the decoders
	 */
	public List<FilterChain> getDecoders() {
		return decoders;
	}
	
	public FilterChain getDecoder(String id) {
		for(FilterChain decoder: decoders) {
			if (decoder.getId().equalsIgnoreCase(id)) {
				return decoder;
			}
		}
		return null;
	}

	/**
	 * @param decoders the decoders to set
	 */
	public void setDecoders(List<FilterChain> decoders) {
		this.decoders = decoders;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the fileExtensions
	 */
	public String getFileExtensions() {
		return fileExtensions;
	}

	/**
	 * @param fileExtensions the fileExtensions to set
	 */
	public void setFileExtensions(String fileExtensions) {
		this.fileExtensions = fileExtensions;
	}

	/**
	 * @return the descriptionURI
	 */
	public String getDescriptionURI() {
		return descriptionURI;
	}

	/**
	 * @param descriptionURI the descriptionURI to set
	 */
	public void setDescriptionURI(String descriptionURI) {
		this.descriptionURI = descriptionURI;
	}

	/**
	 * @return the tags
	 */
	public String getTags() {
		return tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}

	/**
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * @param pattern the pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
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
	 * @param encodingChain the encodingChain to set
	 */
	public void setEncodingChain(FilterChain encodingChain) {
		this.encodingChain = encodingChain;
	}

	/**
	 * @return the encodingChain
	 */
	public FilterChain getEncodingChain() {
		return encodingChain;
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

	public String getId() {
		return getName() + "-" + getVersion();
	}
	
	public String getScriptFolder() {
		return Settings.TRANSFORMS_FOLDER_PATH + "/" + name + "/" + (version == null? "0.0":version);
	}
	
	public String toString() {
		return getName() + "-" + getVersion();
	}
	
	public boolean equals(Object o) {
		if (o instanceof FileTransform) {
			return (((FileTransform)o).name.equals(name) && 
					((FileTransform)o).version.equals(version));
		}
		
		return false;
	}
	
	public static FileTransform getDefault() {
		
		FileTransform defaultTransform = new FileTransform("raw", "0.0",
				"Default transform leaving files unchanged.", 
				"*");
		
		//FileTransformFilter filter = new FileTransformFilter("clear", "pass through filter", "");
		FilterChain decoder = new FilterChain("raw", "0.0", "pass through filter chain");
		defaultTransform.getDecoders().add(decoder);
		defaultTransform.getEncodingChain().getFilters().add(new FileTransformFilter("clear", "pass through filter", ""));
		
		return defaultTransform;
	}

	@Override
	public String toJSONString() {
		String json = "";

		try {
			JSONWriter writer = new JSONStringer()
				.object()
					.key("name").value(getId())
					.key("description").value(getId())
					.key("enabled").value(getId())
					.key("encoder")
						.object()
							.key("name").value(getEncodingChain().getName())
							.key("description").value(getEncodingChain().getDescription())
						.endObject()
					.key("decoders").array();
					
			for (FilterChain decoder: getDecoders()) {
				writer.object()
					.key("name").value(decoder.getName())
					.key("description").value(decoder.getDescription())
				.endObject();
			}		
			json = writer.endArray().endObject().toString();
			
		} catch (JSONException e) {
			return "{\"name\": \"" + getId() + "\"}";
		}
		
		
		return json;
	}
}
