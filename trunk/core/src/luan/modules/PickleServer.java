package luan.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class PickleServer {

	private final PickleCon con;
	private boolean isRunning;

	PickleServer(LuanState luan,DataInputStream in,DataOutputStream out) {
		this(new PickleCon(luan,in,out));
	}

	PickleServer(PickleCon con) {
		this.con = con;
	}

	void next() throws IOException {
		try {
			List<String> list = new ArrayList<String>();
			try {
				Object[] result = Luan.array(con.read());
				list.add( "return true" );
				for( Object obj : result ) {
					list.add( ", " );
					list.add( con.pickle(obj) );
				}
			} catch(LuanException e) {
//				System.out.println(e);
//e.printStackTrace();
				list.add( "return false, " );
				list.add( con.pickle(e.getMessage()) );
				list.add( ", " );
				list.add( con.pickle(con.src) );
			}
			list.add( "\n" );
			con.write( list.toArray() );
		} catch(LuanException e2) {
			throw new RuntimeException(e2);
		}
	}

	public void run() {
		LuanTable io = (LuanTable)PackageLuan.loaded(con.luan).get("luan:Io");
		LuanTable env = con.env;
		Object old_reverse_pickle = io.get("reverse_pickle");
		Object old_unreverse_pickle = env.get("_unreverse_pickle");
		try {
			try {
				io.put("reverse_pickle", new LuanJavaFunction(
					PickleServer.class.getMethod( "reverse_pickle", LuanFunction.class ), this
				) );
				env.put("_unreverse_pickle", new LuanJavaFunction(
					PickleServer.class.getMethod( "_unreverse_pickle" ), this
				) );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			isRunning = true;
			try {
				while( isRunning ) {
					next();
				}
			} catch(EOFException e) {
				// done
			} catch(IOException e) {
				e.printStackTrace();
			}
			if( isRunning ) {
				try {
					con.close();
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			io.put("reverse_pickle",old_reverse_pickle);
			env.put("_unreverse_pickle",old_unreverse_pickle);
		}
	}

	public void reverse_pickle(LuanFunction fn) throws IOException, LuanException {
		try {
			con.write( "return _reversed_pickle()\n" );
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
		PickleClient pc = new PickleClient(con);
		try {
			con.luan.call(fn,new Object[]{pc.table()});
		} finally {
			try {
				pc.call( "_unreverse_pickle()\n" );
			} catch(LuanException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void _unreverse_pickle() {
		isRunning = false;
	}

}
