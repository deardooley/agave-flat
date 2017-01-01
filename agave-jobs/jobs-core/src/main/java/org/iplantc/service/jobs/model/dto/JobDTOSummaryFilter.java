package org.iplantc.service.jobs.model.dto;

import org.iplantc.service.jobs.model.enumerations.JobStatusType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
public class JobDTOSummaryFilter extends SimpleBeanPropertyFilter {

    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return true;
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return true;
    }

    @Override
    public void serializeAsField(Object pojo,
                                 JsonGenerator jgen,
                                 SerializerProvider provider,
                                 PropertyWriter writer) throws Exception {
        if (pojo instanceof JobDTO) {
        	if (!((JobDTO)pojo).isArchive_output() && 
        			("archiveSystem".equals(writer.getName()) || "archivePath".equals(writer.getName()))) {
        		writer.serializeAsOmittedField(pojo, jgen, provider);
        	}
        	else if ("message".equals(writer.getName()) && !JobStatusType.FAILED.name().equals(((JobDTO)pojo).getStatus())) {
        		writer.serializeAsOmittedField(pojo, jgen, provider);
        	}
        	else {
        		super.serializeAsField(pojo, jgen, provider, writer);
        	}
        } else {
            super.serializeAsField(pojo, jgen, provider, writer);
        }
    }
}