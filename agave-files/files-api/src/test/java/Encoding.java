import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;


public class Encoding {
	
	private static final Logger log = Logger.getLogger(Encoding.class);
	
	public Encoding() {
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		log.info(new Date().toString());
		System.out.println("Date: " + new Date().toString());
		log.info(new DateTime().toString());
		System.out.println("Date: " + new DateTime().toString());
		
		System.out.println("System property: " + System.getProperty("file.encoding"));
		System.out.println("FileReader encoding: " + new FileReader("/etc/hosts").getEncoding());
		
	}

}
