package org.iplantc.service.jobs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Utility class to create slugs in Java.<br>
 * <pre>
 * Slugify slg = new Slugify();
 * // Result: hello-world
 * String s = slg.slugify("Hello, world!");
 * </pre>
 * You can set custom replacements as well using a replacements.properties
 * file in your classpath, or by passing in a {@link Map<String, String>}:<br>
 * <pre>
 * Slugify slg = new Slugify();
 * slg.setCustomReplacements(new HashMap<String, String>() {
 *     put("foo", "bar");
 * });
 * 
 * // Result: hello-bar
 * String s = slg.slugify("Hello foo");
 * </pre>
 * Or if you want case sensitivity:<br>
 * <pre>
 * Slugify slg = new Slugify(false);
 * // Result: Hello-World
 * String s = slg.slugify("Hello, World!");
 * </pre>
 * Static accessors are also available:<br>
 * <pre>
 * // Result: hello-world
 * String s = Slug.toSlug("Hello, World!");
 * // Result: Hello-World
 * String s = Slug.toSlug("Hello, World!", false);
 * </pre>
 * 
 * @author dooley
 */
public class Slug {
	
	private final Properties replacements = new Properties();

	private Map<String, String> customReplacements;
	private boolean lowerCase;

	public Slug() throws IOException {
		this(true);
	}

	public Slug(final boolean lowerCase) throws IOException {
		setLowerCase(lowerCase);
		InputStream replacementsStream = getClass().getClassLoader().getResourceAsStream("replacements.properties");
		if (replacementsStream != null) {
			try {replacements.load(replacementsStream);}
			finally {try {replacementsStream.close();} catch (Exception e){}}
		}
	}
	
	/**
	 * Static accessor to slugify the given input and forcing the result to lowercase.
	 * 
	 * @param input String to slugify
	 * @param lowerCase if true, returned value will be lowercase. Original case otherwise.
	 * @return
	 */
	public static String toSlug(String input)
	{
		return Slug.toSlug(input, true);
	}
	
	/**
	 * Static accessor to create a slug explicitly specifiying case behavior.
	 * 
	 * @param input String to slugify
	 * @param lowerCase if true, returned value will be lowercase. Original case otherwise.
	 * @return
	 */
	public static String toSlug(String input, boolean lowerCase) 
	{
		try
		{
			return new Slug(lowerCase).slugify(input);
		}
		catch (IOException e)
		{
			input = Normalizer.normalize(input, Normalizer.Form.NFD)
					.replaceAll("[^\\p{ASCII}]", "")
					.replaceAll("[^\\w+]", "-")
					.replaceAll("\\s+", "-")
					.replaceAll("[-]+", "-")
					.replaceAll("^-", "")
					.replaceAll("-$", "");

			if (lowerCase) {
				input = input.toLowerCase();
			}
			
			return input;
		}
	}
	
	/**
	 * Turn a string into a slug. Custom replacemetns are honored before creating the slug,
	 * thus any non-alphanumeric values will be either dropped or converted to dashes.
	 * 
	 * @param input String to slugify
	 * @return valid slug
	 */
	public String slugify(String input) {
		if (input == null) {
			return "";
		}

		input = input.trim();

		Map<String, String> customReplacements = getCustomReplacements();
		if (customReplacements != null) {
			for (Entry<String, String> entry : customReplacements.entrySet()) {
				input = input.replace(entry.getKey(), entry.getValue());
			}
		}

		for (Entry<Object, Object> e : replacements.entrySet()) {
			input = input.replace((String) e.getKey(), (String) e.getValue());
		}

		input = Normalizer.normalize(input, Normalizer.Form.NFD)
				.replaceAll("'", "")
				.replaceAll("[^\\p{ASCII}]", "")
				.replaceAll("[^\\w+]", "-")
				.replaceAll("\\s+", "-")
				.replaceAll("[-]+", "-")
				.replaceAll("^-", "")
				.replaceAll("-$", "");

		if (getLowerCase()) {
			input = input.toLowerCase();
		}

		return input;
	}

	/**
	 * Get list of custom replacement values in key-value form as a map.
	 * @return Map of replacement values
	 */
	public Map<String, String> getCustomReplacements() {
		return customReplacements;
	}

	/**
	 * Set custom replacement values for all inputs processed with this
	 * instance. Values are specified in key value form as a Map.
	 * @param customReplacements Map of replacement values.
	 */
	public void setCustomReplacements(Map<String, String> customReplacements) {
		this.customReplacements = customReplacements;
	}

	/**
	 * Returns whether the resulting slugs will be forced to lowercase.
	 * @return
	 */
	public boolean getLowerCase() {
		return lowerCase;
	}

	/**
	 * Force the resulting slug to lowercase.
	 * 
	 * @param lowerCase if true, returned value will be lowercase. Original case otherwise
	 */
	public void setLowerCase(boolean lowerCase) {
		this.lowerCase = lowerCase;
	}
	
	public static void main(String args[]) {
		
		String input = "";
		
		while ((input = Slug.getCLI()) != null)
			System.out.println(Slug.toSlug(input));
	}

	public static String getCLI() {
		//  open up standard input
	    System.out.println("Enter input: ");  
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String text = null;
  
		try {
			text = br.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to read your input");
			System.exit(1);
		}
		return text;
	}
}
