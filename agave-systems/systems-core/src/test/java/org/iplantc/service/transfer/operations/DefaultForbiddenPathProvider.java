/**
 * 
 */
package org.iplantc.service.transfer.operations;

/**
 * Default {@link ForbiddenPathProvider} implementation returning standard
 * unix paths traditionally reserved for root users. 
 * 
 * {@code /root/forbidden.txt} will be returned for file paths which should exist
 * {@code /root/forbidden} will be returned for directory paths which should exist
 * {@code /root/forbidden_does_not_exist} will be returned for folder paths which should not exist
 * {@code /root/forbidden_does_not_exist.txt} will be returned for folder paths which should not exist
 * 
 * @author dooley
 *
 */
public class DefaultForbiddenPathProvider implements ForbiddenPathProvider {

    /**
     * Default no-args constructor
     */
    public DefaultForbiddenPathProvider() {}

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.ForbiddenPath#getPath(boolean)
     */
    @Override
    public String getFilePath(boolean shouldExist) {
        return shouldExist ? "/root/forbidden.txt" : "/root/forbidden_does_not_exist.txt";
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.ForbiddenPath#getDirectoryPath(boolean)
     */
    @Override
    public String getDirectoryPath(boolean shouldExist) {
        return shouldExist ? "/root/forbidden" : "/root/forbidden_does_not_exists";
    }

}
