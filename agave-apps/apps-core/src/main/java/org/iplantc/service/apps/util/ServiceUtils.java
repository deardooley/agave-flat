/**
 * 
 */
package org.iplantc.service.apps.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.profile.model.Address;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.stevesoft.pat.Regex;

/**
 * @author dooley
 * 
 */
public class ServiceUtils {

	@SuppressWarnings("unused")
	public static String exec(String command) throws IOException
	{
		String output = "";

		InputStream in = Runtime.getRuntime().exec(command).getInputStream();

		byte[] b = new byte[512];
		int len = 0;
		while ( ( len = in.read(b) ) >= 0)
		{
			output += new String(b).trim();
		}
		b = null;
		return output;
	}

	public static String getUsernameFromDn(String dn)
	{
		return dn.substring(dn.indexOf("=") + 1, dn.indexOf(",")).trim();
	}

	public static boolean isValid(String value)
	{
		return ( value != null ) && !value.equals("");
	}

	public static boolean isValid(File value)
	{
		return ( value != null ) && value.exists();
	}

	public static boolean isValid(Collection<?> value)
	{
		return ( value != null ) && ( !value.isEmpty() );
	}

	public static boolean isValid(Map<?,?> value)
	{
		return ( value != null ) && ( value.size() > 0 );
	}

	public static boolean isValid(Calendar value)
	{
		return value != null;
	}

	public static boolean isValid(Date value)
	{
		return value != null;
	}

	public static boolean isValid(Long value)
	{
		return value != null && value.intValue() >= 0;
	}

	public static boolean isValid(Integer value)
	{
		return value != null && value.intValue() >= 0;
	}
	
	public static boolean isValidString(JSONObject json, String attribute) throws JSONException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		TreeNode node = mapper.readTree(json.toString()).get(attribute);
		return node != null && (node instanceof TextNode) && ((TextNode)node).asText() != null;
	}
	
	public static boolean isValidString(JsonNode node) throws JSONException, JsonProcessingException, IOException {
		return node != null && (node instanceof TextNode) && ((TextNode)node).asText() != null;
	}
	
	public static boolean isNonEmptyString(JSONObject json, String attribute) throws JSONException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		TreeNode node = mapper.readTree(json.toString()).get(attribute);
		return node != null && (node instanceof TextNode) && !StringUtils.isEmpty(((TextNode)node).asText());
	}
	
	public static boolean isNonEmptyString(JsonNode node) throws JSONException, JsonProcessingException, IOException {
		return node != null && (node instanceof TextNode) && !StringUtils.isEmpty(((TextNode)node).asText());
	}

	/**
	 * Determines of the given string is an alphanumeric string. All job names,
	 * research project names, etc must be alphanumeric strings. This
	 * requirement is imposed for two reasons. First, directory and file names
	 * are derived from the research project and job names, thus they must
	 * conform to *nix file naming rules. Second, we need to use
	 * non-alphanumeric characters to delimit query terms for searching.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isAlphaNumeric(String s)
	{
		if (!isValid(s))
			return false;
		boolean valid = true;
		if (s == null || s.indexOf(":") > -1 || s.indexOf(";") > -1
				|| s.indexOf(",") > -1 || s.indexOf(" ") > -1 ||
				// s.indexOf("#") > -1 ||
				s.indexOf("\\") > -1 /*
									 * || s.indexOf("/") > -1 || s.indexOf("%")
									 * > -1 || s.indexOf("$") > -1 ||
									 * s.indexOf("@") > -1 || s.indexOf("!") >
									 * -1 || s.indexOf("^") > -1 ||
									 * s.indexOf("&") > -1 || s.indexOf("*") >
									 * -1 || s.indexOf("(") > -1 ||
									 * s.indexOf(")") > -1 || s.indexOf("{") >
									 * -1 || s.indexOf("}") > -1 ||
									 * s.indexOf("|") > -1 || s.indexOf("'") >
									 * -1 || s.indexOf("\"") > -1 ||
									 * s.indexOf("~") > -1 || s.indexOf("`") >
									 * -1 || s.indexOf("#") > -1 ||
									 * s.indexOf(".") > -1 || s.indexOf("?") >
									 * -1 || s.indexOf("<") > -1 ||
									 * s.indexOf(">") > -1 || //s.indexOf("=") >
									 * -1 || s.indexOf("+") > -1
									 */)
		{
			valid = false;
		}

		return valid;
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

	public static boolean isValid(Address address)
	{
		if (address == null)
			return false;
		if (!isValid(address.getStreetAddress()))
			return false;
		if (!isValid(address.getLocality()))
			return false;
		if (!isValid(address.getRegion()))
			return false;
		if (!isValid(address.getPostalCode())
				|| address.getPostalCode().length() != 5)
			return false;
		try
		{
			Integer.parseInt(address.getPostalCode());
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Fetch the entire contents of a text file, and return it in a String. This
	 * style of implementation does not throw Exceptions to the caller.
	 * 
	 * @param aFile
	 *            is a file which already exists and can be read.
	 */
	public static String getContents(File aFile)
	{
		// ...checks on aFile are elided
		StringBuilder contents = new StringBuilder();

		try
		{
			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try
			{
				String line = null; // not declared within while loop

				/*
				 * readLine is a bit quirky : it returns the content of a line
				 * MINUS the newline. it returns null only for the END of the
				 * stream. it returns an empty String if two newlines appear in
				 * a row.
				 */
				while ( ( line = input.readLine() ) != null)
				{
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			}
			finally
			{
				input.close();
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		return contents.toString();
	}

	/**
	 * Used to wrap all output from the service in a JSON object with two
	 * attributes "status" and "message." the former is success or failure. The
	 * latter is the output from a service invocation or the error message.
	 * 
	 * @param status
	 * @param json
	 * @return
	 */
	private static String wrapOutput(String status, String message, String json)
	{
		JSONWriter writer = new JSONStringer();
		try
		{
			writer.object().key("status").value(status).key("message").value(
					message).key("result").value(json).endObject();
		}
		catch (Exception e)
		{
			try
			{
				writer.object().key("status").value("error").key("message")
						.value(e.getMessage()).endObject();
			}
			catch (Exception e1)
			{
			}
		}
		return writer.toString();
	}

	/**
	 * Used to wrap the payload of a service invocation in a json object
	 * 
	 * @param message
	 * @param json
	 * @return
	 */
	public static String wrapSuccess(String message, String json)
	{
		return wrapOutput("success", null, json);
	}

	/**
	 * Used to wrap all output from the service in a JSON object with two
	 * attributes "status" and "message." the former is success or failure. The
	 * latter is the output from a service invocation or the error message.
	 * 
	 * @param status
	 * @param json
	 * @return
	 */
	public static String wrapSuccess(String json)
	{
		return wrapSuccess(null, json);
	}

	/**
	 * Used to wrap all output from the service in a JSON object with two
	 * attributes "status" and "message." the former is success or failure. The
	 * latter is the output from a service invocation or the error message.
	 * 
	 * @param status
	 * @param json
	 * @return
	 */
	public static String wrapError(String message, String json)
	{
		return wrapOutput("error", message, null);
	}

	public static String stripSurroundingBrackets(String str) 
	{
		if (str.startsWith("[")) str = str.substring(1).trim();
		if (str.equals("]"))
			str = null;
		else {
			if (str.endsWith("]")) str = str.substring(0, str.lastIndexOf("]") - 1).trim();
			str = str.replaceAll("\"", "");
		}
		
		return str;
	}
	
	public static void main(String[] args) {
		
		System.out.println(ServiceUtils.stripSurroundingBrackets("[\\n\"pint\",\"trees\"\\n]"));
	}

	public static boolean isEmailAddress(String endpoint)
	{
		String emailPattern = "^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}$";
		return Pattern.matches(emailPattern, endpoint);
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
	
	public static boolean isPublisher(String username, ExecutionSystem system)
	{	
		return system.getUserRole(username).canPublish();
//		
//		InputStream stream = AppsApplication.class.getClassLoader().getResourceAsStream("trusted_publishers.txt");
//		try
//		{
//			String trustedUserList = IOUtils.toString(stream, "UTF-8");
//			if (isValid(trustedUserList)) {
//				for(String user: trustedUserList.split(",")) {
//					if (username.equalsIgnoreCase(user.trim())) {
//						return true;
//					}
//				}
//				return false;
//			} else {
//				return false;
//			}
//		}
//		catch (IOException e)
//		{
//			 //log.error("Failed to locate trusted user file");
//			return false;
//		}
	}
	
	public static String explode(String glue, Collection<?> list)
	{	
		String explodedList = "";
		
		if (!ServiceUtils.isValid(list)) return explodedList;
		
		for(Object field: list)
			explodedList += glue + field.toString();
		
		return explodedList.substring(glue.length());
	}
	
	public static String[] implode(String separator, String tags)
	{
		if (!ServiceUtils.isValid(tags)) 
		{
			return new String[]{""};
		}
		else if (!tags.contains(separator))
		{
			return new String[]{tags};
		}
		else
		{
			return StringUtils.split(tags, separator);
		}
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

	/**
	 * Determines if a JsonNode represents a boolean value. We need this
	 * method due to our support for form submissions where numbers are
	 * serialized to integers.
	 * 
	 * @param jsonNode
	 * @return true if the JsonNode is a JSON boolean primary type, JSON integer primary 
	 * type with value 0 or 1, or a string with value equal to 0, 1, true, or false.
	 */
	public static boolean isBooleanOrNumericBoolean(JsonNode jsonNode)
	{
		if (jsonNode.isBoolean()) 
		{
			return true;
		}
		else if (jsonNode.isIntegralNumber())
		{
			return jsonNode.asInt() == 1 || jsonNode.asInt() == 0;
		}
		else if (jsonNode.isTextual()) 
		{
			 return StringUtils.equals(jsonNode.asText(), "1") ||
					 StringUtils.equals(jsonNode.asText(), "0") || 
					 StringUtils.equals(jsonNode.asText(), "true") ||
					 StringUtils.equals(jsonNode.asText(), "false");
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Wraps a string in properly escaped quotation marks.
	 * @param s
	 * @return
	 */
	public static String enquote(String s) {
	    s = StringUtils.strip(s, "\"");
	    if (StringUtils.isEmpty(s)) {
			s = "";
		} 
		        
		return "\"" + s + "\"";
	}
	
	/**
	 * Checks whether a string is a valid JSON array of value nodes.
	 * This is used primarily when checking for job input arrays to enquote.
	 * 
	 * @param sJson String to check
	 * @return True if a json array with only value nodes
	 */
	public static boolean isJsonArrayOfStrings(final String sJson) 
	{
		try 
		{
			final JsonNode json = new ObjectMapper().readTree(sJson);
			if (json.isArray())
			{
				for(Iterator<JsonNode> iter = json.elements(); iter.hasNext(); ) 
				{
					JsonNode child = iter.next();
					if (!child.isValueNode()) {
						return false;
					}
				}
			} 
			else
			{
	    	  return false;
			}
	      
			return true;
		} 
		catch (Exception jpe) {
			return false;
		}
	}
	
	/**
	 * Returns the primary values from a JSON array as a String array. Any non-primary type values
	 * in the array will not be included in the response.
	 * 
	 * @param JsonNode 
	 * @return String array of values from the JSON array. Any primary types will get converted here.
	 * @throws JsonParseException
	 * @throws IOException
	 */
	public static String[] getStringValuesFromJsonArray(final ArrayNode json, final boolean enquoteValues)
	{
		if (json == null || json.size() == 0 || json.isNull())
		{
			return new String[]{};
		}
		else
		{
			String[] arrayValues = new String[json.size()];
			for(int i=0; i<json.size(); i++) 
			{
				JsonNode child = json.path(i);
				if (child.isValueNode()) {
					if (enquoteValues) {
						arrayValues[i] = ServiceUtils.enquote(child.asText());
					} else {
						arrayValues[i] = child.asText();
					}
				}
			}
			return arrayValues;
		}
	}

	/**
	 * Returns the values from a JSON array of primary types as a String array by parsing the
	 * given string into a JsonNode and calling the corresponding method.
	 * 
	 * @param sJson 
	 * @return String array of values from the JSON array. Any primary types will get converted here.
	 * @throws JsonParseException
	 * @throws IOException
	 */
	public static String[] getStringValuesFromJsonArray(final String sJson, final boolean enquoteValues)
	throws JsonParseException, IOException
	{
		try 
		{
			final JsonNode json = new ObjectMapper().readTree(sJson);
			if (json.isArray()) {
				return getStringValuesFromJsonArray((ArrayNode)json, enquoteValues);
			} else {
	    	  throw new JsonParseException("Value is not a valid JSON array of primary values.", new JsonLocation(sJson, sJson.length(), 1, 1));
			}
		} 
		catch (JsonParseException e) {
			throw e;
		}
		catch (JsonProcessingException e) {
			throw e;
		}
	}
	
	/**
	 * Utility function to verify a string value against a given regex.
	 * 
	 * @param singleInput string value to test.
	 * @return true if validator is null or the string matches. false otherwise
	 */
	public static boolean doesValueMatchValidatorRegex(String value, String regex)
	{
		if (StringUtils.isEmpty(regex) || value == null) {
			return true;
		}
		else 
		{
			try 
			{
				Regex r = new Regex();
				r.compile(regex);
				return r.search(value);
			}
			catch (Throwable e) {
				return false;
			}
		}
	}
}
