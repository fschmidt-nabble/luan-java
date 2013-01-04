package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanJavaFunction;


public final class MathLib {

	public static void register(LuanState luan) {
		LuanTable module = new LuanTable();
		LuanTable global = luan.global();
		global.put("math",module);
		try {
			add( module, "floor", Double.TYPE );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(MathLib.class.getMethod(method,parameterTypes),null) );
	}

	public static double floor(double x) {
		return Math.floor(x);
	}

}
