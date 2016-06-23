/**
 * 
 */
package org.iplantc.service.apps.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * @author dooley
 * 
 */
public class ZipUtil extends org.iplantc.service.common.util.ZipUtil 
{
	public static void zip(File directory, File zipFile) throws IOException
	{
		URI base = directory.toURI();
		LinkedList<File> queue = new LinkedList<File>();
		queue.add(directory);
		OutputStream out = new FileOutputStream(zipFile);
		Closeable res = out;
		ZipOutputStream zout = null;
		try
		{
			zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty())
			{
				directory = queue.removeLast();
				for (File kid : directory.listFiles())
				{
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory())
					{
						queue.add(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					}
					else
					{
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		}
		finally
		{
			try {res.close();} catch (Exception e) {}
			try {out.close();} catch (Exception e) {}
			try {zout.close();} catch (Exception e) {}
		}
	}

	public static void unzip(File zipFile, File directory) throws IOException
	{
		ZipFile zFile = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> entries = zFile.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			File file = new File(directory, entry.getName());
			if (entry.isDirectory())
			{
				file.mkdirs();
			}
			else
			{
				file.getParentFile().mkdirs();
				InputStream in = zFile.getInputStream(entry);
				try
				{
					copy(in, file);
				}
				finally
				{
					in.close();
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void unzipBundleLibraryToRemote(File zipFile,
			File remoteDirectory, RemoteDataClient client) throws IOException,
			RemoteDataException
	{
		ZipFile zFile = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> entries = zFile.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();

			// only copy the contents of the library directory remotely
			if (!entry.getName().startsWith("library"))
			{
				continue;
			}

			File file = new File(remoteDirectory, entry.getName().substring(
					"library/".length()));

			if (entry.isDirectory())
			{

				if (client.doesExist(file.getPath()))
				{
					System.out.println("Folder exists, skipping mkdir of "
							+ file.getPath());
				}
				else
				{
					System.out.println("Creating folder " + file.getPath());
					client.mkdirs(file.getPath());
				}
			}
			else
			{
				if (client.doesExist(file.getPath()))
				{
					System.out.println("File exists, skipping copy of "
							+ file.getPath());
					continue;
				}
				System.out.println("Copying file " + file.getPath());
				InputStream in = zFile.getInputStream(entry);

				RemoteOutputStream out = client.getOutputStream(file.getPath(), false, false);

				try
				{
					copy(in, out);
				}
				finally
				{
					in.close();
					out.close();
				}
			}
		}
	}

	private static void copy(InputStream in, OutputStream out)
			throws IOException
	{
		byte[] buffer = new byte[1024];
		while (true)
		{
			int readCount = in.read(buffer);
			if (readCount < 0)
			{
				break;
			}
			out.write(buffer, 0, readCount);
		}
		buffer = null;
	}

	private static void copy(File file, OutputStream out) throws IOException
	{
		InputStream in = new FileInputStream(file);
		try
		{
			copy(in, out);
		}
		finally
		{
			in.close();
		}
	}

	private static void copy(InputStream in, File file) throws IOException
	{
		OutputStream out = new FileOutputStream(file);
		try
		{
			copy(in, out);
		}
		finally
		{
			out.close();
		}
	}

}
