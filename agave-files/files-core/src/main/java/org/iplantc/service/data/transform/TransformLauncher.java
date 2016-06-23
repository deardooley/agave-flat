/**
 * 
 */
package org.iplantc.service.data.transform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.iplantc.service.io.exceptions.TransformException;

/**
 * Worker class to invoke file transform handlers on file that have been
 * staged into IRODS. The handlers are responsible for calling back to 
 * an I/O instance to notify the service of completion. Handlers must be 
 * local in this version of the service.
 * 
 * @author dooley
 *
 */
public class TransformLauncher {
	private static final Logger log = Logger.getLogger(TransformLauncher.class);
	
	public static void invoke(String handlerPath, String sourcePath, String destPath, String triggerBaseUrl) throws TransformException {
		
		File handlerFile = new File(handlerPath);
		
		// make sure the handler is present prior to invocation
		if (!handlerFile.exists()) {
			throw new TransformException("Transform handler " + 
					handlerFile.getAbsolutePath() + " is not present on this machine.");
		}
		
		try {
			log.debug("Invoking transform: " + handlerFile.getAbsolutePath() + " "  + sourcePath + " " + destPath + " " + triggerBaseUrl);
			ProcessBuilder processBuilder = new ProcessBuilder(handlerFile.getAbsolutePath(), sourcePath, destPath, triggerBaseUrl);
			processBuilder.redirectErrorStream(true);
			processBuilder.start();
			
			//Runtime.getRuntime().exec(handlerFile.getAbsolutePath() + " " + sourcePath + " " + destPath + " " + triggerBaseUrl);
			
		} catch (IOException e) {
			throw new TransformException("Failed to invoke transform " + handlerPath + " on file " + sourcePath, e);
		}
	}
	
	public static int invokeBlocking(String handlerPath, String sourcePath, String destPath) throws TransformException {
		
		File handlerFile = new File(handlerPath);
		
		// make sure the handler is present prior to invocation
		if (!handlerFile.exists()) {
			throw new TransformException("Transform handler " + 
					handlerFile.getAbsolutePath() + " is not present on this machine.");
		}
		
		try {
			
			log.debug("Invoking transform: " + " " + handlerFile.getAbsolutePath() + " " + sourcePath + " " + destPath);
			ProcessBuilder processBuilder = new ProcessBuilder(handlerFile.getAbsolutePath(), sourcePath, destPath);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String line;

			log.debug("Output of running " + handlerFile.getAbsolutePath() + " " + sourcePath + " " + destPath + " is:");
			
			while ((line = br.readLine()) != null) {	
				log.debug(line);
			}
			
			return process.waitFor();
			
		} catch (Exception e) {
			throw new TransformException("Failed to invoke transform " + handlerPath + " on file " + sourcePath, e);
		}
	}
}
