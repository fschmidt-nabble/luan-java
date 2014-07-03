package luan;

public interface MetatableGetter extends DeepCloneable<MetatableGetter> {
	public static final String KEY = "_METATABLE_GETTER";
	public LuanTable getMetatable(Object obj);
}
