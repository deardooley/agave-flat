package org.iplantc.service.clients.util;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ClientUtils {

	public static boolean isNonEmptyString(JsonNode node) throws JSONException, JsonProcessingException, IOException {
		return node != null && (node instanceof TextNode) && !StringUtils.isEmpty(((TextNode)node).asText());
	}
	
	public static boolean isValidString(JsonNode node) throws JSONException, JsonProcessingException, IOException {
		return node != null && (node instanceof TextNode) && ((TextNode)node).asText() != null;
	}
	
	public static boolean isValidEmailAddress(String value)
	{
		if (StringUtils.isEmpty(value)) return false;
		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}

	public static boolean isValidPhoneNumber(String value)
	{
		if (StringUtils.isEmpty(value)) return false;
		String expression = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$";
		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
}
