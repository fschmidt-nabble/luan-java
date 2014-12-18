package luan.modules;

import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanMethod;


public final class BinaryLuan {

	@LuanMethod public static byte[] pack(byte... bytes) {
		return bytes;
	}

	@LuanMethod public static Byte[] unpack(byte[] binary) {
		Byte[] bytes = new Byte[binary.length];
		for( int i=0; i<binary.length; i++ ) {
			bytes[i] = binary[i];
		}
		return bytes;
	}

}
