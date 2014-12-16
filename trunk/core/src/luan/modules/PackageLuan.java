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

	public static final LuanFunction requireFn;
	static {
		try {
			requireFn = new LuanJavaFunction(PackageLuan.class.getMethod("require",LuanState.class,String.class),null);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static LuanTable loaded(LuanState luan) {
		LuanTable tbl = (LuanTable)luan.registry().get("Package.loaded");
		if( tbl == null ) {
			tbl = Luan.newTable();
			luan.registry().put("Package.loaded",tbl);
		}
		return tbl;
	}

	public static Object require(LuanState luan,String modName) throws LuanException {
		Object mod = load(luan,modName);
		if( mod==null )
			throw luan.exception( "module '"+modName+"' not found" );
		return mod;
	}

	public static Object load(LuanState luan,String modName) throws LuanException {
		if( modName.startsWith("java:") )
			return JavaLuan.load(luan,modName.substring(5));
		LuanTable loaded = loaded(luan);
		Object mod = loaded.get(modName);
		if( mod == null ) {
			String src = read(luan,modName+".luan");
			if( src == null )
				return null;
			LuanFunction loader = BasicLuan.load(luan,src,modName,null,false);
			mod = Luan.first(luan.call(loader,"<require \""+modName+"\">",new Object[]{modName}));
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

	static String read(LuanState luan,String uri) throws LuanException {
		LuanTable t = IoLuan.Uri(luan,uri);
		if( t == null )
			return null;
		LuanFunction reader = (LuanFunction)t.get("read_text");
		return (String)Luan.first(luan.call(reader));
	}

}
