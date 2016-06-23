package org.iplantc.service.metadata.model.serialization;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.model.MetadataItem;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

/**
 * @author dooley
 *
 */
public class MetadataResourceSupportWriter extends BeanPropertyWriter {
    BeanPropertyWriter _writer;
 
    public MetadataResourceSupportWriter(BeanPropertyWriter w) {
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
            
            gen.writeObjectFieldStart("permissions");
                gen.writeStringField("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE + "data/" + ((MetadataItem) bean).getUuid()) + "/permissions");
            gen.writeEndObject();
            
            gen.writeObjectFieldStart("profile");
                gen.writeStringField("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE + ((MetadataItem) bean).getOwner()));
            gen.writeEndObject();
            
            if (!((MetadataItem) bean).getAssociations().isEmpty()) {
                Entry<String, JsonNode> association = null;
                for (Iterator<Entry<String, JsonNode>> fields = ((MetadataItem) bean).getAssociations().getReferenceGroupMap().fields(); fields.hasNext(); association = fields.next()) {
                    gen.writeObjectField(association.getKey(), association.getValue());
                    gen.writeEndObject();
                }
            }
            
            if (StringUtils.isNotEmpty(((MetadataItem) bean).getSchemaId()))  
            {
                gen.writeObjectFieldStart(UUIDType.SCHEMA.name().toLowerCase());
                    gen.writeStringField("href", TenancyHelper.resolveURLToCurrentTenant(((MetadataItem) bean).getSchemaId()));
                gen.writeEndObject();
            }
        gen.writeEndObject();
    }
}