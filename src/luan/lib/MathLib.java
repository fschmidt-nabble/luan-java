package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;


public final class MathLib {

	public static final String NAME = "math";

	public static final LuanFunction LOADER = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			LuanTable global = luan.global;
			global.put(NAME,module);
			try {
				add( module, "floor", Double.TYPE );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return LuanFunction.EMPTY_RTN;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(MathLib.class.getMethod(method,parameterTypes),null) );
	}

	public static double floor(double x) {
		return Math.floor(x);
	}

}
