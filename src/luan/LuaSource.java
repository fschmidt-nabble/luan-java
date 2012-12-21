package luan;


public final class LuaSource {
	public final String name;
	public final String text;

	public LuaSource(String name,String text) {
		this.name = name;
		this.text = text;
	}

	public static final class Element extends LuaElement {
		public final LuaSource source;
		public final int start;
		public final int end;

		public Element(LuaSource source,int start,int end) {
			if( source==null )
				throw new NullPointerException("source is null");
			this.source = source;
			this.start = start;
			this.end = end;
		}

		public String text() {
			return source.text.substring(start,end);
		}

		@Override String location() {
			return source.name + ':' + lineNumber();
		}

		private int lineNumber() {
			int line = 0;
			int i = -1;
			do {
				line++;
				i = source.text.indexOf('\n',i+1);
			} while( i != -1 && i < start );
			return line;
		}

	}
}
