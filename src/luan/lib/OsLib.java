package luan.lib;

import java.io.File;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class OsLib {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				add( module, "File", String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(OsLib.class.getMethod(method,parameterTypes),null) );
	}

	public static class LuanFile {
		private final File file;

		public LuanFile(String name) {
			this(new File(name));
		}

		public LuanFile(File file) {
			this.file = file;
		}

		public LuanTable child(String name) {
			return new LuanFile(new File(file,name)).table();
		}

		public LuanTable io_file() {
			return new IoLib.LuanFile(file).table();
		}

		LuanTable table() {
			LuanTable tbl = new LuanTable();
			try {
				tbl.put( "name", new LuanJavaFunction(
					File.class.getMethod( "toString" ), file
				) );
				tbl.put( "exists", new LuanJavaFunction(
					File.class.getMethod( "exists" ), file
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
				tbl.put( "child", new LuanJavaFunction(
					LuanFile.class.getMethod( "child", String.class ), this
				) );
				tbl.put( "io_file", new LuanJavaFunction(
					LuanFile.class.getMethod( "io_file" ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return tbl;
		}
	}

	public static LuanTable File(String name) throws LuanException {
		return new LuanFile(name).table();
	}

}
