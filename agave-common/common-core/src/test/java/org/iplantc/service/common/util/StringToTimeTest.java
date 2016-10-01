package org.iplantc.service.common.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StringToTimeTest {

	@Test
	public void testMySqlDateFormat() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DATE, 26);
		cal.set(Calendar.YEAR, 1981);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 26);
		cal.set(Calendar.SECOND, 3);
		cal.set(Calendar.MILLISECOND, 435);
		
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("1981-10-26 15:26:03.435", now));
	}
	
	/* FIXME
	public void testISO8601() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DATE, 26);
		cal.set(Calendar.YEAR, 1981);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 25);
		cal.set(Calendar.SECOND, 2);
		cal.set(Calendar.MILLISECOND, 435);
		
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("1981-10-26T15:26:03.435ZEST", now));
	}
	*/
	
	@Test
	public void test1200Seconds() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		cal.set(Calendar.SECOND, cal.get(Calendar.SECOND)+1200);
		Assert.assertTrue(new Date(cal.getTimeInMillis()).equals(new StringToTime("+1200 s", now)));
		Assert.assertFalse(new Date(cal.getTimeInMillis()).equals(new StringToTime("+1 s", now)));
	}
	
	@Test
	public void testVariousExpressionsOfTimeOfDay() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 0);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("11:59:59 PM", now));
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("23:59:59", now));
		
		cal.set(Calendar.SECOND, 0);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("23:59", now));
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("11:59 PM", now));
		
		cal.set(Calendar.MILLISECOND, 123);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("23:59:00.123"));
		
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DATE, 26);
		cal.set(Calendar.YEAR, 1981);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 27);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("October 26, 1981 3:27:00 PM", now));
		
		cal.set(Calendar.HOUR, 5);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.AM_PM, Calendar.PM);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("10/26/81 5PM", now));
		
		cal.setTime(now);
		cal.set(Calendar.DATE, cal.get(Calendar.DATE)+1);
		cal.set(Calendar.HOUR, 5);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.AM_PM, Calendar.PM);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("tomorrow 5PM", now));
		
		cal.set(Calendar.DATE, cal.get(Calendar.DATE)-2);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("yesterday 5PM", now));
		Assert.assertEquals(new StringToTime("yesterday evening", now), new StringToTime("yesterday 5PM", now));
	}
	
	@Test
	public void testStaticMethods() {
		Date now = new Date();
		
		// timestamp
		Long time = (Long) StringToTime.time("now", now);
		Assert.assertEquals(new Date(now.getTime()), new Date(time));
		
		// calendar
		Calendar cal = (Calendar) StringToTime.cal("now", now);
		Assert.assertEquals(new Date(now.getTime()), new Date(cal.getTimeInMillis()));
		
		// date
		Date date = (Date) StringToTime.date("now", now);
		Assert.assertEquals(new Date(now.getTime()), date);
	}

	@Test
	public void testInstancePattern() {
		StringToTime date = new StringToTime("26 October 1981");
		BeanWrapper bean = new BeanWrapperImpl(date);
		Calendar cal = new GregorianCalendar(1981, Calendar.OCTOBER, 26);
		Long myBirthday = cal.getTimeInMillis();
		
		// string value of the StringToTime object is the timestamp
		Assert.assertEquals(myBirthday, new Long(date.getTime()));
		
		// formatting controlled by constructor
		date = new StringToTime("26 October 1981", "d MMM yyyy");
		Assert.assertEquals("26 Oct 1981", date.toString());
		date = new StringToTime("26 October 1981", "M/d/yy");
		Assert.assertEquals("10/26/81", date.toString());
		
		// time property
		Assert.assertEquals(myBirthday, bean.getPropertyValue("time"));
		
		// date property
		Date now = new Date(myBirthday);
		Assert.assertEquals(now, date);
		
		// calendar property
		Assert.assertEquals(cal, bean.getPropertyValue("cal"));
		
		// format on demand
		Assert.assertEquals("October 26, 1981", date.format("MMMM d, yyyy"));
	}
	
	@Test
	public void testNow() {
		Date now = new Date();
		Assert.assertEquals(new Date(now.getTime()), new StringToTime("now", now));
	}
	
	@Test
	public void testToday() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("00:00:00.000", now), new StringToTime("today", now));
	}
	
	@Test
	public void testThisMorning() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("07:00:00.000", now), new StringToTime("this morning", now));
		Assert.assertEquals(new StringToTime("morning", now), new StringToTime("this morning", now));
	}
	
	@Test
	public void testNoon() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("12:00:00.000", now), new StringToTime("noon", now));
	}
	
	@Test
	public void testThisAfternoon() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("13:00:00.000", now), new StringToTime("this afternoon", now));
		Assert.assertEquals(new StringToTime("afternoon", now), new StringToTime("this afternoon", now));
	}
	
	@Test
	public void testThisEvening() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("17:00:00.000", now), new StringToTime("this evening", now));
		Assert.assertEquals(new StringToTime("evening", now), new StringToTime("this evening", now));
	}
	
	@Test
	public void testTonight() {
		Date now = new Date();
		Assert.assertEquals(StringToTime.time("20:00:00.000", now), StringToTime.time("tonight", now));
	}
	
	@Test
	public void testIncrements() {
		Date now = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)+1);
		Assert.assertEquals(cal.getTimeInMillis(), StringToTime.time("+1 hour", now));
		
		cal.setTime(now);
		cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR)+52);
		Assert.assertEquals(cal.getTimeInMillis(), StringToTime.time("+52 weeks", now));
		
		Assert.assertEquals(new StringToTime("1 year", now), new StringToTime("+1 year", now));
		
		Assert.assertEquals(new StringToTime("+1 year", now), new StringToTime("+12 months", now));
		
		Assert.assertEquals(new StringToTime("+1 year 6 months", now), new StringToTime("+18 months", now));
		
		Assert.assertEquals(new StringToTime("12 months 1 day 60 seconds", now), new StringToTime("1 year 24 hours 1 minute", now));
	}
	
	@Test
	public void testDecrements() {
		Date now = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("-1 hour", now));
	}
	
	@Test
	public void testTomorrow() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.set(Calendar.DATE, cal.get(Calendar.DATE)+1);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("tomorrow", now));
		Assert.assertEquals(new StringToTime("now +24 hours", now), new StringToTime("tomorrow", now));
	}
	
	@Test
	public void testTomorrowMorning() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("this morning +24 hours", now), new StringToTime("tomorrow morning", now));
	}
	
	@Test
	public void testTomorrowNoon() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("noon +24 hours", now), new StringToTime("tomorrow noon", now));
		Assert.assertEquals(new StringToTime("noon +24 hours", now), new StringToTime("noon tomorrow", now));
	}
	
	@Test
	public void testTomorrowAfternoon() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("this afternoon +24 hours", now), new StringToTime("tomorrow afternoon", now));
	}
	
	@Test
	public void testTomorrowEvening() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("this evening +24 hours", now), new StringToTime("tomorrow evening", now));
	}
	
	@Test
	public void testTomorrowNight() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("tonight +24 hours", now), new StringToTime("tomorrow night", now));
	}
	
	// e.g., October 26, 1981, or Oct 26, 1981, or 26 October 1981, or 26 Oct 1981, or 26 Oct 81
	@Test
	public void testLongHand() throws Exception {
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("October 26, 1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("Oct 26, 1981"));
		
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 October 1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 Oct 1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 Oct 81"));
		
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 october 1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 oct 1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("26 oct 81"));
		
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("1/1/2000"), new StringToTime("1 Jan 2000"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("1/1/2000"), new StringToTime("1 Jan 00"));
		
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("1/1/2000"), new StringToTime("1 jan 2000"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("1/1/2000"), new StringToTime("1 jan 00"));
	}
	
	// e.g., 10/26/1981 or 10/26/81
	@Test
	public void testWithSlahesMonthFirst() throws Exception {
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("10/26/1981"));
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("10/26/81"));
	}

	// e.g., 1981/10/26
	@Test
	public void testWithSlashesYearFirst() throws Exception {
		Assert.assertEquals(new SimpleDateFormat("M/d/y").parse("10/26/1981"), new StringToTime("1981/10/26"));
	}
	
	// e.g., October 26 and Oct 26
	@Test
	public void testMonthAndDate() throws Exception {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DATE, 26);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("October 26", now));
		Assert.assertEquals(new StringToTime("Oct 26", now), new StringToTime("October 26", now));
	}
	
	// e.g., 10/26
	@Test
	public void testWithSlahesMonthAndDate() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DATE, 26);
		Assert.assertEquals(new Date(cal.getTimeInMillis()), new StringToTime("10/26"));
	}
	
	// e.g., October or Oct
	@Test
	public void testMonth() throws Exception {
		Date now = new Date();
		
		Assert.assertEquals(new StringToTime("October", now), new StringToTime("Oct", now));
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		// it should be this year
		Assert.assertEquals(cal.get(Calendar.YEAR), new StringToTime("January", now).getCal().get(Calendar.YEAR));
        Assert.assertEquals(cal.get(Calendar.YEAR), new StringToTime("December", now).getCal().get(Calendar.YEAR));
	}
	
	@Test
	public void testDayOfWeek() throws Exception {
		Date now = new Date();
		Assert.assertEquals(StringToTime.date("Friday", now), StringToTime.date("Fri", now));
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		
		// if today's day of the week is greater than or equal to our test day of the week (Wednesday)
		if (cal.get(Calendar.DAY_OF_WEEK) >= 3) // then the day of the week on the date returned should be next week
			Assert.assertEquals(cal.get(Calendar.WEEK_OF_YEAR)+1, new StringToTime("Wednesday", now).getCal().get(Calendar.WEEK_OF_YEAR));
		else // otherwise, it should be this year
			Assert.assertEquals(cal.get(Calendar.WEEK_OF_YEAR), new StringToTime("Wednesday", now).getCal().get(Calendar.WEEK_OF_YEAR));
	}
	
	@Test
	public void testNext() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("next January 15", now), new StringToTime("Jan 15", now));
		Assert.assertEquals(new StringToTime("next Dec", now), new StringToTime("December", now));
		Assert.assertEquals(new StringToTime("next Sunday", now), new StringToTime("Sun", now));
		Assert.assertEquals(new StringToTime("next Sat", now), new StringToTime("Saturday", now));
	}
	
	@Test
	public void testLast() {
		Date now = new Date();
		Assert.assertEquals(new StringToTime("last January 15", now), new StringToTime("Jan 15 -1 year", now));
		Assert.assertEquals(new StringToTime("last Dec", now), new StringToTime("December -1 year", now));
		Assert.assertEquals(new StringToTime("last Sunday", now), new StringToTime("Sun -1 week", now));
		Assert.assertEquals(new StringToTime("last Sat", now), new StringToTime("Saturday -1 week", now));
	}
	
	
	
}