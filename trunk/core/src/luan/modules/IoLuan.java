package luan.modules;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
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
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class IoLuan {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = Luan.newTable();
			try {
				add( module, "read_console_line", String.class );

				module.put( "protocols", newProtocols() );

				add( module, "get", LuanState.class, String.class );

				LuanTable stdin = Luan.newTable();
				stdin.put( "read_text", new LuanJavaFunction(
					IoLuan.class.getMethod( "stdin_read_text" ), null
				) );
				stdin.put( "read_binary", new LuanJavaFunction(
					IoLuan.class.getMethod( "stdin_read_binary" ), null
				) );
				stdin.put( "read_lines", new LuanJavaFunction(
					IoLuan.class.getMethod( "stdin_read_lines" ), null
				) );
				stdin.put( "read_blocks", new LuanJavaFunction(
					IoLuan.class.getMethod( "stdin_read_blocks", Integer.class ), null
				) );
				module.put( "stdin", stdin );

				add( module, "Socket", String.class, Integer.TYPE );
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
		t.put( method, new LuanJavaFunction(IoLuan.class.getMethod(method,parameterTypes),null) );
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
		LuanTable writer = Luan.newTable();
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
		LuanTable writer = Luan.newTable();
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
		public abstract String to_string();

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
			LuanTable tbl = Luan.newTable();
			try {
				tbl.put( "to_string", new LuanJavaFunction(
					LuanIn.class.getMethod( "to_string" ), this
				) );
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

		private LuanUrl(URL url) throws LuanException {
			this.url = url;
		}

		@Override InputStream inputStream() throws IOException {
			return url.openStream();
		}

		@Override public String to_string() {
			return url.toString();
		}
	}

	public static final class LuanFile extends LuanIO {
		private final File file;

		private LuanFile(LuanState luan,File file) throws LuanException {
			this(file);
			check(luan,file.toString());
		}

		private LuanFile(File file) {
			this.file = file;
		}

		@Override InputStream inputStream() throws IOException {
			return new FileInputStream(file);
		}

		@Override OutputStream outputStream() throws IOException {
			return new FileOutputStream(file);
		}

		@Override public String to_string() {
			return file.toString();
		}

		public LuanTable child(LuanState luan,String name) throws LuanException {
			return new LuanFile(luan,new File(file,name)).table();
		}

		public LuanTable children(LuanState luan) throws LuanException {
			File[] files = file.listFiles();
			if( files==null )
				return null;
			LuanTable list = Luan.newTable();
			for( File f : files ) {
				list.add(new LuanFile(luan,f).table());
			}
			return list;
		}

		public boolean exists() {
			return file.exists();
		}

		@Override LuanTable table() {
			LuanTable tbl = super.table();
			try {
				tbl.put( "name", new LuanJavaFunction(
					File.class.getMethod( "getName" ), file
				) );
				tbl.put( "exists", new LuanJavaFunction(
					LuanFile.class.getMethod( "exists" ), this
				) );
				tbl.put( "is_directory", new LuanJavaFunction(
					File.class.getMethod( "isDirectory" ), file
				) );
				tbl.put( "is_file", new LuanJavaFunction(
					File.class.getMethod( "isFile" ), file
				) );
				tbl.put( "delete", new LuanJavaFunction(
					File.class.getMethod( "delete" ), file
				) );
				tbl.put( "mkdir", new LuanJavaFunction(
					File.class.getMethod( "mkdir" ), file
				) );
				tbl.put( "mkdirs", new LuanJavaFunction(
					File.class.getMethod( "mkdirs" ), file
				) );
				tbl.put( "last_modified", new LuanJavaFunction(
					File.class.getMethod( "lastModified" ), file
				) );
				tbl.put( "child", new LuanJavaFunction(
					LuanFile.class.getMethod( "child", LuanState.class, String.class ), this
				) );
				tbl.put( "children", new LuanJavaFunction(
					LuanFile.class.getMethod( "children", LuanState.class ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return tbl;
		}
	}

	public static LuanTable file(LuanState luan,String name) throws LuanException {
		File file = new File(name);
		if( !file.exists() )
			return null;
		return new LuanFile(file).table();
	}

	public static LuanTable classpath(LuanState luan,String path) throws LuanException {
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
		if( url == null )
			return null;
		return new LuanUrl(url).table();
	}

	private static LuanTable newProtocols() {
		LuanTable protocols = Luan.newTable();
		try {
			add( protocols, "file", LuanState.class, String.class );
			add( protocols, "classpath", LuanState.class, String.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return protocols;
	}

	private static LuanTable protocols(LuanState luan) {
		LuanTable t = (LuanTable)PackageLuan.loaded(luan).get("Io");
		if( t == null )
			return newProtocols();
		t = (LuanTable)t.get("protocols");
		if( t == null )
			return newProtocols();
		return t;
	}

	public static LuanTable get(LuanState luan,String name) throws LuanException {
		int i = name.indexOf(':');
		if( i == -1 )
			throw luan.exception( "invalid IO name '"+name+"', missing protocol" );
		String protocol = name.substring(0,i);
		String location = name.substring(i+1);
		LuanTable protocols = protocols(luan);
		LuanFunction opener = (LuanFunction)protocols.get(protocol);
		if( opener == null )
			throw luan.exception( "invalid protocol '"+protocol+"' in '"+name+"'" );
		return (LuanTable)Luan.first(luan.call(opener,"<open \""+name+"\">",new Object[]{location}));
	}

	public static final class LuanSocket extends LuanIO {
		private final Socket socket;

		private LuanSocket(String host,int port) throws IOException {
			this(new Socket(host,port));
		}

		private LuanSocket(Socket socket) throws IOException {
			this.socket = socket;
		}

		@Override InputStream inputStream() throws IOException {
			return socket.getInputStream();
		}

		@Override OutputStream outputStream() throws IOException {
			return socket.getOutputStream();
		}

		@Override public String to_string() {
			return socket.toString();
		}

		public LuanTable Pickle_client(LuanState luan) throws IOException {
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
				tbl.put( "Pickle_client", new LuanJavaFunction(
					LuanSocket.class.getMethod( "Pickle_client", LuanState.class ), this
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

	public static LuanTable Socket(String host,int port) throws IOException {
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


	// security

	public interface Security {
		public void check(LuanState luan,String name) throws LuanException;
	}

	private static String SECURITY_KEY = "Io.Security";

	private static void check(LuanState luan,String name) throws LuanException {
		Security s = (Security)luan.registry().get(SECURITY_KEY);
		if( s!=null )
			s.check(luan,name);
	}

	public static void setSecurity(LuanState luan,Security s) {
		luan.registry().put(SECURITY_KEY,s);
	}

	public static class DirSecurity implements Security {
		private final String[] dirs;

		public DirSecurity(LuanState luan,String[] dirs) {
			this.dirs = dirs;
		}

		@Override public void check(LuanState luan,String name) throws LuanException {
			if( name.contains("..") )
				throw luan.exception("Security violation - '"+name+"' contains '..'");
			for( String dir : dirs ) {
				if( name.startsWith(dir) )
					return;
			}
			throw luan.exception("Security violation - '"+name+"' not in allowed directory");
		}
	}


	private void IoLuan() {}  // never
}
