package org.iplantc.service.profile.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

public class ServiceUtils {

	public static boolean isValid(String val) {
		return (val != null && !val.equals(""));
	}

	public static boolean isValidString(JSONObject json, String attribute) throws JSONException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		TreeNode node = mapper.readTree(json.toString()).get(attribute);
		return node != null && (node instanceof TextNode) && ((TextNode)node).asText() != null;
	}
	
	public static boolean isNonEmptyString(JSONObject json, String attribute) throws JSONException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		TreeNode node = mapper.readTree(json.toString()).get(attribute);
		return node != null && (node instanceof TextNode) && !StringUtils.isEmpty(((TextNode)node).asText());
	}
	
	public static boolean isValidEmailAddress(String value)
	{
		if (!isValid(value))
			return false;
		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}

	public static boolean isValidPhoneNumber(String value)
	{
		if (!isValid(value))
			return false;
		String expression = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$";
		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	/**
	 * Formats a 10 digit phone number into (###) ###-#### format
	 * 
	 * @param phone
	 * @return formatted phone number string
	 */
	public static String formatPhoneNumber(String phone) 
	{	
		if (StringUtils.isEmpty(phone)) { 
			return null;
		}
		else 
		{
			phone = phone.replaceAll("[^\\d.]", "");
			return String.format("(%s) %s-%s", 
					phone.substring(0, 3), 
					phone.substring(3, 6), 
					phone.substring(6, 10));
		}
	}
	
	public static boolean isAdmin(String username)
	{	
		if (TenancyHelper.isTenantAdmin()) return true;
		
		InputStream stream = ServiceUtils.class.getClassLoader().getResourceAsStream("trusted_admins.txt");
		try
		{
			String trustedUserList = IOUtils.toString(stream, "UTF-8");
			if (isValid(trustedUserList)) {
				for(String user: trustedUserList.split(",")) {
					if (username.equalsIgnoreCase(user.trim())) {
						return true;
					}
				}
				return false;
			} else {
				return false;
			}
		}
		catch (IOException e)
		{
			 //log.error("Failed to locate trusted user file");
			return false;
		}
	}

}
