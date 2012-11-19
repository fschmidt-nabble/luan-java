package luan.interp;

import java.util.Scanner;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.errors.ErrorUtils;
import luan.Lua;
import luan.LuaState;


public class LuaParser extends BaseParser<Object> {

	public Rule Target() {
		return Sequence(ConstExpr(), EOI);
	}

	public Rule ConstExpr() {
		return Sequence(
			Const(),
			push(new ConstExpr(pop()))
		);
	}

	public Rule Const() {
		return FirstOf(
			NilConst(),
			BinaryConst(),
			NumberConst()
		);
	}

	public Rule NilConst() {
		return Sequence(
			String("nil"),
			push(null)
		);
	}

	public Rule BinaryConst() {
		return FirstOf(
			TrueConst(),
			FalseConst()
		);
	}

	public Rule TrueConst() {
		return Sequence(
			String("true"),
			push(true)
		);
	}

	public Rule FalseConst() {
		return Sequence(
			String("false"),
			push(false)
		);
	}

	public Rule NumberConst() {
		return Sequence(
			Number(),
			push(Double.parseDouble(match()))
		);
	}

	public Rule Number() {
		return FirstOf(
			Sequence(
				Int(),
				Optional(
					Ch('.'),
					Optional(Int())
				),
				NumberExp()
			),
			Sequence(
				Ch('.'),
				Int(),
				NumberExp()
			)
		);
	}

	public Rule NumberExp() {
		return Optional(
			IgnoreCase('e'),
			Optional(AnyOf("+-")),
			Int()
		);
	}

    Rule Int() {
        return OneOrMore(Digit());
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

	// for testing
	public static void main(String[] args) throws Exception {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		while( true ) {
            String input = new Scanner(System.in).nextLine();
			ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(input);
			if( result.hasErrors() ) {
				System.out.println("Parse Errors:\n" + ErrorUtils.printParseErrors(result));
			} else {
				Expr expr = (Expr)result.resultValue;
				LuaState lua = new LuaState();
				Object val = expr.eval(lua);
				System.out.println("Result: "+Lua.toString(val));
			}
		}
	}
}
