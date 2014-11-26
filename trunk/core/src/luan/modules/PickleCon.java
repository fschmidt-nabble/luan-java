package luan.modules;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;
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
	private final InputStream in;
	private final LuanFunction _read_binary;
	private final OutputStream out;
	private final List<byte[]> binaries = new ArrayList<byte[]>();
	String src;
	final LuanTable env = Luan.newTable();

	PickleCon(LuanState luan,InputStream in,OutputStream out) {
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

	public byte[] _read_binary(int size) throws IOException/*, LuanException*/ {
		byte[] a = new byte[size];
		int i = 0;
		while( i < size ) {
			int n = in.read(a,i,size-i);
			if( n == -1 )
//				throw luan.exception( "end of stream" );
				throw new EOFException();
			i += n;
		}
		return a;
	}

	public Object read() throws IOException, LuanException {
		env.put("_read_binary",_read_binary);
		try {
			src = readString();
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
		writeString( sb.toString() );
//System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa");
//System.out.println(sb);
//System.out.println("zzzzzzzzzzzzzzzzzzzzzzzz");
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

	String readString() throws IOException {
		int len = readInt();
		byte[] a = _read_binary(len);
		return new String(a,StandardCharsets.UTF_8);
	}

	int readInt() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	void writeString(String s) throws IOException {
		byte[] a = s.getBytes(StandardCharsets.UTF_8);
		writeInt(a.length);
		out.write(a);
	}

	void writeInt(int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>>  0) & 0xFF);
	}
}
