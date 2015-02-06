package luan.impl;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.LuanTable;
import luan.Luan;
import java.util.Map;


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compile(LuanState luan,LuanSource src,LuanTable env,boolean allowExpr) throws LuanException {
		boolean passedEnv = env != null;
		if( !passedEnv )
			env = Luan.newTable();
		UpValue.Getter envGetter = new UpValue.ValueGetter(env);
		LuanParser parser = new LuanParser(src,envGetter);
		FnDef fnDef = parse(luan,parser,allowExpr);
		final LuanStateImpl luanImpl = (LuanStateImpl)luan;
		final Closure c = new Closure(luanImpl,fnDef);
		if( passedEnv )
			return c;
		return new LuanFunction() {
			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				Object rtn = c.call(luan,args);
				if( rtn instanceof Object[] && ((Object[])rtn).length==0 )
					rtn = c.upValues()[0].get();
				return rtn;
			}
		};
	}

	private static FnDef parse(LuanState luan,LuanParser parser,boolean allowExpr) throws LuanException {
		try {
			if( allowExpr ) {
				FnDef fnDef = parser.Expression();
				if( fnDef != null )
					return fnDef;
			}
			return parser.RequiredModule();
		} catch(ParseException e) {
//e.printStackTrace();
			LuanElement le = new LuanSource.CompilerElement(parser.source);
			throw luan.bit(le).exception( e.getFancyMessage() );
		}
	}

	public static LuanState newLuanState() {
		return new LuanStateImpl();
	}
}
