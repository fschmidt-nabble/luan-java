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

	public void deepenClone(Object[] a) {
		for( int i=0; i<a.length; i++ ) {
			a[i] = get(a[i]);
		}
	}

	public Object get(Object obj) {
		if( !(obj instanceof DeepCloneable) )
			return obj;
		@SuppressWarnings("unchecked")
		DeepCloneable dc = deepClone((DeepCloneable)obj);
		return dc;
	}
}
