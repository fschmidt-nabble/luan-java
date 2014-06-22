package luan;


public interface DeepCloneable<T extends DeepCloneable> {
	public T shallowClone();
	public void deepenClone(T clone,DeepCloner cloner);
}
