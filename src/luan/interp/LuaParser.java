package luan.interp;

import java.util.Scanner;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.errors.ErrorUtils;
import luan.Lua;
import luan.LuaNumber;
import luan.LuaState;


public class LuaParser extends BaseParser<Object> {

	Rule Target() {
		return Sequence(ConstExpr(), EOI);
	}

	Rule ConstExpr() {
		return Sequence(
			Const(),
			push(new ConstExpr(pop()))
		);
	}

	Rule Const() {
		return FirstOf(
			NilConst(),
			BooleanConst(),
			NumberConst(),
			StringConst()
		);
	}

	Rule NilConst() {
		return Sequence(
			String("nil"),
			push(null)
		);
	}

	Rule BooleanConst() {
		return FirstOf(
			Sequence(
				String("true"),
				push(true)
			),
			Sequence(
				String("false"),
				push(false)
			)
		);
	}

	Rule NumberConst() {
		return Sequence(
			Number(),
			push(new LuaNumber((Double)pop()))
		);
	}

	Rule Number() {
		return FirstOf(
			Sequence(
				IgnoreCase("0x"),
				OneOrMore(HexDigit()),
				push((double)Long.parseLong(match(),16))
			),
			Sequence(
				DecNumber(),
				push(Double.parseDouble(match()))
			)
		);
	}

	Rule DecNumber() {
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

	Rule NumberExp() {
		return Optional(
			IgnoreCase('e'),
			Optional(AnyOf("+-")),
			Int()
		);
	}

	Rule Int() {
		return OneOrMore(Digit());
	}

	Rule HexDigit() {
		return FirstOf(
			Digit(),
			AnyOf("abcdefABCDEF")
		);
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

	Rule StringConst() {
		return FirstOf(
			QuotedString('"'),
			QuotedString('\'')
		);
	}

	Rule QuotedString(char quote) {
		return Sequence(
			Ch(quote),
			push(new StringBuffer()),
			ZeroOrMore(
				FirstOf(
					Sequence(
						NoneOf("\\\n"+quote),
						append(matchedChar())
					),
					EscSeq()
				)
			),
			Ch(quote),
			push(((StringBuffer)pop()).toString())
		);
	}

	Rule EscSeq() {
		return Sequence(
			Ch('\\'),
			FirstOf(
				Sequence( Ch('b'), append('\b') ),
				Sequence( Ch('f'), append('\f') ),
				Sequence( Ch('n'), append('\n') ),
				Sequence( Ch('r'), append('\r') ),
				Sequence( Ch('t'), append('\t') ),
				Sequence( Ch('\\'), append('\\') ),
				Sequence( Ch('"'), append('"') ),
				Sequence( Ch('\''), append('\'') )
			)
		);
	}

	boolean append(char ch) {
		StringBuffer sb = (StringBuffer)peek();
		sb.append(ch);
		return true;
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
