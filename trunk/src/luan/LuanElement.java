package luan;


public abstract class LuanElement {

	final String toString(String fnName) {
		return location() + ": in " + (fnName==null ? "main chunk" : "function '"+fnName+"'");
	}

	abstract String location();

	public static final LuanElement JAVA = new LuanElement(){
		@Override String location() {
			return "Java";
		}
	};

	public static final LuanElement COMPILER = new LuanElement(){
		@Override String location() {
			return "Compiler";
		}
	};
}
