package luan;

public interface MetatableGetter extends DeepCloneable<MetatableGetter> {
	public LuanTable getMetatable(Object obj);
}
