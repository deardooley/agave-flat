package org.iplantc.service.common.quartz.util;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * use this to describe who can access the utility.
 * </p>
 * created : 12/10/14 20:09
 *
 * @author Silviu Ilie
 */
public interface QuartzUtilityAuthorization {

    boolean authorize(HttpSession session);

}
