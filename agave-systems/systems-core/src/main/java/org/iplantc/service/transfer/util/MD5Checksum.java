package org.iplantc.service.transfer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Checksum {

	// see this How-to for a faster way to convert
	// a byte array to a HEX string
	public static String getMD5Checksum(File file) throws IOException
	{
	    InputStream in = null;
	    try {
	        in = new FileInputStream(file);
	        return DigestUtils.md5Hex(in);
	    } 
	    finally {
	        try {in.close();} catch (Exception e) {}
	    }
	}
}