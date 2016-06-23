/**
 * 
 */
package org.iplantc.service.common.auth;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.clients.beans.Profile;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * @author rion1
 *
 */
@Test(singleThreaded=true)
@SuppressWarnings("unused")
public class JWTClient 
{
	private static final Logger log = Logger.getLogger(JWTClient.class);
	
	private static final String IDENTITY_STORE_PUBLIC_KEY_FILE = "jwtcert.pem";
	private static final ThreadLocal<JSONObject> threadJWTPayload = new ThreadLocal<JSONObject>();

	private static RSAPublicKey identityServerPublicKey;
	
//	public static RSAPublicKey getIdentityServerPublicKey()
//	throws Exception 
//	{
//		if (identityServerPublicKey == null) 
//		{
//			InputStream is = null;
//			try 
//			{
//				Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//				
//				CertificateFactory cf = CertificateFactory.getInstance("X509", "BC");
//				is = Thread.currentThread().getContextClassLoader().getResourceAsStream(IDENTITY_STORE_PUBLIC_KEY_FILE);
//				X509Certificate certificate = (X509Certificate) cf.generateCertificate(is);
//				identityServerPublicKey = (RSAPublicKey)certificate.getPublicKey();
//			} 
//			finally {
//				try { is.close(); } catch (Exception e) {}
//			}
//		}
//		
//		return identityServerPublicKey;
//	}
	public static boolean parse(String serializedToken, String tenantId)
	{
		if (tenantId == null) return false;
		
		try 
		{
			// Create HMAC signer
			SignedJWT signedJWT = SignedJWT.parse(serializedToken);
			
//			RSASSAVerifier.SUPPORTED_ALGORITHMS.add(new JWSAlgorithm("SHA256withRSA", Requirement.OPTIONAL));
//			
//			JWSVerifier verifier = new RSASSAVerifier(getIdentityServerPublicKey()) {
//				@Override
//				public boolean verify(final ReadOnlyJWSHeader header, 
//					              final byte[] signedContent, 
//					              final Base64URL signature)
//					throws JOSEException {
//
//					
//					try {
//						Signature verifier = RSASignature.getInstance("SHA256withRSA");// getRSASignerAndVerifier(JWSAlgorithm.RS256);
//						
//						verifier.initVerify(getPublicKey());
//						verifier.update(signedContent);
//						return verifier.verify(signature.decode());
//						
//					} catch (InvalidKeyException e) {
//
//						throw new JOSEException("Invalid public RSA key: " + e.getMessage(), e);
//
//					} catch (SignatureException e) {
//
//						throw new JOSEException("RSA signature exception: " + e.getMessage(), e);
//					} catch (NoSuchAlgorithmException e) {
//						throw new JOSEException("RSA signature exception: " + e.getMessage(), e);
//					}
//				}
//			};
//            
//			boolean verifiedSignature = signedJWT.verify(verifier);

//			if (verifiedSignature)
			if (signedJWT != null)
			{
				ReadOnlyJWTClaimsSet claims = signedJWT.getJWTClaimsSet();
				
				Date expirationDate = claims.getExpirationTime();
				
				Assert.assertNotNull(expirationDate, 
						"No expiration date in the JWT header. Authentication failed.");
				
				Assert.assertTrue(expirationDate.after(new Date()), 
						"JWT has expired. Authentication failed.");
				
				JSONObject json = claims.toJSONObject();
				json.put("rawTenantId", tenantId); // unmodified tenant id
				tenantId = StringUtils.lowerCase(tenantId);
				tenantId = StringUtils.replaceChars(tenantId, '_', '.');
				tenantId = StringUtils.replaceChars(tenantId, '-', '.');
				json.put("tenantId", tenantId);
//				log.debug(json.toJSONString());
				setCurrentJWSObject(json);
				
				Assert.assertNotNull(getCurrentEndUser(), 
						"No end user specified in the JWT header. Authentication failed.");
				
				Assert.assertNotNull(getCurrentTenant(), 
						"No tenant specified in the JWT header. Authentication failed.");
				
				Assert.assertNotNull(getCurrentSubscriber(), 
						"No subscriber specified in the JWT header. Authentication failed.");
				
				return true;
			}
			else
			{
				log.error("Invalid JWT signature.");
				return false;
			} 
		} catch (ParseException e) {
			log.error("Failed to parse JWT object. Authentication failed.", e);
			return false;
		} catch (Throwable e) {
			log.error("Error processing JWT header. Authentication failed.", e);
			return false;
		}
	}
	
	public static JSONObject getCurrentJWSObject()
	{
		return threadJWTPayload.get();
	}
	
	public static void setCurrentJWSObject(JSONObject json)
	{
		threadJWTPayload.set(json);
	}
	
	public static String getCurrentApplicationId()
	{
		try {
			JSONObject claims = getCurrentJWSObject();
			return (String)claims.get("http://wso2.org/claims/applicationid");
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getCurrentSubscriber() 
	{
		try {
			JSONObject claims = getCurrentJWSObject();
			return (String)claims.get("http://wso2.org/claims/subscriber");
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getCurrentTenant()
	{
		try 
		{
			JSONObject claims = getCurrentJWSObject();
			if (claims.containsKey("tenantId") || !StringUtils.isEmpty((String)claims.get("tenantId"))) {
				return (String)claims.get("tenantId");
			}
			else
			{
				String subscriber = (String)claims.get("http://wso2.org/claims/subscriber");
				if (StringUtils.countMatches(subscriber, "@") > 0) {
					return subscriber.substring(subscriber.lastIndexOf("@") + 1);
				} else {
					return (String)claims.get("http://wso2.org/claims/enduserTenantId");
				}
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void setCurrentTenant(String tenantId)
	{
		JSONObject claims = getCurrentJWSObject();
		if (claims == null) {
			claims = new JSONObject();
		}
		claims.put("tenantId", tenantId);
		claims.put("http://wso2.org/claims/subscriber", tenantId);
		setCurrentJWSObject(claims);
	}
	
	public static void setCurrentEndUser(String username)
	{
		JSONObject claims = getCurrentJWSObject();
		if (claims == null) {
			claims = new JSONObject();
		}
		claims.put("http://wso2.org/claims/enduser", username);
		setCurrentJWSObject(claims);
	}

	public static String getCurrentEndUser() 
	{
		try {
			JSONObject claims = getCurrentJWSObject();
			String tenant = getCurrentTenant();
			String endUser = (String)claims.get("http://wso2.org/claims/enduser");
			endUser = StringUtils.replace(endUser, "@carbon.super", "");
			if (StringUtils.endsWith(endUser, tenant)) {
				return StringUtils.substring(endUser, 0, (-1 * tenant.length() - 1));
			} else if (endUser.contains("@")){
				return StringUtils.substringBefore(endUser, "@");
			} else if (endUser.contains("/")){
				return StringUtils.substringAfter(endUser, "/");
			} else {
				return endUser;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
   public static void setCurrentBearerToken(String bearerToken)
    {
        JSONObject claims = getCurrentJWSObject();
        if (claims == null) {
            claims = new JSONObject();
        }
        claims.put("bearerToken", bearerToken);
        setCurrentJWSObject(claims);
    }
    
    public static String getCurrentBearerToken()
    {
        JSONObject claims = getCurrentJWSObject();
        if (claims == null) {
            claims = new JSONObject();
        }
        return (String)claims.get("bearerToken");
    }
	    
    public static boolean isTenantAdmin() {
		try {
			JSONObject claims = getCurrentJWSObject();
			
			String roles = (String)claims.get("http://wso2.org/claims/role");
			if (!StringUtils.isEmpty(roles)) {
				for(String role: Arrays.asList(StringUtils.split(roles, ","))) {
					if (StringUtils.endsWith(role, "-services-admin") || StringUtils.endsWith(role, "-super-admin")) {
						if (role.contains("/")) {
							role = role.substring(role.lastIndexOf("/") + 1);
						}
						if (StringUtils.startsWith(role, (String)claims.get("rawTenantId"))) {
							return true;
						}
					}
				}
				return false;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isSuperAdmin() {
		try {
			JSONObject claims = getCurrentJWSObject();
			
			String roles = (String)claims.get("http://wso2.org/claims/role");
			if (!StringUtils.isEmpty(roles)) {
				for(String role: Arrays.asList(StringUtils.split(roles, ","))) {
					if (StringUtils.endsWith(role, "-super-admin")) {
						if (role.contains("/")) {
							role = role.substring(role.lastIndexOf("/") + 1);
						}
						if (StringUtils.startsWith(role, (String)claims.get("rawTenantId"))) {
							return true;
						}
					}
				}
				return false;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public static String getJwtHeaderKeyForTenant(String tenantId)
	throws TenantException
	{
		Tenant tenant = new TenantDao().findByTenantId(tenantId);
		
		if (tenant == null) {
			throw new TenantException("Unknown tenant id");
		} else {
			return ("x-jwt-assertion-" + StringUtils.replace(tenantId, ".", "-")).toLowerCase();
		}
	}
	
	public static String createJwtForTenantUser(String username, String tenantId, boolean resolveUserDetails) 
	throws TenantException
	{
		Tenant tenant = new TenantDao().findByTenantId(tenantId);
		
		if (tenant == null) {
			throw new TenantException("Unknown tenant id");
		}
		
		Profile profile = null;
		if (resolveUserDetails) {
			try {
				profile = new AgaveProfileServiceClient(Settings.IPLANT_PROFILE_SERVICE, null, null)
					.getUser(username, tenantId);
			} catch (Exception e) {
				log.error("Failed to resolve profile information for " + username + " in tenant " + tenantId);
			}
		}
		
		if (profile == null) 
		{
			profile = new Profile();
			profile.setEmail(tenant.getContactEmail());
			String[] names = StringUtils.split(tenant.getContactName(), " ", 2);
			if (names != null) {
				profile.setFirstName(names[0]);
				if (names.length > 1) {
					profile.setLastName(names[1]);
				}
			}
			profile.setUsername(username);
		}
		
		KeyPairGenerator keyGenerator;
		
		try 
		{
			keyGenerator = KeyPairGenerator.getInstance("RSA");
			
			keyGenerator.initialize(1024);

			KeyPair kp = keyGenerator.genKeyPair();
			RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
			RSAPrivateKey privateKey = (RSAPrivateKey)kp.getPrivate();

			// Create RSA-signer with the private key
			JWSSigner signer = new RSASSASigner(privateKey);

			// Prepare JWT with claims set
			JWTClaimsSet claimsSet = new JWTClaimsSet();
			claimsSet.setSubject(username);
			claimsSet.setIssueTime(new Date());
			claimsSet.setIssuer("wso2.org/products/am");
			claimsSet.setExpirationTime(new DateTime().plusHours(4).toDate());
			claimsSet.setCustomClaim("http://wso2.org/claims/subscriber", username);
			claimsSet.setCustomClaim("http://wso2.org/claims/applicationid", "-9999");
			claimsSet.setCustomClaim("http://wso2.org/claims/applicationname", "SSOInternal");
			claimsSet.setCustomClaim("http://wso2.org/claims/applicationtier", "Unlimited");
			claimsSet.setCustomClaim("http://wso2.org/claims/apicontext", "/myproxy");
			claimsSet.setCustomClaim("http://wso2.org/claims/version", Settings.SERVICE_VERSION);
			claimsSet.setCustomClaim("http://wso2.org/claims/tier", "Unlimited");
			claimsSet.setCustomClaim("http://wso2.org/claims/keytype", "PRODUCTION");
			claimsSet.setCustomClaim("http://wso2.org/claims/usertype", "APPLICATION_USER");
			claimsSet.setCustomClaim("http://wso2.org/claims/enduser", username);
			claimsSet.setCustomClaim("http://wso2.org/claims/enduserTenantId", "-9999");
			claimsSet.setCustomClaim("http://wso2.org/claims/emailaddress", tenant.getContactEmail());
			claimsSet.setCustomClaim("http://wso2.org/claims/fullname", profile.getFirstName() + " " + profile.getLastName());
			claimsSet.setCustomClaim("http://wso2.org/claims/givenname", profile.getFirstName());
			claimsSet.setCustomClaim("http://wso2.org/claims/lastname", profile.getLastName());
			claimsSet.setCustomClaim("http://wso2.org/claims/primaryChallengeQuestion", "N/A");
			claimsSet.setCustomClaim("http://wso2.org/claims/role", "Internal/everyone");
			claimsSet.setCustomClaim("http://wso2.org/claims/title", "N/A");
			

			SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

			// Compute the RSA signature
			signedJWT.sign(signer);

			return signedJWT.serialize();
		}
		catch (JOSEException e) {
			throw new TenantException("Failed to construct a valid JWT.", e);
		} 
		catch (NoSuchAlgorithmException e) {
			throw new TenantException("Failed to sign JWT. Unable to locate RSA algorithm.", e);
		}
				
	}
}
