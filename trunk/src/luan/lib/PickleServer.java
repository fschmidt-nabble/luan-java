package luan.lib;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanState;
import luan.LuanException;


final class PickleServer {

	private final PickleCon con;

	PickleServer(LuanState luan,DataInputStream in,DataOutputStream out) {
		con = new PickleCon(luan,in,out);
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
		try {
			while( true ) {
				next();
			}
		} catch(EOFException e) {
			// done
		} catch(IOException e) {
			e.printStackTrace();
		}
		try {
			con.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

}
