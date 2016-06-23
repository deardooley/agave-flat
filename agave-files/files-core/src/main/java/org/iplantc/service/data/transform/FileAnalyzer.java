package org.iplantc.service.data.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.util.GrepUtil;
import org.iplantc.service.transfer.RemoteDataClient;

public class FileAnalyzer {
	
	private static final Logger log = Logger.getLogger(FileAnalyzer.class);
	
	private String path;
	private RemoteDataClient client;
	
	public FileAnalyzer(RemoteDataClient client, String path) {
		this.client = client;
		this.path = path;
	}
	
	public FileTransform findTransform() throws TransformException 
	{
		for (FileTransform transform: new FileTransformProperties().getTransforms()) 
		{
			// only check the transforms who have their corresponding handlers installed
			if (transform.isEnabled()) 
			{
				log.debug("Analyzing " + path + " for " + transform.getName() + " characteristics..");
			
				GrepUtil gu = new GrepUtil(transform.getPattern());
				
				try {
					if (gu.grep(client, path)) {
						return transform;
					}
				} catch (Exception e) {
					throw new TransformException("Failed to find transform", e);
				}
			} 
		}
		
		return null;
	}

	public List<FileTransform> findAllMatchingTransforms() throws TransformException {
		List<FileTransform> matches = new ArrayList<FileTransform>();
		
		for (FileTransform transform: new FileTransformProperties().getTransforms()) {
			// only check the transforms who have their corresponding handlers installed
			if (transform.isEnabled()) {
				log.debug("Analyzing " + path + " for " + transform.getName() + " characteristics..");
			
				GrepUtil gu = new GrepUtil(transform.getPattern());
				
				try {
					if (gu.grep(client, path)) {
						matches.add(transform);
					}
				} catch (Exception e) {
					throw new TransformException("Failed to parse file " + path + " for transform " + transform.getName(), e);
				}
			} 
		}
		
		return matches;
	}

}
