package luan.lib;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.net.URL;
import luan.LuanState;
import luan.LuanException;
import luan.LuanElement;


public final class Utils {
	private Utils() {}  // never

	public static void checkNotNull(LuanState luan,Object v,String expected) throws LuanException {
		if( v == null )
			throw new LuanException(luan,LuanElement.JAVA,"bad argument #1 ("+expected+" expected, got nil)");
	}

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
