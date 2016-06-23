package org.iplantc.service.common.auth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.X509Principal;
import org.globus.gsi.util.CertificateIOUtil;
import org.globus.myproxy.MyProxyException;
import org.globus.myproxy.MyTrustManager;


public class MyProxy extends org.globus.myproxy.MyProxy
{
	private static final Logger logger = Logger.getLogger(MyProxy.class);
	private String trustrootPath;
	
	static {
		System.setProperty("jsse.enableCBCProtection", "false");
	}
	
	public MyProxy(String host, int port) {
		this(host,port, null);
	}

	/**
	 * Full constructor assigning a custom default path to store the
	 * trustroots and write them to disk.
	 * @param host
	 * @param port
	 * @param trustrootPath
	 */
	public MyProxy(String host, int port, String trustrootPath) {
		super(host, port);
		if (StringUtils.isEmpty(trustrootPath)) {
			trustrootPath = MyProxy.getTrustRootPath();
		}
		setInstanceTrustRootPath(trustrootPath);
	}
	
	public void setInstanceTrustRootPath(String trustrootPath) {
		this.trustrootPath = trustrootPath;
	}	
	
	public String getInstanceTrustRootPath() {
		return this.trustrootPath;
	}	
	
	/**
     * Writes the retrieved trust roots to the trusted certificates
     * directory set for this client instance.
     * @return true if trust roots are written successfully, false if no
     *         trust roots are available to be written
     */
    public boolean writeTrustRoots() throws IOException {
        return writeTrustRoots(this.trustrootPath);
    }
	
	/**
     * Bootstraps trustroot information from the MyProxy server.
     *
     * @exception MyProxyException
     *         If an error occurred during the operation.
     */
	@Override
    public void bootstrapTrust() throws MyProxyException {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            MyTrustManager myTrustManager = new MyTrustManager();
            TrustManager[] trustAllCerts = new TrustManager[] { myTrustManager };
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sf = sc.getSocketFactory();
            SSLSocket socket = (SSLSocket)sf.createSocket(this.host, this.port);
            socket.setEnabledProtocols(new String[] { "SSLv3", "TLSv1" });
            socket.startHandshake();
            socket.close();

            X509Certificate[] acceptedIssuers = myTrustManager.getAcceptedIssuers();
            if (acceptedIssuers == null) {
                throw new MyProxyException("Failed to determine MyProxy server trust roots in bootstrapTrust.");
            }
            for (int idx = 0; idx < acceptedIssuers.length; idx++)
            {
                File x509Dir = new File(this.trustrootPath);
                
                StringBuffer newSubject = new StringBuffer();
                String[] subjArr = acceptedIssuers[idx].getSubjectDN().getName().split(", ");
                for(int i = (subjArr.length - 1); i > -1; i--)
                {
                    newSubject.append("/");
                    newSubject.append(subjArr[i]);
                }
                String subject = newSubject.toString();

                File tmpDir = new File(getTrustRootPath() + "-" +
                                       System.currentTimeMillis());
                if (tmpDir.mkdir() == true)
                {
                    String hash = opensslHash(acceptedIssuers[idx]);
                    String filename = tmpDir.getPath() + tmpDir.separator + hash + ".0";

                    FileOutputStream os = new FileOutputStream(new File(filename));
                    CertificateIOUtil.writeCertificate(os, acceptedIssuers[idx]);

                    os.close();
                    if (logger.isDebugEnabled()) {
                        logger.debug("wrote trusted certificate to " + filename);
                    }

                    filename = tmpDir.getPath() + tmpDir.separator + hash + ".signing_policy";

                    os = new FileOutputStream(new File(filename));
                    Writer wr = new OutputStreamWriter(os, Charset.forName("UTF-8"));
                    wr.write("access_id_CA X509 '");
                    wr.write(subject);
                    wr.write("'\npos_rights globus CA:sign\ncond_subjects globus \"*\"\n");
                    wr.flush();
                    wr.close();
                    os.close();

                    if (logger.isDebugEnabled()) {
                        logger.debug("wrote trusted certificate policy to " + filename);
                    }

                    // success. commit the bootstrapped directory if a ca directory was not already there
                    if (x509Dir.exists())
                    {
                        // copy in each file, overwriting the existing one
                        for (File certFile: tmpDir.listFiles()) {
                            FileUtils.copyFileToDirectory(certFile, x509Dir);
                            if (logger.isDebugEnabled()) {
                                logger.debug("copied " + certFile.getPath() + " to " +
                                             x509Dir.getPath());
                            }
                        }
                    }
                    else {
                        if (tmpDir.renameTo(x509Dir) == true)
                        {
                            if (logger.isDebugEnabled()) {
                                logger.debug("renamed " + tmpDir.getPath() + " to " +
                                             x509Dir.getPath());
                            }
                        }
                        else
                        {
                            throw new MyProxyException("Unable to rename " + tmpDir.getPath() + " to " +
                                                x509Dir.getPath());
                        }
                    }
                }
                else
                {
                    throw new MyProxyException("Cannot create temporary directory: " + tmpDir.getName());
                }
            }
        } catch(Exception e) {
        	throw new MyProxyException("MyProxy bootstrapTrust failed.", e);
        }
    }
	
	/**
	 * The following methods are based off code to compute the subject 
	 * name hash from:
	 * http://blog.piefox.com/2008/10/javaopenssl-ca-generation.html
	 */
	private static String opensslHash(X509Certificate cert) 
	{
		try {
			return openssl_X509_NAME_hash(cert.getSubjectX500Principal());
		}
		catch (Exception e) {
			throw new Error("MD5 isn't available!", e);
		}
	}
	
	/**
     * Generates a hex X509_NAME hash (like openssl x509 -hash -in cert.pem)
     * Based on openssl's crypto/x509/x509_cmp.c line 321
     * @param p {@link X509Principal} for which to calculate the hah
     * @return
     * @throws Exception
     */
    private static String openssl_X509_NAME_hash(X500Principal p) throws Exception {
        // This code replicates OpenSSL's hashing function
        // DER-encode the Principal, MD5 hash it, then extract the first 4 bytes and reverse their positions
        byte[] derEncodedSubject = p.getEncoded();
        byte[] md5 = MessageDigest.getInstance("MD5").digest(derEncodedSubject);

        // Reduce the MD5 hash to a single unsigned long
        byte[] result = new byte[] { md5[3], md5[2], md5[1], md5[0] };
        return toHex(result);
    }
	
    /**
     * Encode binary to hex
     * @param bin byte array to encode
     * @return
     */
    private static String toHex(final byte[] bin) {
        if (bin == null || bin.length == 0)
            return "";

        char[] buffer = new char[bin.length * 2];

        final char[] hex = "0123456789abcdef".toCharArray();

        // i tracks input position, j tracks output position
        for (int i = 0, j = 0; i < bin.length; i++)
        {
            final byte b = bin[i];
            buffer[j++] = hex[(b >> 4) & 0x0F];
            buffer[j++] = hex[b & 0x0F];
        }
        return new String(buffer);
    }
}
