/**
 * 
 */
package org.iplantc.service.jobs.managers.parsers;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;

/**
 * Parses the output from a condor_sumit command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class CondorJobIdParser implements RemoteJobIdParser 
{
	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException
	{
		// there should be only two lines if things are good
    	List<String> lines = Arrays.asList(output.replaceAll("\r", "\\n").split("\n"));
        if(lines != null && !lines.isEmpty()){
            // lines second element should be ie. "1 job(s) submitted to cluster 95."
            return parseJobNumber(lines.get(1));
        } else {
        	throw new RemoteJobIDParsingException(output); 
        }
	}
	
	/**
     * Parse the output of the condor submit process to acquire the job number
     * @param lineOfText line of text from output that contains the job number
     * @return String of numeric chars of job number.
     */
    private String parseJobNumber(String lineOfText) { // example text "1 job(s) submitted to cluster 95."
        StringTokenizer st= null;
        if (lineOfText.contains("job(s) submitted to cluster"))
        {
            st = new StringTokenizer(lineOfText);
            // cycle thru tokens until we get to the one that represents the job number
            int tokens = 0;
            while(st.hasMoreTokens() && tokens < 5 ){
                st.nextToken();
                tokens++;
            }
        }
        return st.nextToken().trim();//.replace('.',' ').trim();
    }
}
