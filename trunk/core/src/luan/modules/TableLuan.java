package luan.modules;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.LuanRuntimeException;


public final class TableLuan {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = Luan.newTable();
			try {
				add( module, "concat", LuanState.class, LuanTable.class, String.class, Integer.class, Integer.class );
				add( module, "insert", LuanTable.class, Integer.TYPE, Object.class );
				add( module, "pack", new Object[0].getClass() );
				add( module, "remove", LuanTable.class, Integer.TYPE );
				add( module, "sort", LuanState.class, LuanTable.class, LuanFunction.class );
				add( module, "sub_list", LuanTable.class, Integer.TYPE, Integer.TYPE );
				add( module, "unpack", LuanTable.class, Integer.class, Integer.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(TableLuan.class.getMethod(method,parameterTypes),null) );
	}

	public static String concat(LuanState luan,LuanTable list,String sep,Integer i,Integer j) throws LuanException {
		int first = i==null ? 1 : i;
		int last = j==null ? list.length() : j;
		StringBuilder buf = new StringBuilder();
		for( int k=first; k<=last; k++ ) {
			Object val = list.get(k);
			if( val==null )
				break;
			if( sep!=null && k > first )
				buf.append(sep);
			String s = Luan.asString(val);
			if( s==null )
				throw luan.exception( "invalid value ("+Luan.type(val)+") at index "+k+" in table for 'concat'" );
			buf.append(val);
		}
		return buf.toString();
	}

	public static void insert(LuanTable list,int pos,Object value){
		list.insert(pos,value);
	}

	public static Object remove(LuanTable list,int pos) {
		return list.remove(pos);
	}

	private static interface LessThan {
		public boolean isLessThan(Object o1,Object o2);
	}

	public static void sort(final LuanState luan,LuanTable list,final LuanFunction comp) throws LuanException {
		final LessThan lt;
		if( comp==null ) {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return luan.isLessThan(o1,o2);
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					}
				}
			};
		} else {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return Luan.toBoolean(Luan.first(luan.call(comp,"comp-arg",new Object[]{o1,o2})));
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					}
				}
			};
		}
		try {
			list.sort( new Comparator<Object>() {
				public int compare(Object o1,Object o2) {
					return lt.isLessThan(o1,o2) ? -1 : lt.isLessThan(o2,o1) ? 1 : 0;
				}
			} );
		} catch(LuanRuntimeException e) {
			throw (LuanException)e.getCause();
		}
	}

	public static LuanTable pack(Object... args) {
		LuanTable tbl = Luan.newTable();
		boolean hasNull = false;
		for( int i=0; i<args.length; i++ ) {
			Object v = args[i];
			if( v==null ) {
				hasNull = true;
			} else if( !hasNull ) {
				tbl.add(v);
			} else {
				tbl.put(i+1,v);
			}
		}
		tbl.put( "n", args.length );
		return tbl;
	}

	public static Object[] unpack(LuanTable tbl,Integer iFrom,Integer iTo) {
		int from = iFrom!=null ? iFrom : 1;
		int to = iTo!=null ? iTo : tbl.length();
		List<Object> list = new ArrayList<Object>();
		for( int i=from; i<=to; i++ ) {
			list.add( tbl.get(i) );
		}
		return list.toArray();
	}

	public static LuanTable sub_list(LuanTable list,int from,int to) {
		return list.subList(from,to);
	}

}
