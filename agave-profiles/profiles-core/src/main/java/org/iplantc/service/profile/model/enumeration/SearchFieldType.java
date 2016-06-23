package org.iplantc.service.profile.model.enumeration;

public enum SearchFieldType
{
	NAME, EMAIL, USERNAME;
	
	public static SearchFieldType fromString(String s) {
	       if ("NAME".equalsIgnoreCase(s)) {
	           return NAME;
	       } else if ("EMAIL".equalsIgnoreCase(s)) {
	           return EMAIL;
	       }  else if ("USERNAME".equalsIgnoreCase(s)) {
	           return USERNAME;
	       }  
	       return valueOf(s); 
	   }
}
