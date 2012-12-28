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

	public static LuanFunction compile(LuanState luan,LuanSource src) throws LuanException {
		LuanParser parser = Parboiled.createParser(LuanParser.class);
		parser.source = src;
		ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(src.text);
//		ParsingResult<?> result = new TracingParseRunner(parser.Target()).run(src);
		if( result.hasErrors() )
			throw new LuanException( luan, null, ErrorUtils.printParseErrors(result) );
		Chunk chunk = (Chunk)result.resultValue;
		return new Closure((LuanStateImpl)luan,chunk);
	}

	public static LuanState newLuanState() {
		return new LuanStateImpl();
	}
}
