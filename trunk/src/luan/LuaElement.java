package luan;


public abstract class LuaElement {
	abstract String toString(String fnName);

	final String function(String fnName) {
		return fnName==null ? "main chunk" : "function '"+fnName+"'";
	}

	public static final LuaElement JAVA = new LuaElement(){
		@Override public String toString(String fnName) {
			return "java: in " + function(fnName);
		}
	};
}
