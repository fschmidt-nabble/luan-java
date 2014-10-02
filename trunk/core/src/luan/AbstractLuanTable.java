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
import java.util.regex.Pattern;


public abstract class AbstractLuanTable implements LuanTable {

	@Override public boolean isEmpty() {
		return isList() && length()==0;
	}

	@Override public boolean isList() {
		return asList().size() == asMap().size();
	}

	@Override public List<Object> asList() {
		return Collections.emptyList();
	}

	@Override public Map<Object,Object> asMap() {
		return Collections.emptyMap();
	}

	protected abstract String type();

	@Override public final String toString() {
		return type() + ": " + Integer.toHexString(hashCode());
	}

	@Override public void put(Object key,Object val) {
		throw new UnsupportedOperationException("can't put into a "+type());
	}

	@Override public void insert(int pos,Object value) {
		throw new UnsupportedOperationException("can't insert into a "+type());
	}

	@Override public void add(Object value) {
		throw new UnsupportedOperationException("can't add to a "+type());
	}

	@Override public Object remove(int pos) {
		throw new UnsupportedOperationException("can't remove from a "+type());
	}

	@Override public void sort(Comparator<Object> cmp) {
	}

	@Override public int length() {
		return 0;
	}

	@Override public LuanTable subList(int from,int to) {
		throw new UnsupportedOperationException("can't get a sub-list of a "+type());
	}

	@Override public LuanTable getMetatable() {
		return null;
	}

	@Override public void setMetatable(LuanTable metatable) {
		throw new UnsupportedOperationException("can't set a metatable on a "+type());
	}
}
