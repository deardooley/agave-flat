/**
 * 
 */
package org.iplantc.service.common.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Allows case insensitive deserialization of enums.
 * @author dooley
 *
 */
public class CaseInsensitiveEnumDeserializer  extends SimpleDeserializers {

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
        return createDeserializer((Class<Enum>) type);
    }

    private <T extends Enum<T>> JsonDeserializer<?> createDeserializer(Class<T> enumCls) {
        T[] enumValues = enumCls.getEnumConstants();
        HashMap<String, T> map = createEnumValuesMap(enumValues);
        return new EnumDeserializer(new EnumCaseInsensitiveResolver<T>(enumCls, enumValues, map));
    }

    private <T extends Enum<T>> HashMap<String, T> createEnumValuesMap(T[] enumValues) {
        HashMap<String, T> map = new HashMap<String, T>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            T e = enumValues[i];
            map.put(e.toString(), e);
        }
        return map;
    }

    public static class EnumCaseInsensitiveResolver<T extends Enum<T>> extends EnumResolver<T> {
        protected EnumCaseInsensitiveResolver(Class<T> enumClass, T[] enums, HashMap<String, T> map) {
            super(enumClass, enums, map);
        }

        @Override
        public T findEnum(String key) {
            for (Map.Entry<String, T> entry : _enumsById.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) { // magic line <--
                    return entry.getValue();
                }
            }
            return null;
        }
    }
}