package org.iplantc.service.remote.local;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.exec.LogOutputStream;

public class CollectingLogOutputStream extends LogOutputStream {

    private final List<String> lines = new LinkedList<String>();

    @Override protected void processLine(String line, int level)
    {
        lines.add(line);
    }

    /**
     * Get the lines of output as a list.
     * @return List consisting of lines of text
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Get the output lines as one long String of text with each line having an eol char.
     * @return String of all lines
     */
    public String getLinesAsString(){
        StringBuffer sb = new StringBuffer();
        for(Object line : this.lines){
            sb.append((String)line);
            sb.append("\n");
        }
        return sb.toString();
    }
}
