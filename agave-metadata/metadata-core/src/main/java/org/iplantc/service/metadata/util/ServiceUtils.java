/**
 * 
 */
package org.iplantc.service.metadata.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.jackson.MongoDBSafeKey;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;


/**
 * @author dooley
 *
 */
public class ServiceUtils {
	
	private static final Logger log = Logger.getLogger(ServiceUtils.class);

	@SuppressWarnings("unused")
	public static String exec(String command) throws IOException {
		String output = "";
		
		InputStream in = Runtime.getRuntime().exec(command).getInputStream();
		
		byte[] b = new byte[512];
		int len = 0;
		while ((len = in.read(b)) >= 0) {
			output += new String(b).trim();
		}
		
		return output;
	}

	
	public static boolean isValid(String value) {
		return (value != null) && !value.equals("");
	}
	
	public static boolean isValid(File value) {
		return (value != null) && value.exists();
	}
	
	public static boolean isValid(Collection<?> value) {
		return (value !=null) && (!value.isEmpty());
	}
	
	public static boolean isValid(Map<?,?> value) {
		return (value != null) && (value.size() > 0);
	}
	
	public static boolean isValid(Calendar value) {
		return value != null;
	}
	
	public static boolean isValid(Date value) {
		return value != null;
	}
	
	public static boolean isValid(Long value) {
		return value != null && value.intValue() >= 0;
	}
	
	public static boolean isValid(Integer value) {
		return value != null && value.intValue() >= 0;
	}
	
	/**
	 * Determines of the given string is an alphanumeric string. All job names, 
	 * research project names, etc must be alphanumeric strings. This requirement
	 * is imposed for two reasons.  First, directory and file names are derived
	 * from the research project and job names, thus they must conform to *nix
	 * file naming rules. Second, we need to use non-alphanumeric characters to
	 * delimit query terms for searching.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isAlphaNumeric(String s) {
		if (!isValid(s)) return false;
	    boolean valid = true;
	    if (s == null || 
	            s.indexOf(":") > -1 ||
	            s.indexOf(";") > -1 || 
	            s.indexOf(",") > -1 || 
	            s.indexOf(" ") > -1 ||
	            //s.indexOf("#") > -1 ||
	            s.indexOf("\\") > -1 /*||
	            s.indexOf("/") > -1 ||
	            s.indexOf("%") > -1 ||
	            s.indexOf("$") > -1 ||
	            s.indexOf("@") > -1 ||
	            s.indexOf("!") > -1 || 
	            s.indexOf("^") > -1 ||
	            s.indexOf("&") > -1 ||
	            s.indexOf("*") > -1 ||
	            s.indexOf("(") > -1 ||
	            s.indexOf(")") > -1 ||
	            s.indexOf("{") > -1 ||
	            s.indexOf("}") > -1 ||
	            s.indexOf("|") > -1 ||
	            s.indexOf("'") > -1 ||
	            s.indexOf("\"") > -1 ||
	            s.indexOf("~") > -1 ||
	            s.indexOf("`") > -1 ||
	            s.indexOf("#") > -1 ||
	            s.indexOf(".") > -1 ||
	            s.indexOf("?") > -1 ||
	            s.indexOf("<") > -1 ||
	            s.indexOf(">") > -1 ||
	            //s.indexOf("=") > -1 ||
	            s.indexOf("+") > -1*/) {
	        valid = false;
	    }
	    
	    return valid;
	}
	
	public static boolean isValidEmailAddress(String value) {
		if (!isValid(value)) return false;
		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		Pattern pattern = Pattern.compile(expression,
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	public static boolean isValidPhoneNumber(String value) {
		if (!isValid(value)) return false;
		String expression = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$";
		Pattern pattern = Pattern.compile(expression,
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	/**
     * Fetch the entire contents of a text file, and return it in a String.
     * This style of implementation does not throw Exceptions to the caller.
     *
     * @param aFile is a file which already exists and can be read.
     */
    public static String getContents(File aFile) {
       //...checks on aFile are elided
       StringBuilder contents = new StringBuilder();
       
       try {
         //use buffering, reading one line at a time
         //FileReader always assumes default encoding is OK!
    	 BufferedReader input =  new BufferedReader(new FileReader(aFile));
         try {
           String line = null; //not declared within while loop
           
           /*
           * readLine is a bit quirky :
           * it returns the content of a line MINUS the newline.
           * it returns null only for the END of the stream.
           * it returns an empty String if two newlines appear in a row.
           */
           while (( line = input.readLine()) != null){
             contents.append(line);
             contents.append(System.getProperty("line.separator"));
           }
         }
         finally {
           input.close();
         }
       }
       catch (IOException ex){
         ex.printStackTrace();
       }
       
       return contents.toString();
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
	
	public static boolean isEmailAddress(String endpoint)
	{
		String emailPattern = "^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}$";
		return Pattern.matches(emailPattern, endpoint);
	}


	public static boolean isValidCallback(String sCallback)
	{
		if (!ServiceUtils.isValid(sCallback)) {
			return false;
		} else if (ServiceUtils.isEmailAddress(sCallback)) {
			return true;
		} else {
			if (ServiceUtils.isValidCallback(sCallback)) {
			
				try {
					URL url = new URL(sCallback);
					if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https") ) {
						return false;
					}	
					return true;
				} catch (Exception e) {
					return false;
				}
			} else {
				return false;
			}
		}
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
		
		InputStream stream = null;
		try
		{
			stream = ServiceUtils.class.getClassLoader().getResourceAsStream("trusted_admins.txt");
			String trustedUserList = IOUtils.toString(stream, "UTF-8");
			if (StringUtils.isNotEmpty(trustedUserList)) {
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
			log.warn("Failed to load trusted user file", e);
			return false;
		}
        finally {
        	if (stream != null) try {stream.close();} catch (Exception e){}
        }
	}
	
	public static String explode(String glue, List<?> list)
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
	 * Unescapes persisted reference fields in serialized json schema 
	 * objects from their unicode values for persisting in MongoDB.
	 * 
	 * @param json
	 * @return
	 */
	public static String unescapeSchemaRefFieldNames(String json) {
    	String escapedRef = MongoDBSafeKey.escapeFieldName("$ref");
    	String escapedSchema = MongoDBSafeKey.escapeFieldName("$schema");
    	return StringUtils.replace(StringUtils.replace(json, escapedRef, "$ref"), escapedSchema, "$schema");
	}

	/**
	 * Escapes {@code $ref} fields in serialized json schema objects
	 * with their unicode values for persisting in MongoDB.
	 * 
	 * @param json
	 * @return
	 */
	public static String escapeSchemaRefFieldNames(String json) {
		String escapedRef = MongoDBSafeKey.escapeFieldName("$ref");
		String escapedSchema = MongoDBSafeKey.escapeFieldName("$schema");
    	return StringUtils.replace(StringUtils.replace(json, "$ref", escapedRef), "$schema", escapedSchema);
	}
	
}
