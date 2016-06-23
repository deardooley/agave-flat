package org.iplantc.service.jobs.util;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/28/13
 * Time: 1:51 PM
 * To change this template use File | Settings | File Templates.
 */
class GCommonTestUtil {

    /**
     * This method gets the current working directory
     * @return String of directory path
     */
    public String getWorkDirectory(){
        String wd = "";
        try {
            wd = new File("test.txt").getCanonicalPath().replace("/test.txt","");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wd;
    }

}
