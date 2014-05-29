package luan.lib;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import luan.LuanState;
import luan.LuanException;
import luan.LuanElement;


public final class Utils {
	private Utils() {}  // never

	private static final int bufSize = 8192;

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

	public static void copyAll(InputStream in,OutputStream out)
		throws IOException
	{
		byte[] a = new byte[bufSize];
		int n;
		while( (n=in.read(a)) != -1 ) {
			out.write(a,0,n);
		}
	}

	public static byte[] readAll(File file)
		throws IOException
	{
		int len = (int)file.length();
		ByteArrayOutputStream out = new ByteArrayOutputStream(len) {
			public byte[] toByteArray() {
				return buf;
			}
		};
		FileInputStream in = new FileInputStream(file);
		copyAll(in,out);
		in.close();
		return out.toByteArray();
	}

	public static void write(File file,String s)
		throws IOException
	{
		Writer out = new FileWriter(file);
		out.write(s);
		out.close();
	}

	public static void writeAll(byte[] a,OutputStream out)
		throws IOException
	{
		copyAll(new ByteArrayInputStream(a),out);
	}

	public static void writeAll(File file,byte[] a)
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);
		writeAll(a,fos);
		fos.close();
	}

	public static byte[] readAll(InputStream in)
		throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyAll(in,out);
		return out.toByteArray();
	}

}
