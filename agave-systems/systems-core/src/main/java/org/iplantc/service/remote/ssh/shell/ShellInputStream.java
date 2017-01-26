package org.iplantc.service.remote.ssh.shell;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ShellInputStream
extends InputStream {
    private String beginCommandMarker;
    private byte[] endCommandMarker;
    private byte[] promptMarker;
    private int markerPos;
    private StringBuffer currentLine;
    private String cmd;
    private StringBuffer commandOutput = new StringBuffer();
    private boolean expectingEcho = true;
    private int exitCode = Integer.MIN_VALUE;
    private Shell shell;
    private BufferedInputStream sessionIn;
    private boolean active = true;
    private boolean matchPromptMarker;
    private static Logger log = LoggerFactory.getLogger(ShellInputStream.class);
    private static boolean verboseDebug = Boolean.getBoolean("maverick.shell.verbose");

    ShellInputStream(Shell shell, String beginCommandMarker, String endCommandMarker, String cmd, boolean matchPromptMarker, String promptMarker) {
        this.beginCommandMarker = beginCommandMarker;
        this.endCommandMarker = endCommandMarker.getBytes();
        this.matchPromptMarker = matchPromptMarker;
        this.promptMarker = promptMarker.getBytes();
        this.shell = shell;
        this.cmd = cmd;
        this.sessionIn = shell.sessionIn;
    }

    public int getExitCode() throws IllegalStateException {
        return this.exitCode;
    }

    public boolean isComplete() {
        return this.exitCode == Integer.MIN_VALUE;
    }

    public boolean hasSucceeded() {
        return this.exitCode == 0;
    }

    public String getCommandOutput() {
        return this.commandOutput.toString().trim();
    }

    private String readLine() throws IOException {
        int ch;
        this.sessionIn.mark(-1);
        StringBuffer line = new StringBuffer();
        do {
            if ((ch = this.sessionIn.read()) <= -1) continue;
            line.append((char)ch);
        } while (ch != 10 && ch != 13 && ch != -1);
        this.sessionIn.mark(1);
        if (ch == 13 && this.sessionIn.read() != 10) {
            this.sessionIn.reset();
        }
        if (ch == -1 && line.toString().trim().length() == 0) {
            return null;
        }
        return line.toString().trim();
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        int ch = this.read();
        if (ch > -1) {
            buf[off] = (byte)ch;
            return 1;
        }
        return -1;
    }

    public int read() throws IOException {
        int ch;
        if (this.expectingEcho) {
            String tmp;
            if (log.isDebugEnabled()) {
                log.debug(this.cmd + ": Expecting begin marker");
            }
            while ((tmp = this.readLine()) != null && !tmp.endsWith(this.beginCommandMarker)) {
            }
            if (tmp == null) {
                if (log.isDebugEnabled()) {
                    log.debug(this.cmd + ": Failed to read from shell whilst waiting for begin marker");
                }
                this.shell.internalClose();
                return -1;
            }
            this.currentLine = new StringBuffer();
            this.expectingEcho = false;
            if (log.isDebugEnabled()) {
                log.debug(this.cmd + ": Found begin marker");
            }
        }
        int readLength = Math.max(this.endCommandMarker.length, this.promptMarker.length);
        this.sessionIn.mark(readLength);
        boolean endMarkerMatched = false;
        boolean promptMarkerMatched = false;
        boolean collectExitCode = true;
        byte[] selectedMarker = null;
        StringBuffer tmp = new StringBuffer();
        do {
            ch = this.sessionIn.read();
            if (!endMarkerMatched && !promptMarkerMatched) {
                if (this.markerPos < this.endCommandMarker.length && this.endCommandMarker[this.markerPos] == ch) {
                    endMarkerMatched = true;
                    readLength = this.endCommandMarker.length;
                    selectedMarker = this.endCommandMarker;
                } else {
                    if (!this.matchPromptMarker || this.promptMarker[this.markerPos] != ch) break;
                    promptMarkerMatched = true;
                    readLength = this.promptMarker.length;
                    selectedMarker = this.promptMarker;
                    collectExitCode = false;
                }
            } else if (endMarkerMatched) {
                if (this.endCommandMarker[this.markerPos] != ch) {
                    endMarkerMatched = false;
                    break;
                }
            } else if (promptMarkerMatched && this.promptMarker[this.markerPos] != ch) {
                promptMarkerMatched = false;
                break;
            }
            tmp.append((char)ch);
        } while (this.markerPos++ < readLength - 1 && (endMarkerMatched || promptMarkerMatched));
        if (selectedMarker != null && this.markerPos == selectedMarker.length) {
            if (log.isDebugEnabled()) {
                log.debug(this.cmd + ": " + tmp.toString());
            }
            this.cleanup(collectExitCode, collectExitCode ? "end" : "prompt");
            return -1;
        }
        this.sessionIn.reset();
        ch = this.sessionIn.read();
        if (ch == -1) {
            this.cleanup(false, "EOF");
            return -1;
        }
        this.markerPos = 0;
        this.currentLine.append((char)ch);
        this.commandOutput.append((char)ch);
        if (ch == 10) {
            this.currentLine = new StringBuffer();
        }
        if (verboseDebug && log.isDebugEnabled()) {
            log.debug(this.cmd + ": Current Line [" + this.currentLine.toString() + "]");
        }
        this.sessionIn.mark(-1);
        return ch;
    }

    void cleanup(boolean collectExitCode, String markerType) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(this.cmd + ": Found " + markerType + " marker");
        }
        this.exitCode = collectExitCode ? this.collectExitCode() : -2147483647;
        this.shell.state = 1;
        this.active = false;
    }

    boolean isActive() {
        return this.active;
    }

    int collectExitCode() throws IOException {
        char ch;
        if (log.isDebugEnabled()) {
            log.debug(this.cmd + ": Looking for exit code");
        }
        StringBuffer tmp = new StringBuffer();
        int exitCode = -1;
        do {
            ch = (char)this.sessionIn.read();
            tmp.append(ch);
        } while (ch != '\n');
        try {
            exitCode = Integer.parseInt(tmp.toString().trim());
            if (log.isDebugEnabled()) {
                log.debug(this.cmd + ": Exit code is " + exitCode);
            }
        }
        catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug(this.cmd + ": Failed to get exit code: " + tmp.toString().trim());
            }
            exitCode = -2147483647;
        }
        return exitCode;
    }
}