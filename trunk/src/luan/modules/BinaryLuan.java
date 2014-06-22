package luan.modules;

import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class BinaryLuan {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = new LuanTable();
			try {
				add( module, "to_string", new byte[0].getClass() );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(BinaryLuan.class.getMethod(method,parameterTypes),null) );
	}

	public static String to_string(byte[] bytes) {
		return new String(bytes);
	}

}
