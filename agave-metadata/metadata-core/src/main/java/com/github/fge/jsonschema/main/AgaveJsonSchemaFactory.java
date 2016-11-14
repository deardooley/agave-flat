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

import java.util.Map;

import net.jcip.annotations.Immutable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.fge.jsonschema.cfg.LoadingConfiguration;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.exceptions.unchecked.JsonReferenceError;
import com.github.fge.jsonschema.exceptions.unchecked.LoadingConfigurationError;
import com.github.fge.jsonschema.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.messages.JsonReferenceErrors;
import com.github.fge.jsonschema.messages.LoadingConfigurationMessages;
import com.github.fge.jsonschema.processing.Processor;
import com.github.fge.jsonschema.processing.ProcessorMap;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.jsonschema.processors.data.SchemaContext;
import com.github.fge.jsonschema.processors.data.ValidatorList;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.github.fge.jsonschema.processors.validation.AgaveValidationChain;
import com.github.fge.jsonschema.processors.validation.ValidationProcessor;
import com.github.fge.jsonschema.ref.JsonRef;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ReportProvider;
import com.github.fge.jsonschema.util.Frozen;
import com.google.common.base.Function;

/**
 * The main validator provider
 *
 * <p>From an instance of this factory, you can obtain the following:</p>
 *
 * <ul>
 *     <li>a {@link SyntaxValidator}, to validate schemas;</li>
 *     <li>a {@link JsonValidator}, to validate an instance against a schema;
 *     </li>
 *     <li>a {@link JsonSchema}, to validate instances against a fixed schema.
 *     </li>
 * </ul>
 *
 * @see JsonSchemaFactoryBuilder
 */
@Immutable
public final class AgaveJsonSchemaFactory
    implements Frozen<JsonSchemaFactoryBuilder>
{
    /*
     * Elements provided by the builder
     */
    final ReportProvider reportProvider;
    final LoadingConfiguration loadingCfg;
    final ValidationConfiguration validationCfg;

    /*
     * Generated elements
     */
    private final AgaveSchemaLoader loader;
    private final AgaveJsonValidator validator;
    private final SyntaxValidator syntaxValidator;

    /**
     * Return a default factory
     *
     * <p>This default factory has validators for both draft v4 and draft v3. It
     * defaults to draft v4.</p>
     *
     * @return a factory with default settings
     * @see JsonSchemaFactoryBuilder#JsonSchemaFactoryBuilder()
     */
    public static AgaveJsonSchemaFactory byDefault()
    {
    	return new AgaveJsonSchemaFactory(newBuilder());
    }

    /**
     * Return a factory builder
     *
     * @return a {@link JsonSchemaFactoryBuilder}
     */
    public static JsonSchemaFactoryBuilder newBuilder()
    {
        return new JsonSchemaFactoryBuilder();
    }

    /**
     * Package private constructor to build a factory out of a builder
     *
     * @param builder the builder
     * @see JsonSchemaFactoryBuilder#freeze()
     */
    AgaveJsonSchemaFactory(final JsonSchemaFactoryBuilder builder)
    {
        reportProvider = builder.reportProvider;
        loadingCfg = builder.loadingCfg;
        validationCfg = builder.validationCfg;

        loader = new AgaveSchemaLoader(loadingCfg);
        final Processor<SchemaContext, ValidatorList> processor
            = buildProcessor();
        validator = new AgaveJsonValidator(loader,
            new ValidationProcessor(processor), reportProvider);
        syntaxValidator = new SyntaxValidator(validationCfg);
    }

    /**
     * Return the main schema/instance validator provided by this factory
     *
     * @return a {@link AgaveJsonValidator}
     */
    public AgaveJsonValidator getValidator()
    {
        return validator;
    }

    /**
     * Return the syntax validator provided by this factory
     *
     * @return a {@link SyntaxValidator}
     */
    public SyntaxValidator getSyntaxValidator()
    {
        return syntaxValidator;
    }

    /**
     * Build an instance validator tied to a schema
     *
     * <p>Note that the validity of the schema is <b>not</b> checked. Use {@link
     * #getSyntaxValidator()} if you are not sure.</p>
     *
     * @param schema the schema
     * @return a {@link JsonSchema}
     * @throws ProcessingException schema is a {@link MissingNode}
     * @throws LoadingConfigurationError schema is null
     */
    public JsonSchema getJsonSchema(final JsonNode schema)
        throws ProcessingException
    {
        if (schema == null)
            throw new LoadingConfigurationError(new ProcessingMessage()
                .message(LoadingConfigurationMessages.NULL_SCHEMA));
        return validator.buildJsonSchema(schema, JsonPointer.empty());
    }

    /**
     * Build an instance validator tied to a subschema from a main schema
     *
     * <p>Note that the validity of the schema is <b>not</b> checked. Use {@link
     * #getSyntaxValidator()} if you are not sure.</p>
     *
     * @param schema the schema
     * @param ptr a JSON Pointer as a string
     * @return a {@link JsonSchema}
     * @throws ProcessingException {@code ptr} is not a valid JSON Pointer, or
     * resolving the pointer against the schema leads to a {@link MissingNode}
     * @throws LoadingConfigurationError schema is null
     * @throws JsonReferenceError pointer is null
     */
    public JsonSchema getJsonSchema(final JsonNode schema, final String ptr)
        throws ProcessingException
    {
        if (schema == null)
            throw new LoadingConfigurationError(new ProcessingMessage()
                .message(LoadingConfigurationMessages.NULL_SCHEMA));
        if (ptr == null)
            throw new JsonReferenceError(new ProcessingMessage()
                .message(JsonReferenceErrors.NULL_JSON_POINTER));
        return validator.buildJsonSchema(schema, new JsonPointer(ptr));
    }

    /**
     * Build an instance validator out of a schema loaded from a URI
     *
     * @param uri the URI
     * @return a {@link JsonSchema}
     * @throws ProcessingException failed to load from this URI
     * @throws JsonReferenceError URI is null
     */
    public JsonSchema getJsonSchema(final String uri)
        throws ProcessingException
    {
        if (uri == null)
            throw new JsonReferenceError(new ProcessingMessage()
                .message(JsonReferenceErrors.NULL_URI));
        return validator.buildJsonSchema(uri);
    }

    /**
     * Return the raw validation processor
     *
     * <p>This will allow you to chain the full validation processor with other
     * processors of your choice. Useful if, for instance, you wish to add post
     * checking which JSON Schema cannot do by itself.</p>
     *
     * @return the processor.
     */
    public Processor<FullData, FullData> getProcessor()
    {
        return validator.getProcessor();
    }

    /**
     * Return a thawed instance of that factory
     *
     * @return a {@link JsonSchemaFactoryBuilder}
     * @see JsonSchemaFactoryBuilder#JsonSchemaFactoryBuilder(JsonSchemaFactory)
     */
    @Override
    public JsonSchemaFactoryBuilder thaw()
    {
    	JsonSchemaFactoryBuilder builder = new JsonSchemaFactoryBuilder();
    	builder.setReportProvider(this.reportProvider);
    	builder.setLoadingConfiguration(this.loadingCfg);
    	builder.setValidationConfiguration(this.validationCfg);
    	return builder;
    }

    private Processor<SchemaContext, ValidatorList> buildProcessor()
    {
        final AgaveRefResolver resolver = new AgaveRefResolver(loader);
        final boolean useFormat = validationCfg.getUseFormat();

        final Map<JsonRef, Library> libraries = validationCfg.getLibraries();
        final Library defaultLibrary = validationCfg.getDefaultLibrary();
        final AgaveValidationChain defaultChain
            = new AgaveValidationChain(resolver, defaultLibrary, useFormat);
        ProcessorMap<JsonRef, SchemaContext, ValidatorList> map
            = new FullChain().setDefaultProcessor(defaultChain);

        JsonRef ref;
        AgaveValidationChain chain;

        for (final Map.Entry<JsonRef, Library> entry: libraries.entrySet()) {
            ref = entry.getKey();
            chain = new AgaveValidationChain(resolver, entry.getValue(), useFormat);
            map = map.addEntry(ref, chain);
        }

        return map.getProcessor();
    }

    private static final class FullChain
        extends ProcessorMap<JsonRef, SchemaContext, ValidatorList>
    {
        @Override
        protected Function<SchemaContext, JsonRef> f()
        {
            return new Function<SchemaContext, JsonRef>()
            {
                @Override
                public JsonRef apply(final SchemaContext input)
                {
                    return input.getSchema().getDollarSchema();
                }
            };
        }
    }
}
