package luan;

import java.util.List;
import java.util.Scanner;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import luan.interp.Expressions;
import luan.interp.Stmt;
import luan.interp.LuaParser;
import luan.lib.BasicLib;


public class CmdLine {

	public static void main(String[] args) throws Exception {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		LuaState lua = new LuaState();
		while( true ) {
			System.out.print("> ");
			String input = new Scanner(System.in).nextLine();
			ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(input);
			if( result.hasErrors() ) {
				System.out.println("Parse Errors:\n" + ErrorUtils.printParseErrors(result));
			} else {
				Object resultValue = result.resultValue;
				if( resultValue instanceof Expressions ) {
					Expressions expressions = (Expressions)resultValue;
					List vals = expressions.eval(lua);
					if( !vals.isEmpty() )
						BasicLib.print( vals.toArray() );
				} else {
					Stmt stmt = (Stmt)resultValue;
					stmt.eval(lua);
				}
			}
		}
	}
}
