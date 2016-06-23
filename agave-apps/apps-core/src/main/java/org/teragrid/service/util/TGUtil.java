package org.teragrid.service.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TGUtil {
	private static final Logger	log	= LogManager.getLogger(TGUtil.class);

	static Connection getJNDIConnection() throws NamingException, SQLException
	{
		String DATASOURCE_CONTEXT = "java:comp/env/jdbc/TGCDB";

		Connection result = null;

		Context initialContext = new InitialContext();
		
		DataSource datasource = (DataSource) initialContext
				.lookup(DATASOURCE_CONTEXT);
		if (datasource != null)
		{
			result = datasource.getConnection();
		}
		else
		{
			log.error("Failed to lookup datasource.");
		}

		return result;
	}

	public static boolean isEmpty(String value)
	{
		if (value == null || value.equals(""))
			return true;
		return false;
	}

	public static String getHtmlHeader(String title, String linkUrl)
	{
		return "<HTML>\n" + "<HEAD><TITLE>"
				+ title
				+ "</TITLE></HEAD>\n"
				+ "<BODY><div id=\"masthead\"><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"
				+ "<table width=\"98%\" height=\"80\" border=0 cellspacing=0 cellpadding=0>\n"
				+ "<tr>\n"
				+ "<td width=\"41\"><a href=\"http://info.teragrid.org\"><img src=\"http://info.teragrid.org/images/tg_is_logo.gif\" width=\"417\" height=\"68\" border=\"0\" alt=\"TeraGrid IS Logo\"></a></td>\n"
				+ "<td>\n"
				+ "<table width=\"100%\" height=\"80\" border=0 cellspacing=0 cellpadding=0>\n"
				+ "<tr height=\"25%\">\n"
				+ "<td align=\"right\" valign=\"middle\"></td>\n"
				+ "</tr>\n"
				+ "<tr height=\"75%\">\n"
				+ "<td align=\"center\" valign=\"middle\"></td>\n"
				+ "</tr>\n"
				+ "</table>\n"
				+ "</td>\n"
				+ "</tr>\n"
				+ "</table>\n"
				+ "</div> \n"
				+ "<hr>\n"
				+ "<table width=\"98%\" border=0 cellspacing=0 cellpadding=0>\n"
				+ "<tr>\n"
				+ "\t<td width=\"22%\" align=\"left\"></td>\n"
				+ "\t<td width=\"52%\" align=\"center\"><h1>"
				+ title
				+ "</h1></td>\n"
				+ "\t<td width=\"22%\" align=\"right\" valign=\"top\">"
				+ "<a target=\"_blank\" title=\"CSV\" style=\"border:1px solid;border-color:#FC9 #630 #330 #F96;padding:0 3px;font:bold 10px verdana,sans-serif;color:#FFF;background:#009900;text-decoration:none;margin:0;\" href=\""
				+ linkUrl.replace("html", "csv")
				+ "\">CSV</a>&nbsp;"
				+ "<a target=\"_blank\" title=\"JSON\" style=\"border:1px solid;border-color:#FC9 #630 #330 #F96;padding:0 3px;font:bold 10px verdana,sans-serif;color:#FFF;background:#0066FF;text-decoration:none;margin:0;\" href=\""
				+ linkUrl.replace("html", "json")
				+ "\">JSON</a>&nbsp;"
				+ "<a target=\"_blank\" title=\"PERL\" style=\"border:1px solid;border-color:#FC9 #630 #330 #F96;padding:0 3px;font:bold 10px verdana,sans-serif;color:#FFF;background:#003399;text-decoration:none;margin:0;\" href=\""
				+ linkUrl.replace("html", "perl")
				+ "\">PERL</a>&nbsp;"
				+ "<a target=\"_blank\" title=\"XML\" style=\"border:1px solid;border-color:#FC9 #630 #330 #F96;padding:0 3px;font:bold 10px verdana,sans-serif;color:#FFF;background:#F60;text-decoration:none;margin:0;\" href=\""
				+ linkUrl.replace("html", "xml") + "\">XML</a></td>\n"
				+ "</table>\n"
				+ "<table border=1 cellspacing=0 cellpadding=3>\n";

	}

	public static String getHtmlFooter(String linkUrl)
	{
		return "</table><br><hr>\n"
				+ "<table width=\"98%\" border=0 cellspacing=0 cellpadding=0>\n"
				+ "<tr>\n"
				+ "\t<td width=\"32%\" align=\"left\"><a href=\"mailto:help@teragrid.org?subject=Information Services feedback&body=Re: "
				+ linkUrl
				+ "\">Page or content feedback</a></td>\n"
				+ "\t<td width=\"32%\" align=\"center\"></td>\n"
				+ "\t<td width=\"32%\" align=\"right\"><a href=\"http://www.teragrid.org/about/\">About the TeraGrid</a></td>\n"
				+ "</table>\n" + "</BODY>\n" + "</HTML>\n";
	}

	public static String formatUTC(Date date)
	{
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(date);
	}
}
