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

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = Luan.newTable();
			module.put( "loaded", loaded(luan) );
			try {
				module.put("require",requireFn);
				add( module, "load", LuanState.class, String.class );
				add( module, "load_lib", LuanState.class, String.class );
				add( module, "search", LuanState.class, String.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	public static final LuanFunction requireFn;
	static {
		try {
			requireFn = new LuanJavaFunction(PackageLuan.class.getMethod("require",LuanState.class,String.class),null);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(PackageLuan.class.getMethod(method,parameterTypes),null) );
	}

	public static LuanTable loaded(LuanState luan) {
		return luan.registryTable("Package.loaded");
	}

	private static LuanTable blocked(LuanState luan) {
		return luan.registryTable("Package.blocked");
	}

	private static Object pkg(LuanState luan,String key) {
		LuanTable t = (LuanTable)loaded(luan).get("Package");
		return t==null ? null : t.get(key);
	}

	public static Object require(LuanState luan,String modName) throws LuanException {
		Object mod = load(luan,modName);
		if( mod==null )
			throw luan.exception( "module '"+modName+"' not found" );
		return mod;
	}

	public static Object load(LuanState luan,String modName) throws LuanException {
		LuanTable loaded = loaded(luan);
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

	static LuanFunction loader(LuanState luan,String name,boolean loading) throws LuanException {
		LuanTable t = IoLuan.get(luan,name,loading);
		if( t == null )
			return null;
		LuanFunction loader = (LuanFunction)t.get("loader");
		if( loader == null )
			return null;
		return (LuanFunction)Luan.first(luan.call(loader,new Object[]{name}));
	}

	public static Object[] search(LuanState luan,String modName) throws LuanException {
		LuanFunction fn = loader(luan,modName,true);
		return fn==null ? null : new Object[]{fn,modName};
	}


	public static void block(LuanState luan,String key) {
		blocked(luan).put(key,true);
	}

	public static boolean is_blocked(LuanState luan,String key) {
		return blocked(luan).get(key) != null;
	}

	public static LuanFunction load_lib(LuanState luan,String path)
		throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, LuanException
	{
		int i = path.lastIndexOf('.');
		String clsPath = path.substring(0,i);
		String fld = path.substring(i+1);
		Class cls = Class.forName(clsPath);
		return (LuanFunction)cls.getField(fld).get(null);
	}

}
