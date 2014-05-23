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
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;


public final class PackageLib {

	public static final String NAME = "Package";

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			LuanTable global = luan.global();
			module.put("loaded",luan.loaded());
			module.put("preload",luan.preload());
			module.put("path","?.luan");
			try {
				global.put("require",require);
				add( module, "search_path", String.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			LuanTable searchers = luan.searchers();
			searchers.add(preloadSearcher);
			searchers.add(fileSearcher);
			searchers.add(javaFileSearcher);
			module.put("searchers",searchers);
			return new Object[]{module};
		}
	};

	public static final LuanFunction require;
	static {
		try {
			require = new LuanJavaFunction(PackageLib.class.getMethod("require", LuanState.class, String.class),null);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLib.class.getMethod(method,parameterTypes),null) );
	}

	public static Object require(LuanState luan,String modName) throws LuanException {
		LuanTable loaded = luan.loaded();
		Object mod = loaded.get(modName);
		if( mod == null ) {
			LuanTable searchers = (LuanTable)luan.get("Package.searchers");
			if( searchers == null )
				searchers = new LuanTable(Collections.<Object>singletonList(preloadSearcher));
			for( Object s : searchers.asList() ) {
				LuanFunction searcher = (LuanFunction)s;
				Object[] a = luan.JAVA.call(searcher,"<searcher>",modName);
				if( a.length >= 1 && a[0] instanceof LuanFunction ) {
					LuanFunction loader = (LuanFunction)a[0];
					a[0] = modName;
					mod = Luan.first(luan.JAVA.call(loader,"<require \""+modName+"\">",a));
					if( mod != null ) {
						loaded.put(modName,mod);
					} else {
						mod = loaded.get(modName);
						if( mod==null )
							loaded.put(modName,true);
					}
					break;
				}
			}
			if( mod == null )
				throw luan.JAVA.exception( "module '"+modName+"' not found" );
		}
		return mod;
	}

	public static String search_path(String name,String path) {
		for( String s : path.split(";") ) {
			String file = s.replaceAll("\\?",name);
			if( new File(file).exists() )
				return file;
		}
		return null;
	}

	public static final LuanFunction fileLoader = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String fileName = (String)args[1];
			LuanFunction fn = BasicLib.load_file(luan,fileName);
			return fn.call(luan,new Object[]{args[0],fileName});
		}
	};

	public static final LuanFunction fileSearcher = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			String path = (String)luan.get("Package.path");
			if( path==null )
				return LuanFunction.EMPTY;
			String file = search_path(modName,path);
			return file==null ? LuanFunction.EMPTY : new Object[]{fileLoader,file};
		}
	};

	public static final LuanFunction preloadSearcher = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			Object mod = luan.preload().get(modName);
			return new Object[]{mod};
		}
	};




	public static final LuanFunction javaFileLoader = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			URL url = (URL)args[0];
			try {
				String src = Utils.read(url);
				LuanFunction fn = BasicLib.load(luan,src,url.toString(),false);
				return fn.call(luan,new Object[]{args[0],url.toString()});
			} catch(IOException e) {
				throw luan.JAVA.exception(e);
			}
		}
	};

	public static final LuanFunction javaFileSearcher = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			String path = (String)luan.get("Package.jpath");
			if( path==null )
				return LuanFunction.EMPTY;
			for( String s : path.split(";") ) {
				String file = s.replaceAll("\\?",modName);
				URL url = ClassLoader.getSystemResource(file);
				if( url != null ) {
					return new Object[]{javaFileLoader,url};
				}
			}
			return LuanFunction.EMPTY;
		}
	};

}
