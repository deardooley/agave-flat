package org.iplantc.service.remote.local;

public class CmdLineProcessOutput {
    int exitCode;
    String condorJobId;
    String outString;
    String errString;

    public CmdLineProcessOutput(int exitCode, String outString, String errString){
        this.exitCode = exitCode;
        this.outString = outString;
        this.errString = errString;
    }

    public String getOutString(){
        return outString;
    }

    public String getErrString(){
        return errString;
    }

    public int getExitCode(){
        return exitCode;
    }

    public String getCondorJobId(){
        return condorJobId;
    }
}
