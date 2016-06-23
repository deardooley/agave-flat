package org.iplantc.service.data.transform;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FileTransformPropertiesConverter implements Converter {

    @SuppressWarnings("rawtypes")
	public boolean canConvert(Class clazz) {
            return clazz.equals(FileTransformProperties.class);
    }

    @SuppressWarnings("unchecked")
	public void marshal(Object value, HierarchicalStreamWriter writer,
                    MarshallingContext context) {
    	List<FileTransform> transforms = (List<FileTransform>) value;
        writer.startNode("transforms");
        for (FileTransform transform: transforms) {
        	context.convertAnother(transform);
        }
        writer.endNode();
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                    UnmarshallingContext context) {
    	List<FileTransform> transforms = new ArrayList<FileTransform>();
        reader.moveDown();
        while(reader.hasMoreChildren()) {
        	transforms.add((FileTransform)context.get(reader.getValue()));
        }
        reader.moveUp();
        return transforms;
    }

}