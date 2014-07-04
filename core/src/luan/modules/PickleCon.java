package luan.modules;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanTable;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class PickleCon {
	final LuanState luan;
	private final DataInputStream in;
	private final LuanFunction _read_binary;
	private final DataOutputStream out;
	private final List<byte[]> binaries = new ArrayList<byte[]>();
	String src;
	final LuanTable env = new LuanTable();

	PickleCon(LuanState luan,DataInputStream in,DataOutputStream out) {
		this.in = in;
		this.luan = luan;
		try {
			this._read_binary = new LuanJavaFunction(
				PickleCon.class.getMethod( "_read_binary", Integer.TYPE ), this
			);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		this.out = out;
	}

	public byte[] _read_binary(int size) throws IOException, LuanException {
		byte[] a = new byte[size];
		int i = 0;
		while( i < size ) {
			int n = in.read(a,i,size-i);
			if( n == -1 )
				throw luan.exception( "end of stream" );
			i += n;
		}
		return a;
	}

	public Object read() throws IOException, LuanException {
		env.put("_read_binary",_read_binary);
		try {
			src = in.readUTF();
			LuanFunction fn = BasicLuan.load(luan,src,"pickle-reader",env,false);
			return luan.call(fn);
		} finally {
			env.put("_binaries",null);
			env.put("_read_binary",null);
		}
	}

	public String pickle(Object obj) throws LuanException {
		if( obj == null )
			return "nil";
		if( obj instanceof Boolean )
			return Luan.toString((Boolean)obj);
		if( obj instanceof Number )
			return Luan.toString((Number)obj);
		if( obj instanceof String )
			return "\"" + Luan.stringEncode((String)obj) + "\"";
		if( obj instanceof LuanTable )
			return pickle( (LuanTable)obj, Collections.newSetFromMap(new IdentityHashMap<LuanTable,Boolean>()) );
		if( obj instanceof byte[] ) {
			byte[] a = (byte[])obj;
			binaries.add(a);
			return "_binaries[" + binaries.size() + "]";
		}
		throw luan.exception( "invalid type: " + obj.getClass() );
	}

	private String pickle(Object obj,Set<LuanTable> set) throws LuanException {
		return obj instanceof LuanTable ? pickle((LuanTable)obj,set) : pickle(obj);
	}

	private String pickle(LuanTable tbl,Set<LuanTable> set) throws LuanException {
		if( !set.add(tbl) ) {
			throw luan.exception( "circular reference in table" );
		}
		StringBuilder sb = new StringBuilder();
		sb.append( "{" );
		for( Map.Entry<Object,Object> entry : tbl ) {
			sb.append( "[" );
			sb.append( pickle(entry.getKey(),set) );
			sb.append( "]=" );
			sb.append( pickle(entry.getValue(),set) );
			sb.append( ", " );
		}
		sb.append( "}" );
		return sb.toString();
	}

	public void write(Object... args) throws LuanException, IOException {
		StringBuilder sb = new StringBuilder();
		if( !binaries.isEmpty() ) {
			sb.append( "_binaries = {}\n" );
			for( byte[] a : binaries ) {
				sb.append( "_binaries[#_binaries+1] = _read_binary(" + a.length + ")\n" );
			}
		}
		for( Object obj : args ) {
			sb.append( luan.toString(obj) );
		}
		out.writeUTF( sb.toString() );
		for( byte[] a : binaries ) {
			out.write(a);
		}
		out.flush();
		binaries.clear();
	}

	public void close() throws IOException {
		in.close();
		out.close();
	}
}
