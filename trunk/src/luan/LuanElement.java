package luan;


public abstract class LuanElement {

	final String toString(String fnName) {
		String s = location();
		if( fnName != null )
			s += ": in function '" + fnName + "'";
		return s;
	}

	abstract String location();

	public static final LuanElement JAVA = new LuanElement(){
		@Override String location() {
			return "Java";
		}
	};
}
