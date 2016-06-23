package org.iplantc.service.tags.utils;

import java.util.UUID;

import org.iplantc.service.tags.utils.BindingUtils;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class PathVariableUtils {

    /**
     * Throws a {@link ResourceException} with status error 404 if no Integer can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static void checkInteger(String value) {
        toInteger(value);
    }

    /**
     * Returns the Integer parsed from the variable's value. Throws a {@link ResourceException} with status error 404 if
     * no Integer can be parsed.
     *
     * @param value
     *            The variable's value.
     */
    public static Integer toInteger(String value) {
        try {
            return BindingUtils.toInteger(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    /**
     * Throws a {@link ResourceException} with status error 404 if no Long can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static void checkLong(String value) {
        toLong(value);
    }

    /**
     * Returns the Long parsed from the variable's value.
     * Throws a {@link ResourceException} with status error 404 if no Long can be parsed.
     *
     * @param value
     *            The variable's value.
     */
    public static Long toLong(String value) {
        try {
            return BindingUtils.toLong(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    /**
     * Throws a {@link ResourceException} with status error 404 if no Long can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    // The expected date format is an epoch
    public static void checkDate(String value) {
        toDate(value);
    }

    /**
     * Returns the Long parsed from the variable's value.
     * Throws a {@link ResourceException} with status error 404 if no Long can be parsed.
     *
     * @param value
     *            The variable's value.
     */
    // The expected date format is an epoch
    public static Long toDate(String value) {
        try {
            return BindingUtils.toLong(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    /**
     * Throws a {@link ResourceException} with status error 404 if no Double can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static void checkDouble(String value) {
        toDouble(value);
    }

    /**
     * Returns the Double parsed from the variable's value.
     * Throws a {@link ResourceException} with status error 404 if no Double can be parsed.
     *
     * @param value
     *            The variable's value.
     */
    public static Double toDouble(String value) {
        try {
            return BindingUtils.toDouble(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    /**
     * Throws a {@link ResourceException} with status error 404 if no Float can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static void checkFloat(String value) {
        toFloat(value);
    }

    /**
     * Returns the Float parsed from the variable's value.
     * Throws a {@link ResourceException} with status error 400 if
     * no Float can be parsed.
     *
     * @param value
     *            The variable's value.
     */
    public static Float toFloat(String value) {
        try {
            return BindingUtils.toFloat(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    /**
     * Throws a {@link ResourceException} with status error 404 if no Boolean can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static void checkBoolean(String value) {
        toBoolean(value);
    }

    /**
     * Returns the Boolean parsed from the variable's value.
     * Throws a {@link ResourceException} with status error 404 if no Boolean can be parsed from the variable's value.
     *
     * @param value
     *            The variable's value.
     */
    public static Boolean toBoolean(String value) {
        try {
            return BindingUtils.toBoolean(value);
        } catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

}
