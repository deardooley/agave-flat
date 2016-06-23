package org.iplantc.service.common.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.exceptions.SearchSyntaxException;

/**
 * Tuple to hold mapping of job attribute and
 * sql value
 * @author dooley
 *
 */
public class SearchTerm implements Comparable<SearchTerm>
{
	/**
	 * Mapping construct to define conditions for a subsequent 
	 * search based on the operators involved.  
	 * @author dooley
	 *
	 */
	public enum Operator {
		EQ("%s = :%s"), 
		ON("DATE_FORMAT(%s,'%s') = DATE_FORMAT(DATE(:%s),'%s')"),
		NEQ("%s <> :%s"),
		LT("%s < :%s"),
		BEFORE("DATE_FORMAT(%s,'%s') < :%s"),
		LTE("%s <= :%s"),
		GT("%s > :%s"),
		AFTER("DATE_FORMAT(%s,'%s') > :%s"),
		GTE("%s >= :%s"),
		IN("%s in :%s"),
		NIN("%s not in :%s"),
		LIKE("%s like :%s"),
		RLIKE("%s REGEXP :%s"),
		NLIKE(":%s not like :%s"),
		BETWEEN("DATE_FORMAT(%s,'%s') >= :%s0 and DATE_FORMAT(%s,'%s') <= :%s1");
		
		private String template;
		
		private Operator(String template) {
			this.setTemplate(template);
		}

		public String getTemplate() {
			return template;
		}

		public void setTemplate(String template) {
			this.template = template;
		}

		/**
		 * Returns true if this operator can be applied to a 
		 * set of values rather than just one.
		 * @return true if set operator, false otherwise
		 */
		public boolean isSetOperator() {
			return (this == IN || this == NIN || this == BETWEEN);
		}
		
		/**
		 * Returns true if this operator can be only be  
		 * applied to a single value. This method delegates to 
		 * {@link #isSetOperation}
		 * @return true if unary operator, false otherwise
		 */
		public boolean isUnaryOperator() {
			return !isSetOperator();
		}
		
		@Override
		public String toString() {
			return getTemplate();
		}

		/**
		 * Null-safe filter for a search value. Known wildcards "*" will be
		 * resolved to "%" and the result returned. Lists will be returned
		 * unchanged.
		 *   
		 * @param searchValue
		 * @return
		 */
		public Object applyWildcards(Object searchValue) {
			if (searchValue == null) {
				searchValue = "";
			} else if (searchValue instanceof List) {
			    if (this == BETWEEN) {
			        List<String> dates = new ArrayList<String>();
			        for(Date d: (List<Date>)searchValue) {
			            dates.add(new SimpleDateFormat("YYYY-MM-dd HH:mm").format(d));
			        }
			        
			        return dates;
			    } else {
			        return searchValue;
			    }
			} else if (searchValue instanceof Date) {
			    if (this == ON) {
			        return new SimpleDateFormat("YYYY-MM-dd").format((Date)searchValue);
			    } else {
			        return new SimpleDateFormat("YYYY-MM-dd HH:mm").format((Date)searchValue);
			    }
			}
			
			if (this == LIKE || this == NLIKE || this == RLIKE) {
				return searchValue.toString().replaceAll("\\*", "%");
			} else {
				return searchValue;
			}
		}

        /**
         * Returns true of the operator checks for equality or
         * inequality only
         * 
         * @return
         */
        public boolean isEqualityOperator() {
            return this == EQ || this == NEQ;
        }
	}
	
	/**
	 * The qualifier prefix to apply to the mappedField. For example, "j." 
	 * could be a prefix for a "name" mappedField which would result in 
	 * "j.name" being the effective mappedField in the generated expression.
	 */
	private String prefix;
	
	/**
	 * The user-supplied search field name.
	 */
	private String searchField;
	/**
	 * The field mapped to by the {@link SearchTerm#searchField}. For example, 
	 * a user-facing resource ID might map to a UUID on the matching entity. In 
	 * order to translate between the two, we define the mappedField here. 
	 */
	private String mappedField;
	/**
	 * The conditional operation to perform on the mappedField during the
	 * subsequent query. 
	 */
	private Operator operator = Operator.EQ;
	
	/**
	 * Creates a {@link SearchTerm} by parsing the searchFieldWithOperator
	 * into a {@link SearchTerm#searchField} and {@link SearchTerm#operator} and
	 * assigning the mappedField as the field to be used in the resulting query.
	 * 
	 * @param searchFieldWithOperator {@link SearchTerm#searchField} with the 
	 * operation appended to the end. Ex. <code>name.eq</code>. This is the 
	 * user-supplied search field name.
	 * @param prefix the string to prepend to the searchField when generating 
	 * the final expression 
	 * @throws SearchSyntaxException
	 */
	public SearchTerm(String searchFieldWithOperator, String prefix) 
	throws SearchSyntaxException 
	{
		parseSearchField(searchFieldWithOperator);
		setPrefix(prefix);	
	}
	
	/**
	 * Creates a {@link SearchTerm} by parsing the searchFieldWithOperator
	 * into a {@link SearchTerm#searchField} and {@link SearchTerm#operator} and
	 * assigning the mappedField as the field to be used in the resulting query.
	 * 
	 * @param searchFieldWithOperator {@link SearchTerm#searchField} with the 
	 * operation appended to the end. Ex. <code>name.eq</code>. This is the 
	 * user-supplied search field name.
	 * @param mappedField the field to be used in the resulting query
	 * @param prefix the string to prepend to the searchField when generating 
	 * the final expression 
	 * @throws SearchSyntaxException
	 */
	public SearchTerm(String searchFieldWithOperator, String mappedField, String prefix) 
	throws SearchSyntaxException 
	{
		this(searchFieldWithOperator, prefix);
		setMappedField(mappedField);	
	}
	
	/**
	 * Full argument constructor. This will overwrite any operator specified
	 * in the searchFieldWithOperator field.
	 * 
	 * @param searchFieldWithOperator {@link SearchTerm#searchField} with the 
	 * operation appended to the end. Ex. <code>name.eq</code>.
	 * @param mappedField the field to be used in the resulting query
	 * @param operator the conditional operation to perform on the mapped 
	 * field in the resulting query 
	 * @param prefix the string to prepend to the searchField when generating 
	 * the final expression 
	 * @throws SearchSyntaxException
	 */
	public SearchTerm(String searchFieldWithOperator, String mappedField, Operator operator, String prefix)
	throws SearchSyntaxException
	{
		this(searchFieldWithOperator, mappedField, prefix);
		setOperator(operator);
		
	}
	
	/**
	 * Assigns the {@link SearchTerm#searchField} and {@link SearchTerm#operator} 
	 * fields by parsing the given searchFieldWithOperator and splitting on the 
	 * last ".". For example, <code>name.eq</code> would result in a {@link SearchTerm#searchField}
	 * of "name" and an {@link SearchTerm#operator} of {@link SearchTerm.Operator#EQ}  
	 * If the searchFieldWithOperator value does specify an operator, equivalence
	 * is used.
	 * @param searchFieldWithOperator user-supplied search field with operator
	 * @throws SearchSyntaxException
	 */
	private void parseSearchField(String searchFieldWithOperator) 
	throws SearchSyntaxException
	{
		String field = StringUtils.trimToEmpty(searchFieldWithOperator);
		
		if (field.contains(".")) 
		{
			setSearchField(StringUtils.substringBeforeLast(field, "."));
			
			try {
				Operator op = Operator.valueOf(StringUtils.upperCase(StringUtils.substringAfterLast(field, ".")));
				setOperator(op);
			} catch(IllegalArgumentException e) {
			    setOperator(Operator.EQ);
			    setSearchField(field);
//			    throw new SearchSyntaxException("Invalid operator in the " + searchField + " field. "
//						+ "Valid operators are " + StringUtils.join(Operator.values(), ", "));
			}
		} 
		else 
		{
			setSearchField(field);
			setOperator(Operator.EQ);
		}
	}

	

	/**
	 * @return the searchField
	 */
	public String getSearchField() {
		return searchField;
	}
	
	/**
	 * Strips dot notation in search term association references.
	 * 
	 * @return {@link #searchField} without periods
	 */
	public String getSafeSearchField() {
	    return StringUtils.remove(getSearchField(), '.');
    }

	/**
	 * @param searchField the searchField to set
	 */
	public void setSearchField(String searchField)
	throws SearchSyntaxException 
	{
		if (StringUtils.isEmpty(searchField)) {
			throw new SearchSyntaxException("searchField in search term cannot be null");
		}
		this.searchField = searchField;
	}

	/**
	 * @return the mappedField
	 */
	public String getMappedField() {
		return mappedField;
	}

	/**
	 * @param mappedField the mappedField to set
	 */
	public void setMappedField(String mappedField) 
	throws SearchSyntaxException 
	{
		if (StringUtils.isEmpty(mappedField)) {
			throw new SearchSyntaxException("mappedField in search term cannot be null");
		}
		this.mappedField = mappedField;
	}

	/**
	 * @return the operator
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * @param operator the operator to set
	 */
	public void setOperator(Operator operator) 
	throws SearchSyntaxException 
	{
		if (operator == null) {
			throw new SearchSyntaxException("Search operator cannot be null");
		}
		this.operator = operator;
	}

	/**
	 * @return
	 */
	public String getPrefix() {
		return prefix == null ? "" : prefix;
	}

	/**
	 * @param prefix
	 */
	public void setPrefix(String prefix) {
		
		this.prefix = (prefix == null ? "" : prefix);
	}

	/**
	 * The conditional expression generated from the mapping of searchTerm
	 * to mappedField. This expression is later used in the where clause 
	 * of a SQL query.
	 * @return conditional expression representing a clause in a where statement
	 */
	public String getExpression()
	{  
		String prefixedMappedfield = getMappedField().replaceAll("%s", getPrefix());
		if (this.operator == Operator.BETWEEN) {
			return String.format(operator.getTemplate(), 
			        prefixedMappedfield, 
			        "%Y-%m-%d %H:%i",
			        getSafeSearchField(), 
			        prefixedMappedfield, 
			        "%Y-%m-%d %H:%i",
			        getSafeSearchField());
		} else if (this.operator == Operator.ON) {
		    return String.format(operator.getTemplate(), 
                    prefixedMappedfield,
                    "%Y-%m-%d",
                    getSafeSearchField(),
                    "%Y-%m-%d");
		} else if (this.operator == Operator.BEFORE || this.operator == Operator.AFTER) {
            return String.format(operator.getTemplate(), 
                    prefixedMappedfield, "%Y-%m-%d", getSafeSearchField());
        } else {
			return String.format(operator.getTemplate(), 
					prefixedMappedfield, getSafeSearchField());
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.searchField;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SearchTerm t)
	{
		if (StringUtils.equals(this.searchField, t.searchField)) {
			if (StringUtils.equals(this.prefix, t.prefix)) {
				return this.operator.compareTo(t.operator);
			} else {
				return this.prefix.compareTo(t.prefix);
			}
		} else {
			return this.searchField.compareTo(t.searchField);
		}
	}

	/**
	 * Compares the search field in this object to another string.
	 * This is used for quick existence checking for search terms
	 * that have already been defined.
	 * @param anotherString
	 * @return
	 */
	public int compareTo(String anotherString)
	{
		return searchField.compareTo(anotherString);
	}
}