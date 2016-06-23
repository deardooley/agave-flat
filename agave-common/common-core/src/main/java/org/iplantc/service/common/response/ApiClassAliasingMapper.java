package org.iplantc.service.common.response;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Forces a one-way, lowercase shortname serialization of class names by XStream.
 * This is not bi-directional, so don't attempt to use it that way.
 * @author dooley
 *
 */
public class ApiClassAliasingMapper extends ClassAliasingMapper {

    public ApiClassAliasingMapper(Mapper wrapped) {
        super(wrapped);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Class realClass(String elementName) {
        return super.realClass(elementName);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public String serializedClass(Class type) {
    	 String alias = super.serializedClass(type);
    	 if (type.getName().equalsIgnoreCase(alias)) 
    	 { 
    		alias = type.getSimpleName();
    		alias = StringUtils.removeEnd(alias, ".class");
    		alias = StringUtils.removeEnd(alias, "[]");
    		alias = alias.toLowerCase();
    	 }
    	 return alias;
    }
}