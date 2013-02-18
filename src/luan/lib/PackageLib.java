package luan.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;


public final class PackageLib {

	public static final String NAME = "package";

	public static final LuanFunction LOADER = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			LuanTable global = luan.global();
			LuanTable module = new LuanTable();
			module.put("loaded",luan.loaded());
			module.put("preload",luan.preload());
//			module.put("path","?.lua");
			try {
				add( global, "require", LuanState.class, String.class );
				add( module, "module", LuanState.class, String.class );
				add( module, "search_path", String.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			module.put("searchers",new LuanTable(Arrays.<Object>asList(preloadSearcher,fileSearcher,javaFileSearcher)));
			return new Object[]{module};
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLib.class.getMethod(method,parameterTypes),null) );
	}

	public static void require(LuanState luan,String modName) throws LuanException {
		Object mod = module(luan,modName);
		if( mod instanceof LuanTable )
			luan.global().put(modName,mod);
	}

	public static Object module(LuanState luan,String modName) throws LuanException {
		Object mod = luan.loaded().get(modName);
		if( mod == null ) {
			LuanTable searchers = (LuanTable)luan.get("package.searchers");
			for( Object s : searchers.asList() ) {
				LuanFunction searcher = (LuanFunction)s;
				Object[] a = luan.call(searcher,LuanElement.JAVA,"searcher",modName);
				if( a.length >= 1 && a[0] instanceof LuanFunction ) {
					LuanFunction loader = (LuanFunction)a[0];
					Object extra = a.length >= 2 ? a[1] : null;
					mod = Luan.first(luan.call(loader,LuanElement.JAVA,"loader",modName,extra));
					if( mod == null )
						mod = true;
					luan.loaded().put(modName,mod);
				}
			}
			if( mod == null )
				throw new LuanException( luan, LuanElement.JAVA, "module '"+modName+"' not found" );
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

	private static final LuanFunction fileLoader = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String fileName = (String)args[1];
			LuanFunction fn = BasicLib.load_file(luan,fileName);
			return luan.call(fn,LuanElement.JAVA,modName,args);
		}
	};

	public static final LuanFunction fileSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String path = (String)luan.get("package.path");
			if( path==null )
				return LuanFunction.EMPTY_RTN;
			String file = search_path(modName,path);
			return file==null ? LuanFunction.EMPTY_RTN : new Object[]{fileLoader,file};
		}
	};

	public static final LuanFunction preloadSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			Object mod = luan.preload().get(modName);
			return new Object[]{mod};
		}
	};




	private static final LuanFunction javaFileLoader = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			try {
				String modName = (String)args[0];
				URL url = (URL)args[1];
				String src = Utils.read(url);
				LuanFunction fn = BasicLib.load(luan,src,url.toString());
				return luan.call(fn,LuanElement.JAVA,modName,args);
			} catch(IOException e) {
				throw new LuanException(luan,LuanElement.JAVA,e);
			}
		}
	};

	public static final LuanFunction javaFileSearcher = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			String modName = (String)args[0];
			String path = (String)luan.get("package.jpath");
			if( path==null )
				return LuanFunction.EMPTY_RTN;
			for( String s : path.split(";") ) {
				String file = s.replaceAll("\\?",modName);
				URL url = ClassLoader.getSystemResource(file);
				if( url != null ) {
					return new Object[]{javaFileLoader,url};
				}
			}
			return LuanFunction.EMPTY_RTN;
		}
	};

}
