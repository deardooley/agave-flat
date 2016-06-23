import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;


public class Encoding {

	public Encoding() {
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		System.out.println("System property: " + System.getProperty("file.encoding"));
		System.out.println("FileReader encoding: " + new FileReader("/etc/hosts").getEncoding());
		
	}

}
