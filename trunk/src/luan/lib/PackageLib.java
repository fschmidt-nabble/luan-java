package luan.lib;

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


public final class PackageLib {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			LuanTable global = luan.global();
			module.put("loaded",luan.loaded());
			module.put("preload",luan.preload());
			module.put("path","?.luan;java:luan/modules/?.luan");
			try {
				add( global, "require", LuanState.class, String.class );
				add( module, "get_loader", String.class );
				add( module, "search_path", String.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			LuanTable searchers = luan.searchers();
			searchers.add(preloadSearcher);
			searchers.add(fileSearcher);
			module.put("searchers",searchers);
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLib.class.getMethod(method,parameterTypes),null) );
	}

	public static Object require(LuanState luan,String modName) throws LuanException {
		LuanTable loaded = luan.loaded();
		Object mod = loaded.get(modName);
		if( mod == null ) {
			List<Object> list = null;
			String searchFor = modName;
			LuanTable searchers = (LuanTable)luan.get("Package.searchers");
			if( searchers == null ) {
				list = Collections.<Object>singletonList(preloadSearcher);
			} else {
				list = searchers.asList();
			}
			for( Object s : list ) {
				LuanFunction searcher = (LuanFunction)s;
				Object[] a = Luan.array(luan.call(searcher,"<searcher>",new Object[]{searchFor}));
				if( a.length >= 1 && a[0] instanceof LuanFunction ) {
					LuanFunction loader = (LuanFunction)a[0];
					a[0] = modName;
					mod = Luan.first(luan.call(loader,"<require \""+modName+"\">",a));
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
				throw luan.exception( "module '"+modName+"' not found" );
		}
		return mod;
	}

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
			LuanFunction fn = BasicLib.load_file(luan,fileName);
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

	public static final LuanFunction preloadSearcher = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			String modName = (String)args[0];
			return luan.preload().get(modName);
		}
	};



	public static LuanFunction get_loader(String path)
		throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
	{
		int i = path.lastIndexOf('.');
		String clsPath = path.substring(0,i);
		String fld = path.substring(i+1);
		Class cls = Class.forName(clsPath);
		return (LuanFunction)cls.getField(fld).get(null);
	}

}
