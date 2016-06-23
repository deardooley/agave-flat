package org.iplantc.service.common.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

import com.mchange.lang.ByteUtils;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

/**
* FreeMarker template loader based on a Context's client dispatcher. You can
* set an instance on a FreeMarker configuration via the
* {@link Configuration#setTemplateLoader(TemplateLoader)} method.
*
* @author Jerome Louvel
*/
public class ContextTemplateLoader implements TemplateLoader {

    /** The base URI. */
    private final String baseUri;

    /** The Restlet context. */
    private final Context context;

    /**
     * Constructor.
     *
     * @param context
     *            The Restlet context.
     * @param baseRef
     *            The base reference.
     */
    public ContextTemplateLoader(Context context, Reference baseRef) {
        this(context, baseRef.toString());
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Restlet context.
     * @param baseUri
     *            The base URI.
     */
    public ContextTemplateLoader(Context context, String baseUri) {
        this.context = context;
        this.baseUri = baseUri;
    }

    /**
     * Close the template source.
     *
     * @param templateSource
     *            The template source {@link Representation}.
     */
    public void closeTemplateSource(Object templateSource) throws IOException {
        if (templateSource instanceof Representation) {
            ((Representation) templateSource).release();
        }
    }

    /**
     * Finds the object that acts as the source of the template with the given
     * name.
     *
     * @param name
     *            The template name.
     * @return The template source {@link Representation}.
     */
    public Object findTemplateSource(String name) throws IOException {
        StringWriter writer = new StringWriter();
        InputStream in = null;
        try {
        	in = this.getClass().getClassLoader().getResourceAsStream(name);
        	if (in == null) {
        		return null;
        	} else {
	        	IOUtils.copy(in, writer, "utf-8");
	        	return new StringRepresentation(writer.toString());
        	}
        }
        finally {
        	try { in.close(); } catch (Exception e) {}
        	try { writer.close(); } catch (Exception e) {}
        }
//        if (getBaseUri().endsWith("/")) {
//            fullUri = getBaseUri() + name;
//        } else {
//            fullUri = getBaseUri() + "/" + name;
//        }
//
//        return (getContext() == null) ? null : getContext()
//                .getClientDispatcher().handle(new Request(Method.GET, fullUri))
//                .getEntity();
    }

    /**
     * Returns the base URI.
     *
     * @return The base URI.
     */
    private String getBaseUri() {
        return baseUri;
    }

    /**
     * Returns the Restlet context.
     *
     * @return The Restlet context.
     */
    private Context getContext() {
        return context;
    }

    /**
     * Returns the modification time.
     *
     * @param templateSource
     *            The template source {@link Representation}.
     * @return The modification time.
     */
    public long getLastModified(Object templateSource) {
        Date lastModified = ((Representation) templateSource)
                .getModificationDate();
        return (lastModified == null) ? -1L : lastModified.getTime();
    }

    /**
     * Returns the reader for the template source.
     *
     * @param templateSource
     *            The template source {@link Representation}.
     * @param encoding
     *            The reader encoding (currently ignored).
     */
    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        return ((Representation) templateSource).getReader();
    }

}