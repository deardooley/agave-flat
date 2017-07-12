package org.iplantc.service.transfer.sftp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.sftp.MaverickSFTP;

import com.maverick.sftp.FileTransferProgress;
import com.maverick.sftp.SftpFile;
import com.sshtools.sftp.DirectoryOperation;
import com.sshtools.sftp.SftpClient;

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