package org.iplantc.service.uuid.utils;

import java.util.Date;

import org.iplantc.service.uuid.utils.BindingUtils;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class QueryParameterUtils {

    /**
     * Throws an {@link IllegalArgumentException} if no Integer can be parsed
     * from the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkInteger(String name, String value) {
        toInteger(name, value);
    }

    /**
     * Returns the Integer parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Integer can be parsed.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Integer toInteger(String name, String value) {
        try {
            return BindingUtils.toInteger(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Long can be parsed from
     * the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkLong(String name, String value) {
        toLong(name, value);
    }

    /**
     * Returns the Long parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Long can be parsed.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Long toLong(String name, String value) {
        try {
            return BindingUtils.toLong(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Date can be parsed from
     * the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkDate(String name, String value) {
        toDate(name, value);
    }

    /**
     * Returns the Date parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Date can be parsed.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Date toDate(String name, String value) {
        try {
            return BindingUtils.toDate(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Double can be parsed
     * from the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkDouble(String name, String value) {
        toDouble(name, value);
    }

    /**
     * Returns the Double parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Double can be parsed.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Double toDouble(String name, String value) {
        try {
            return BindingUtils.toDouble(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Float can be parsed from
     * the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkFloat(String name, String value) {
        toFloat(name, value);
    }

    /**
     * Returns the Float parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Float can be parsed.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Float toFloat(String name, String value) {
        try {
            return BindingUtils.toFloat(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if no Boolean can be parsed
     * from the variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static void checkBoolean(String name, String value) {
        toBoolean(name, value);
    }

    /**
     * Returns the Boolean parsed from the variable's value. Throws an
     * {@link IllegalArgumentException} if no Boolean can be parsed from the
     * variable's value.
     * 
     * @param name
     *            The name of the query parameter.
     * @param value
     *            The variable's value.
     */
    public static Boolean toBoolean(String name, String value) {
        try {
            return BindingUtils.toBoolean(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Bad query parameter [" + name + "]: " + e.getMessage());
        }
    }
}
