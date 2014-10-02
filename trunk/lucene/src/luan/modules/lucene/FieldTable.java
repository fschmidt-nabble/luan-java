package luan.modules.lucene;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import luan.AbstractLuanTable;


class FieldTable extends AbstractLuanTable {
	final Map<String,String> map = new ConcurrentHashMap<String,String>();
	final Map<String,String> reverseMap = new ConcurrentHashMap<String,String>();

	FieldTable() {
		put("type","type index");
		put("id","id index");
	}

	@Override public void put(Object key,Object value) {
		if( !(key instanceof String) )
			throw new UnsupportedOperationException("key must be string");
		String name = (String)key;
		if( value==null ) {  // delete
			reverseMap.remove(map.remove(name));
			return;
		}
		if( !(value instanceof String) )
			throw new UnsupportedOperationException("value must be string");
		String field = (String)value;
		String oldField = map.put(name,field);
		if( oldField != null )
			reverseMap.remove(oldField);
		String oldName = reverseMap.put(field,name);
		if( oldName != null ) {
			reverseMap.put(field,oldName);
			map.remove(name);
			throw new IllegalArgumentException("field '"+oldName+"' is already assigned to '"+field+"'");
		}
	}

	@Override public final Object get(Object key) {
		return map.get(key);
	}

	@Override public final Iterator<Map.Entry<Object,Object>> iterator() {
		return new HashMap<Object,Object>(map).entrySet().iterator();
	}

	@Override protected String type() {
		return "lucene-field-table";
	}
}
