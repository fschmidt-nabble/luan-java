package luan.lib;

import luan.Lua;
import luan.LuaTable;
import luan.LuaJavaFunction;


public class BasicLib {

	public static void register(LuaTable t) {
		add( t, "print", new Object[0].getClass() );
		add( t, "type", Object.class );
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) {
		try {
			t.set( method, new LuaJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void print(Object... args) {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				System.out.print('\t');
			System.out.print( Lua.checkString(args[i]) );
		}
		System.out.println();
	}

	public static String type(Object obj) {
		return Lua.type(obj);
	}

}
