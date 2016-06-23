package org.iplantc.service.common.quartz.util.ui;

/**
 * Created by silviu
 * Date: 22/03/14 / 17:06261
 * <p/>
 * <p>
 * describes a quartz trigger : id, cron expression and state (paused y/n).
 * </p>
 * <p/>
 * <br/>
 * rss-reader|eu.pm.tools.quartz.ui
 *
 * @author Silviu Ilie
 */
public class SimpleCronExpression {

    public String id;
    public String expression;
    public boolean paused;

    public SimpleCronExpression(String id, String expression, boolean paused) {
        this.id = id;
        this.expression = expression;
        this.paused = paused;
    }

    public String getId() {
        return id;
    }

    public String getExpression() {
        return expression;
    }
}
