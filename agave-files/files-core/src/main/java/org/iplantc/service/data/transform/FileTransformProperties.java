package org.iplantc.service.data.transform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.util.ServiceUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
@SuppressWarnings({ "unchecked" })
public class FileTransformProperties {
	
	private static final Logger log = Logger.getLogger(FileTransformProperties.class);
	
	private List<FileTransform> transforms;
	
	private static XStream xstream = new XStream(new DomDriver());
	
	static { 
        xstream.alias("transform", FileTransform.class);
        xstream.alias("filter", FileTransformFilter.class);
        xstream.alias("decodingChain", FilterChain.class);
        xstream.alias("transforms", List.class);
        xstream.omitField(FileTransform.class, "enabled");
        xstream.omitField(FileTransformFilter.class, "enabled");
        xstream.addDefaultImplementation(java.util.Date.class, java.sql.Date.class);
        xstream.addDefaultImplementation(java.util.Date.class, java.sql.Time.class);
        xstream.addDefaultImplementation(java.util.Date.class, java.sql.Timestamp.class);
    }
	
	public FileTransformProperties() {
		
		//transforms = new FileTransformList();
		transforms = new ArrayList<FileTransform>();
		
		transforms.add(FileTransform.getDefault());
		
	}
	
	public List<FileTransform> getTransforms() throws TransformException {

		InputStream in = null;
		try {
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream("transforms.xml");
			//FileTransformList transformList = (FileTransformList)xstream.fromXML(in);
			transforms = (List<FileTransform>)xstream.fromXML(in);
			//if (!transformList.transforms.isEmpty()) {
			if (!transforms.isEmpty()) {
				//for (FileTransform transform: transformList.getTransforms()) {
				for (FileTransform transform: transforms) {
					// if the script file isn't present on disk, disable the transform
					boolean atleastonefilterexists = false;
					for (FileTransformFilter filter: transform.getEncodingChain().getFilters()) {
						File filterScript = new File(transform.getScriptFolder(), filter.getHandle());
						boolean filterExist = filterScript.exists();
						
						// if any filter exists, then set the flag to enable the whole transform
						if (filterExist) atleastonefilterexists = true;
						
						filter.setEnabled(filterExist);
					}

                    //Ignore Decoders for one-way transforms, such as HEAD, TAIL, etc
                    if (transform.getDecoders() != null) {
                        for (FilterChain decoder : transform.getDecoders()) {
                            for (FileTransformFilter filter : decoder.getFilters()) {
                                File filterScript = new File(transform.getScriptFolder(), filter.getHandle());
                                boolean filterExist = filterScript.exists();

                                // if any filter exists, then set the flag to enable the whole transform
                                if (filterExist) atleastonefilterexists = true;

                                filter.setEnabled(filterExist);
                            }
                        }
                    }

                    transform.setEnabled(atleastonefilterexists);
				}
			}
			
			return transforms;
		} catch (Exception e) {
			log.error("Unable to retrieve transforms.", e);
			throw new TransformException("Failed to read in transform file", e);
		} finally {
			if (in != null) try {in.close();} catch(IOException e) {}
		}
	}
	
	public List<String> getTransformIds() throws TransformException {
		List<String> names = new ArrayList<String>();
		for (FileTransform transform: getTransforms()) {
			if (transform.isEnabled()) {
				names.add(transform.getName());
			}
		}
		return names;
	}
	
	/**
	 * Returns a list of FileTransforms who have the given tag.
	 * 
	 * @param tag
	 * @return List of FileTransforms with the given tag
	 * @throws TransformException
	 */
	public List<FileTransform> getTransformsByTag(String tag) throws TransformException {
		if (!ServiceUtils.isValid(tag)) {
			throw new TransformException("Invalid transform tag");
		}
		List<FileTransform> fileTransforms = new ArrayList<FileTransform>();
		for (FileTransform transform: getTransforms()) {
			String[] tags = StringUtils.split(transform.getTags(),',');
			for (String ttag: tags) {
				if (ttag.trim().equalsIgnoreCase(tag)) {
					fileTransforms.add(transform);
					break;
				}
			}
		}
		return fileTransforms;
	}
	
	public FileTransform getTransform(String id) throws TransformException {
		
		if (!ServiceUtils.isValid(id)) {
			throw new TransformException("Invalid transform name");
		}
		
		for (FileTransform transform: getTransforms()) {
			if (transform.getId().equalsIgnoreCase(id)) {
				return transform;
			}
			else if (transform.getId().toLowerCase().startsWith("raw")) {
				if (StringUtils.startsWith(transform.getId().toLowerCase(), id.toLowerCase())) {
					return transform;
				}
			}
		}
		return null;
	}
	
	public void addTransform(FileTransform transform) {
		
		transforms.add(transform);
	}
	
	public void saveTransforms() throws TransformException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream("test_transform.xml");
			xstream.toXML(transforms, out);
		} catch (IOException e) {
			throw new TransformException("Failed to save transform file", e);
		} finally {
			try {
				out.close();
			} catch(IOException e) {}
		}
	}
	
}
