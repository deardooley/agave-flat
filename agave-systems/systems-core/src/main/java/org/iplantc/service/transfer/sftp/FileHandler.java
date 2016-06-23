package org.iplantc.service.transfer.sftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Roland H. Niedner <br />
 *
 * A FileHandler implementation enables basic file operations on local or
 * remote filesystems. The target filesystems can be hard coded into the class
 * or set via the configuration method.
 *
 */
public interface FileHandler {

	public static class FileAttributes
	{
		public String filename;
		public boolean isDirectory;
		public long size;
		public Date mtime;
	};


	/**
	* 	Release any resources held by filehandler.
	*/
	public void close();

	/**
	 * Tests if path exists
	 *
	 * @param file or directory path
	 * @return true if file or directory exists, false otherwise.
	 */
	public boolean exists(String path) throws Exception;


	/**
	 * Method creates the submitted directory on the target filesystem.
	 *
	 * @param directory
	 * @throws IOException
	 */
	public void createDirectory(String directory) throws IOException, Exception;

	/**
	 * Method removes the submitted directory on the target filesystem.
	 *
	 * @param directoryPath
	 * @param deleteContent
	 * @throws IOException
	 */
	public void removeDirectory(String directoryPath, boolean deleteContent) throws IOException, Exception;

	/**
	 * Method returns a List with all the sub-directories in the submitted directory.
	 *
	 * @param directory
	 * @return sub-directories
	 * @throws IOException
	 */
	public List<String> listSubdirectories(String directory) throws IOException, Exception;

	/**
	 * Method returns a List with all the fileNames in the submitted directory.
	 * Subdirectory names are not included in the list.
	 *
	 * @param directory
	 * @return fileNames
	 * @throws IOException
	 */
	public List<String> listFiles(String directory) throws IOException, Exception;

	/**
	 * Method returns a List with all the entries in the submitted directory.
	 * Unlike the version of listFiles that returns List<String> of filenames, this
	 * method includes subdirectories in the list that's returned.
	 *
	 * @param directory
	 * @return attributes
	 * @throws IOException
	 */
	public List<FileAttributes> list(String directory) throws IOException, Exception;

	/**
	 * Method returns a List with all the fileNames in the submitted directory
	 * keyed to their extension.Files without an extension are keyed to "" -
	 * an empty String.
	 *
	 * @param directory
	 * @return fileNameMap
	 * @throws IOException
	 */
	public Map<String, List<String>> listFilesByExtension(String directory) throws IOException, Exception;

	/**
	 * Method removes a the submitted file on the target filesystem. If the boolean
	 * parameter is set to false then the method will throw an Exception when
	 * the directory is not empty.
	 *
	 * @param fileName
	 * @throws IOException
	 */
	public void removeFile(String fileName) throws IOException, Exception;

	/**
	 * Method reads the submitted file and returns its content.
	 * @param fileName
	 * @return fileContent
	 * @throws IOException
	 */
	public byte[] readFile(String fileName) throws IOException, Exception;

	/**
	 * Method reads the all files with the specified extension
	 * and returns its content or null if there aren't any.
	 * Example: if you like to get all *.gif files then
	 * fileExtension = "gif".
	 *
	 * @param directory
	 * @param fileExtension
	 * @return fileContents or null
	 * @throws IOException
	 */
	public Map<String, byte[]> readFilesWithExtension(String directory, String fileExtension) throws IOException, Exception;

	/**
	 * Method reads the all files with the specified extensions.
	 * The outer Map keys are the submitted file extensions where
	 * actual file where found. The inner Map keys are the file names,
	 * the values are the content as byte[].
	 *
	 * and returns its content or null if there aren't any.
	 * Example: if you like to get all *.gif files then
	 * fileExtension = "gif".
	 *
	 * @param directory
	 * @param fileExtension
	 * @return fileContents or null
	 */
	/*
	 I'm commenting this out because it doesn't appear to be used and I didn't update the impls to handle "*"
	 when I updated the version of readFilesExtension above.

	public Map<String, Map<String, byte[]>> readFilesWithExtensions(String directory, Set<String> fileExtension);
	*/

	/**
	 *
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public InputStream getInputStream(String fileName) throws Exception;

	/**
	 * Method writes the content (second argument) into a file with the
	 * submitted path and name (first argument) on the target filesystem.
	 *
	 * @param fileName
	 * @param content
	 * @throws IOException
	 */
	public void writeFile(String fileName, String content) throws IOException, Exception;

	/**
	 * Method writes the content (second argument) into a file with the
	 * submitted path and name (first argument) on the target filesystem.
	 *
	 * @param fileName
	 * @param content
	 * @throws IOException
	 */
	public void writeFile(String fileName, byte[] content) throws IOException, Exception;

	/**
	 * Method copies the contents of the submitted file (second argument) into
	 * a file with the submitted path and name (first argument) on the target filesystem.
	 *
	 * @param newFileName
	 * @param file
	 * @throws IOException
	 */
	public void writeFile(String newFileName, File file) throws IOException, Exception;

	/**
	 * Method copies the contents of the submitted stream (second argument) into
	 * a file with the submitted path and name (first argument) on the target filesystem.
	 *
	 * @param fileName
	 * @param inStream
	 * @throws IOException
	 */
	public void writeFile(String fileName, InputStream inStream) throws IOException, Exception;

	/**
	 * Method moves or renames a file.
	 *
	 * @param fileName
	 * @param newFileName
	 * @throws IOException
	 */
	public void moveFile(String fileName, String newFileName) throws IOException, Exception;

	/**
	 * Method moves or renames a directory.
	 *
	 * @param directoryName
	 * @param newDirectoryName
	 * @throws IOException
	 */
	public void moveDirectory(String directoryName, String newDirectoryName) throws IOException, Exception;

	public boolean isDirectory(String fileName) throws IOException, Exception;

	/**
	 * Length of file in bytes.
	 *
	 * @throws IOException
	 */
	public long getSize(String fileName) throws IOException, Exception;

	 /**
	 * Date file/dir was last modified in milliseconds since Jan 1, 1970
	 *
	 * @throws IOException
	 */
	public Date getMTime(String fileName) throws IOException, Exception;
}
