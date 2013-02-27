package luan.interp;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import luan.LuanFunction;
import luan.LuanState;
import luan.LuanException;
import luan.LuanSource;
import luan.LuanElement;
import luan.LuanTable;


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compile(LuanState luan,LuanSource src,LuanTable env) throws LuanException {
		UpValue.Getter envGetter = env!=null ? new UpValue.ValueGetter(env) : new UpValue.EnvGetter();
		LuanParser parser = Parboiled.createParser(LuanParser.class,src,envGetter);
		ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(src.text);
//		ParsingResult<?> result = new TracingParseRunner(parser.Target()).run(src);
		if( result.hasErrors() )
			throw luan.COMPILER.exception( ErrorUtils.printParseErrors(result) );
		FnDef fnDef = (FnDef)result.resultValue;
		return new Closure((LuanStateImpl)luan,fnDef);
	}

	public static LuanState newLuanState() {
		return new LuanStateImpl();
	}
}
