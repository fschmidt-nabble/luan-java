package luan;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class LuaTable {
	private Map<Object,Object> map = null;
	private List<Object> list = null;
	private LuaTable metatable = null;

	public LuaTable() {}

	public LuaTable(List<Object> list) {
		this.list = list;
	}

	@Override public String toString() {
		return "table: " + Integer.toHexString(hashCode());
	}

	public Object get(Object key) {
		if( list != null && key instanceof LuaNumber ) {
			LuaNumber ln = (LuaNumber)key;
			int i = (int)ln.n;
			if( i == ln.n ) {
				i--;
				if( i>=0 && i<list.size() )
					return list.get(i);
			}
		}
		if( map==null )
			return null;
		return map.get(key);
	}

	public Object put(Object key,Object val) {
		if( key instanceof LuaNumber ) {
			LuaNumber ln = (LuaNumber)key;
			int i = (int)ln.n;
			if( i == ln.n ) {
				i--;
				if( list == null && i == 0 )
					list = new ArrayList<Object>();
				if( list != null ) {
					if( i == list.size() ) {
						if( val != null ) {
							list.add(val);
							if( map != null ) {
								while(true) {
									Object v = map.remove(LuaNumber.of(list.size()+1));
									if( v == null )
										break;
									list.add(v);
								}
							}
						}
						return null;
					} else if( i>=0 && i<list.size() ) {
						Object old = list.get(i);
						list.set(i,val);
						if( val == null && i == list.size()-1 ) {
							while( i>=0 && list.get(i)==null ) {
								list.remove(i--);
							}
						}
						return old;
					}
				}
			}
		}
		if( map==null ) {
			map = new HashMap<Object,Object>();
		}
		if( val == null ) {
			return map.remove(key);
		} else {
			return map.put(key,val);
		}
	}

	public void insert(int pos,Object value) {
		if( list == null )
			list = new ArrayList<Object>();
		list.add(pos-1,value);
	}

	public Object remove(int pos) {
		if( list == null )
			list = new ArrayList<Object>();
		return list.remove(pos-1);
	}

	public void sort(Comparator<Object> cmp) {
		if( list != null )
			Collections.sort(list,cmp);
	}

	public int length() {
		return list==null ? 0 : list.size();
	}

	public Iterator<Map.Entry<Object,Object>> iterator() {
		if( list == null ) {
			if( map == null )
				return Collections.<Map.Entry<Object,Object>>emptyList().iterator();
			return map.entrySet().iterator();
		}
		if( map == null )
			return listIterator();
		return new Iterator<Map.Entry<Object,Object>>() {
			Iterator<Map.Entry<Object,Object>> iter = listIterator();
			boolean isList = true;
			public boolean hasNext() {
				boolean b = iter.hasNext();
				if( !b && isList ) {
					iter = map.entrySet().iterator();
					isList = false;
					b = iter.hasNext();
				}
				return b;
			}
			public Map.Entry<Object,Object> next() {
				return iter.next();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Map.Entry<Object,Object>> listIterator() {
		if( list == null )
			return Collections.<Map.Entry<Object,Object>>emptyList().iterator();
		final ListIterator iter = list.listIterator();
		return new Iterator<Map.Entry<Object,Object>>() {
			public boolean hasNext() {
				return iter.hasNext();
			}
			public Map.Entry<Object,Object> next() {
				LuaNumber key = LuaNumber.of(iter.nextIndex()+1);
				return new MapEntry(key,iter.next());
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Object[] listToArray() {
		return list==null ? new Object[0] : list.toArray();
	}

	public LuaTable subList(int from,int to) {
		if( list == null )
			list = new ArrayList<Object>();
		return new LuaTable(new ArrayList<Object>(list.subList(from-1,to-1)));
	}

	public LuaTable getMetatable() {
		return metatable;
	}

	public void setMetatable(LuaTable metatable) {
		this.metatable = metatable;
	}

	private static final class MapEntry implements Map.Entry<Object,Object> {
		private final Object key;
		private final Object value;

		MapEntry(Object key,Object value) {
			this.key = key;
			this.value = value;
		}

		@Override public Object getKey() {
			return key;
		}

		@Override public Object getValue() {
			return value;
		}

		@Override public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
	}
}
