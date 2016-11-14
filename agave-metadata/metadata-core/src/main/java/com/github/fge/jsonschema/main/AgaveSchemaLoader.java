/*
 * Copyright (c) 2013, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.fge.jsonschema.main;

import static com.github.fge.jsonschema.messages.LoadingConfigurationMessages.NULL_SCHEMA;
import static com.github.fge.jsonschema.messages.RefProcessingMessages.URI_NOT_ABSOLUTE;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import net.jcip.annotations.ThreadSafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.cfg.LoadingConfiguration;
import com.github.fge.jsonschema.cfg.LoadingConfigurationBuilder;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.exceptions.unchecked.JsonReferenceError;
import com.github.fge.jsonschema.exceptions.unchecked.ProcessingError;
import com.github.fge.jsonschema.load.AgaveMetadataURIManager;
import com.github.fge.jsonschema.load.Dereferencing;
import com.github.fge.jsonschema.load.URIManager;
import com.github.fge.jsonschema.ref.JsonRef;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.tree.SchemaTree;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * JSON Schema loader
 *
 * <p>All schema registering and downloading is done through this class.</p>
 *
 * <p>Note that if the id of a schema is not absolute (that is, the URI itself
 * is absolute and it has no fragment part, or an empty fragment), then the
 * whole schema will be considered anonymous.</p>
 *
 */
@ThreadSafe
public final class AgaveSchemaLoader
{
    /**
     * The URI manager
     */
    private final AgaveMetadataURIManager manager;

    /**
     * The default namespace
     */
    private final JsonRef namespace;

    /**
     * Schema cache
     */
    private final LoadingCache<URI, JsonNode> cache;

    /**
     * Our dereferencing mode
     */
    private final Dereferencing dereferencing;

    /**
     * Create a new schema loader with a given loading configuration
     *
     * @param cfg the configuration
     * @see LoadingConfiguration
     * @see LoadingConfigurationBuilder
     */
    public AgaveSchemaLoader(final LoadingConfiguration cfg)
    {
        namespace = JsonRef.fromURI(cfg.getNamespace());
        dereferencing = cfg.getDereferencing();
        manager = new AgaveMetadataURIManager(cfg);
        cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<URI, JsonNode>()
            {
                @Override
                public JsonNode load(final URI key)
                    throws ProcessingException
                {
                    return manager.getContent(key);
                }
            });
        cache.putAll(cfg.getPreloadedSchemas());
    }

    /**
     * Create a new schema loader with the default loading configuration
     */
    public AgaveSchemaLoader()
    {
        this(LoadingConfiguration.byDefault());
    }

    /**
     * Create a new tree from a schema
     *
     * <p>Note that it will always create an "anonymous" tree, that is a tree
     * with an empty loading URI.</p>
     *
     * @param schema the schema
     * @return a new tree
     * @see Dereferencing#newTree(JsonNode)
     * @throws ProcessingError schema is null
     */
    public SchemaTree load(final JsonNode schema)
    {
        if (schema == null)
            throw new ProcessingError(new ProcessingMessage()
                .message(NULL_SCHEMA));
        return dereferencing.newTree(schema);
    }

    /**
     * Get a schema tree from the given URI
     *
     * <p>Note that if the URI is relative, it will be resolved against this
     * registry's namespace, if any.</p>
     *
     * @param uri the URI
     * @return a schema tree
     * @throws ProcessingException URI is not an absolute JSON reference, or
     * failed to dereference this URI
     * @throws JsonReferenceError URI is null
     */
    public SchemaTree get(final URI uri)
        throws ProcessingException
    {
        final JsonRef ref = namespace.resolve(JsonRef.fromURI(uri));

        final ProcessingMessage msg = new ProcessingMessage()
            .put("uri", ref);

        if (!ref.isAbsolute())
            throw new ProcessingException(msg.message(URI_NOT_ABSOLUTE));

        final URI realURI = ref.toURI();

        try {
            final JsonNode node = cache.get(realURI);
            return dereferencing.newTree(ref, node);
        } catch (ExecutionException e) {
            throw (ProcessingException) e.getCause();
        }
    }

    @Override
    public String toString()
    {
        return cache.toString();
    }
}
