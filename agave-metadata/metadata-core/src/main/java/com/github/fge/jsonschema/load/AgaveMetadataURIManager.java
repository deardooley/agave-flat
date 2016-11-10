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

package com.github.fge.jsonschema.load;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.fge.jsonschema.cfg.LoadingConfiguration;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.util.JacksonUtils;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.HTTPSClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.AgaveUriRegex;
import org.iplantc.service.common.uri.AgaveUriUtil;
import org.iplantc.service.metadata.managers.MetadataSchemaPermissionManager;

import static com.github.fge.jsonschema.messages.RefProcessingMessages.*;

/**
 * Class to fetch JSON documents with custom lookups for Agave 
 * {@link MetadataSchemaItem} references. 
 *
 * <p>This uses a map of {@link URIDownloader} instances to fetch the contents
 * of a URI as an {@link InputStream}, then tries and turns this content into
 * JSON using an {@link ObjectMapper}.</p>
 *
 * <p>Normally, you will never use this class directly.</p>
 *
 * @see DefaultDownloadersDictionary
 * @see SchemaLoader
 */
public final class AgaveMetadataURIManager
{
    private static final ObjectReader READER = JacksonUtils.getReader();

    private final Map<String, URIDownloader> downloaders;

    private final Map<URI, URI> schemaRedirects;

    public AgaveMetadataURIManager()
    {
        this(LoadingConfiguration.byDefault());
    }

    public AgaveMetadataURIManager(final LoadingConfiguration cfg)
    {
        downloaders = cfg.getDownloaders().entries();
        schemaRedirects = cfg.getSchemaRedirects();
    }

    /**
     * Get the content at a given URI as a {@link com.fasterxml.jackson.databind.JsonNode}
     *
     * @param uri the URI
     * @return the content
     * @throws com.github.fge.jsonschema.exceptions.ProcessingException scheme is not registered, failed to get
     * content, or content is not JSON
     */
    public JsonNode getContent(final URI uri)
        throws ProcessingException
    {
        Preconditions.checkNotNull(uri, "null URI");

        final URI target = schemaRedirects.containsKey(uri)
            ? schemaRedirects.get(uri) : uri;

        final ProcessingMessage msg = new ProcessingMessage()
            .put("uri", uri);

        if (!target.isAbsolute())
            throw new ProcessingException(msg.message(URI_NOT_ABSOLUTE));

        final String scheme = target.getScheme();

        final URIDownloader downloader = downloaders.get(scheme);

        if (downloader == null)
            throw new ProcessingException(msg.message(UNHANDLED_SCHEME)
                .put("scheme", scheme));

        final InputStream in;

        try {
        	
        	String metadataSchemBaseUrl = TenancyHelper.resolveURLToCurrentTenant(target.toString());
        	String schemaRegex = "(?:" + metadataSchemBaseUrl.replaceAll("\\:", "\\\\:") + ")" + AgaveUriRegex.METADATA_SCHEMA_URI;
        	
        	Matcher matcher = Pattern.compile(schemaRegex, Pattern.CASE_INSENSITIVE).matcher(target.toString());
        	if (matcher.matches()) {
        		// String schemaUuid = matcher.group(1);
        		try {
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("Authorization", "Bearer " + TenancyHelper.getCurrentBearerToken());
					HTTPSClient client = new HTTPSClient(target.toString(), headers);
					String response = client.getText();
					
					if (StringUtils.isEmpty(response)) {
						throw new IOException("Empty response found when querying " + target.toString());
					}
					else {
						JsonNode json = READER.readTree(response);
						if (json.hasNonNull("result")) {
							if (json.get("result").hasNonNull("schema")) {
								return json.get("result").get("schema");
							} 
							else {
								throw new ProcessingException(msg.message(URI_NOT_JSON)
						                .put("parsingMessage", "No schema definition found in the response from " + target.toString()));
							}
						}
						else if (json.hasNonNull("message")) {
							throw new ProcessingException(msg.message(URI_NOT_JSON)
					                .put("parsingMessage", json.get("message").textValue()));
						}
						else {
							throw new ProcessingException(msg.message(URI_NOT_JSON)
					                .put("parsingMessage", "Empty response from the server when fetching " + target.toString()));
						}
					}
				} catch (Exception e) {
					throw new ProcessingException(msg.message(URI_NOT_JSON)
			                .put("parsingMessage", "Unable to fetch json schema reference from " + target.toString()));
				}
        	}
    		else {
	            in = downloader.fetch(target);
	            return READER.readTree(in);
        	}
        } catch (ProcessingException e) {
        	throw e;
        } catch (JsonProcessingException e) {
            throw new ProcessingException(msg.message(URI_NOT_JSON)
                .put("parsingMessage", e.getOriginalMessage()));
        } catch (IOException e) {
            throw new ProcessingException(msg.message(URI_IOERROR)
                .put("exceptionMessage", e.getMessage()));
        }
    }
}
