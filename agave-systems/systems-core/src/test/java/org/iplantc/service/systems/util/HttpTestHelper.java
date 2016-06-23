package org.iplantc.service.systems.util;

import org.iplantc.service.remote.local.CmdLineProcessHandler;
import org.iplantc.service.remote.local.CmdLineProcessOutput;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 4/11/13
 * Time: 10:12 PM
 * To change this template use File | Settings | File Templates.
 */

public class HttpTestHelper{
    Map<String,String> fileList = new HashMap<String,String>();

    private String user = "";
    private String passwd = "";
    private String verb = "GET";
    private String hostRoot = "http://localhost:8080/";
    private String endpoint ="";
    private String pathToFile="";
    private String formData="";
    private String command;
    @SuppressWarnings("unused")
	private String curlSimpleCommand = "curl -X "+verb+" -sku "+user+":"+passwd+" "+hostRoot+endpoint;


    public HttpTestHelper(String verb, String hostRoot, String endpoint, String user, String passwd){
        this.hostRoot = hostRoot;
        this.endpoint = endpoint;
        this.user = user;
        this.passwd = passwd;
        this.verb = verb;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPathToFile() {
        return pathToFile;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    public String getHostRoot() {

        return hostRoot;
    }

    public void setHostRoot(String hostRoot) {
        this.hostRoot = hostRoot;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    @SuppressWarnings("unused")
	private void loadSoftwareFileList(){
        String root = "src/test/resources/software/";
        fileList.put("head-lonestar",root + "head-lonestar.tacc.teragrid.org.json");
        fileList.put("head-stampede",root + "head-stampede.tacc.utexas.edu.json");
        fileList.put("head-trestles",root + "head-trestles.sdsc.teragrid.org.json");
        fileList.put("system-software",root + "system-software.json");
        fileList.put("wca-iplant-condor",root + "wca-iplant-condor.tacc.utexas.edu.json");
        fileList.put("wc-iplant-condor",root + "wc-iplant-condor.tacc.utexas.edu.json");
    }

    /**
     * Executes curl command as os process and collects the results
     * @return CmdLineProcessOutput contains the std out and err of the command line execution.
     */
    private CmdLineProcessOutput curl(){
        CmdLineProcessHandler cmdLineProcessHandler = new CmdLineProcessHandler();
        int exitCode = cmdLineProcessHandler.executeCommand(command);
        System.out.println(" Exitcode : " +exitCode);
        if (exitCode != 0) {
            System.out.println("do something here on error");
            //throw new JobException("Job exited with error code " + exitCode + " please check your arguments and try again.");
        }
        CmdLineProcessOutput cmdLineProcessOutput = cmdLineProcessHandler.getProcessOutput();

        System.out.println("stderr : "+cmdLineProcessOutput.getErrString());
        System.out.println("stdout : "+cmdLineProcessOutput.getOutString());
        return  cmdLineProcessOutput;
    }

    public CmdLineProcessOutput curlFileUpload(String pathToFile){
        String curlFileUploadCommand = "curl -X "+verb+" -sku \""+user+":"+passwd+"\" -F \"fileToUpload="+pathToFile+"\" "+hostRoot+endpoint;
        command = curlFileUploadCommand;
        return curl();
    }

    // assumes formData is not encoded
    @SuppressWarnings("deprecation")
	public CmdLineProcessOutput curlFormPost(String formData){
        formData = URLEncoder.encode(formData);
        String curlFormPostCommand = "curl -X "+verb+" -sku \""+user+":"+passwd+"\" --data \""+formData+"\" "+hostRoot+endpoint;
        command = curlFormPostCommand;
        return curl();
    }

    public CmdLineProcessOutput curlSimple(){
        return curl();
    }

    public static void main(String[] args){
        HttpTestHelper helper = new HttpTestHelper("POST","http://localhost:8080/","apps-v1/apps/list","sterry1","anna1200");
        //helper.loadSoftwareFileList();
        //helper.curlPostFile(helper.fileList.get("wca-iplant-condor"));
        //helper.curlFileUpload("tmp/file.json");
        helper.curlFormPost("var1=steve&var2=left is; better");

    }

}




