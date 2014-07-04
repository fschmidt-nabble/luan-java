package luan.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import luan.Luan;
import luan.LuanState;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanFunction;


public final class PickleClient {

	private final PickleCon con;
	private final LuanFunction _reversed_pickle;

	PickleClient(LuanState luan,DataInputStream in,DataOutputStream out) {
		this(new PickleCon(luan,in,out));
	}

	PickleClient(PickleCon con) {
		this.con = con;
		try {
			this._reversed_pickle = new LuanJavaFunction(
				PickleClient.class.getMethod( "_reversed_pickle" ), this
			);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public Object _reversed_pickle() throws LuanException, IOException {
		new PickleServer(con).run();
		return con.read();
	}

	public Object call(Object... args) throws LuanException, IOException {
		con.write(args);
		Object[] result;
		con.env.put("_reversed_pickle",_reversed_pickle);
		try {
			result = Luan.array(con.read());
		} finally {
			con.env.put("_reversed_pickle",null);
		}
		boolean ok = (boolean)result[0];
		if( ok ) {
			Object[] rtn = new Object[result.length-1];
			System.arraycopy(result,1,rtn,0,rtn.length);
			return rtn;
		} else {
			String msg = (String)result[1];
			String src = (String)result[2];
			throw con.luan.exception(
				msg + "\n"
				+ "in:\n"
				+ "------------------\n"
				+ formatCode(src) + "\n"
				+ "------------------\n"
			);
		}
	}

	LuanTable table() {
		LuanTable tbl = new LuanTable();
		try {
			tbl.put( "pickle", new LuanJavaFunction(
				PickleCon.class.getMethod( "pickle", Object.class ), con
			) );
			tbl.put( "call", new LuanJavaFunction(
				PickleClient.class.getMethod( "call", new Object[0].getClass() ), this
			) );
			tbl.put( "close", new LuanJavaFunction(
				PickleCon.class.getMethod( "close" ), con
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}


	public static String formatCode(String s) {
		StringBuilder buf = new StringBuilder();
		int line = 1;
		int i = 0;
		int i2 = 0;
		while( i2 != -1 ) {
			buf.append( line++ );
			buf.append( '\t' );
			i2 = s.indexOf('\n',i);
			String lineStr = i2 == -1 ? s.substring(i) : s.substring(i,i2+1);
			int j;
			for( j=0; j<lineStr.length() && lineStr.charAt(j)=='\t'; j++ ) {
				buf.append( "    " );
			}
			buf.append( lineStr.substring(j) );
			i = i2 + 1;
		}
		return buf.toString();
	}


}
