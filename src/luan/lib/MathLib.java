package luan.lib;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanLoader;
import luan.LuanJavaFunction;


public final class MathLib {

	public static final String NAME = "Math";

	public static final LuanLoader LOADER = new LuanLoader() {
		@Override protected void load(LuanState luan) {
			LuanTable module = new LuanTable();
			try {
				add( module, "floor", Double.TYPE );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			luan.loaded().put(NAME,module);
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(MathLib.class.getMethod(method,parameterTypes),null) );
	}

	public static double floor(double x) {
		return Math.floor(x);
	}

}
