package org.iplantc.service.metadata.model.serialization;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.model.MetadataItem;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

/**
 * Appends the HAL+JSON object for {@link MetadataItem} 
 * @author dooley
 *
 */
public class MetadataCollectionSupportWriter extends BeanPropertyWriter {
    BeanPropertyWriter _writer;
 
    public MetadataCollectionSupportWriter(BeanPropertyWriter w) {
        super(w);
        _writer = w;
    }
 
    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, 
      SerializerProvider prov) throws Exception {
        
        gen.writeObjectFieldStart("_links");
            gen.writeObjectFieldStart("self");
                gen.writeStringField("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE + "data/" + ((MetadataItem) bean).getUuid()));
            gen.writeEndObject();
        gen.writeEndObject();
    }
}