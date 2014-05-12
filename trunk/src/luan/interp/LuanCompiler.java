package luan.interp;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.LuanTable;
import luan.parser.ParseException;


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compile(LuanState luan,LuanSource src,LuanTable env) throws LuanException {
		UpValue.Getter envGetter = env!=null ? new UpValue.ValueGetter(env) : new UpValue.EnvGetter();
		try {
			FnDef fnDef = LuanParser.parse(src,envGetter);
			return new Closure((LuanStateImpl)luan,fnDef);
		} catch(ParseException e) {
//e.printStackTrace();
			LuanElement le = new LuanSource.CompilerElement(src);
			throw luan.bit(le).exception( e.getFancyMessage() );
		}
	}

	public static LuanState newLuanState() {
		return new LuanStateImpl();
	}
}
