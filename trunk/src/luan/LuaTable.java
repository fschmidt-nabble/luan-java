package luan;

import java.util.Map;
import java.util.HashMap;


public class LuaTable {
	public final Map<Object,Object> map = new HashMap<Object,Object>();

	@Override public String toString() {
		return "table: " + Integer.toHexString(hashCode());
	}

	public Object get(Object key) {
		return map.get(key);
	}

	public void set(Object key,Object val) {
		if( val == null ) {
			map.remove(key);
		} else {
			map.put(key,val);
		}
	}

	public int length() {
		int i = 0;
		while( map.containsKey( new LuaNumber(i) ) ) {
			i++;
		}
		return i;
	}

}
