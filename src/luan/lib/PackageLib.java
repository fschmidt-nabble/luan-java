package luan.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanLoader;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;


public final class PackageLib {

	public static final String NAME = "Package";

	public static final LuanLoader LOADER = new LuanLoader() {
		@Override protected void load(LuanState luan) {
			LuanTable module = new LuanTable();
			LuanTable global = new LuanTable();
			module.put( LuanState._G, global );
			module.put("loaded",luan.loaded());
			module.put("preload",luan.preload());
			module.put("path","?.lua");
			try {
				add( global, "require", LuanState.class, String.class );
				add( global, "module", LuanState.class, String.class, String.class );
				add( module, "search_path", String.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			module.put("searchers",new LuanTable(Arrays.<Object>asList(preloadSearcher,fileSearcher,javaFileSearcher)));
			luan.loaded().put(NAME,module);
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLib.class.getMethod(method,parameterTypes),null) );
	}

	public static void module(LuanState luan,String modName,String superMod) throws LuanException {
		LuanTable module;
		if( superMod==null ) {
			module = new LuanTable();
		} else {
			require(luan,superMod);
			module = (LuanTable)luan.loaded().get(superMod);
		}
		luan.currentEnvironment().put(modName,module);
		luan.loaded().put(modName,module);
	}

	public static void require(LuanState luan,String modName) throws LuanException {
		require(luan,modName,luan.currentEnvironment());
	}

	public static void require(LuanState luan,String modName,LuanTable env) throws LuanException {
		LuanTable mod = (LuanTable)luan.loaded().get(modName);
		if( mod == null ) {
			LuanTable searchers = (LuanTable)luan.get("Package.searchers");
			if( searchers == null )
				searchers = new LuanTable(Collections.<Object>singletonList(preloadSearcher));
			for( Object s : searchers.asList() ) {
				LuanFunction searcher = (LuanFunction)s;
				Object[] a = luan.JAVA.call(searcher,"<searcher>",modName);
				if( a.length >= 1 && a[0] instanceof LuanFunction ) {
					LuanFunction loader = (LuanFunction)a[0];
					luan.JAVA.call(loader,"<require \""+modName+"\">");
					mod = (LuanTable)luan.loaded().get(modName);
					if( mod==null )
						throw luan.JAVA.exception( "module '"+modName+"' didn't define its module" );
					break;
				}
			}
			if( mod == null )
				throw luan.JAVA.exception( "module '"+modName+"' not found" );
		}
		if( env != null )
			env.put(modName,mod);
	}

	public static String search_path(String name,String path) {
		for( String s : path.split(";") ) {
			String file = s.replaceAll("\\?",name);
			if( new File(file).exists() )
				return file;
		}
		return null;
	}

	private static final class FileLoader extends LuanLoader {
		private final String fileName;

		FileLoader(String fileName) {
			this.fileName = fileName;
		}

		@Override protected void load(LuanState luan) throws LuanException {
			LuanFunction fn = BasicLib.load_file(luan,fileName,null);
			fn.call(luan,EMPTY);
		}
	};

	public static final LuanFunction fileSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String path = (String)luan.get("Package.path");
			if( path==null )
				return LuanFunction.EMPTY;
			String file = search_path(modName,path);
			return file==null ? LuanFunction.EMPTY : new Object[]{new FileLoader(file)};
		}
	};

	public static final LuanFunction preloadSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			Object mod = luan.preload().get(modName);
			return new Object[]{mod};
		}
	};




	private static final class JavaFileLoader extends LuanLoader {
		private final URL url;

		JavaFileLoader(URL url) {
			this.url = url;
		}

		@Override protected void load(LuanState luan) throws LuanException {
			try {
				String src = Utils.read(url);
				LuanFunction fn = BasicLib.load(luan,src,url.toString(),null);
				fn.call(luan,EMPTY);
			} catch(IOException e) {
				throw luan.JAVA.exception(e);
			}
		}
	};

	public static final LuanFunction javaFileSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String path = (String)luan.get("Package.jpath");
			if( path==null )
				return LuanFunction.EMPTY;
			for( String s : path.split(";") ) {
				String file = s.replaceAll("\\?",modName);
				URL url = ClassLoader.getSystemResource(file);
				if( url != null ) {
					return new Object[]{new JavaFileLoader(url)};
				}
			}
			return LuanFunction.EMPTY;
		}
	};

}
