package org.iplantc.service.jobs.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JobSearchFilterTest extends AbstractDaoTest
{
    
    private String alternateCase(String val) {
		boolean upper = false;
		StringBuilder b = new StringBuilder();
		for (char c: val.toCharArray() ) {
			if (upper) {
				b.append(String.valueOf(c).toUpperCase());
			} else {
				b.append(c);
			}
			upper = !upper;
		}
		return b.toString();
	}
	
	@DataProvider(name="filterCriteriaTestProvider", parallel=true)
	public Object[][] filterCriteriaTestProvider() throws Exception
	{
		List<Object[]> testData = new ArrayList<Object[]>();
		JobSearchFilter jobSearchFilter = new JobSearchFilter();
		for (String key: jobSearchFilter.getSearchTermMappings().keySet())
		{
		    // handle sets and ranges independently
		    if (StringUtils.endsWithIgnoreCase(key, "between") 
                    || StringUtils.endsWithIgnoreCase(key, "in") 
                    || StringUtils.endsWithIgnoreCase(key, "nin")) continue;
            
            testData.add(new Object[]{ key, true, "Exact terms should be accepted" });
			testData.add(new Object[]{ key.toUpperCase(), true, "Uppercase terms should be accepted" });
			testData.add(new Object[]{ alternateCase(key), true, "Mixed case terms should be accepted" });
			testData.add(new Object[]{ key + "s", false, "Partially matched terms on prefix should be filtered" });
			testData.add(new Object[]{ "s" + key, false, "Partial match terms on suffix should be filtered" });
		}
		
		testData.add(new Object[]{ "", false, "Empty key should be filtered" });
		
		return testData.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="filterCriteriaTestProvider")
	public void filterInvalidSearchCriteria(String criteria, boolean shouldExistAfterFiltering, String message) throws Exception
	{
		_filterInvalidSearchCriteria(criteria, shouldExistAfterFiltering, message); 
	}
	
	@DataProvider(name="filterInvalidSearchCriteriaWithOperatorsProvider", parallel=true)
	public Object[][] filterInvalidSearchCriteriaWithOperatorsProvider() throws Exception
	{
		List<Object[]> testData = new ArrayList<Object[]>();
		JobSearchFilter jobSearchFilter = new JobSearchFilter();
		Map<String,Class> searchTypeMappings = jobSearchFilter.getSearchTypeMappings();
		for (String key: jobSearchFilter.getSearchTermMappings().keySet())
		{
		    // handle sets and ranges independently
            if (StringUtils.endsWithIgnoreCase(key, "between") 
                    || StringUtils.endsWithIgnoreCase(key, "in") 
                    || StringUtils.endsWithIgnoreCase(key, "nin")) continue;
            
            for (SearchTerm.Operator operator: SearchTerm.Operator.values()) {
				if (searchTypeMappings.get(key) == Date.class && operator.isSetOperator() && SearchTerm.Operator.BETWEEN != operator) {
				    continue;
				}
				String op =  "." + operator.name();
                
				testData.add(new Object[]{ key + op, true, "Exact terms with uppercase operator should be accepted" });
				testData.add(new Object[]{ key + op.toLowerCase(), true, "Lowercase terms should be accepted" });
				testData.add(new Object[]{ (key + op).toUpperCase(), true, "Uppercase terms should be accepted" });
				testData.add(new Object[]{ alternateCase(key + op), true, "Mixed case terms should be accepted" });
				testData.add(new Object[]{ key + op + "s", false, "Invalid operation should be filtered" });
				testData.add(new Object[]{ op + key, false, "Prefixing search term with operation should be filtered" });
			}
		}
		
		testData.add(new Object[]{ "", false, "Empty key should be filtered" });
		
		return testData.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="filterInvalidSearchCriteriaWithOperatorsProvider")
	public void filterInvalidSearchCriteriaWithOperators(String criteria, boolean shouldExistAfterFiltering, String message) throws Exception
	{
		_filterInvalidSearchCriteria(criteria, shouldExistAfterFiltering, message); 
	}
	
	@DataProvider(name="filterCommaSeparatedSetSearchCriteriaIntoListProvider", parallel=true)
	public Object[][] filterCommaSeparatedSetSearchCriteriaIntoListProvider()
	{
		List<Object[]> testData = new ArrayList<Object[]>();
		JobSearchFilter jobSearchFilter = new JobSearchFilter();
		for (String key: jobSearchFilter.getSearchTermMappings().keySet())
		{
		    // handle sets and ranges independently
		    if (StringUtils.endsWithIgnoreCase(key, "between") 
                    || StringUtils.endsWithIgnoreCase(key, "in") 
                    || StringUtils.endsWithIgnoreCase(key, "nin")) continue;
            
		    Class clazz = jobSearchFilter.getSearchTypeMappings().get(key);
			String value = "";
			if (clazz == String.class) {
				value = "a,b,c,d,e,f,g";
			} else if (clazz == Boolean.class) {
				value = "true,false";
			} else if (clazz == Date.class) {
				value = "yesterday,today";
			} else if (clazz == JobStatusType.class) {
				value = StringUtils.join(JobStatusType.values(), ",");
			} else {
				value = "0,1,2,3,4,5,6,7,8,9,10";
			}
			for (SearchTerm.Operator operator: SearchTerm.Operator.values()) {
			    
			    String op =  "." + operator.name();
				if (operator.isSetOperator()) {
				    if (clazz != Date.class || SearchTerm.Operator.BETWEEN == operator)  {
				        testData.add(new Object[]{ key + op, value, clazz, "Set operation terms should result in list of values" });
				    } else {
				        // other combinations throw exceptions
				    }
				} else {
//					testData.add(new Object[]{ key + op, value, clazz, "Union operation terms should result in original value" });
				}
			}
		}
		
		return testData.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="filterCommaSeparatedSetSearchCriteriaIntoListProvider")
	public void filterCommaSeparatedSetSearchCriteriaIntoList(String field, String value, Class clazz, String message) throws Exception
	{
		JobSearchFilter jobSearchFilter = new JobSearchFilter();
		Map<String, String> searchCriteria = new HashMap<String, String>();
		searchCriteria.put(field, value);
		
		// apply the filter
		Map<SearchTerm, Object> searchTerms = jobSearchFilter.filterCriteria(searchCriteria);
		
		// if the term was filtered, the resulting map will be empty
		Assert.assertFalse(searchTerms.isEmpty(), "List of values was filtered despite the set operator");
		Assert.assertEquals(searchTerms.size(), 1, "Incorrect number of search terms returned after filtering set operation");
		SearchTerm filteredSearchTerm = searchTerms.keySet().iterator().next();
		Object filteredList = searchTerms.get(filteredSearchTerm);
		if (filteredSearchTerm.getOperator().isSetOperator()) {
			Assert.assertTrue(filteredList instanceof List, "Comma separated list of values was not filtered into a list when the set operation was provided");
		
			for(Object o: (List)filteredList) {
			    if (StringUtils.startsWithIgnoreCase(filteredSearchTerm.getSearchField(), "maxruntime")) {
			        Assert.assertEquals(o.getClass(), Integer.class, "filtered maxruntime list did not resolve to integer values");
			    } else {
			        Assert.assertEquals(o.getClass(), clazz, "Filtered list did not retain original typing");
			    }
			}
		} else {
//			Assert.assertEquals(filteredList.getClass(), clazz, "Comma separated list of values should not be filtered into a list when a unary operation was provided");
		}
	}
	
	public void _filterInvalidSearchCriteria(String testField, boolean shouldExistAfterFiltering, String message) throws Exception
	{
		JobSearchFilter jobSearchFilter = new JobSearchFilter();
		Map<String, String> searchCriteria = new HashMap<String, String>();
		// we use a dummy value because no validation of values happens here. just filtering of
		// search terms
		Class clazz = null;
		SearchTerm testTerm;
		try {
			testTerm = new SearchTerm(StringUtils.lowerCase(testField), null);
			clazz = jobSearchFilter.getSearchTypeMappings().get(testTerm.getSearchField());
		} catch (Exception e) {
			// testing an empty value
			clazz = jobSearchFilter.getSearchTypeMappings().get(StringUtils.lowerCase(testField));
		}
		
		if (clazz == null) {
			Assert.assertFalse(shouldExistAfterFiltering, message);
		} else {
			if (clazz == String.class) {
				searchCriteria.put(testField, testField);
			} else if (clazz == Boolean.class) {
				searchCriteria.put(testField, "true");
			} else if (clazz == Date.class) {
			    if (StringUtils.containsIgnoreCase(testField, "between")) {
			        searchCriteria.put(testField, "2015-01-01,tomorrow");
			    } else {
			        searchCriteria.put(testField, "2015-01-01");
			    }
			} else if (clazz == JobStatusType.class) {
				searchCriteria.put(testField, "RUNNING");
			} else {
				searchCriteria.put(testField, "0");
			}
			
			// apply the filter
			Map<SearchTerm, Object> searchTerms = jobSearchFilter.filterCriteria(searchCriteria);
			
			// if the term was filtered, the resulting map will be empty
			Assert.assertNotEquals(searchTerms.isEmpty(), shouldExistAfterFiltering, message);
		}
	}

}
