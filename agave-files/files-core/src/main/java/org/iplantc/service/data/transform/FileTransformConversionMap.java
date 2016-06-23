/**
 * 
 */
package org.iplantc.service.data.transform;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.iplantc.service.io.Settings;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author dooley
 *
 */
public class FileTransformConversionMap {
	private static final Logger log = LogManager.getLogger(FileTransformConversionMap.class);
	private static XStream xstream = new XStream(new DomDriver());
	
	static { 
        xstream.alias("conversionmap", Hashtable.class);
        xstream.alias("transforms", List.class);
        xstream.alias("transforms", List.class);
    }
	
	
	public static boolean isValid(String source, String dest) {
		List<ConversionEntry> conversionEntries = getConversionEntries();
		
		if (!conversionEntries.contains(source)) {
			return false;
		} else {
			return conversionEntries.get(conversionEntries.indexOf(source)).getDestinationTransforms().contains(dest);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static List<ConversionEntry> getConversionEntries() {
		FileInputStream in = null;
		try {
			in = new FileInputStream(Settings.TRANSFORM_CONVERSION_MAP_FILE_PATH);
			
			return (List<ConversionEntry>)xstream.fromXML(in);
		} catch (Exception e) {
			log.error("Failed to load transform conversion map",e);
			return new ArrayList<ConversionEntry>();
		}
	}
	
	class ConversionEntry {
		private String sourceTransform;
		private List<String> destinationTransforms;
		
		ConversionEntry() {
			destinationTransforms = new ArrayList<String>();
		}
		
		ConversionEntry(String sourceTransform, List<String> destinationTransforms) {
			this.sourceTransform = sourceTransform;
			this.destinationTransforms = destinationTransforms;
		}

		/**
		 * @return the sourceTransform
		 */
		public String getSourceTransform() {
			return sourceTransform;
		}

		/**
		 * @param sourceTransform the sourceTransform to set
		 */
		public void setSourceTransform(String sourceTransform) {
			this.sourceTransform = sourceTransform;
		}

		/**
		 * @return the destinationTransforms
		 */
		public List<String> getDestinationTransforms() {
			return destinationTransforms;
		}

		/**
		 * @param destinationTransforms the destinationTransforms to set
		 */
		public void setDestinationTransforms(List<String> destinationTransforms) {
			this.destinationTransforms = destinationTransforms;
		}
		
		public boolean equals(Object o) {
			if (o instanceof String) {
				return sourceTransform.equals((String)o);
			} else if (o instanceof ConversionEntry) {
				return sourceTransform.equals(((ConversionEntry)o).sourceTransform);
			} else {
				return false;
			}
		}
		
	}

}
