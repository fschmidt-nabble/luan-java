package luan.lib;

import java.io.Reader;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import luan.LuanState;
import luan.LuanException;


public final class Utils {
	private Utils() {}  // never

	static final int bufSize = 8192;

	public static void checkNotNull(LuanState luan,Object v,String expected) throws LuanException {
		if( v == null )
			throw luan.JAVA.exception("bad argument #1 ("+expected+" expected, got nil)");
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

}
