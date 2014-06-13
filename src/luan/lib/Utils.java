package luan.lib;

import java.io.Reader;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import luan.LuanState;
import luan.LuanException;


public final class Utils {
	private Utils() {}  // never

	static final int bufSize = 8192;

	public static void checkNotNull(LuanState luan,Object v,String expected) throws LuanException {
		if( v == null )
			throw luan.exception("bad argument #1 ("+expected+" expected, got nil)");
	}

	public static String readAll(Reader in)
		throws IOException
	{
		char[] a = new char[bufSize];
		StringBuilder buf = new StringBuilder();
		int n;
		while( (n=in.read(a)) != -1 ) {
			buf.append(a,0,n);
		}
		return buf.toString();
	}

	public static void copyAll(InputStream in,OutputStream out)
		throws IOException
	{
		byte[] a = new byte[bufSize];
		int n;
		while( (n=in.read(a)) != -1 ) {
			out.write(a,0,n);
		}
	}

	public static byte[] readAll(InputStream in)
		throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyAll(in,out);
		return out.toByteArray();
	}

	public static boolean isFile(String path) {
		return new File(path).exists();
	}

	public static String toUrl(String path) {
		if( path.indexOf(':') == -1 )
			return null;
		if( path.startsWith("java:") ) {
			path = path.substring(5);
			URL url = ClassLoader.getSystemResource(path);
			return url==null ? null : url.toString();
		}
		try {
			new URL(path);
			return path;
		} catch(MalformedURLException e) {}
		return null;
	}

	public static boolean exists(String path) {
		return isFile(path) || toUrl(path)!=null;
	}
}
