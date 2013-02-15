package luan;

import java.util.Map;
import java.util.IdentityHashMap;


public final class DeepCloner {
	private final Map<Object,Object> cloned = new IdentityHashMap<Object,Object>();

	public <T extends DeepCloneable<T>> T deepClone(T obj) {
		@SuppressWarnings("unchecked")
		T rtn = (T)cloned.get(obj);
		if( rtn == null ) {
			rtn = obj.shallowClone();
			cloned.put(obj,rtn);
			obj.deepenClone(rtn,this);
		}
		return rtn;
	}

	public <T> T[] deepClone(T[] obj) {
		if( obj.length == 0 )
			return obj;
		@SuppressWarnings("unchecked")
		T[] rtn = (T[])cloned.get(obj);
		if( rtn == null ) {
			rtn = obj.clone();
			cloned.put(obj,rtn);
			for( int i=0; i<rtn.length; i++ ) {
				@SuppressWarnings("unchecked")
				T t = (T)get(rtn[i]);
				rtn[i] = t;
			}
		}
		return rtn;
	}

	public Object get(Object obj) {
		if( !(obj instanceof DeepCloneable) )
			return obj;
		@SuppressWarnings("unchecked")
		DeepCloneable dc = deepClone((DeepCloneable)obj);
		return dc;
	}
}
