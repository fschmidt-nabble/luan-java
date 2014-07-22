package luan;

import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Iterator;


class LuanPropertyTable extends LuanTableImpl {

	@Override public LuanTableImpl shallowClone() {
		return new LuanPropertyTable();
	}

	private Object fixValue(Object obj) {
		return obj instanceof LuanProperty ? ((LuanProperty)obj).get() : obj;
	}

	@Override String repr(Set<LuanTableImpl> set,Object obj) {
		return super.repr(set,fixValue(obj));
	}

	@Override public Object get(Object key) {
		return fixValue(super.get(key));
	}

	@Override public void put(Object key,Object val) {
		Object v = super.get(key);
		if( v instanceof LuanProperty ) {
			LuanProperty lp = (LuanProperty)v;
			if( lp.set(val) )
				return;
		}
		super.put(key,val);
	}

	@Override public Iterator<Map.Entry<Object,Object>> iterator() {
		final Iterator<Map.Entry<Object,Object>> i = super.iterator();
		return new Iterator<Map.Entry<Object,Object>>() {
			public boolean hasNext() {
				return i.hasNext();
			}
			public Map.Entry<Object,Object> next() {
				Map.Entry<Object,Object> entry = i.next();
				Object v = entry.getValue();
				if( v instanceof LuanProperty ) {
					LuanProperty lp = (LuanProperty)v;
					return new AbstractMap.SimpleEntry<Object,Object>(entry.getKey(),lp.get());
				}
				return entry;
			}
			public void remove() {
				i.remove();
			}
		};
	}

}
