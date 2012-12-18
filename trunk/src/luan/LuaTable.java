package luan;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;


public class LuaTable {
	private final Map<Object,Object> map = new HashMap<Object,Object>();
	private LuaTable metatable;

	@Override public String toString() {
		return "table: " + Integer.toHexString(hashCode());
	}

	public Object get(Object key) {
		return map.get(key);
	}

	public Object put(Object key,Object val) {
		if( val == null ) {
			return map.remove(key);
		} else {
			return map.put(key,val);
		}
	}

	public int length() {
		int i = 0;
		while( map.containsKey( new LuaNumber(i) ) ) {
			i++;
		}
		return i;
	}

	public Iterator<Map.Entry<Object,Object>> iterator() {
		return map.entrySet().iterator();
	}

	public LuaTable getMetatable() {
		return metatable;
	}

	public void setMetatable(LuaTable metatable) {
		this.metatable = metatable;
	}
}
