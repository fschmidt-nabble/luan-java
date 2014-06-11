package luan.lib;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.MalformedURLException;
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
				add( module, "java_resource_to_url", String.class );
				add( module, "url", String.class );
				add( module, "java_resource", String.class );
				add( module, "read_console_line", String.class );

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
				stdin.put( "read_blocks", new LuanJavaFunction(
					IoLib.class.getMethod( "stdin_read_blocks", Integer.class ), null
				) );
				module.put( "stdin", stdin );

				add( module, "socket", String.class, Integer.TYPE );
				add( module, "socket_server", Integer.TYPE );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			module.put( "stdout", textWriter(System.out) );
			module.put( "stderr", textWriter(System.err) );
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(IoLib.class.getMethod(method,parameterTypes),null) );
	}


	public static String stdin_read_text() throws IOException {
		return Utils.readAll(new InputStreamReader(System.in));
	}

	public static byte[] stdin_read_binary() throws IOException {
		return Utils.readAll(System.in);
	}

	public static LuanFunction stdin_read_lines() throws IOException {
		return lines(new BufferedReader(new InputStreamReader(System.in)));
	}

	public static LuanFunction stdin_read_blocks(Integer blockSize) throws IOException {
		int n = blockSize!=null ? blockSize : Utils.bufSize;
		return blocks(System.in,n);
	}

	public static String java_resource_to_url(String path) throws IOException {
		URL url = ClassLoader.getSystemResource(path);
		return url==null ? null : url.toString();
	}

	public static LuanTable java_resource(String path) throws IOException {
		return url(java_resource_to_url(path));
	}

	public static String read_console_line(String prompt) throws IOException {
		if( prompt==null )
			prompt = "> ";
		return System.console().readLine(prompt);
	}


	public interface LuanWriter {
		public void write(LuanState luan,Object... args) throws LuanException, IOException;
		public void close() throws IOException;
	}

	public static LuanTable textWriter(final PrintStream out) {
		LuanWriter luanWriter = new LuanWriter() {

			public void write(LuanState luan,Object... args) throws LuanException {
				for( Object obj : args ) {
					out.print( luan.toString(obj) );
				}
			}

			public void close() {
				out.close();
			}
		};
		return writer(luanWriter);
	}

	public static LuanTable textWriter(final Writer out) {
		LuanWriter luanWriter = new LuanWriter() {

			public void write(LuanState luan,Object... args) throws LuanException, IOException {
				for( Object obj : args ) {
					out.write( luan.toString(obj) );
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

	static LuanFunction lines(final BufferedReader in) {
		return new LuanFunction() {
			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				try {
					if( args.length > 0 ) {
						if( args.length > 1 || !"close".equals(args[0]) )
							throw luan.exception( "the only argument allowed is 'close'" );
						in.close();
						return null;
					}
					String rtn = in.readLine();
					if( rtn==null )
						in.close();
					return rtn;
				} catch(IOException e) {
					throw luan.exception(e);
				}
			}
		};
	}

	static LuanFunction blocks(final InputStream in,final int blockSize) {
		return new LuanFunction() {
			final byte[] a = new byte[blockSize];

			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				try {
					if( args.length > 0 ) {
						if( args.length > 1 || !"close".equals(args[0]) )
							throw luan.exception( "the only argument allowed is 'close'" );
						in.close();
						return null;
					}
					if( in.read(a) == -1 ) {
						in.close();
						return null;
					}
					return a;
				} catch(IOException e) {
					throw luan.exception(e);
				}
			}
		};
	}



	public static abstract class LuanIn {
		abstract InputStream inputStream() throws IOException;

		public String read_text() throws IOException {
			Reader in = new InputStreamReader(inputStream());
			String s = Utils.readAll(in);
			in.close();
			return s;
		}

		public byte[] read_binary() throws IOException {
			InputStream in = inputStream();
			byte[] a = Utils.readAll(in);
			in.close();
			return a;
		}

		public LuanFunction read_lines() throws IOException {
			return lines(new BufferedReader(new InputStreamReader(inputStream())));
		}

		public LuanFunction read_blocks(Integer blockSize) throws IOException {
			int n = blockSize!=null ? blockSize : Utils.bufSize;
			return blocks(inputStream(),n);
		}

		LuanTable table() {
			LuanTable tbl = new LuanTable();
			try {
				tbl.put( "read_text", new LuanJavaFunction(
					LuanIn.class.getMethod( "read_text" ), this
				) );
				tbl.put( "read_binary", new LuanJavaFunction(
					LuanIn.class.getMethod( "read_binary" ), this
				) );
				tbl.put( "read_lines", new LuanJavaFunction(
					LuanIn.class.getMethod( "read_lines" ), this
				) );
				tbl.put( "read_blocks", new LuanJavaFunction(
					LuanIn.class.getMethod( "read_blocks", Integer.class ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return tbl;
		}
	}

	public static abstract class LuanIO extends LuanIn {
		abstract OutputStream outputStream() throws IOException;

		public void write(LuanState luan,Object obj) throws LuanException, IOException {
			if( obj instanceof String ) {
				String s = (String)obj;
				Writer out = new OutputStreamWriter(outputStream());
				out.write(s);
				out.close();
				return;
			}
			if( obj instanceof byte[] ) {
				byte[] a = (byte[])obj;
				OutputStream out = outputStream();
				Utils.copyAll(new ByteArrayInputStream(a),out);
				out.close();
				return;
			}
			throw luan.exception( "bad argument #1 to 'write' (string or binary expected)" );
		}

		public LuanTable text_writer() throws IOException {
			return textWriter(new BufferedWriter(new OutputStreamWriter(outputStream())));
		}

		public LuanTable binary_writer() throws IOException {
			return binaryWriter(new BufferedOutputStream(outputStream()));
		}

		@Override LuanTable table() {
			LuanTable tbl = super.table();
			try {
				tbl.put( "write", new LuanJavaFunction(
					LuanIO.class.getMethod( "write", LuanState.class, Object.class ), this
				) );
				tbl.put( "text_writer", new LuanJavaFunction(
					LuanIO.class.getMethod( "text_writer" ), this
				) );
				tbl.put( "binary_writer", new LuanJavaFunction(
					LuanIO.class.getMethod( "binary_writer" ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return tbl;
		}
	}

	public static final class LuanUrl extends LuanIn {
		private final URL url;

		public LuanUrl(String s) throws MalformedURLException {
			this.url = new URL(s);
		}

		InputStream inputStream() throws IOException {
			return url.openStream();
		}
	}

	public static LuanTable url(String s) throws MalformedURLException {
		return new LuanUrl(s).table();
	}

	public static final class LuanFile extends LuanIO {
		private final File file;

		public LuanFile(String name) {
			this.file = new File(name);
		}

		InputStream inputStream() throws IOException {
			return new FileInputStream(file);
		}

		OutputStream outputStream() throws IOException {
			return new FileOutputStream(file);
		}
	}

	public static LuanTable file(String name) {
		return new LuanFile(name).table();
	}

	public static final class LuanSocket extends LuanIO {
		private final Socket socket;

		public LuanSocket(String host,int port) throws IOException {
			this(new Socket(host,port));
		}

		public LuanSocket(Socket socket) throws IOException {
			this.socket = socket;
		}

		InputStream inputStream() throws IOException {
			return socket.getInputStream();
		}

		OutputStream outputStream() throws IOException {
			return socket.getOutputStream();
		}

		public LuanTable pickle_client(LuanState luan) throws IOException {
			DataInputStream in = new DataInputStream(new BufferedInputStream(inputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(outputStream()));
			return new PickleClient(luan,in,out).table();
		}

		public void run_pickle_server(LuanState luan) throws IOException {
			DataInputStream in = new DataInputStream(new BufferedInputStream(inputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(outputStream()));
			new PickleServer(luan,in,out).run();
		}

		@Override LuanTable table() {
			LuanTable tbl = super.table();
			try {
				tbl.put( "pickle_client", new LuanJavaFunction(
					LuanSocket.class.getMethod( "pickle_client", LuanState.class ), this
				) );
				tbl.put( "run_pickle_server", new LuanJavaFunction(
					LuanSocket.class.getMethod( "run_pickle_server", LuanState.class ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return tbl;
		}
	}

	public static LuanTable socket(String host,int port) throws IOException {
		return new LuanSocket(host,port).table();
	}

	public static LuanFunction socket_server(int port) throws IOException {
		final ServerSocket ss = new ServerSocket(port);
		return new LuanFunction() {
			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				try {
					if( args.length > 0 ) {
						if( args.length > 1 || !"close".equals(args[0]) )
							throw luan.exception( "the only argument allowed is 'close'" );
						ss.close();
						return null;
					}
					return new LuanSocket(ss.accept()).table();
				} catch(IOException e) {
					throw luan.exception(e);
				}
			}
		};
	}
}
