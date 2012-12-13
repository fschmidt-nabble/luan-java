package luan.interp;


final class VarArgs implements Expressions {
	static final VarArgs INSTANCE = new VarArgs();

	private VarArgs() {}

	@Override public Object[] eval(LuaStateImpl lua) {
		return lua.varArgs();
	}
}
