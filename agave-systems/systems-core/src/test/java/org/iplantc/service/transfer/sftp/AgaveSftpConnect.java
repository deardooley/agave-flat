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
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.sftp.MaverickSFTP;

import com.sshtools.sftp.SftpClient;

/**
 * This example demonstrates the connection process connecting to an SSH2 server
 * and usage of the SFTP client.
 * 
 * @author Lee David Painter
 */
public class AgaveSftpConnect {

	public static int ITERATIONS = 2;
	public static int BOCK_COUNT = 50000;
	
	public static void calculateTime(long uend, long ustart, long dend, long dstart, long length) {
		long ue = uend - ustart;
//		System.out.println("Took " + String.valueOf(e)
//				+ " milliseconds");
		float ukbs = 0;
		if (ue >= 1000) {
			ukbs = ((float) length / 1024) / ((float) ue / 1000);
//			System.out.println("Upload Transfered at "
//					+ String.valueOf(kbs) + " kbs");
		}
		
		long de = dend - dstart;
//		System.out.println("Took " + String.valueOf(e)
//				+ " milliseconds");
		float dkbs = 0;
		if (de >= 1000) {
			dkbs = ((float) length / 1024) / ((float) de / 1000);
//			System.out.println("Upload Transfered at "
//					+ String.valueOf(kbs) + " kbs");
		}
		
		System.out.println(String.valueOf(length) 
				+ "\t" + String.valueOf(ue) + "\t" + String.valueOf(ukbs)
				+ "\t" + String.valueOf(de) + "\t" + String.valueOf(dkbs));
	}
	
	public static void main(String[] args) {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		try {
//			System.out.print("Hostname: ");
			String hostname = "api.staging.agaveapi.co";
//			hostname = reader.readLine();
//
//			int idx = hostname.indexOf(':');
			int port = 22;
//			if (idx > -1) {
//				port = Integer.parseInt(hostname.substring(idx + 1));
//				hostname = hostname.substring(0, idx);
//
//			}
//			System.out.print("Username [Enter for "
//					+ System.getProperty("user.name") + "]: ");

//			String username = null;
//			username = reader.readLine();

//			if (username == null || username.trim().equals(""))
			String	username = System.getProperty("user.name");
			System.out.println("Connecting to " + hostname);
			
			String privateKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"));
			String publicKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa.pub"));
			
//			/**
//			 * Create an SshConnector instance
//			 */
//			SshConnector con = SshConnector.createInstance();
//
//			// Lets do some host key verification
//
//			con.getContext().setHostKeyVerification(
//					new ConsoleKnownHostsKeyVerification());
//			con.getContext().setPreferredPublicKey(
//					Ssh2Context.PUBLIC_KEY_SSHDSS);
//
//			/**
//			 * Connect to the host
//			 */
//			SocketTransport t = new SocketTransport(hostname, port);
//			t.setTcpNoDelay(true);
//
//			SshClient ssh = con.connect(t, username, true);
//
////			Ssh2Client ssh2 = (Ssh2Client) ssh;
//			/**
//			 * Authenticate the user using password authentication
//			 */
//			PasswordAuthentication pwd = new PasswordAuthentication();
//
//			do {
//				System.out.print("Password: ");
//				pwd.setPassword(reader.readLine());
//			} while (ssh2.authenticate(pwd) != SshAuthentication.COMPLETE
//					&& ssh.isConnected());
//
//			/**
//			 * Start a session and do basic IO
//			 */
//			if (ssh.isAuthenticated()) {
//
//				SftpClient sftp = new SftpClient(ssh2);
//
//				/**
//				 * Perform some text mode operations
//				 */
//				sftp.setTransferMode(SftpClient.MODE_TEXT);

//			System.out.print("Password: ");
//			String passwd = reader.readLine();
			JndiSetup.init();
			MaverickSFTP sftp = new MaverickSFTP(hostname, 22, username, null, "/home/" + username, "/", publicKey, privateKey);
			sftp.authenticate();
			
////				/**
////				 * List the contents of the directory
////				 */
////				List<RemoteFileInfo> ls = sftp.ls("");
////				for (RemoteFileInfo fi : ls) {
////					System.out.println(fi.toString());
////				}
//				/**
//				 * Generate a temporary file for uploading/downloading
//				 */
				File f = new File(System.getProperty("user.home"), "sftp-file");
				if (!f.exists() || f.length() != BOCK_COUNT * 4096) {
					java.util.Random rnd = new java.util.Random();
				
					FileOutputStream out = new FileOutputStream(f);
					byte[] buf = new byte[4096];
					for (int i = 0; i < BOCK_COUNT; i++) {
						rnd.nextBytes(buf);
						out.write(buf);
					}
					out.close();
				}
				long length = f.length();
				long t1=0, t2=0, t3, t4;
				
				/**
				 * Create a directory
				 */
				String remoteDirName = "test-sftp-" + System.currentTimeMillis();
				sftp.mkdirs(remoteDirName);
				
				/**
				 * Create a directory to download the file into a new location
				 */
				File f2 = new File(System.getProperty("user.home"),
						"downloaded");
				f2.mkdir();
				
				/**
				 * Put a file into our new directory without a 
				 * transfer listener
				 */
				System.out.println("Test: (raw maverick legacy)");
				SftpClient msftp = sftp.getClient();
				msftp.setTransferMode(com.sshtools.sftp.SftpClient.MODE_BINARY);
				
				for (int z=0; z<ITERATIONS; z++) {
					
					// put
					t1 = System.currentTimeMillis();
					msftp.put(f.getAbsolutePath(), remoteDirName);
					t2 = System.currentTimeMillis();
					
					// get
					t3 = System.currentTimeMillis();
					msftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName());
					t4 = System.currentTimeMillis();
					
					calculateTime(t2, t1, t4, t3, length);
				}

				/**
				 * Put a file into our new directory without a 
				 * transfer listener
				 */
				System.out.println("Test: (no listener, no task)");
				
				for (int z=0; z<ITERATIONS; z++) {
				
					// put
					t1 = System.currentTimeMillis();
					sftp.put(f.getAbsolutePath(), remoteDirName);
					t2 = System.currentTimeMillis();
					
					// get
					t3 = System.currentTimeMillis();
					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName());
					t4 = System.currentTimeMillis();
					
					calculateTime(t2, t1, t4, t3, length);
				}
				
				/**
				 * Put a file into our new directory with a 
				 * transfer listener, no transfer task
				 */
				System.out.println("\nTest: (listener, no task)");
				
				for (int z=0; z<ITERATIONS; z++) {
					
					// put
					t1 = System.currentTimeMillis();
					sftp.put(f.getAbsolutePath(), remoteDirName, new RemoteTransferListener(null));
					t2 = System.currentTimeMillis();
					
					// get
					t3 = System.currentTimeMillis();
					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName(), new RemoteTransferListener(null));
					t4 = System.currentTimeMillis();
				
					calculateTime(t2, t1, t4, t3, length);
				}
				
				/**
				 * Put a file into our new directory with a 
				 * transfer listener, valid transfer task
				 */
				System.out.println("\nPutting file (listener, task)");
				
				for (int z=0; z<ITERATIONS; z++) {
					
					// put
					TransferTask tt1 = new TransferTask("agave://localhost/" + f.getAbsolutePath(), sftp.getUriForPath(remoteDirName + "/" + f.getName()).toString());
					tt1.setOwner(System.getProperty("user.name"));
					TransferTaskDao.persist(tt1);
					
					t1 = System.currentTimeMillis();
					sftp.put(f.getAbsolutePath(), remoteDirName, new RemoteTransferListener(tt1));
					t2 = System.currentTimeMillis();

					// get
					TransferTask tt2 = new TransferTask(sftp.getUriForPath(remoteDirName + "/" + f.getName()).toString(), "agave://localhost/" + f.getAbsolutePath());
					tt2.setOwner(System.getProperty("user.name"));
					TransferTaskDao.persist(tt2);
					
					t3 = System.currentTimeMillis();
					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName(), new RemoteTransferListener(tt2));
					t4 = System.currentTimeMillis();
					
					
					calculateTime(t2, t1, t4, t3, length);
				}
				
//				/**
//				 * Get the attributes of the uploaded file
//				 */
//				System.out.println("\nGetting attributes of the remote file");
//				SftpFileAttributes attrs = sftp.stat(remoteDirName + "/" + f.getName());
//				System.out
//						.println(SftpClient.formatLongname(attrs, f.getName()));

				
//					
//				/**
//				 * Get a file into our local directory without a 
//				 * transfer listener
//				 */
//				System.out.println("\nGetting file (no listener, no task)");

//				for (int z=0; z<10; z++) {					
//					t1 = System.currentTimeMillis();
//					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName());
//					t2 = System.currentTimeMillis();
//					
//					calculateTime(t2, t1, length);
//				}
//				
//				/**
//				 * Get a file into our local directory with a 
//				 * transfer listener, no task
//				 */
//				System.out.println("\nGetting file (listener, no task)");
//				for (int z=0; z<10; z++) {
//					t1 = System.currentTimeMillis();
//					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName(), new RemoteTransferListener(null));
//					t2 = System.currentTimeMillis();
//				
//					calculateTime(t2, t1, length);
//				}
//				
//				/**
//				 * Get a file into our local directory with a 
//				 * transfer listener, valid task
//				 */
//				System.out.println("\nGetting file (listener, task)");
//				
//				for (int z=0; z<10; z++) {
//					TransferTask tt2 = new TransferTask(sftp.getUriForPath(remoteDirName + "/" + f.getName()).toString(), "agave://localhost/" + f.getAbsolutePath());
//					tt2.setOwner(System.getProperty("user.name"));
//					TransferTaskDao.persist(tt2);
//					
//					t1 = System.currentTimeMillis();
//					sftp.get(remoteDirName + "/" + f.getName(), f2.getAbsolutePath() + "/" + f.getName(), new RemoteTransferListener(tt2));
//					t2 = System.currentTimeMillis();
//					
//					calculateTime(t2, t1, length);
//				}
				
				// cd back to teh original directory
				
				// delete the test directory
				System.out.println("Cleaning up remote directory");
				sftp.delete(remoteDirName);
				
				System.out.println("Cleaning up local directory");
				FileUtils.deleteQuietly(f);
				FileUtils.deleteQuietly(f2);
				
		} catch (Throwable th) {
			th.printStackTrace();
		}
		finally {
			JndiSetup.close();
		}
	}
}