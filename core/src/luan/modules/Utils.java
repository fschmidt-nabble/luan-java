package luan.modules;

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

	public static boolean exists(File file) {
		try {
			return file.exists() && file.getName().equals(file.getCanonicalFile().getName());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File toFile(String path) {
		if( path.contains("//") )
			return null;
		File file = new File(path);
		return exists(file) ? file : null;
	}

	public static URL toUrl(String path) {
		if( path.indexOf(':') == -1 )
			return null;
		if( path.startsWith("java:") ) {
			path = path.substring(5);
			if( path.contains("//") )
				return null;
			URL url;
			if( !path.contains("#") ) {
				url = ClassLoader.getSystemResource(path);
			} else {
				String[] a = path.split("#");
				url = ClassLoader.getSystemResource(a[0]);
				if( url==null ) {
					for( int i=1; i<a.length; i++ ) {
						url = ClassLoader.getSystemResource(a[0]+"/"+a[i]);
						if( url != null ) {
							try {
								url = new URL(url,".");
							} catch(MalformedURLException e) {
								throw new RuntimeException(e);
							}
							break;
						}
					}
				}
			}
			return url==null ? null : url;
		}
		try {
			return new URL(path);
		} catch(MalformedURLException e) {}
		return null;
	}

	public static boolean exists(String path) {
		return toFile(path)!=null || toUrl(path)!=null;
	}
}
