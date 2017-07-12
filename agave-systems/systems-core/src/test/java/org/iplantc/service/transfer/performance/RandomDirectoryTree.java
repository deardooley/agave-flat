/**
 * 
 */
package org.iplantc.service.transfer.performance;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author dooley
 *
 */
public class RandomDirectoryTree {

	private static final Logger log = Logger
			.getLogger(RandomDirectoryTree.class);
	
	
	/**
	 * Utility class to generate a random string of 
	 * up to {@code maxFilenameSize} characters.
	 * @author dooley
	 *
	 */
	class RandomNameGenerator {
		int maxFilenameSize;
		public RandomNameGenerator(int maxFilenameSize) {
			this.maxFilenameSize = maxFilenameSize;
		}
		
		public String generate() {
			int length = RandomUtils.nextInt(maxFilenameSize);
			return StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0, length-1);
		}
	}
	
	private File rootPath;
	private int firstLevelDirs = 2;
	private int minChildDirs = 1;
	private int maxChildDirs = 4;
	private int maxChildFiles = 10;
	private int maxDepth = 5;
	private int maxSize = 1024 * 1024;
	private int maxFilnameSize = 20;
	private RandomNameGenerator randomNameGenerator = null;
	
	public RandomDirectoryTree(File rootPath) {
		this.rootPath = rootPath;
		this.randomNameGenerator = new RandomNameGenerator(maxFilnameSize);
	}
	
	public File createTree(boolean force) 
	throws IOException 
	{
		for (int i=0; i<firstLevelDirs; i++) {
			File fld = new File(rootPath, randomNameGenerator.generate());
			createSubtree(fld, 
					RandomUtils.nextInt() % maxDepth, 
					RandomUtils.nextInt() % maxChildDirs,
					RandomUtils.nextInt() % maxChildFiles);
		}
		
		return rootPath;
	}
	
	protected File createSubtree(File parent, int depth, int childDirs, int childFiles )
	throws IOException 
	{
		for (int i=0; i<childDirs; i++) {
			File childDir = new File(parent, randomNameGenerator.generate());
			if (depth > 0) {
				createSubtree(childDir, 
						RandomUtils.nextInt() % (depth - 1), 
						RandomUtils.nextInt() % maxChildDirs,
						RandomUtils.nextInt() % maxChildFiles);
			}
		}
		
		for (int i=0; i<childFiles; i++) {
			createFile(new File(parent, randomNameGenerator.generate()), RandomUtils.nextInt(maxSize));
		}
		
		return parent;
	}
	
	protected File createFile(File file, int length) 
	throws IOException 
	{
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			raf.setLength(length);
		}
		finally {
			raf.close();
		}
		
		return file;
	}
}
