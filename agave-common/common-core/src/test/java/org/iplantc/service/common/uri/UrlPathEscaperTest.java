package org.iplantc.service.common.uri;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test of the {@link UrlPathEscaperTest} class to encode and decode
 * URL paths.
 * @author dooley
 *
 */
public class UrlPathEscaperTest {

	@DataProvider
	protected Object[][] decodeTokenProvider() {
		return new Object[][] {
				{ "test dir name.txt", "test dir name.txt", "Whitespace should be unchanged after URL decoding"},
				{ "test dir name.txt", "test dir name.txt", "Whitespace should be unchanged after URL decoding"},
				{ "test+dir+name.txt", "test dir name.txt", "Plus signs should be replaced with whitespace after URL decoding" },
				{ "test%20dir%20name.txt", "test dir name.txt", "URL encoded white space should be replaced with white space after URL decoding"},
				{ "test%2bdir%2bname.txt", "test+dir+name.txt", "URL encoded plus signs should be replaced with plus signs after URL decoding" },
				{ "test++dir+++name.txt", "test  dir   name.txt", "Multiple URL encoded plus signs should be replaced with plus signs after URL decoding" },
				{ "test%20%20dir%20%20%20name.txt", "test  dir   name.txt", "Multiple URL encoded whitespace should be replaced with plus signs after URL decoding" },
				{ "test%20+%20dir+%20+name.txt", "test   dir   name.txt", "Multiple mixed URL encoded whitespace and plus signs should be replaced with plus signs after URL decoding" },
				{ "test${FOO}dir${BAR}name.txt", "test${FOO}dir${BAR}name.txt", "AGAVE macros should remain unchanged after URL decoding." },
				{ "test%24%7bFOO%7ddir%24%7bBAR%7dname.txt", "test${FOO}dir${BAR}name.txt", "URL encoded AGAVE macros should remain unchanged after URL decoding." },
		};
	}
  
	@Test(dataProvider="decodeTokenProvider")
	public void decodeToken(String value, String expected, String message) {
		Assert.assertEquals(UrlPathEscaper.decode(value), expected, message);
	}
  
	@DataProvider
	protected Object[][] decodePathProvider() {
		return new Object[][] {
				{ "test/file/path.txt", "test/file/path.txt", "Slashes should be preserved after URL decoding"},
				{ "/test/file/path.txt", "/test/file/path.txt", "Leading slashes should be preserved after URL decoding"},
				{ "/test/file/path.txt/", "/test/file/path.txt/", "Trailing slashes should be preserved after URL decoding"},
		};
	}
  
	@Test(dataProvider="decodePathProvider")
	public void decodePath(String value, String expected, String message) {
		Assert.assertEquals(UrlPathEscaper.decode(value), expected, message);
	}
	
	@DataProvider
	protected Object[][] escapePathProvider() {
		return new Object[][] {
				{ "test/file/path.txt", "test/file/path.txt", "Slashes should be preserved after URL escaping"},
				{ "/test/file/path.txt", "/test/file/path.txt", "Leading slashes should be preserved after URL escaping"},
				{ "/test/file/path.txt/", "/test/file/path.txt/", "Trailing slashes should be preserved after URL escaping"},
				{ "//test/file/path.txt/", "//test/file/path.txt/", "Double slashes should be preserved after URL escaping"},
				{ "//test//file//path.txt//", "//test//file//path.txt//", "Multiple double slashes should be preserved after URL escaping"},
				{ "/ / / / /", "/%20/%20/%20/%20/", "White space should be encoded and preserved when it is the only thing between two slashesafter URL escaping"},
		};
	}
	
	@Test(dataProvider="escapePathProvider")
	public void escapePath(String value, String expected, String message) {
		Assert.assertEquals(UrlPathEscaper.escape(value), expected, message);
	}
	
  
}
