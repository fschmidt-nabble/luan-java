package luan.interp;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.LuanTable;
import luan.parser.ParseException;
import java.util.Map;


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compileModule(LuanState luan,LuanSource src) throws LuanException {
		UpValue.Getter envGetter = new UpValue.EnvGetter();
		LuanParser parser = new LuanParser(src,envGetter);
		for( Map.Entry<Object,Object> entry : luan.global() ) {
			Object key = entry.getKey();
			if( key instanceof String )
				parser.addVar( (String)key, entry.getValue() );
		}
		try {
			FnDef fnDef = parser.RequiredModule();
			final Closure c = new Closure((LuanStateImpl)luan,fnDef);
			return new LuanFunction() {
				@Override public Object call(LuanState luan,Object[] args) throws LuanException {
					Object rtn = c.call(luan,args);
					if( rtn instanceof Object[] && ((Object[])rtn).length==0 )
						rtn = c.upValues()[0].get();
					return rtn;
				}
			};
		} catch(ParseException e) {
//e.printStackTrace();
			LuanElement le = new LuanSource.CompilerElement(src);
			throw luan.bit(le).exception( e.getFancyMessage() );
		}
	}

	public static LuanFunction compileInteractive(LuanState luan,LuanSource src) throws LuanException {
		UpValue.Getter envGetter = UpValue.globalGetter;
		LuanParser parser = new LuanParser(src,envGetter);
		try {
			FnDef fnDef = parser.Expressions();
			if( fnDef == null )
				fnDef = parser.RequiredModule();
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
