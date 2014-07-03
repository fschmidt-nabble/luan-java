package luan.impl;

import luan.MetatableGetter;
import luan.LuanTable;
import luan.DeepCloneable;
import luan.DeepCloner;


final class MtGetterLink implements DeepCloneable<MtGetterLink> {
	private MetatableGetter mg;
	private MtGetterLink next;

	private MtGetterLink() {}

	MtGetterLink(MetatableGetter mg,MtGetterLink next) {
		this.mg = mg;
		this.next = next;
	}

	LuanTable getMetatable(Object obj,MetatableGetter beforeThis) {
		if( beforeThis != null ) {
			if( beforeThis==mg )
				beforeThis = null;
		} else {
			LuanTable mt = mg.getMetatable(obj);
			if( mt != null )
				return mt;
		}
		return next==null ? null : next.getMetatable(obj,beforeThis);
	}

	boolean contains(MetatableGetter mg) {
		return this.mg==mg || next!=null && next.contains(mg);
	}

	@Override public MtGetterLink shallowClone() {
		return new MtGetterLink();
	}

	@Override public void deepenClone(MtGetterLink clone,DeepCloner cloner) {
		clone.mg = cloner.deepClone(mg);
		clone.next = cloner.deepClone(next);
	}
}
