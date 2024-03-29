package luan;


public final class LuanSource {
	public final String name;
	public final String text;

	public LuanSource(String name,String text) {
		this.name = name;
		this.text = text;
	}

	public static final class CompilerElement extends LuanElement {
		public final LuanSource source;

		public CompilerElement(LuanSource source) {
			if( source==null )
				throw new NullPointerException("source is null");
			this.source = source;
		}

		@Override String location() {
			return "Compiling " + source.name;
		}
	}

	public static final class Element extends LuanElement {
		public final LuanSource source;
		public final int start;
		public final int end;
		private final String text;

		public Element(LuanSource source,int start,int end) {
			this(source,start,end,null);
		}

		public Element(LuanSource source,int start,int end,String text) {
			if( source==null )
				throw new NullPointerException("source is null");
			this.source = source;
			this.start = start;
			while( end > 0 && Character.isWhitespace(source.text.charAt(end-1)) ) {
				end--;
			}
			this.end = end;
			this.text = text;
		}

		public String text() {
			return text!=null ? text : source.text.substring(start,end);
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
