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

import org.apache.commons.io.FileUtils;

import com.maverick.ssh.PublicKeyAuthentication;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.components.SshKeyPair;
import com.maverick.ssh2.Ssh2Client;
import com.maverick.ssh2.Ssh2Context;
import com.maverick.ssh2.Ssh2PublicKeyAuthentication;
import com.sshtools.publickey.ConsoleKnownHostsKeyVerification;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.sftp.SftpClient;

/**
 * This example demonstrates the connection process connecting to an SSH2 server
 * and usage of the SFTP client.
 * 
 * @author Lee David Painter
 */
public class SftpConnect {
	public static int ITERATIONS = 10;
	public static int BOCK_COUNT = 500;
	
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
			System.out.print("Hostname: ");
			String hostname = "129.114.6.121";
//			hostname = reader.readLine();

//			int idx = hostname.indexOf(':');
			int port = 22;
//			if (idx > -1) {
//				port = Integer.parseInt(hostname.substring(idx + 1));
//				hostname = hostname.substring(0, idx);
//
//			}
//
//			System.out.print("Username [Enter for "
//					+ System.getProperty("user.name") + "]: ");
//
//			String username;
//			username = reader.readLine();
//
//			if (username == null || username.trim().equals(""))
//				username = System.getProperty("user.name");
//			System.out.println("Connecting to " + hostname);

			String	username = "rodeo";//System.getProperty("user.name");
			System.out.println("Connecting to " + hostname);
			
			String privateKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"));
			String publicKey = FileUtils.readFileToString(new File(System.getProperty("user.home") + "/.ssh/id_rsa.pub"));
			
			String password = "";
			
			/**
			 * Create an SshConnector instance
			 */
			SshConnector con = SshConnector.createInstance();

			// Lets do some host key verification

			con.getContext(2).setHostKeyVerification(
					new ConsoleKnownHostsKeyVerification());
			((Ssh2Context) con.getContext(2)).setPreferredPublicKey(
					Ssh2Context.PUBLIC_KEY_SSHDSS);

			/**
			 * Connect to the host
			 */
//			SocketTransport t = new SocketTransport(hostname, port);
//			t.setTcpNoDelay(true);
//
//			SshClient ssh = con.connect(t, username, true);
			
			SocketAddress sockaddr = null; 
	        Socket t = new Socket();

			sockaddr = new InetSocketAddress(hostname, port);
			
			t.connect(sockaddr, 15000);
			
			t.setTcpNoDelay(true);
			t.setPerformancePreferences(0, 1, 2);
//			t.setSendBufferSize(4096*BOCK_COUNT/10);
//	        t.setReceiveBufferSize(4096*BOCK_COUNT/10);
			
			SshClient ssh = con.connect(new com.sshtools.net.SocketWrapper(t), username);
			
			Ssh2Client ssh2 = (Ssh2Client) ssh;
//			/**
//			 * Authenticate the user using password authentication
//			 */
//			PasswordAuthentication pwd = new PasswordAuthentication();
//
//			do {
//				System.out.print("Password: ");
//				pwd.setPassword(reader.readLine());
//				pwd.setPassword(");
//			} while (ssh2.authenticate(pwd) != SshAuthentication.COMPLETE
//					&& ssh.isConnected());

			Ssh2PublicKeyAuthentication auth = new Ssh2PublicKeyAuthentication();
			int authStatus = 0;
			do {
				SshPrivateKeyFile pkfile = SshPrivateKeyFileFactory.parse(privateKey.getBytes());
				
				SshKeyPair pair;
				if (pkfile.isPassphraseProtected()) {
                    pair = pkfile.toKeyPair(password);
				} else {
				    pair = pkfile.toKeyPair(null);
				
				}

				((PublicKeyAuthentication)auth).setPrivateKey(pair.getPrivateKey());
				((PublicKeyAuthentication)auth).setPublicKey(pair.getPublicKey());
				authStatus = ssh2.authenticate(auth);
			}
			while (authStatus != SshAuthentication.COMPLETE 
					&& authStatus != SshAuthentication.FAILED
					&& authStatus != SshAuthentication.CANCELLED
					&& ssh.isConnected());
			
			/**
			 * Start a session and do basic IO
			 */
			if (ssh.isAuthenticated()) {

				SftpClient sftp = new SftpClient(ssh2);
				
//				/**
//				 * List the contents of the directory
//				 */
//				SftpFile[] ls = sftp.ls();
//				for (int i = 0; i < ls.length; i++) {
//					ls[i].getParent();
//					System.out.println(SftpClient.formatLongname(ls[i]));
//				}
				/**
				 * Generate a temporary file for uploading/downloading
				 */
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
				long t1=0,t2=0,t3=0,t4=0;
				long length = f.length();
				
				
				/**
				 * Perform some text mode operations
				 */
//				sftp.setTransferMode(SftpClient.MODE_TEXT);
				sftp.setTransferMode(SftpClient.MODE_BINARY);
				

//				sftp.setBufferSize((int)f.length());
//				sftp.setBufferSize(-1);
//				sftp.setBufferSize(4096*BOCK_COUNT/10);
//				sftp.setBlockSize(32768*4);
				/**
				 * Create a directory
				 */
				String remoteDirName = "test-sftp-" + System.currentTimeMillis();
				sftp.mkdirs(remoteDirName);

				/**
				 * Download the file inot a new location
				 */
				File f2 = new File(System.getProperty("user.home"),
						"downloaded");
				f2.mkdir();
				System.out.println("Test: (maverick legacy native api)");
				for(int z=0;z<ITERATIONS;z++)
				{
					/**
					 * Change directory
					 */
					sftp.cd(remoteDirName);
	
					/**
					 * Put a file into our new directory
					 */
					
//					System.out.println("Putting file");
					t1 = System.currentTimeMillis();
					sftp.put(f.getAbsolutePath());
					t2 = System.currentTimeMillis();
	//				System.out.println("Completed.");
	//				long e = t2 - t1;
	//				System.out.println("Took " + String.valueOf(e)
	//						+ " milliseconds");
	//				float kbs;
	//				if (e >= 1000) {
	//					kbs = ((float) length / 1024) / ((float) e / 1000);
	//					System.out.println("Upload Transfered at "
	//							+ String.valueOf(kbs) + " kbs");
	//				}
	//				/**
	//				 * Get the attributes of the uploaded file
	//				 */
	//				System.out.println("Getting attributes of the remote file");
	//				SftpFileAttributes attrs = sftp.stat(f.getName());
	//				System.out
	//						.println(SftpClient.formatLongname(attrs, f.getName()));
	
					
					sftp.lcd(f2.getAbsolutePath());
	
//					sftp.setTransferMode(SftpClient.MODE_TEXT);
//					sftp.setBlockSize(32*1024 * 16);
//					System.out.println("Getting file");
					
					InputStream in = null;
					OutputStream out = null;
					try {
						in = sftp.getInputStream(f.getName());
						out = new FileOutputStream("/dev/null");
						byte[] buf = new byte[1024*1024];
					    int bytesRead = in.read(buf);
					    
					    t3 = System.currentTimeMillis();
						
					    while (bytesRead != -1) {
							out.write(buf, 0, bytesRead);
					      bytesRead = in.read(buf);
					    }
					    out.flush();

						
						t4 = System.currentTimeMillis();
					}
					catch (Exception e) {
						try { in.close();} catch (Exception e1){}
						try { out.close();} catch (Exception e1){}
					}
					
//					sftp.setMaxAsyncRequests(2048);
//					t3 = System.currentTimeMillis();
//					sftp.get(f.getName(), "/dev/null");
//					t4 = System.currentTimeMillis();
					
					
//					System.out.println("Completed.");
//					e = t2 - t1;
//					System.out.println("Took " + String.valueOf(e)
//							+ " milliseconds");
//					if (e >= 1000) {
//						kbs = ((float) length / 1024) / ((float) e / 1000);
//						System.out.println("Download Transfered at "
//								+ String.valueOf(kbs) + " kbs");
//					}
					
					calculateTime(t2, t1, t4, t3, length);
					
					// cd back to teh original directory
					sftp.cd("");
				}
				
				// delete the test directory
				sftp.rm(remoteDirName, true, true);

				System.out.println("Cleaning up local directory");
				FileUtils.deleteQuietly(f);
				FileUtils.deleteQuietly(f2);
				
//				/**
//				 * Set the permissions on the file and check they were changed
//				 * they should be -rw-r--r--
//				 */
//				sftp.chmod(0644, f.getName());
//				attrs = sftp.stat(f.getName());
//				System.out
//						.println(SftpClient.formatLongname(attrs, f.getName()));
//
//				sftp.lcd(System.getProperty("user.home"));
//				System.out.println(sftp.lpwd());
//				File f3 = new File(System.getProperty("user.home"), "testfiles");
//				f3.mkdir();
//				sftp.lcd("testfiles");
//				sftp.cd("");
//				/**
//				 * get a file using getFiles with default no reg exp matching
//				 */
//				SftpFile[] remotefiles = sftp.ls();
//				if (remotefiles.length > 2) {
//					int i = 0;
//					while ((remotefiles[i].getFilename().equals(".") | remotefiles[i]
//							.getFilename().equals(".."))
//							& (i < remotefiles.length)) {
//						i++;
//					}
//					System.out.println("\n first remote filename"
//							+ remotefiles[i].getFilename());
//					sftp.getFiles(remotefiles[i].getFilename());
//					System.out.println("\nGot " + remotefiles[i].getFilename()
//							+ "\n");
//				}
//
//				// change reg exp syntax from default SftpClient.NoSyntax (no
//				// reg exp matching) to SftpClient.GlobSyntax
//				sftp.setRegularExpressionSyntax(SftpClient.GlobSyntax);
//
//				/**
//				 * get all files in the remote directory using *.*
//				 */
//				sftp.getFiles("*.txt");
//				System.out.println("\nGot *.txt\n");
//
//				System.out
//						.println("Check that copied all remote txt files to local, press enter.");
//				reader.readLine();
//
//				/**
//				 * get all files in the remote directory using *
//				 */
//				sftp.getFiles("*");
//				System.out.println("\nGot *\n");
//
//				System.out
//						.println("Check that copied all remote files to local, press enter.");
//				reader.readLine();
//
//				/**
//				 * put all txt files in the local directory into the remote
//				 * directory using *.txt
//				 */
//				sftp.putFiles("*.txt");
//				System.out.println("\nPut *.txt\n");

			}
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}
}