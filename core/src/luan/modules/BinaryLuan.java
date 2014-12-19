package luan.modules;

import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanMethod;


public final class BinaryLuan {

	public static Object __index(LuanState luan,final byte[] binary,Object key) throws LuanException {
		LuanTable mod = (LuanTable)PackageLuan.loaded(luan).get("luan:Binary");
		if( mod!=null ) {
			Object obj = mod.get(key);
			if( obj instanceof LuanFunction ) {
				final LuanFunction fn = (LuanFunction)obj;
				return new LuanFunction() {
					@Override public Object call(LuanState luan,Object[] args) throws LuanException {
						Object[] a = new Object[args.length+1];
						a[0] = binary;
						System.arraycopy(args,0,a,1,args.length);
						return fn.call(luan,a);
					}
				};
			}
		}
		return null;
	}

	static int start(byte[] binary,int i) {
		int len = binary.length;
		return i==0 ? 0 : i > 0 ? Math.min(i-1,len) : Math.max(len+i,0);
	}

	static int start(byte[] binary,Integer i,int dflt) {
		return i==null ? dflt : start(binary,i);
	}

	static int end(byte[] binary,int i) {
		int len = binary.length;
		return i==0 ? 0 : i > 0 ? Math.min(i,len) : Math.max(len+i+1,0);
	}

	static int end(byte[] binary,Integer i,int dflt) {
		return i==null ? dflt : end(binary,i);
	}

	@LuanMethod public static Byte[] byte_(LuanState luan,byte[] binary,Integer i,Integer j) throws LuanException {
		Utils.checkNotNull(luan,binary);
		int start = start(binary,i,1);
		int end = end(binary,j,start+1);
		Byte[] bytes = new Byte[end-start];
		for( int k=0; k<bytes.length; k++ ) {
			bytes[k] = binary[start+k];
		}
		return bytes;
	}

	@LuanMethod public static byte[] binary(byte... bytes) {
		return bytes;
	}

}
