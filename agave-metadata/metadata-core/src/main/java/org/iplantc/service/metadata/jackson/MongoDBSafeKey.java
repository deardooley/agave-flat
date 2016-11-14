package org.iplantc.service.metadata.jackson;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.regex.Matcher.quoteReplacement;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Provides a key compatible with mongoDB field names by escaping the restricted characters with the Unicode full width equivalents:
 * U+FF04 (i.e. "＄") and U+FF0E (i.e. "．").
 * @author Erik Torres <etserrano@gmail.com>
 * @see <a href="http://docs.mongodb.org/manual/reference/limits/#Restrictions-on-Field-Names">mongoDB Restrictions on Field Names</a>
 * @see <a href="http://docs.mongodb.org/manual/faq/developers/#dollar-sign-operator-escaping">Dollar Sign Operator Escaping</a>
 */
public class MongoDBSafeKey {

	private static final Pattern DOLLAR_PATTERN = compile(quote("$"));
	private static final Pattern DOT_PATTERN = compile(quote("."));

	private static final String DOLLAR_REPLACEMENT = quoteReplacement("\\") + "uff04";
	private static final String DOT_REPLACEMENT = quoteReplacement("\\") + "uff0e";

	private static final Pattern UDOLLAR_PATTERN = compile(quote("\uff04"));
	private static final Pattern UDOT_PATTERN = compile(quote("\uff0e"));

	private static final String UDOLLAR_REPLACEMENT = quoteReplacement("$");
	private static final String UDOT_REPLACEMENT = quoteReplacement(".");

	private String key;

	public MongoDBSafeKey() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	@JsonIgnore
	public String getUnescapedKey() {
		return unescapeFieldName(key);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof MongoDBSafeKey)) {
			return false;
		}
		final MongoDBSafeKey other = MongoDBSafeKey.class.cast(obj);
		return Objects.equals(key, other.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}

	@Override
	public String toString() {
		return toStringHelper(this)
				.add("key", key)
				.toString();
	}

	public static MongoDBSafeKey escapeMapKey(final String name) {
		final MongoDBSafeKey instance = new MongoDBSafeKey();
		instance.setKey(escapeFieldName(name));
		return instance;
	}

	public static String escapeFieldName(final String name) {
		String name2 = null;
		checkArgument(isNotBlank(name2 = trimToNull(name)), "Uninitialized or invalid field name");		
		String escaped = DOLLAR_PATTERN.matcher(name2).replaceAll(DOLLAR_REPLACEMENT);
		escaped = DOT_PATTERN.matcher(escaped).replaceAll(DOT_REPLACEMENT);
		return unescapeJava(escaped);
	}

	public static String unescapeFieldName(final String name) {
		String name2 = null;
		checkArgument(isNotBlank(name2 = trimToNull(name)), "Uninitialized or invalid field name");
		String unescaped = UDOLLAR_PATTERN.matcher(name2).replaceAll(UDOLLAR_REPLACEMENT);
		unescaped = UDOT_PATTERN.matcher(unescaped).replaceAll(UDOT_REPLACEMENT);
		return unescaped;
	}

}