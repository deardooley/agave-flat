package org.iplantc.service.remote.local;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;


/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/27/13
 * Time: 2:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class CmdLineProcessHandler {
    private static final Logger log	= Logger.getLogger(CmdLineProcessHandler.class);

    DefaultExecutor executor = new DefaultExecutor();
    public CollectingLogOutputStream clog = new CollectingLogOutputStream();
    CollectingLogOutputStream elog = new CollectingLogOutputStream();
    InputStream is=null;
    int exitValue = -1;
    ExecuteWatchdog watchdog = new ExecuteWatchdog(6000); // default timeout for child process
    CmdLineProcessOutput cpo;

    public CmdLineProcessOutput getProcessOutput(){
        return cpo;
    }

    /**
     * Executes the commandline string and collects the std_out and std_err of the child process.
     * @param commandLine the commands sent via command line to shell
     * @return the exitValue from the child process
     */
    public int executeCommand(String commandLine){
        // runs commands in a bash login shell
        log.debug("Attempted command : "+commandLine);
        CommandLine command = new CommandLine("/bin/bash");
        command.addArgument("-l");
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        try {
            is = new ByteArrayInputStream(commandLine.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        PumpStreamHandler streamHandler = new PumpStreamHandler(clog,elog,is);
        executor.setStreamHandler(streamHandler);
//        executor.setWatchdog(watchdog);

        // execute command
        try {
            executor.execute(command, resultHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the process to complete
        try {
            resultHandler.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exitValue = resultHandler.getExitValue();
        cpo = new CmdLineProcessOutput(exitValue,clog.getLinesAsString(),elog.getLinesAsString());
        
        return exitValue;
    }

    /**
    * Must be called before executeCommand or default setting will be used
    * @param timeOut long value of milli seconds
    */
    public void setWatchdogTimeOut(long timeOut){
        watchdog = new ExecuteWatchdog(timeOut);
    }

    /**
     * Log the output of the process to File if needed
     * @param processOutput either the out or err outputstream of the child process
     * @param outputFile    the file to write to
     * @throws IOException  if any of the file operations fail throw an IOException
     */
    public void logOutputToFile(CollectingLogOutputStream processOutput,File outputFile) 
    throws IOException
    {
        if(!outputFile.exists()){
            outputFile.createNewFile();
        }
        
        FileOutputStream fos = null;
        try {
	        fos = new FileOutputStream(outputFile);
	        fos.write(processOutput.getLinesAsString().getBytes());
        } 
        finally {
        	try { fos.close(); } catch (Exception e) {}
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CmdLineProcessHandler iph = new CmdLineProcessHandler();

        iph.executeCommand("sleep 3; echo 'Hello'\n\n");

        if(iph.watchdog.killedProcess()){
            System.out.println("killed the child process \n"+iph.elog.getLinesAsString());
        }
        System.out.println(iph.clog.getLinesAsString());
        //iph.executeCommand("cd /Users/steve/condor/steve/job1; condor_submit steve.condor\n\n");
    }

}
