package org.iplantc.service.io.util;

/*
 * @(#)Grep.java	1.3 01/12/13
 * Search a list of files for lines that match a given regular-expression
 * pattern.  Demonstrates NIO mapped byte buffers, charsets, and regular
 * expressions.
 *
 * Copyright 2001-2002 Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 * notice, this  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * 
 * Neither the name of Oracle and/or its affiliates. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY 
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR 
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR 
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class GrepUtil {
	private static final Logger log = Logger.getLogger(GrepUtil.class);
	
	// Charset and decoder for ISO-8859-15
	private Charset charset = Charset.forName("ISO-8859-15");
	private CharsetDecoder decoder = charset.newDecoder();

	// Pattern used to parse lines
	private Pattern linePattern = Pattern.compile(".*\r?\n");

	// The input pattern that we're looking for
	private Pattern pattern;

	public GrepUtil(String pattern) {
		compile(pattern);
	}

	// Compile the pattern from the command line
	//
	private void compile(String pat) {
		try {
			pattern = Pattern.compile(pat);
		} catch (PatternSyntaxException x) {
			System.err.println(x.getMessage());
			System.exit(1);
		}
	}

	// Use the linePattern to break the given CharBuffer into lines, applying
	// the input pattern to each line to see if we have a match
	//
	@SuppressWarnings("unused")
	private boolean grep(CharBuffer cb) {
		Matcher lm = linePattern.matcher(cb); // Line matcher
		Matcher pm = null; // Pattern matcher
		
		int lines = 0;
		while (lm.find()) {
			lines++;
			CharSequence cs = lm.group(); // The current line
			if (pm == null)
				pm = pattern.matcher(cs);
			else
				pm.reset(cs);
			if (pm.find()) {
				log.debug("Found matching line " + cs);
				return true;
			}
			
			if (lm.end() == cb.limit())
				break;
		}
		return false;
	}
	
	/**
	 * Most files won't be local, so we need to stream a portion of them in from the remote store
	 * to process for file type info.
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 * @throws RemoteDataException 
	 * @throws URISyntaxException 
	 */
	@SuppressWarnings({ "unused" })
	public boolean grep(RemoteDataClient client, String remotePath) throws RemoteDataException, IOException {
		InputStream fis = null;
		try {
			// Open the file and then get a channel from the stream
			fis = client.getInputStream(remotePath, true);
			byte[] bytes = new byte[4096]; // increase to parse more of the input file
			fis.read(bytes, 0, bytes.length);
			fis.close();
			
			String s = new String(bytes);
			
			Matcher lm = linePattern.matcher(s); // Line matcher
			Matcher pm = null; // Pattern matcher
			int lines = 0;
			while (lm.find()) 
			{
				lines++;
				CharSequence cs = lm.group(); // The current line
				if (pm == null)
					pm = pattern.matcher(cs);
				else
					pm.reset(cs);
				if (pm.find()) {
					log.debug("Found matching line " + cs);
					return true;
				}
				
				if (s.endsWith(cs.toString()))
					break;
			}
			return false;
		} 
		finally 
		{
			// Close the channel and the stream
			try { fis.close(); } catch (Exception e) {}
			try { client.disconnect(); } catch (Exception e) {}
		}
	}

	// Search for occurrences of the input pattern in the given file
	//
	public boolean grep(File f) throws IOException {
		FileInputStream fis = null;
		FileChannel fc = null;
		try {
			// Open the file and then get a channel from the stream
		
			fis = new FileInputStream(f);
			fc = fis.getChannel();
	
			// Get the file's size and then map it into memory
			int sz = (int) fc.size();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
	
			// Decode the file into a char buffer
			CharBuffer cb = decoder.decode(bb);
	
			// Perform the search
			return grep(cb);
		} finally {
			// Close the channel and the stream
			try { fis.close(); } catch (Throwable e) {}
			try { fc.close(); } catch (Throwable e) {}
		}
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java Grep pattern file...");
			return;
		}
		GrepUtil gu = new GrepUtil(args[0]);
		
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]);
			try {
				gu.grep(f);
			} catch (IOException x) {
				System.err.println(f + ": " + x);
			}
		}
	}

}
