 /**
  * 
  */
 package org.iplantc.service.common.auth;
 
 import java.io.IOException;
 import java.io.InputStream;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.iplantc.service.common.persistence.TenancyHelper;
 
 /**
  * Utility class to check for admin status of a given user.
  * @author dooley
  *
  */
 public class AuthorizationHelper {
     
     private static final String TRUSTED_ADMIN_FILE = "trusted_admins.txt";
     
     /**
      * Checks whether the current authenticated user is a tenant
      * admin based on whether they are currently authenticated and 
      * have the appropriate role and the existence of the user in
      * the local admin config file.
      * 
      * @param username
      * @return true if they are explicitly or implicitly defined as an admin
      */
     public static boolean isTenantAdmin(String username)
     {   
         if (TenancyHelper.isTenantAdmin(username)) return true;
         
         InputStream stream = AuthorizationHelper.class.getClassLoader().getResourceAsStream("trusted_admins.txt");
         try
         {
             String trustedUserList = IOUtils.toString(stream, "UTF-8");
             if (StringUtils.isNotEmpty(trustedUserList)) {
                 for(String user: trustedUserList.split(",")) {
                     if (username.equalsIgnoreCase(user.trim())) {
                         return true;
                     }
                 }
                 return false;
             } else {
                 return false;
             }
         }
         catch (IOException e)
         {
              //log.error("Failed to locate trusted user file");
             return false;
         }
     }
     
     /**
      * Checks whether the current authenticated user is a super
      * admin based on whether they are currently authenticated and 
      * have the appropriate role and the existence of the user in
      * the local admin config file.
      * 
      * @param username
      * @return true if they are explicitly or implicitly defined as an admin
      */
     public static boolean isSuperAdmin(String username)
     {   
         return (TenancyHelper.isTenantAdmin(username) 
                 || isUserInLocalConfigurationFile(username));
     }
     
     /**
      * Checks the local {@link #TRUSTED_ADMIN_FILE} in the service deployment
      * for the existence of the given user in the list of super users.
      * 
      * @param username
      * @return true if the username is present, false otherwise
      */
     
     private static boolean isUserInLocalConfigurationFile(String username) 
     {
         InputStream stream = AuthorizationHelper.class.getClassLoader().getResourceAsStream(TRUSTED_ADMIN_FILE);
         try
         {
             String trustedUserList = IOUtils.toString(stream, "UTF-8");
             if (StringUtils.isNotEmpty(trustedUserList)) {
                 for(String user: trustedUserList.split(",")) {
                     if (username.equalsIgnoreCase(user.trim())) {
                         return true;
                     }
                 }
                 return false;
             } else {
                 return false;
             }
         }
         catch (IOException e)
         {
             return false;
         }
     }
 }
