package org.iplantc.service.common.quartz.util;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by silviu
 * Date: 16/02/14 / 00:07538
 * <p>
 * <p/>
 * describes an action response.
 * <p/>
 * </p>
 * <br/>
 *
 * @author Silviu Ilie
 */
public class QuartzConfigResetResponse {

    public enum Type {
        ERROR, SUCCESS
    }

    private String message;
    private Type type;
    private Date momentum;

    /**
     *  all responses.
     */
    private static final ArrayList<QuartzConfigResetResponse> allResponses = new ArrayList<QuartzConfigResetResponse>();


    /**
     * lists responses.
     * @return {@code ArrayList<QuartzConfigResetResponse>}
     */
    public static synchronized ArrayList<QuartzConfigResetResponse> list() {
        return allResponses;
    }

    /**
     * gather all responses.
     *
     * @param response {@link QuartzConfigResetResponse}.
     */
    private synchronized void _addResponse(QuartzConfigResetResponse response) {
        allResponses.add(response);
    }

    QuartzConfigResetResponse(final String message, final Type result) {
        this.message = message;
        this.type = result;
        this.momentum = new Date();

        _addResponse(this);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    public Date getMomentum() {
        return momentum;
    }
}
