package org.iplantc.service.jobs.managers.monitors.parsers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 2/8/13
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class CondorJobMonitorResponseParser implements JobMonitorResponseParser {
    File logFile;                     // the runtime log from condor
    List<String> logLines;            // lines of the job log from condor
    Map<String,String> logSections;   // map with condor code as key for log message
    boolean isValid;                  // is the log file valid for processing
    boolean isFinished;               // does the terminated code appear in the log
    boolean isSubmitted;
    boolean isRunning;
    boolean isFailed;

    static final String TERMINATED = "005";
    static final String MISSING_INPUT = "884";
    static final String MISSING_OUTPUT = "769";
    static final String EXECUTING  = "001";
    static final String SUBMITTED  = "000";
    static final String ABORTED    = "009";
    static final String HELD       = "012";
    static final String FAILED     = "007";


    /**
     * Default constructor
     */
    public CondorJobMonitorResponseParser(){}

    /**
     * Constructor with logFile input to fully parse file for processing
     * @param logFile File to be parsed
     */
    public CondorJobMonitorResponseParser(File logFile) throws IOException {
        this.logFile = logFile;
        if(logFile != null){
            logLines = FileUtils.readLines(logFile);
            init();
        }
    }

    /**
     * Condition to test if Job failed or succeeded
     * @return boolean value to indicated if the job failed
     */
    public boolean isJobFailed() {
        boolean status = logSections.containsKey(FAILED) || logSections.containsKey(ABORTED) || 
        		logSections.containsKey(MISSING_INPUT) || logSections.containsKey(MISSING_OUTPUT) ;
        if(status){
            return this.isFailed = true;
        }
        return isFailed = false;
    }

    /**
     * Checks to see if job is finished for our purposes ie job is terminated and output is available.
     * @return
     */
    public boolean isJobFinished() {
        if(logSections.containsKey(TERMINATED)){
            return this.isFinished = true;
        }
        return isFinished = false;
    }
    
    /**
     * Checks to see if job is running for our purposes ie job is executing on Condor and shows up in queue.
     * @return
     */
    public boolean isJobRunning(String response) {
    	logLines = Arrays.asList(StringUtils.stripToEmpty(response).split("[\\r\\n]+"));
        init();
        return isRunning;
    }
    
    /**
     * Checks to see if job is running for our purposes ie job is executing on Condor and shows up in queue.
     * @return
     */
    public boolean isJobRunning() {
        return isRunning;
    }

    public boolean isJobSubmitted() {
        isSubmitted = logSections.containsKey(CondorJobMonitorResponseParser.SUBMITTED);
        return isSubmitted;
    }

    /**
     *
     * @return
     */
    private boolean isValidLogFile(){
        String firstLine = logLines.get(0);
        if(firstLine.contains(SUBMITTED)){
            isValid = true;
            return true;
        }
        isValid = false;
        return false;
    }

    /**
     *
     * @param index
     * @return
     */
    private int readToSectionEnd(int index){
        // this is the first line of the section
        String line = logLines.get(index);
        String code = line.substring(0,3);
        logSections.put(code,line);  // add the first line of the section
        // read the next lines
        while(!line.contains("...")){
            line = logLines.get(index++);
        }
        return index;
    }



    /**
     * walk the lines marking section starting lines
     * a section start looks like "000 (154.000.000) 01/31 14:27:43 Job submitted from host: <129.116.126.94:56091> "
     * the "xxx" three digit field beginning the line marks the beginning of a section and can translate what that
     * section does
     * "..." three periods isolated on a line end a section
     */
    private void init() {
        if(logLines != null && !logLines.isEmpty()){

            if(isValidLogFile()){
                logSections = new HashMap<String,String>();
                // are there any lines to read
                int index_start = 0;
                while(index_start < logLines.size()){
                    // send index of section start
                    index_start = readToSectionEnd(index_start);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File log = new File("src/test/resources/condor_jobs/sterry1-1-1369597889740/runtime.log");
        CondorJobMonitorResponseParser pcjl = new CondorJobMonitorResponseParser(log);
        System.out.println("is failed "+pcjl.isJobFailed());
        System.out.println("is submitted "+pcjl.isJobSubmitted());
        System.out.println("is running "+pcjl.isJobRunning());
        System.out.println("is finished "+pcjl.isJobFinished());
    }

}

