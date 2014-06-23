package luan.modules;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;


public final class PackageLuan {

	private static final String jpath = "luan.modules.?Luan.LOADER";

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			module.put("loaded",luan.loaded());
			module.put("preload",luan.preload());
			module.put("path","?.luan;java:luan/modules/?.luan");
			module.put("jpath",jpath);
			try {
				add( module, "require", LuanState.class, String.class );
				add( module, "load", LuanState.class, String.class );
				add( module, "load_lib", String.class );
				add( module, "search_path", String.class, String.class );
				add( module, "search", LuanState.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			LuanTable searchers = luan.searchers();
			searchers.add(preloadSearcher);
			searchers.add(fileSearcher);
			searchers.add(javaSearcher);
			module.put("searchers",searchers);
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLuan.class.getMethod(method,parameterTypes),null) );
	}

	public static Object require(LuanState luan,String modName) throws LuanException {
		Object mod = load(luan,modName);
		if( mod==null )
			throw luan.exception( "module '"+modName+"' not found" );
		return mod;
	}

	public static Object load(LuanState luan,String modName) throws LuanException {
		LuanTable loaded = luan.loaded();
		Object mod = loaded.get(modName);
		if( mod == null ) {
			Object[] a = search(luan,modName);
			if( a == null )
				return null;
			LuanFunction loader = (LuanFunction)a[0];
			a[0] = modName;
			mod = Luan.first(luan.call(loader,"<require \""+modName+"\">",a));
			if( mod != null ) {
				loaded.put(modName,mod);
			} else {
				mod = loaded.get(modName);
				if( mod==null ) {
					mod = true;
					loaded.put(modName,mod);
				}
			}
		}
		return mod;
	}

	public static Object[] search(LuanState luan,String modName) throws LuanException {
		List<Object> list = null;
		LuanTable searchers = (LuanTable)luan.get("Package.searchers");
		if( searchers == null ) {
			list = Collections.<Object>singletonList(javaSearcher);
		} else {
			list = searchers.asList();
		}
		for( Object s : list ) {
			LuanFunction searcher = (LuanFunction)s;
			Object[] a = Luan.array(luan.call(searcher,"<searcher>",new Object[]{modName}));
			if( a.length >= 1 && a[0] instanceof LuanFunction )
				return a;
		}
		return null;
	}

	public static final LuanFunction preloadSearcher = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			return luan.preload().get(modName);
		}
	};

	public static String search_path(String name,String path) {
		for( String s : path.split(";") ) {
			String file = s.replaceAll("\\?",name);
			if( Utils.exists(file) )
				return file;
		}
		return null;
	}

	public static final LuanFunction fileLoader = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) throws LuanException {
			String fileName = (String)args[1];
			LuanFunction fn = BasicLuan.load_file(luan,fileName);
			return fn.call(luan,args);
		}
	};

	public static final LuanFunction fileSearcher = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			String path = (String)luan.get("Package.path");
			if( path==null )
				return LuanFunction.NOTHING;
			String file = search_path(modName,path);
			return file==null ? LuanFunction.NOTHING : new Object[]{fileLoader,file};
		}
	};


	public static final LuanFunction javaLoader = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) throws LuanException {
			try {
				String objName = (String)args[1];
				LuanFunction fn = load_lib(objName);
				return fn.call(luan,args);
			} catch(ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch(NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	};

	public static final LuanFunction javaSearcher = new LuanFunction() {
		@Override public Object[] call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			modName = modName.replace('/','.');
			String path = (String)luan.get("Package.jpath");
			if( path==null )
				path = jpath;
			for( String s : path.split(";") ) {
				String objName = s.replaceAll("\\?",modName);
				try {
					load_lib(objName);  // throws exception if not found
					return new Object[]{javaLoader,objName};
				} catch(ClassNotFoundException e) {
				} catch(NoSuchFieldException e) {
				} catch(IllegalAccessException e) {
				}
			}
			return LuanFunction.NOTHING;
		}
	};


	public static LuanFunction load_lib(String path)
		throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
	{
		int i = path.lastIndexOf('.');
		String clsPath = path.substring(0,i);
		String fld = path.substring(i+1);
		Class cls = Class.forName(clsPath);
		return (LuanFunction)cls.getField(fld).get(null);
	}

}
