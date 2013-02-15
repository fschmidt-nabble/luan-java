package luan;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.IdentityHashMap;


public class LuanTable implements DeepCloneable<LuanTable> {
	private Map<Object,Object> map = null;
	private List<Object> list = null;
	private LuanTable metatable = null;

	public LuanTable() {}

	public LuanTable(List<Object> list) {
		this.list = list;
	}

	public LuanTable(Map<Object,Object> map) {
		this.map = map;
	}

	public LuanTable(Set<Object> set) {
		map = new HashMap<Object,Object>();
		for( Object obj : set ) {
			map.put(obj,Boolean.TRUE);
		}
	}

	@Override public LuanTable shallowClone() {
		return new LuanTable();
	}

	@Override public void deepenClone(LuanTable clone,DeepCloner cloner) {
		if( map != null ) {
			clone.map = new HashMap<Object,Object>();
			for( Map.Entry<Object,Object> entry : map.entrySet() ) {
				clone.map.put( cloner.get(entry.getKey()), cloner.get(entry.getValue()) );
			}
		}
		if( list != null ) {
			clone.list = new ArrayList<Object>();
			for( Object obj : list ) {
				clone.list.add( cloner.get(obj) );
			}
		}
		if( metatable != null )
			clone.metatable = cloner.deepClone(metatable);
	}

	public boolean isList() {
		return map==null || map.isEmpty();
	}

	public List<Object> asList() {
		return list!=null ? list : Collections.emptyList();
	}

	public Map<Object,Object> asMap() {
		if( list == null || list.isEmpty() )
			return map!=null ? map : Collections.emptyMap();
		Map<Object,Object> rtn = map!=null ? new HashMap<Object,Object>(map) : new HashMap<Object,Object>();
		for( ListIterator iter = list.listIterator(); iter.hasNext(); ) {
			int i = iter.nextIndex();
			rtn.put(i+1,iter.next());
		}
		return rtn;
	}

	public boolean isSet() {
		if( list != null ) {
			for( Object obj : list ) {
				if( obj!=null && !obj.equals(Boolean.TRUE) )
					return false;
			}
		}
		if( map != null ) {
			for( Object obj : map.values() ) {
				if( !obj.equals(Boolean.TRUE) )
					return false;
			}
		}
		return true;
	}

	public Set<Object> asSet() {
		if( list == null || list.isEmpty() )
			return map!=null ? map.keySet() : Collections.emptySet();
		Set<Object> rtn = map!=null ? new HashSet<Object>(map.keySet()) : new HashSet<Object>();
		for( int i=1; i<=list.size(); i++ ) {
			rtn.add(i);
		}
		return rtn;
	}

	@Override public String toString() {
		return toString( Collections.newSetFromMap(new IdentityHashMap<LuanTable,Boolean>()) );
	}

	private String toString(Set<LuanTable> set) {
//		return "table: " + Integer.toHexString(hashCode());
		if( !set.add(this) ) {
			return "...";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean isFirst = true;
		if( list != null ) {
			boolean gotNull = false;
			for( int i=0; i<list.size(); i++ ) {
				Object obj = list.get(i);
				if( obj==null ) {
					gotNull = true;
				} else {
					if( isFirst ) {
						isFirst = false;
					} else {
						sb.append(", ");
					}
					if( gotNull )
						sb.append(i+1).append('=');
					sb.append(toString(set,obj));
				}
			}
		}
		if( map != null ) {
			for( Map.Entry<Object,Object> entry : map.entrySet() ) {
				if( isFirst ) {
					isFirst = false;
				} else {
					sb.append(", ");
				}
				sb.append(toString(set,entry.getKey())).append('=').append(toString(set,entry.getValue()));
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private static String toString(Set<LuanTable> set,Object obj) {
		if( obj instanceof LuanTable ) {
			LuanTable t = (LuanTable)obj;
			return t.toString(set);
		} else {
			return Luan.toString(obj);
		}
	}

	public Object get(Object key) {
		if( list != null ) {
			Integer iT = Luan.asInteger(key);
			if( iT != null ) {
				int i = iT - 1;
				if( i>=0 && i<list.size() )
					return list.get(i);
			}
		}
		if( map==null )
			return null;
		return map.get(key);
	}

	public Object put(Object key,Object val) {
		Integer iT = Luan.asInteger(key);
		if( iT != null ) {
			int i = iT - 1;
			if( list != null || i == 0 ) {
				if( i == list().size() ) {
					if( val != null ) {
						list.add(val);
						mapToList();
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
		if( map==null ) {
			map = new HashMap<Object,Object>();
		}
		if( key instanceof Number && !(key instanceof Double) ) {
			Number n = (Number)key;
			key = Double.valueOf(n.doubleValue());
		}
		if( val == null ) {
			return map.remove(key);
		} else {
			return map.put(key,val);
		}
	}

	private void mapToList() {
		if( map != null ) {
			while(true) {
				Object v = map.remove(Double.valueOf(list.size()+1));
				if( v == null )
					break;
				list.add(v);
			}
		}
	}

	private List<Object> list() {
		if( list == null ) {
			list = new ArrayList<Object>();
			mapToList();
		}
		return list;
	}

	public void insert(int pos,Object value) {
		list().add(pos-1,value);
	}

	public Object remove(int pos) {
		return list().remove(pos-1);
	}

	public void sort(Comparator<Object> cmp) {
		Collections.sort(list(),cmp);
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
				Double key = Double.valueOf(iter.nextIndex()+1);
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

	public LuanTable subList(int from,int to) {
		return new LuanTable(new ArrayList<Object>(list().subList(from-1,to-1)));
	}

	public LuanTable getMetatable() {
		return metatable;
	}

	public void setMetatable(LuanTable metatable) {
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
