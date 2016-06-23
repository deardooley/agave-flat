package org.iplantc.service.common.representation.jackson;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Representation based on the Jackson library. It can serialize and deserialize
 * automatically in JSON.
 * 
 * @see <a href="http://jackson.codehaus.org/">Jackson project</a>
 * @author Jerome Louvel
 * @param <T>
 *            The type to wrap.
 */
public class JacksonRepresentation<T> extends WriterRepresentation {

    /** The (parsed) object to format. */
    private T object;

    /** The object class to instantiate. */
    private Class<T> objectClass;

    /** The JSON representation to parse. */
    private Representation jsonRepresentation;

    /** The modifiable Jackson object mapper. */
    private ObjectMapper objectMapper;

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The target media type.
     * @param object
     *            The object to format.
     */
    @SuppressWarnings("unchecked")
    public JacksonRepresentation(MediaType mediaType, T object) {
        super(mediaType);
        this.object = object;
        this.objectClass = (Class<T>) ((object == null) ? null : object
                .getClass());
        this.jsonRepresentation = null;
        this.objectMapper = null;
    }

    /**
     * Constructor.
     * 
     * @param representation
     *            The representation to parse.
     */
    public JacksonRepresentation(Representation representation,
            Class<T> objectClass) {
        super(representation.getMediaType());
        this.object = null;
        this.objectClass = objectClass;
        this.jsonRepresentation = representation;
        this.objectMapper = null;
    }

    /**
     * Constructor.
     * 
     * @param object
     *            The object to format.
     */
    public JacksonRepresentation(T object) {
        this(MediaType.APPLICATION_JSON, object);
    }

    /**
     * Creates a Jackson object mapper based on a media type. By default, it
     * calls {@link ObjectMapper#ObjectMapper()}.
     * 
     * @return The Jackson object mapper.
     */
    protected ObjectMapper createObjectMapper() {
    	ObjectMapper mapper = null;
    	JsonFactory factory = new JsonFactory();
		factory.configure(Feature.AUTO_CLOSE_TARGET, false);
        
		mapper = new ObjectMapper(factory);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		
		return mapper;
    }

    /**
     * Returns the wrapped object, deserializing the representation with Jackson
     * if necessary.
     * 
     * @return The wrapped object.
     */
    public T getObject() {
        T result = null;

        if (this.object != null) {
            result = this.object;
        } else if (this.jsonRepresentation != null) {
            try {
                result = getObjectMapper().readValue(
                        this.jsonRepresentation.getStream(), this.objectClass);
            } catch (IOException e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to parse the object with Jackson.", e);
            }
        }

        return result;
    }

    /**
     * Returns the object class to instantiate.
     * 
     * @return The object class to instantiate.
     */
    public Class<T> getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the modifiable Jackson object mapper. Useful to customize
     * mappings.
     * 
     * @return The modifiable Jackson object mapper.
     */
    public ObjectMapper getObjectMapper() {
        if (this.objectMapper == null) {
            this.objectMapper = createObjectMapper();
        }

        return this.objectMapper;
    }

    /**
     * Sets the object to format.
     * 
     * @param object
     *            The object to format.
     */
    public void setObject(T object) {
        this.object = object;
    }

    /**
     *Sets the object class to instantiate.
     * 
     * @param objectClass
     *            The object class to instantiate.
     */
    public void setObjectClass(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * Sets the Jackson object mapper.
     * 
     * @param objectMapper
     *            The Jackson object mapper.
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if (jsonRepresentation != null) {
            jsonRepresentation.write(writer);
        } else if (object != null) {
            getObjectMapper().writeValue(writer, object);
        }
    }
}
