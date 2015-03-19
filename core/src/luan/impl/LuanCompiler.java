package luan.impl;

import luan.LuanFunction;
import luan.LuanState;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.LuanTable;
import luan.Luan;
import luan.modules.JavaLuan;
import luan.modules.PackageLuan;
import java.util.Map;


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compile(LuanState luan,LuanSource src,LuanTable env,boolean allowExpr) throws LuanException {
		if( env==null )
			env = Luan.newTable();
		UpValue.Getter envGetter = new UpValue.ValueGetter(env);
		LuanParser parser = new LuanParser(src,envGetter);
		parser.addVar( "java", JavaLuan.javaFn );
		parser.addVar( "require", PackageLuan.requireFn );
		FnDef fnDef = parse(luan,parser,allowExpr);
		final LuanStateImpl luanImpl = (LuanStateImpl)luan;
		return new Closure(luanImpl,fnDef);
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
