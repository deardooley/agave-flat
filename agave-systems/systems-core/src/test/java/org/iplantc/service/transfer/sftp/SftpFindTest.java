package org.iplantc.service.transfer.sftp;
/**
 * Copyright 2003-2016 SSHTOOLS Limited. All Rights Reserved.
 *
 * For product documentation visit https://www.sshtools.com/
 *
 * This file is part of J2SSH Maverick.
 *
 * J2SSH Maverick is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J2SSH Maverick is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J2SSH Maverick.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;

import com.sshtools.publickey.ConsoleKnownHostsKeyVerification;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.sftp.DirectoryOperation;
import com.sshtools.sftp.FileTransferProgress;
import com.sshtools.sftp.SftpClient;
import com.sshtools.sftp.SftpFile;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PublicKeyAuthentication;
import com.sshtools.ssh.SshAuthentication;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh2.Ssh2Client;
import com.sshtools.ssh2.Ssh2Context;
import com.sshtools.ssh2.Ssh2PublicKeyAuthentication;

/**
 * This example demonstrates the connection process connecting to an SSH2 server
 * and usage of the SFTP client.
 * 
 * @author Lee David Painter
 */
public class SftpFindTest {
	public static int ITERATIONS = 2;
	public static String REMOTE_DIRECTORY_PATH="allenhub/job-3765001591854067225-242ac114-0001-007-allen_diffexpress_test-0-1-19";
	
	public static void calculateTime(long uend, long ustart, long fileCount, long folderCount) {
		long ue = uend - ustart;
		
		System.out.println(String.valueOf(ue) + "\t"
				+ String.valueOf(fileCount) + "\t" 
				+ String.valueOf(folderCount));
	}
	
	public static void main(String[] args) {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		try {
			
//			JndiSetup.init();
			String tenantId = "iplantc.org";
			System.out.print("Tenant: " + tenantId + "\n");
			TenancyHelper.setCurrentTenantId(tenantId);
			
			String systemId = "demo.raven.udel";
			
			System.out.print("System: " + systemId + "\n");
			
			RemoteSystem system = new SystemDao().findBySystemId(systemId);
			
			System.out.println("Connecting to " + tenantId + "/" + system.getStorageConfig().getHost() + "...\n");
			
//			String privateKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"));
//			String publicKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa.pub"));
			
			MaverickSFTP remoteDataClient = (MaverickSFTP)system.getRemoteDataClient();
			remoteDataClient.authenticate();
			
			SftpClient sftp = remoteDataClient.getClient();
			
			long t1=0,t2=0,t3=0,t4=0;
			
			t1 = System.currentTimeMillis();
			final AtomicLong fileCount = new AtomicLong(0);
			final AtomicLong folderCount = new AtomicLong(0);
			DirectoryOperation dirOp = sftp.copyRemoteDirectory(REMOTE_DIRECTORY_PATH,
					"/dev/null",  // forces the entire remote tree to be traversed and pulled
					true,
					true,
					false,
					new FileTransferProgress() {

						@Override
						public void started(long bytesTotal,
								String remoteFile) {
							if (remoteFile.endsWith("/")) {
								folderCount.incrementAndGet();
							} else {
								fileCount.incrementAndGet();
							}
							System.out.println(remoteFile + "\t" + bytesTotal);
						}

						@Override
						public boolean isCancelled() {
							System.out.println("...cancelled...");
							return true;
						}

						@Override
						public void progressed(long bytesSoFar) {
							System.out.println("...progress " + bytesSoFar + "...");
							
						}

						@Override
						public void completed() {
							System.out.println("...completed...");
						}
					});
			t2 = System.currentTimeMillis();
			
			System.out.println("\nUpdated files\n");
			for (Iterator<SftpFile> iter = dirOp.getUpdatedFiles().iterator(); iter.hasNext(); ) {
				SftpFile f = iter.next(); 
				System.out.println(f.getAbsolutePath() + "\t" + f.getAttributes().getSize() + "\n");
				
			}
			
			System.out.println("\nNew files\n");
			for (Iterator<SftpFile> iter = dirOp.getNewFiles().iterator(); iter.hasNext(); ) {
				SftpFile f = iter.next(); 
				System.out.println(f.getAbsolutePath() + "\t" + f.getAttributes().getSize());
				
			}
			
			System.out.println("\nDeleted files\n");
			for (Iterator<SftpFile> iter = dirOp.getDeletedFiles().iterator(); iter.hasNext(); ) {
				SftpFile f = iter.next(); 
				System.out.println(f.getAbsolutePath() + "\t" + f.getAttributes().getSize());
				
			}
			
			calculateTime(t2, t1, dirOp.getFileCount(), dirOp.getTransferSize());
				
//			System.out.println("Cleaning up local directory");
//			FileUtils.deleteQuietly(f);
//			FileUtils.deleteQuietly(f2);
//			/**
//			 * get a file using getFiles with default no reg exp matching
//			 */
//			SftpFile[] remotefiles = sftp.ls();
//			if (remotefiles.length > 2) {
//				int i = 0;
//				while ((remotefiles[i].getFilename().equals(".") | remotefiles[i]
//						.getFilename().equals(".."))
//						& (i < remotefiles.length)) {
//					i++;
//				}
//				System.out.println("\n first remote filename"
//						+ remotefiles[i].getFilename());
//				sftp.getFiles(remotefiles[i].getFilename());
//				System.out.println("\nGot " + remotefiles[i].getFilename()
//						+ "\n");
//			}
//
//			}
		} catch (Throwable th) {
			th.printStackTrace();
		}
		finally {
//			try {
//				JndiSetup.close();
//			} catch (Exception e) {
//				
//			}
		}
	}
}