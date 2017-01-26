package org.iplantc.service.remote.ssh.shell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.PseudoTerminalModes;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshIOException;
import com.sshtools.ssh.SshSession;
import com.sshtools.ssh2.Ssh2Client;

public class Shell {
	
	private static final Logger log = Logger.getLogger(Shell.class);
	
    public static final int OS_WINDOWS = 1;
    public static final int OS_LINUX = 2;
    public static final int OS_SOLARIS = 3;
    public static final int OS_AIX = 4;
    public static final int OS_DARWIN = 5;
    public static final int OS_FREEBSD = 6;
    public static final int OS_OPENBSD = 7;
    public static final int OS_NETBSD = 8;
    public static final int OS_HPUX = 9;
    public static final int OS_UNIX = 20;
    public static final int OS_OPENVMS = 21;
    public static final int OS_UNKNOWN = 99;
    private int osType = 99;
    private String osDescription = "Unknown";
    static final String BEGIN_COMMAND_MARKER = "---BEGIN---";
    static final String END_COMMAND_MARKER = "---END---";
    static final String PROCESS_MARKER = "PROCESS=";
    static final String EXIT_CODE_MARKER = "EXITCODE=";
    static final int WAITING_FOR_COMMAND = 1;
    static final int PROCESSING_COMMAND = 2;
    static final int CLOSED = 3;
    BufferedInputStream sessionIn;
    OutputStream sessionOut;
    int state = 1;
    boolean inStartup;
    private String PIPE_CMD = "";
    private String ECHO_COMMAND = "echo";
    private String EOL = "\r\n";
    private String EXIT_CODE_VARIABLE = "%errorlevel%";
    private int SHELL_INIT_PERIOD = 2000;
    List<Runnable> closeHooks = new ArrayList<Runnable>();
    int numCommandsExecuted = 0;
    private static boolean verboseDebug = Boolean.getBoolean("maverick.shell.verbose");
    private StartupInputStream startupIn;
    private ShellController startupController;
    private boolean childShell = false;
    public static final int EXIT_CODE_PROCESS_ACTIVE = Integer.MIN_VALUE;
    public static final int EXIT_CODE_UNKNOWN = -2147483647;
    long startupTimeout;
    long startupStarted;

    public Shell(SshClient ssh) throws SshException, SshIOException, ChannelOpenException, IOException, ShellTimeoutException {
        this(ssh, null, 30000, "dumb", 1024, 80);
    }

    public Shell(SshClient ssh, ShellStartupTrigger trigger) throws SshException, SshIOException, ChannelOpenException, IOException, ShellTimeoutException {
        this(ssh, trigger, 30000, "dumb", 1024, 80);
    }

    public Shell(SshClient ssh, ShellStartupTrigger trigger, long startupTimeout) throws SshException, SshIOException, ChannelOpenException, IOException, ShellTimeoutException {
        this(ssh, trigger, startupTimeout, "dumb", 1024, 80);
    }

    public Shell(SshClient ssh, ShellStartupTrigger trigger, long startupTimeout, String termtype) throws SshException, SshIOException, ChannelOpenException, IOException, ShellTimeoutException {
        this(ssh, trigger, startupTimeout, termtype, 1024, 80);
    }

    public Shell(SshClient ssh, ShellStartupTrigger trigger, long startupTimeout, String termtype, int cols, int rows) throws SshException, SshIOException, ChannelOpenException, IOException, ShellTimeoutException {
        this.startupTimeout = startupTimeout;
        this.startupStarted = System.currentTimeMillis();
        if (verboseDebug && log.isDebugEnabled()) {
            log.debug("Creating session for interactive shell");
        }
        final SshSession session = ((Ssh2Client)ssh).openSessionChannel(5000);
        PseudoTerminalModes pty = new PseudoTerminalModes(ssh);
        if (termtype != null && !session.requestPseudoTerminal(termtype, cols, rows, 0, 0, pty) && log.isEnabledFor(Priority.WARN)) {
            log.warn("Failed to allocate pseudo terminal; Shell may not function as intended");
        }
        if (!session.startShell()) {
            throw new SshException("Server failed to open session channel", 6);
        }
        this.closeHooks.add(new Runnable(){

            public void run() {
                session.close();
            }
        });
        try {
            Thread.sleep(this.SHELL_INIT_PERIOD);
        }
        catch (InterruptedException var10_9) {
            // empty catch block
        }
        this.determineServerType(ssh);
        this.init(session.getInputStream(), session.getOutputStream(), this.osType != 21, trigger);
    }

    Shell(InputStream in, OutputStream out, String eol, String echoCmd, String exitCodeVar, int osType, String osDescription) throws SshIOException, SshException, IOException, ShellTimeoutException {
        this.EOL = eol;
        this.ECHO_COMMAND = echoCmd;
        this.EXIT_CODE_VARIABLE = exitCodeVar;
        this.osType = osType;
        this.osDescription = osDescription;
        this.childShell = true;
        this.init(in, out, true, null);
    }

    public InputStream getStartupInputStream() {
        return this.startupIn;
    }

    void determineServerType(SshClient ssh) {
        String remoteID = ssh.getRemoteIdentification();
        if (remoteID.indexOf("OpenVMS") > 0) {
            this.osType = 21;
            this.PIPE_CMD = "PIPE ";
            this.ECHO_COMMAND = "WRITE SYS$OUTPUT";
            this.EXIT_CODE_VARIABLE = "$SEVERITY";
        }
    }

    void init(InputStream in, OutputStream out, boolean detectSettings, ShellStartupTrigger trigger) throws SshIOException, SshException, IOException, ShellTimeoutException {
        this.sessionIn = new BufferedInputStream(in);
        this.sessionOut = out;
        this.startupIn = new StartupInputStream("---BEGIN---", detectSettings, trigger);
        if (verboseDebug && log.isDebugEnabled()) {
            log.debug("Session creation complete");
        }
    }

    public boolean inStartup() {
        return this.inStartup;
    }

    public ShellReader getStartupReader() {
        return this.startupController;
    }

    public Shell su(String cmd, String password) throws SshIOException, SshException, IOException, ShellTimeoutException {
        return this.su(cmd, password, "assword", new ShellDefaultMatcher());
    }

    public Shell su(String cmd, String password, String promptExpression) throws SshException, SshIOException, IOException, ShellTimeoutException {
        return this.su(cmd, password, promptExpression, new ShellDefaultMatcher());
    }

    public Shell su(String cmd) throws SshException, SshIOException, IOException, ShellTimeoutException {
        ShellProcess process = this.executeCommand(cmd, false, false);
        return new Shell(process.getInputStream(), process.getOutputStream(), this.EOL, this.ECHO_COMMAND, this.EXIT_CODE_VARIABLE, this.osType, this.osDescription);
    }

    public Shell su(String cmd, String password, String promptExpression, ShellMatcher matcher) throws SshException, SshIOException, IOException, ShellTimeoutException {
        ShellProcess process = this.executeCommand(cmd, false, false);
        ShellProcessController contr = new ShellProcessController(process, matcher);
        if (contr.expectNextLine(promptExpression)) {
            if (log.isDebugEnabled()) {
                log.debug("su password expression matched");
            }
            contr.typeAndReturn(password);
        } else if (log.isDebugEnabled()) {
            log.debug("su password expression not matched");
        }
        if (process.isActive()) {
            return new Shell(process.getInputStream(), process.getOutputStream(), this.EOL, this.ECHO_COMMAND, this.EXIT_CODE_VARIABLE, this.osType, this.osDescription);
        }
        throw new SshException("The command failed: " + cmd, 15);
    }

    public ShellProcess sudo(String cmd, String password) throws SshException, ShellTimeoutException, IOException {
        return this.sudo(cmd, password, "assword", new ShellDefaultMatcher());
    }

    public ShellProcess sudo(String cmd, String password, String promptExpression) throws SshException, ShellTimeoutException, IOException {
        return this.sudo(cmd, password, promptExpression, new ShellDefaultMatcher());
    }

    public ShellProcess sudo(String cmd, String password, String promptExpression, ShellMatcher matcher) throws SshException, ShellTimeoutException, IOException {
        ShellProcess process = this.executeCommand(cmd, false, false);
        ShellProcessController contr = new ShellProcessController(process, matcher);
        if (contr.expectNextLine(promptExpression)) {
            if (log.isDebugEnabled()) {
                log.debug("sudo password expression matched");
            }
            contr.typeAndReturn(password);
        } else if (log.isDebugEnabled()) {
            log.debug("sudo password expression not matched");
        }
        return process;
    }

    public boolean isClosed() {
        return this.state == 3;
    }

    private void updateDescription() {
        this.osDescription = this.osType == 3 ? "Solaris" : (this.osType == 4 ? "AIX" : (this.osType == 1 ? "Windows" : (this.osType == 5 ? "Darwin" : (this.osType == 6 ? "FreeBSD" : (this.osType == 7 ? "OpenBSD" : (this.osType == 8 ? "NetBSD" : (this.osType == 2 ? "Linux" : (this.osType == 9 ? "HP-UX" : (this.osType == 21 ? "OpenVMS" : "Unknown")))))))));
    }

    public void exit() throws IOException, SshException {
        this.sessionOut.write(("exit" + this.EOL).getBytes());
        if (this.childShell) {
            while (this.sessionIn.read() > -1) {
            }
        }
        this.close();
    }

    public void close() throws IOException, SshException {
        this.internalClose();
    }

    public String getNewline() {
        if (this.osType == 1) {
            return "\r\n";
        }
        return "\n";
    }

    public synchronized ShellProcess executeCommand(String origCmd) throws SshException {
        return this.executeCommand(origCmd, false, false, "UTF-8");
    }

    public synchronized ShellProcess executeCommand(String origCmd, boolean consume) throws SshException {
        return this.executeCommand(origCmd, false, consume, "UTF-8");
    }

    public synchronized ShellProcess executeCommand(String origCmd, String charset) throws SshException {
        return this.executeCommand(origCmd, false, false, charset);
    }

    public synchronized ShellProcess executeCommand(String origCmd, boolean consume, String charset) throws SshException {
        return this.executeCommand(origCmd, false, consume, charset);
    }

    public synchronized ShellProcess executeCommand(String origCmd, boolean matchPromptMarker, boolean consume) throws SshException {
        return this.executeCommand(origCmd, matchPromptMarker, consume, "UTF-8");
    }

    public synchronized ShellProcess executeCommand(String origCmd, boolean matchPromptMarker, boolean consume, String charset) throws SshException {
        try {
            String cmd = origCmd;
            if (this.state == 2) {
                throw new SshException("Command still active", 4);
            }
            if (this.state == 3) {
                throw new SshException("Shell is closed!", 4);
            }
            this.checkStartupFinished();
            this.state = 2;
            StringBuffer prompt = new StringBuffer();
            if (matchPromptMarker |= (origCmd.startsWith(".") || origCmd.startsWith("source")) && this.osType == 9) {
                int ch;
                this.sessionOut.write(this.EOL.getBytes());
                this.sessionOut.write(this.EOL.getBytes());
                while ((ch = this.sessionIn.read()) > -1 && ch != 10) {
                }
                while ((ch = this.sessionIn.read()) > -1 && ch != 10) {
                    prompt.append((char)ch);
                }
                if (verboseDebug && log.isDebugEnabled()) {
                    log.debug("Prompt is " + prompt.toString().trim());
                }
            }
            if (verboseDebug && log.isDebugEnabled()) {
                log.debug("Executing command: " + cmd);
            }
            String endCommand = this.nextEndMarker();
            String echoCmd = this.osType == 1 ? this.ECHO_COMMAND + " " + "---BEGIN---" + " && " + cmd + " && " + this.ECHO_COMMAND + " " + endCommand + "0" + " || " + this.ECHO_COMMAND + " " + endCommand + "1" + this.EOL : (this.osType == 21 ? this.PIPE_CMD + this.ECHO_COMMAND + " \"" + "---BEGIN---" + "\" && " + cmd + " && " + this.ECHO_COMMAND + " \"" + endCommand + "0\" || " + this.ECHO_COMMAND + "\"" + endCommand + "1\"" + this.EOL : "echo \"---BEGIN---\"; " + cmd + "; echo \"" + endCommand + this.EXIT_CODE_VARIABLE + "\"" + this.EOL);
            this.sessionOut.write(echoCmd.getBytes(charset));
            ++this.numCommandsExecuted;
            ShellProcess process = new ShellProcess(this, new ShellInputStream(this, "---BEGIN---", endCommand, origCmd, matchPromptMarker, prompt.toString().trim()));
            if (consume) {
                while (process.getInputStream().read() > -1) {
                }
            }
            return process;
        }
        catch (SshIOException ex) {
            throw ex.getRealException();
        }
        catch (IOException ex) {
            throw new SshException("Failed to execute command: " + ex.getMessage(), 6);
        }
    }

    public int getNumCommandsExecuted() {
        return this.numCommandsExecuted;
    }

    private void checkStartupFinished() throws IOException {
        if (verboseDebug && log.isDebugEnabled()) {
            log.debug("Checking state of startup controller");
        }
        if (!this.startupIn.isClosed()) {
            if (verboseDebug && log.isDebugEnabled()) {
                log.debug("Shell still in startup mode, draining startup output");
            }
            while (this.startupIn.read() > -1) {
            }
        }
        if (verboseDebug && log.isDebugEnabled()) {
            log.debug("Shell is ready for command");
        }
    }

    private synchronized String nextEndMarker() {
        return "---END---;PROCESS=" + System.currentTimeMillis() + ";" + "EXITCODE=";
    }

    public int getOsType() {
        return this.osType;
    }

    public String getOsDescription() {
        return this.osDescription;
    }

    void type(String string) throws IOException {
        this.write(string.getBytes());
    }

    void write(byte[] bytes) throws IOException {
        this.sessionOut.write(bytes);
    }

    void type(int b) throws IOException {
        this.write(new byte[]{(byte)b});
    }

    void carriageReturn() throws IOException {
        this.write(this.EOL.getBytes());
    }

    void typeAndReturn(String string) throws IOException {
        this.write((string + this.EOL).getBytes());
    }

    void internalClose() {
        this.state = 3;
        for (Runnable r : this.closeHooks) {
            try {
                r.run();
            }
            catch (Throwable var3_3) {}
        }
    }

    class StartupInputStream
    extends InputStream {
        char[] marker1;
        int markerPos;
        StringBuffer currentLine;
        boolean detectSettings;

        StartupInputStream(String marker1str, boolean detectSettings, ShellStartupTrigger trigger) throws SshException, IOException, ShellTimeoutException {
            this.currentLine = new StringBuffer();
            this.detectSettings = detectSettings;
            this.marker1 = marker1str.toCharArray();
            Shell.this.startupController = new ShellController(Shell.this, new ShellDefaultMatcher(), this);
            if (trigger != null) {
                StringBuffer line = new StringBuffer();
                do {
                    int ch;
                    if ((ch = this.internalRead(Shell.this.sessionIn)) != 10 && ch != 13 && ch != -1) {
                        line.append((char)ch);
                    }
                    if (ch == 10) {
                        line.setLength(0);
                    }
                    if (ch != -1) continue;
                    throw new SshException("Shell output ended before trigger could start shell", 20);
                } while (!trigger.canStartShell(line.toString(), Shell.this.startupController));
            }
            if (detectSettings) {
                String cmd = Shell.this.PIPE_CMD + Shell.this.ECHO_COMMAND + " \"" + marker1str + "\" ; " + Shell.this.ECHO_COMMAND + " $?" + "\r\n";
                if (verboseDebug && log.isDebugEnabled()) {
                    log.debug("Performing marker test: " + cmd);
                }
                Shell.this.sessionOut.write(cmd.getBytes());
            }
            Shell.this.inStartup = detectSettings;
        }

        boolean isClosed() {
            return !Shell.this.inStartup;
        }

        int internalRead(InputStream in) throws IOException {
            do {
                try {
                    return in.read();
                }
                catch (SshIOException e) {
                    if (e.getRealException().getReason() == 21) {
                        if (System.currentTimeMillis() - Shell.this.startupStarted <= Shell.this.startupTimeout) continue;
                        throw new SshIOException(new SshException("", 20));
                    }
                    throw e;
                }
                
            } while (true);
        }

        String internalReadLine(InputStream in) throws IOException {
            int ch;
            StringBuffer tmp = new StringBuffer();
            do {
                if ((ch = this.internalRead(in)) <= -1) continue;
                tmp.append((char)ch);
            } while (ch != -1 && ch != 10);
            return tmp.toString().trim();
        }

        public int read() throws IOException {
            if (Shell.this.inStartup) {
                int ch;
                Shell.this.sessionIn.mark(this.marker1.length + 1);
                StringBuffer tmp = new StringBuffer();
                do {
                    try {
                        do {
                            ch = this.internalRead(Shell.this.sessionIn);
                            tmp.append((char)ch);
                        } while (this.markerPos < this.marker1.length - 1 && this.marker1[this.markerPos++] == ch);
                        break;
                    }
                    catch (SshIOException e) {
                        if (e.getRealException().getReason() == 21) {
                            if (System.currentTimeMillis() - Shell.this.startupStarted <= Shell.this.startupTimeout) continue;
                            throw new SshIOException(new SshException("", 20));
                        }
                        throw e;
                    }
                    
                } while (true);
                
                if (this.markerPos == this.marker1.length - 1) {
                    if (verboseDebug && log.isDebugEnabled()) {
                        log.debug("Potentially found test marker [" + this.currentLine.toString() + tmp.toString() + "]");
                    }
                    if ((ch = this.internalRead(Shell.this.sessionIn)) == 13) {
                        if (verboseDebug && log.isDebugEnabled()) {
                            log.debug("Looking good, found CR");
                        }
                        ch = this.internalRead(Shell.this.sessionIn);
                    }
                    if (ch == 10) {
                        if (verboseDebug && log.isDebugEnabled()) {
                            log.debug("Found test marker");
                        }
                        try {
                            this.detect();
                        }
                        catch (SshException e) {
                            throw new SshIOException(e);
                        }
                        return -1;
                    }
                    if (verboseDebug && log.isDebugEnabled()) {
                        log.debug("Detected echo of test marker command since we did not find LF at end of marker ch=" + Integer.valueOf(ch) + " currentLine=" + this.currentLine.toString() + tmp.toString());
                    }
                }
                
                Shell.this.sessionIn.reset();
                ch = this.internalRead(Shell.this.sessionIn);
                this.markerPos = 0;
                this.currentLine.append((char)ch);
                if (ch == 10) {
                    if (verboseDebug && log.isDebugEnabled()) {
                        log.debug("Shell startup (read): " + this.currentLine.toString());
                    }
                    this.currentLine = new StringBuffer();
                }
                if (verboseDebug && log.isDebugEnabled()) {
                    log.debug("Shell startup (read): " + this.currentLine.toString());
                }
                Shell.this.sessionIn.mark(-1);
                return ch;
            }
            return -1;
        }

        /*
         * Enabled aggressive block sorting
         */
        void detect() throws IOException, SshException {
            Shell.this.inStartup = false;
            if (!this.detectSettings) {
                return;
            }
            if (verboseDebug && log.isDebugEnabled()) {
                log.debug("Detecting shell settings");
            }
            String line = this.internalReadLine(Shell.this.sessionIn);
            if (verboseDebug && log.isDebugEnabled()) {
                log.debug("Shell startup (detect): " + line);
            }
            if (line.equals("0") && Shell.this.osType == 99) {
                String tmp;
                if (verboseDebug && log.isDebugEnabled()) {
                    log.debug("This looks like a *nix type machine, setting EOL to CR only and exit code variable to $?");
                }
                Shell.this.EOL = "\r";
                Shell.this.EXIT_CODE_VARIABLE = "$?";
                ShellProcess proc = Shell.this.executeCommand("uname");
                BufferedReader r2 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                line = "";
                while ((tmp = r2.readLine()) != null) {
                    line = line + tmp;
                }
                switch (proc.getExitCode()) {
                    case 0: {
                        if (verboseDebug && log.isDebugEnabled()) {
                            log.debug("Remote side reported it is " + line.trim());
                        }
                        if ((line = line.toLowerCase()).startsWith("Sun")) {
                            Shell.this.osType = 3;
                            break;
                        }
                        if (line.startsWith("aix")) {
                            Shell.this.osType = 4;
                            break;
                        }
                        if (line.startsWith("darwin")) {
                            Shell.this.osType = 5;
                            break;
                        }
                        if (line.startsWith("freebsd")) {
                            Shell.this.osType = 6;
                            break;
                        }
                        if (line.startsWith("openbsd")) {
                            Shell.this.osType = 7;
                            break;
                        }
                        if (line.startsWith("netbsd")) {
                            Shell.this.osType = 8;
                            break;
                        }
                        if (line.startsWith("linux")) {
                            Shell.this.osType = 2;
                            break;
                        }
                        if (line.startsWith("hp-ux")) {
                            Shell.this.osType = 9;
                            break;
                        }
                        Shell.this.osType = 99;
                        break;
                    }
                    case 127: {
                    	if (verboseDebug && log.isDebugEnabled())
                    		log.debug("Remote side does not support uname");
                        break;
                    }
                    default: {
                    	if (verboseDebug && log.isDebugEnabled())
                    		log.debug("uname returned error code " + proc.getExitCode());
                        break;
                    }
                }
            } else if (Shell.this.osType == 99) {
                String cmd = "echo ---BEGIN--- && echo %errorlevel%\r\n";
                Shell.this.sessionOut.write(cmd.getBytes());
                while ((line = this.internalReadLine(Shell.this.sessionIn)) != null && !line.endsWith("---BEGIN---")) {
                    if (line.trim().equals("") || !log.isDebugEnabled()) continue;
                    log.debug("Shell startup: " + line);
                }
                line = this.internalReadLine(Shell.this.sessionIn);
                if (line.equals("0")) {
                    if (verboseDebug && log.isDebugEnabled()) {
                        log.debug("This looks like a Windows machine, setting EOL to CRLF and exit code variable to %errorlevel%");
                    }
                    Shell.this.EOL = "\r\n";
                    Shell.this.EXIT_CODE_VARIABLE = "%errorlevel%";
                    Shell.this.osType = 1;
                }
            }
            Shell.this.updateDescription();
            if (verboseDebug && log.isDebugEnabled()) {
                log.debug("Shell initialized");
            }
        }
    }

}