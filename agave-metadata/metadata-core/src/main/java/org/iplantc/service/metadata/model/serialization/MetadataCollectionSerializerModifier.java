package org.iplantc.service.metadata.model.serialization;

import java.util.List;

import org.iplantc.service.metadata.model.MetadataItem;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/**
 * Adds HAL-JSON data to {@link MetadataItem} collection responses.
 * @author dooley
 *
 */
public class MetadataCollectionSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, 
                                                     BeanDescription beanDesc, 
                                                     List<BeanPropertyWriter> beanProperties) 
    {
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            if (writer.getName() == "_links") {
                beanProperties.set(i, new MetadataCollectionSupportWriter(writer));
            }
        }
        return beanProperties;
    }
}
