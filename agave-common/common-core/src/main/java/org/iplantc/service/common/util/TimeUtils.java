/**
 * 
 */
package org.iplantc.service.common.util;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * @author dooley
 *
 */
public class TimeUtils {

	/**
	 * 
	 */
	public TimeUtils() {
	}
	
	public static boolean isValidRequestedJobTime(String value)
	{
		if (StringUtils.isEmpty(value)) 
			return false;
		else
			return Pattern.matches("([0-9]{1,4}):([0-5]\\d):([0-5]\\d)", value);
	}
	
	public static int compareRequestedJobTimes(String t1, String t2) throws IllegalArgumentException
	{
		if (!isValidRequestedJobTime(t1)) {
			throw new IllegalArgumentException("Invalid requested time " + t1);
		}
		else if (!isValidRequestedJobTime(t2)) {
			throw new IllegalArgumentException("Invalid requested time " + t2);
		}
		else
		{
			String[] tod1 = t1.split(":");
			String[] tod2 = t2.split(":");
			
			if (NumberUtils.toInt(tod1[0]) == NumberUtils.toInt(tod2[0])) {
				if (NumberUtils.toInt(tod1[1]) == NumberUtils.toInt(tod2[1]))
					return new Integer(NumberUtils.toInt(tod1[2])).compareTo(new Integer(NumberUtils.toInt(tod2[2])));
				else
					return new Integer(NumberUtils.toInt(tod1[1])).compareTo(new Integer(NumberUtils.toInt(tod2[1])));
			}
			else 
			{
				return new Integer(NumberUtils.toInt(tod1[0])).compareTo(new Integer(NumberUtils.toInt(tod2[0])));
			}
		}
		
	}
	
	/**
	 * Converts a string time value in HHH:MM:SS format into a millisecond long value.
	 * 
	 * @param maxTime
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static int getMillisecondsForMaxTimeValue(String maxTime) throws IllegalArgumentException
	{
		if (!isValidRequestedJobTime(maxTime)) {
			throw new IllegalArgumentException("Invalid requested time " + maxTime);
		}
		else if (!isValidRequestedJobTime(maxTime)) {
			throw new IllegalArgumentException("Invalid requested time " + maxTime);
		}
		else
		{
			String[] hhmmss = maxTime.split(":");
			int hours = hhmmss.length > 2 ? NumberUtils.toInt(StringUtils.stripStart(hhmmss[0], "0"), 0) : 0;
			int mins = 0;
			int secs = 0;
			if (hhmmss.length > 2) {
				mins = NumberUtils.toInt(StringUtils.stripStart(hhmmss[1], "0"), 0);
				secs = NumberUtils.toInt(StringUtils.stripStart(hhmmss[2], "0"), 0);
			} else {
				mins = NumberUtils.toInt(StringUtils.stripStart(hhmmss[0], "0"), 0);
				secs = NumberUtils.toInt(StringUtils.stripStart(hhmmss[1], "0"), 0);
			}
			
			return 1000 * ((hours*3600) + (mins * 60) + secs);  
		}
		
	}

}
