package org.iplantc.service.apps.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.iplantc.service.apps.exceptions.SoftwareException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stevesoft.pat.Regex;

public class RegexValidator
{

	public RegexValidator(){}
	
	@DataProvider
	public Object[][] validateRegexProvider() {
		String regex = "^(?:(1\\.00|(?:0\\.[0-9]{2})))$";
		List<Object[]> permutations = new ArrayList<Object[]>();
		for(int i=0;i<10;i++) {
			// verify .# fails
			permutations.add(new Object[]{ regex, String.format(".%d", i), false });
			// verify #. fails
			permutations.add(new Object[]{ regex, String.format("%d.", i), false });
			// verify # fails
			permutations.add(new Object[]{ regex, String.format("%d", i), false });
						
			for(int j=0;j<10;j++) {
				// verify .## fails
				permutations.add(new Object[]{ regex, String.format(".%d%d", i, j), false });
				// verify #.# fails
				permutations.add(new Object[]{ regex, String.format("%d.%d", i, j), false });
				// verify ## fails
				permutations.add(new Object[]{ regex, String.format("%d%d", i, j), false });
				
				for(int k=0;k<10;k++) {
					// verify 0.00 - 1.00 are valid
					permutations.add(new Object[]{ regex, String.format("%d.%d%d", i, j, k), i == 0 || (i==1 && j==0 && k==0) });
					// verify ### fails
					permutations.add(new Object[]{ regex, String.format("%d%d%d", i, j, k), false });
				}
			}
		}
		return permutations.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="validateRegexProvider")
	public void validateRegex(String expression, String value, boolean shouldPass) {
		
		try {
			Regex regex = new Regex();
			regex.compile(expression);
			Assert.assertEquals(shouldPass, regex.search(value));
		} catch (Exception e) {
			throw new SoftwareException(expression + " is not a valid regular expression.", e);
		}
		
	}

}
