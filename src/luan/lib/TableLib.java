package luan.lib;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanElement;
import luan.LuanException;


public final class TableLib {

	public static final String NAME = "table";

	public static final LuanFunction LOADER = new LuanFunction() {
		public Object[] call(LuanState luan,Object[] args) throws LuanException {
			LuanTable module = new LuanTable();
			LuanTable global = luan.global;
			try {
				add( module, "concat", LuanState.class, LuanTable.class, String.class, Integer.class, Integer.class );
				add( module, "insert", LuanState.class, LuanTable.class, Integer.TYPE, Object.class );
				add( module, "pack", new Object[0].getClass() );
				add( module, "remove", LuanState.class, LuanTable.class, Integer.TYPE );
				add( module, "sort", LuanState.class, LuanTable.class, LuanFunction.class );
				add( module, "sub_list", LuanTable.class, Integer.TYPE, Integer.TYPE );
				add( module, "unpack", LuanTable.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return new Object[]{module};
		}
	};

	private static void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(TableLib.class.getMethod(method,parameterTypes),null) );
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
				throw new LuanException( luan, LuanElement.JAVA, "invalid value ("+Luan.type(val)+") at index "+k+" in table for 'concat'" );
			buf.append(val);
		}
		return buf.toString();
	}

	public static void insert(LuanState luan,LuanTable list,int pos,Object value) throws LuanException {
		try {
			list.insert(pos,value);
		} catch(IndexOutOfBoundsException e) {
			throw new LuanException( luan, LuanElement.JAVA, e);
		}
	}

	public static Object remove(LuanState luan,LuanTable list,int pos) throws LuanException {
		try {
			return list.remove(pos);
		} catch(IndexOutOfBoundsException e) {
			throw new LuanException( luan, LuanElement.JAVA, e);
		}
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
						return luan.isLessThan(LuanElement.JAVA,o1,o2);
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					}
				}
			};
		} else {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return Luan.toBoolean(Luan.first(luan.call(comp,LuanElement.JAVA,"comp-arg",o1,o2)));
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

	public static LuanTable pack(Object[] args) {
		return new LuanTable(new ArrayList<Object>(Arrays.asList(args)));
	}

	public static Object[] unpack(LuanTable list) {
		return list.listToArray();
	}

	public static LuanTable sub_list(LuanTable list,int from,int to) {
		return list.subList(from,to);
	}

}
