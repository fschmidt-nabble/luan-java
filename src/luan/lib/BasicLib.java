package luan.lib;

import java.io.FileReader;
import java.io.IOException;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import luan.Lua;
import luan.LuaState;
import luan.LuaTable;
import luan.LuaFunction;
import luan.LuaJavaFunction;
import luan.LuaException;
import luan.interp.LuaParser;
import luan.interp.Expressions;
import luan.interp.Stmt;


public class BasicLib {

	public static void register(LuaState lua) {
		LuaTable t = lua.env();
		add( t, "print", new Object[0].getClass() );
		add( t, "type", Object.class );
	}

	private static void add(LuaTable t,String method,Class<?>... parameterTypes) {
		try {
			t.set( method, new LuaJavaFunction(BasicLib.class.getMethod(method,parameterTypes),null) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void print(Object... args) {
		for( int i=0; i<args.length; i++ ) {
			if( i > 0 )
				System.out.print('\t');
			System.out.print( Lua.toString(args[i]) );
		}
		System.out.println();
	}

	public static String type(Object obj) {
		return Lua.type(obj);
	}

	public static LuaFunction load(String ld) throws LuaException {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(ld);
		if( result.hasErrors() )
			throw new LuaException( ErrorUtils.printParseErrors(result) );
		Object resultValue = result.resultValue;
		if( resultValue instanceof Expressions ) {
			final Expressions expressions = (Expressions)resultValue;
			return new LuaFunction() {
				public Object[] call(LuaState lua,Object... args) throws LuaException {
					return expressions.eval(lua).toArray();
				}
			};
		}
		final Stmt stmt = (Stmt)resultValue;
		return new LuaFunction() {
			public Object[] call(LuaState lua,Object... args) throws LuaException {
				stmt.eval(lua);
				return LuaFunction.EMPTY_RTN;
			}
		};
	}

	public static String readFile(String fileName) throws IOException {
		StringBuilder sb = new StringBuilder();
		FileReader in = new FileReader(fileName);
		char[] buf = new char[8192];
		int n;
		while( (n=in.read(buf)) != -1 ) {
			sb.append(buf,0,n);
		}
		return sb.toString();
	}

	public static LuaFunction loadFile(String fileName) throws LuaException,IOException {
		return load(readFile(fileName));
	}

}
