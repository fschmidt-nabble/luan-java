package luan.interp;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import luan.LuaFunction;
import luan.LuaState;
import luan.LuaException;
import luan.LuaSource;
import luan.LuaElement;


public final class LuaCompiler {
	private LuaCompiler() {}  // never

	public static LuaFunction compile(LuaState lua,LuaSource src) throws LuaException {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		parser.source = src;
		ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(src.text);
//		ParsingResult<?> result = new TracingParseRunner(parser.Target()).run(src);
		if( result.hasErrors() )
			throw new LuaException( lua, null, ErrorUtils.printParseErrors(result) );
		Object resultValue = result.resultValue;
		if( resultValue instanceof Expressions ) {
			final Expressions expressions = (Expressions)resultValue;
			return new LuaFunction() {
				public Object[] call(LuaState lua,Object[] args) throws LuaException {
					return expressions.eval((LuaStateImpl)lua);
				}
			};
		}
		Chunk chunk = (Chunk)resultValue;
		return chunk.newClosure((LuaStateImpl)lua);
	}

	public static LuaState newLuaState() {
		return new LuaStateImpl();
	}
}
