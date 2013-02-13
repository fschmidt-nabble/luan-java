package luan.lib;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.net.URL;


public final class Utils {
	private Utils() {}  // never

	public static String readAll(Reader in)
		throws IOException
	{
		char[] a = new char[8192];
		StringBuilder buf = new StringBuilder();
		int n;
		while( (n=in.read(a)) != -1 ) {
			buf.append(a,0,n);
		}
		return buf.toString();
	}

	public static String read(File file)
		throws IOException
	{
		Reader in = new FileReader(file);
		String s = readAll(in);
		in.close();
		return s;
	}

	public static String read(URL url)
		throws IOException
	{
		Reader in = new InputStreamReader(url.openStream());
		String s = readAll(in);
		in.close();
		return s;
	}

}
