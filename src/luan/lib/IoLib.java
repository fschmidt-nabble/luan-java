package luan.lib;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class IoLib {

	public static final String NAME = "Io";

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				add( module, "file", String.class );

				LuanTable stdin = new LuanTable();
				stdin.put( "read_text", new LuanJavaFunction(
					IoLib.class.getMethod( "stdin_read_text" ), null
				) );
				stdin.put( "read_binary", new LuanJavaFunction(
					IoLib.class.getMethod( "stdin_read_binary" ), null
				) );
				stdin.put( "read_lines", new LuanJavaFunction(
					IoLib.class.getMethod( "stdin_read_lines" ), null
				) );
				module.put( "stdin", stdin );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			module.put( "stdout", writer(System.out) );
			module.put( "stderr", writer(System.err) );
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(IoLib.class.getMethod(method,parameterTypes),null) );
	}

	public static final class LuanFile {
		private final File file;

		private LuanFile(String name) {
			this.file = new File(name);
		}

		public String read_text() throws IOException {
			return Utils.read(file);
		}

		public byte[] read_binary() throws IOException {
			return Utils.readAll(file);
		}

		public void write(LuanState luan,Object obj) throws LuanException, IOException {
			if( obj instanceof String ) {
				String s = (String)obj;
				Utils.write(file,s);
				return;
			}
			if( obj instanceof byte[] ) {
				byte[] a = (byte[])obj;
				Utils.writeAll(file,a);
				return;
			}
			throw luan.JAVA.exception( "bad argument #1 to 'write' (string or binary expected)" );
		}

		public LuanTable text_writer() throws IOException {
			return writer(new FileWriter(file));
		}

		public LuanTable binary_writer() throws IOException {
			return binaryWriter(new FileOutputStream(file));
		}

		public LuanFunction read_lines() throws IOException {
			return lines(new BufferedReader(new FileReader(file)));
		}
	}

	public static LuanTable file(String name) {
		LuanTable tbl = new LuanTable();
		LuanFile file = new LuanFile(name);
		try {
			tbl.put( "read_text", new LuanJavaFunction(
				LuanFile.class.getMethod( "read_text" ), file
			) );
			tbl.put( "read_binary", new LuanJavaFunction(
				LuanFile.class.getMethod( "read_binary" ), file
			) );
			tbl.put( "write", new LuanJavaFunction(
				LuanFile.class.getMethod( "write", LuanState.class, Object.class ), file
			) );
			tbl.put( "text_writer", new LuanJavaFunction(
				LuanFile.class.getMethod( "text_writer" ), file
			) );
			tbl.put( "binary_writer", new LuanJavaFunction(
				LuanFile.class.getMethod( "binary_writer" ), file
			) );
			tbl.put( "read_lines", new LuanJavaFunction(
				LuanFile.class.getMethod( "read_lines" ), file
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}


	public String stdin_read_text() throws IOException {
		return Utils.readAll(new InputStreamReader(System.in));
	}

	public byte[] stdin_read_binary() throws IOException {
		return Utils.readAll(System.in);
	}

	public LuanFunction stdin_read_lines() throws IOException {
		return lines(new BufferedReader(new InputStreamReader(System.in)));
	}


	public interface LuanWriter {
		public void write(LuanState luan,Object... args) throws LuanException, IOException;
		public void close() throws IOException;
	}

	public static LuanTable writer(final PrintStream out) {
		LuanWriter luanWriter = new LuanWriter() {

			public void write(LuanState luan,Object... args) throws LuanException {
				for( Object obj : args ) {
					out.print( luan.JAVA.toString(obj) );
				}
			}

			public void close() {
				out.close();
			}
		};
		return writer(luanWriter);
	}

	public static LuanTable writer(final Writer out) {
		LuanWriter luanWriter = new LuanWriter() {

			public void write(LuanState luan,Object... args) throws LuanException, IOException {
				for( Object obj : args ) {
					out.write( luan.JAVA.toString(obj) );
				}
			}

			public void close() throws IOException {
				out.close();
			}
		};
		return writer(luanWriter);
	}

	private static LuanTable writer(LuanWriter luanWriter) {
		LuanTable writer = new LuanTable();
		try {
			writer.put( "write", new LuanJavaFunction(
				LuanWriter.class.getMethod( "write", LuanState.class, new Object[0].getClass() ), luanWriter
			) );
			writer.put( "close", new LuanJavaFunction(
				LuanWriter.class.getMethod( "close" ), luanWriter
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return writer;
	}


	public static LuanTable binaryWriter(final OutputStream out) {
		LuanTable writer = new LuanTable();
		try {
			writer.put( "write", new LuanJavaFunction(
				OutputStream.class.getMethod( "write", new byte[0].getClass() ), out
			) );
			writer.put( "close", new LuanJavaFunction(
				OutputStream.class.getMethod( "close" ), out
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return writer;
	}

	public static LuanFunction lines(final BufferedReader in) {
		return new LuanFunction() {
			public Object call(LuanState luan,Object[] args) throws LuanException {
				try {
					if( args.length==1 && "close".equals(args[0]) ) {
						in.close();
						return null;
					}
					String rtn = in.readLine();
					if( rtn==null )
						in.close();
					return rtn;
				} catch(IOException e) {
					throw luan.JAVA.exception(e);
				}
			}
		};
	}

}
