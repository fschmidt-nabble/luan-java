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


public final class LuanCompiler {
	private LuanCompiler() {}  // never

	public static LuanFunction compile(LuanState lua,LuanSource src) throws LuanException {
		LuanParser parser = Parboiled.createParser(LuanParser.class);
		parser.source = src;
		ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(src.text);
//		ParsingResult<?> result = new TracingParseRunner(parser.Target()).run(src);
		if( result.hasErrors() )
			throw new LuanException( lua, null, ErrorUtils.printParseErrors(result) );
		Object resultValue = result.resultValue;
		if( resultValue instanceof Expressions ) {
			final Expressions expressions = (Expressions)resultValue;
			return new LuanFunction() {
				public Object[] call(LuanState lua,Object[] args) throws LuanException {
					return expressions.eval((LuanStateImpl)lua);
				}
			};
		}
		Chunk chunk = (Chunk)resultValue;
		return chunk.newClosure((LuanStateImpl)lua);
	}

	public static LuanState newLuaState() {
		return new LuanStateImpl();
	}
}
