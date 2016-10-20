package org.iplantc.service.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class AgaveStringUtils {
    
    /* **************************************************************************** */
    /*                                Constants                                     */
    /* **************************************************************************** */
    public static final String NULL_STRING = "[null]";
    
    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* convertWhitespace:                                                           */
    /* ---------------------------------------------------------------------------- */
    /** Convert all whitespace characters to space characters.
     * This method replaces non-space whitespace characters
     * in a string with space characters, which for example
     * makes error message text acceptable to the restlet 
     * subsystem.
     *  
     * @param s the string to be cleansed of non-space whitespace.
     *          Null or empty strings are returned unchanged.
     * @return the cleansed string or the input if it's null or empty.
     */
    public static String convertWhitespace(String s)
    {
        // Replace all forms of whitespace other than
        // spaces with spaces.
        if (StringUtils.isEmpty(s)) return s;
        return s.replaceAll("[\t\n\\x0B\f\r]", " ");
    }

    /* ---------------------------------------------------------------------------- */
    /* toString:                                                                    */
    /* ---------------------------------------------------------------------------- */
    /** Use reflection to construct a string out of the contents of an object.  The 
     * fields of an object and its superclasses are printed to a string along with
     * the class name and its virtual address.  The address information allows one to 
     * distinguish between different instances of a class.
     * 
     * Be careful not to use this method to inadvertently expose passwords.
     * 
     * Here's an example of a serialized Person object:
     * 
     * <pre>
     * Person@182f0db[
     *   name=John Doe
     *   age=33
     *   smoker=false
     * ]
     * </pre>
     * 
     * @param obj the object whose content will be serialized.  Null is allowed.
     * @return the non-static content of the object represented in a string.
     */
    public static String toString(Object obj) 
    {
     if (obj == null) return NULL_STRING;
     return ReflectionToStringBuilder.toString(obj, ToStringStyle.MULTI_LINE_STYLE);
    }

    /* ---------------------------------------------------------------------------- */
    /* toComparableString:                                                          */
    /* ---------------------------------------------------------------------------- */
    /** Use reflection to construct a string out of the contents of an object.  The 
     * fields of an object and its superclasses are printed to a string along with
     * the class name.  The format is compact and it does not include any information
     * that could distinguish two objects of the same type with the same field values.
     * This latter property allows one to use simple string comparison to determine
     * if two objects contain the same values.
     * 
     * Be careful not to use this method to inadvertently expose passwords.
     *
     * Here's an example of a serialized Person object:
     * 
     * <pre>
     * Person[name=John Doe,age=33,smoker=false]
     * </pre>
     *
     * @param obj the object whose content will be serialized.  Null is allowed.
     * @return the non-static content of the object represented in a string.
     */
    public static String toComparableString(Object obj) 
    {
     if (obj == null) return NULL_STRING;
     return ReflectionToStringBuilder.toString(obj, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
