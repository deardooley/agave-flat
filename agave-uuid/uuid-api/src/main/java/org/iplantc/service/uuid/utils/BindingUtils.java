package org.iplantc.service.uuid.utils;

import java.util.Date;

import org.restlet.engine.util.StringUtils;

public class BindingUtils {

    /**
     * Throws an {@link IllegalArgumentException} if no Integer can be parsed
     * from the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkInteger(String value) {
        toInteger(value);
    }

    /**
     * Returns the Integer parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Integer can be parsed.
     * 
     * @param value
     *            The variable's value.
     */
    public static Integer toInteger(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value ["
                    + value + "]");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Long can be parsed from
     * the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkLong(String value) {
        toLong(value);
    }

    /**
     * Returns the Long parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Long can be parsed.
     * 
     * @param value
     *            The variable's value.
     */
    public static Long toLong(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long value [" + value
                    + "]");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Date can be parsed from
     * the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkDate(String value) {
        toDate(value);
    }

    /**
     * Returns the Date parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Date can be parsed.
     * 
     * @param value
     *            The variable's value.
     */
    public static Date toDate(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        try {
            return new Date(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid epoch timestamp ["
                    + value + "]");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Double can be parsed
     * from the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkDouble(String value) {
        toDouble(value);
    }

    /**
     * Returns the Double parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Double can be parsed.
     * 
     * @param value
     *            The variable's value.
     */
    public static Double toDouble(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid double value [" + value
                    + "]");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Float can be parsed from
     * the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkFloat(String value) {
        toFloat(value);
    }

    /**
     * Returns the Float parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Float can be parsed.
     * 
     * @param value
     *            The variable's value.
     */
    public static Float toFloat(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid float value [" + value
                    + "]");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Boolean can be parsed
     * from the variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static void checkBoolean(String value) {
        toBoolean(value);
    }

    /**
     * Returns the Boolean parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Boolean can be parsed from the
     * variable's value.
     * 
     * @param value
     *            The variable's value.
     */
    public static Boolean toBoolean(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid boolean value ["
                    + value + "]");
        }
    }
}
