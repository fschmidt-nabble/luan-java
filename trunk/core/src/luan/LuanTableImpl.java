package luan;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.regex.Pattern;


class LuanTableImpl extends AbstractLuanTable implements LuanTable, DeepCloneable<LuanTableImpl>, LuanRepr {
	private Map<Object,Object> map = null;
	private List<Object> list = null;
	private LuanTable metatable = null;

	public LuanTableImpl() {}
/*
	public LuanTableImpl(LuanTableImpl tbl) {
		if( tbl.map != null )
			this.map = new HashMap<Object,Object>(tbl.map);
		if( tbl.list != null )
			this.list = new ArrayList<Object>(tbl.list);
	}
*/
	LuanTableImpl(List<Object> list) {
		this.list = list;
		this.map = new HashMap<Object,Object>();
		map.put("n",list.size());
		for( int i=0; i<list.size(); i++ ) {
			if( list.get(i) == null ) {
				listToMap(i);
				break;
			}
		}
	}

	LuanTableImpl(Map<Object,Object> map) {
		map.remove(null);
		for( Iterator<Object> i=map.values().iterator(); i.hasNext(); ) {
			if( i.next() == null )
				i.remove();
		}
		this.map = map;
	}

	LuanTableImpl(Set<Object> set) {
		map = new HashMap<Object,Object>();
		for( Object obj : set ) {
			if( obj != null )
				map.put(obj,Boolean.TRUE);
		}
	}

	@Override public LuanTableImpl shallowClone() {
		return new LuanTableImpl();
	}

	@Override public void deepenClone(LuanTableImpl clone,DeepCloner cloner) {
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
			clone.metatable = cloner.get(metatable);
	}

	@Override public boolean isList() {
		return map==null || map.isEmpty();
	}

	@Override public List<Object> asList() {
		return list!=null ? list : Collections.emptyList();
	}

	@Override public Map<Object,Object> asMap() {
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

	@Override protected String type() {
		return "table";
	}

	@Override public String repr() {
		return repr( Collections.newSetFromMap(new IdentityHashMap<LuanTableImpl,Boolean>()) );
	}

	private String repr(Set<LuanTableImpl> set) {
		if( !set.add(this) ) {
			return "\"<circular reference>\"";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean isFirst = true;
		if( list != null ) {
			for( int i=0; i<list.size(); i++ ) {
				Object obj = list.get(i);
				if( isFirst ) {
					isFirst = false;
				} else {
					sb.append(", ");
				}
				sb.append(repr(set,obj));
			}
		}
		if( map != null ) {
			for( Map.Entry<Object,Object> entry : map.entrySet() ) {
				if( isFirst ) {
					isFirst = false;
				} else {
					sb.append(", ");
				}
				sb.append(reprKey(set,entry.getKey())).append('=').append(repr(set,entry.getValue()));
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private static final Pattern namePtn = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");

	private String reprKey(Set<LuanTableImpl> set,Object obj) {
		if( obj instanceof String ) {
			String s = (String)obj;
			if( namePtn.matcher(s).matches() )
				return s;
		}
		return "[" + repr(set,obj) + "]";
	}

	String repr(Set<LuanTableImpl> set,Object obj) {
		if( obj instanceof LuanTableImpl ) {
			LuanTableImpl t = (LuanTableImpl)obj;
			return t.repr(set);
		} else {
			String s = Luan.repr(obj);
			if( s == null )
				s = "<couldn't repr: " + Luan.stringEncode(Luan.toString(obj)) + ">";
			return s;
		}
	}

	@Override public Object get(Object key) {
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

	@Override public void put(Object key,Object val) {
		Integer iT = Luan.asInteger(key);
		if( iT != null ) {
			int i = iT - 1;
			if( list != null || i == 0 ) {
				if( i == list().size() ) {
					if( val != null ) {
						list.add(val);
						mapToList();
					}
					return;
				} else if( i>=0 && i<list.size() ) {
					list.set(i,val);
					if( val == null ) {
						listToMap(i);
					}
					return;
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
			map.remove(key);
		} else {
			map.put(key,val);
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

	private void listToMap(int from) {
		if( list != null ) {
			while( list.size() > from ) {
				int i = list.size() - 1;
				Object v = list.remove(i);
				if( v != null ) {
					if( map==null )
						map = new HashMap<Object,Object>();
					map.put(i+1,v);
				}
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

	@Override public void insert(int pos,Object value) {
		if( value==null )
			throw new IllegalArgumentException("can't insert a nil value");
		list().add(pos-1,value);
		mapToList();
	}

	@Override public void add(Object value) {
		if( value==null )
			throw new IllegalArgumentException("can't add a nil value");
		list().add(value);
		mapToList();
	}

	@Override public Object remove(int pos) {
		return list().remove(pos-1);
	}

	@Override public void sort(Comparator<Object> cmp) {
		Collections.sort(list(),cmp);
	}

	@Override public int length() {
		return list==null ? 0 : list.size();
	}

	@Override public Iterator<Map.Entry<Object,Object>> iterator() {
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

	private Iterator<Map.Entry<Object,Object>> listIterator() {
		if( list == null )
			return Collections.<Map.Entry<Object,Object>>emptyList().iterator();
		final ListIterator iter = list.listIterator();
		return new Iterator<Map.Entry<Object,Object>>() {
			public boolean hasNext() {
				return iter.hasNext();
			}
			public Map.Entry<Object,Object> next() {
				Double key = Double.valueOf(iter.nextIndex()+1);
				return new AbstractMap.SimpleEntry<Object,Object>(key,iter.next());
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override public LuanTable subList(int from,int to) {
		LuanTableImpl tbl = shallowClone();
		tbl.list = new ArrayList<Object>(list().subList(from-1,to-1));
		return tbl;
	}

	@Override public LuanTable getMetatable() {
		return metatable;
	}

	@Override public void setMetatable(LuanTable metatable) {
		this.metatable = metatable;
	}
}
