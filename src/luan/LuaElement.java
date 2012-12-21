package luan;


public abstract class LuaElement {

	final String toString(String fnName) {
		return location() + ": in " + (fnName==null ? "main chunk" : "function '"+fnName+"'");
	}

	abstract String location();

	public static final LuaElement JAVA = new LuaElement(){
		@Override String location() {
			return "Java";
		}
	};
}
