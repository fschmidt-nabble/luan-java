package luan;

import java.util.Map;
import java.util.List;
import java.util.Comparator;


public interface LuanTable extends Iterable<Map.Entry<Object,Object>> {
	public List<Object> asList();
	public Map<Object,Object> asMap();
	public Object get(Object key);
	public void put(Object key,Object val);
	public void insert(int pos,Object value);
	public void add(Object value);
	public Object remove(int pos);
	public void sort(Comparator<Object> cmp);
	public int length();
	public LuanTable subList(int from,int to);
	public LuanTable getMetatable();
	public void setMetatable(LuanTable metatable);
}
