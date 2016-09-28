package org.iplantc.service.common.util;

import org.apache.commons.lang3.StringUtils;

public class AgaveStringUtils {
    
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

}
