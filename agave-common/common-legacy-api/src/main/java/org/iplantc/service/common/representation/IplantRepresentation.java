/**
 * 
 */
package org.iplantc.service.common.representation;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.util.JsonPropertyFilter;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;

import ch.mfrey.jackson.antpathfilter.Jackson2Helper;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Lf2SpacesIndenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * @author dooley
 *
 */
public abstract class IplantRepresentation extends StringRepresentation {
	private Boolean prettyPrint = null;
	
	/**
	 * Wraps the serialized body returned from the primary route in a standard
	 * JSON object. Optionally pretty prints the response. If a naked response
	 * is requested, the wrapping is bypassed.
	 * 
	 * @param jsonArray
	 */
	protected IplantRepresentation(String status, String message, String json) {
		super("", MediaType.APPLICATION_JSON, null, CharacterSet.UTF_8);

		try {
			DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
			ObjectMapper mapper = new ObjectMapper();
			if (isNaked()) {
				if (Response.getCurrent().getStatus() == Status.SUCCESS_NO_CONTENT
						|| StringUtils.isEmpty(json)) {
					setText(null);
				} else {
					if (isPrettyPrint()) {
						setText(mapper.writer(pp).writeValueAsString(filterOutput(mapper.readTree(json))));
					} else {
						setText(filterOutput(mapper.readTree(json)).toString());
					}
				}
			} else {
				
				ObjectNode jsonWrapper = mapper.createObjectNode()
						.put("status", status)
						.put("message", message)
						.put("version", Settings.SERVICE_VERSION);
				
				if (!StringUtils.isEmpty(json)) {
					jsonWrapper.set("result", filterOutput(mapper.readTree(json)));
				}

				if (isPrettyPrint()) {
					setText(mapper.writer(pp).writeValueAsString(jsonWrapper));
				} else {
					setText(jsonWrapper.toString());
				}
			}
		} catch (Exception e) {
			if (isNaked()) {
				setText(json);
			} else {
				status = status.replaceAll("\"", "\\\"");

				if (message == null) {
					message = "";
				} else {
					message = message.replaceAll("\"", "\\\"");
				}

				StringBuilder builder = new StringBuilder();
				builder.append("{\"status\":\"" + status + "\",");
				builder.append("\"message\":\"" + message + "\",");
				builder.append("\"version\":\"" + Settings.SERVICE_VERSION
						+ "\",");
				builder.append("\"result\":" + json + "}");
				setText(builder.toString());
			}
		}
	}

	/**
	 * @return the prettyPrint
	 */
	public Boolean isPrettyPrint() {
		if (prettyPrint == null) {
			Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
			prettyPrint = Boolean.valueOf(form.getFirstValue("pretty"));
		}

		return prettyPrint;
	}

	/**
	 * @param prettyPrint
	 *            the prettyPrint to set
	 */
	public void setPrettyPrint(Boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	/**
	 * Returns true if the {@code naked} url query parameter was truthy. This
	 * will cause only the result to be returned to the client, excluding the
	 * traditional response object fields.
	 * 
	 * @return true if {@code naked} url query parameter was set to a truthy
	 *         value
	 */
	public boolean isNaked() {
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
		return Boolean.valueOf(form.getFirstValue("naked"));
	}

	public JsonNode filterOutput(JsonNode jsonNode) throws IOException {
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
		String[] sFilters = StringUtils
				.split(form.getFirstValue("filter"), ",");

		try {
			return new JsonPropertyFilter().getFilteredContent(jsonNode, sFilters);
		} catch (Exception e) {
//			ObjectMapper mapper = new ObjectMapper();
//
//			if (prettyPrint) {
//				DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
//				pp.indentArraysWith(new Lf2SpacesIndenter());
//
//				return mapper.writer(pp).writeValueAsString(json);
//			} else {
//				return mapper.writeValueAsString(json);
//			}
			return jsonNode;

		}
	}
	
	/**
	 * Returns the response filter fields provided in the query header.
	 * @return
	 */
	public String[] getJsonPathFilters() {
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
		String sFilters = form.getFirstValue("filter");
		if (StringUtils.isEmpty(sFilters)) {
			return new String[]{};
		}
		else {
			Splitter splitter = Splitter.on(CharMatcher.anyOf(",")).trimResults()
			            .omitEmptyStrings();
			Iterable<String> splitFilters = splitter.split(sFilters);
			if (splitFilters == null || !splitFilters.iterator().hasNext()) {
				return new String[]{};
			} else {
				return Iterables.toArray(splitFilters, String.class);
			}
		}
	}

//	public JsonNode filterOutput(JsonNode nakedJsonResponse) throws IOException {
//		String[] filters = getJsonPathFilters();
//		try {
//			return new JsonPropertyFilter().getFilteredContent(
//					nakedJsonResponse, filters);
//		} catch (Exception e) {
//			return nakedJsonResponse;
//		}
//	}
}