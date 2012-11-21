package luan.interp;

import java.util.List;
import java.util.ArrayList;
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
		return Sequence(Spaces(), Expr(), EOI);
	}

	Rule Expr() {
		return OrExpr();
	}

	Rule OrExpr() {
		return Sequence(
			AndExpr(),
			ZeroOrMore( "or", Spaces(), AndExpr(), push( new OrExpr((Expr)pop(1),(Expr)pop()) ) )
		);
	}

	Rule AndExpr() {
		return Sequence(
			RelExpr(),
			ZeroOrMore( "and", Spaces(), RelExpr(), push( new AndExpr((Expr)pop(1),(Expr)pop()) ) )
		);
	}

	Rule RelExpr() {
		return Sequence(
			ConcatExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( "==", Spaces(), ConcatExpr(), push( new EqExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( "~=", Spaces(), ConcatExpr(), push( new NotExpr(new EqExpr((Expr)pop(1),(Expr)pop())) ) ),
					Sequence( "<=", Spaces(), ConcatExpr(), push( new LeExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( ">=", Spaces(), ConcatExpr(), push( new LeExpr((Expr)pop(),(Expr)pop()) ) ),
					Sequence( "<", Spaces(), ConcatExpr(), push( new LtExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( ">", Spaces(), ConcatExpr(), push( new LtExpr((Expr)pop(),(Expr)pop()) ) )
				)
			)
		);
	}

	Rule ConcatExpr() {
		return Sequence(
			SumExpr(),
			Optional( "..", Spaces(), ConcatExpr(), push( new ConcatExpr((Expr)pop(1),(Expr)pop()) ) )
		);
	}

	Rule SumExpr() {
		return Sequence(
			TermExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '+', Spaces(), TermExpr(), push( new AddExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( '-', Spaces(), TermExpr(), push( new SubExpr((Expr)pop(1),(Expr)pop()) ) )
				)
			)
		);
	}

	Rule TermExpr() {
		return Sequence(
			UnaryExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '*', Spaces(), UnaryExpr(), push( new MulExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( '/', Spaces(), UnaryExpr(), push( new DivExpr((Expr)pop(1),(Expr)pop()) ) ),
					Sequence( '%', Spaces(), UnaryExpr(), push( new ModExpr((Expr)pop(1),(Expr)pop()) ) )
				)
			)
		);
	}

	Rule UnaryExpr() {
		return FirstOf(
			Sequence( '#', Spaces(), PowExpr(), push( new LenExpr((Expr)pop()) ) ),
			Sequence( '-', Spaces(), PowExpr(), push( new UnmExpr((Expr)pop()) ) ),
			Sequence( "not", Spaces(), PowExpr(), push( new NotExpr((Expr)pop()) ) ),
			PowExpr()
		);
	}

	Rule PowExpr() {
		return Sequence(
			SingleExpr(),
			Optional( '^', Spaces(), PowExpr(), push( new PowExpr((Expr)pop(1),(Expr)pop()) ) )
		);
	}

	Rule SingleExpr() {
		return FirstOf(
			TableExpr(),
			PrefixExpr(),
			LiteralExpr()
		);
	}

	Rule TableExpr() {
		return Sequence(
			'{', Spaces(),
			push( new ArrayList<TableExpr.Field>() ),
			push( 1.0 ),  // counter
			Optional(
				Field(),
				ZeroOrMore(
					FieldSep(),
					Field()
				),
				Optional( FieldSep() )
			),
			'}', Spaces(),
			push( newTableExpr() )
		);
	}

	TableExpr newTableExpr() {
		pop();  // counter
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)pop();
		return new TableExpr(list.toArray(new TableExpr.Field[0]));
	}

	Rule FieldSep() {
		return Sequence( AnyOf(",;"), Spaces() );
	}

	Rule Field() {
		return FirstOf(
			Sequence(
				FirstOf( SubExpr(), Name() ),
				'=', Spaces(), Expr(),
				addField()
			),
			Sequence(
				Expr(),
				addIndexedField()
			)
		);
	}

	boolean addField() {
		TableExpr.Field field = new TableExpr.Field( (Expr)pop(1), (Expr)pop() );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	boolean addIndexedField() {
		Expr val = (Expr)pop();
		double i = (Double)pop();
		TableExpr.Field field = new TableExpr.Field( new ConstExpr(new LuaNumber(i)), val );
		push( i + 1 );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	Rule PrefixExpr() {
		return Sequence(
			FirstOf(
				Sequence( '(', Spaces(), Expr(), ')', Spaces() ),
				Var()
			),
			ZeroOrMore(
				FirstOf(
					SubExpr(),
					Sequence( '.', Spaces(), Name() )
				),
				push( new GetExpr( (Expr)pop(1), (Expr)pop() ) )
			)
		);
	}

	Rule SubExpr() {
		return Sequence( '[', Spaces(), Expr(), ']', Spaces() );
	}

	Rule Var() {
		return Sequence(
			Name(),
			push( new GetExpr( EnvExpr.INSTANCE, (Expr)pop() ) )
		);
	}

	Rule Name() {
		return Sequence(
			Sequence(
				NameStart(),
				ZeroOrMore(
					FirstOf( NameStart(), Digit() )
				)
			),
			push( new ConstExpr(match()) ),
			Spaces()
		);
	}

	Rule NameStart() {
		return FirstOf(
			CharRange('a', 'z'),
			CharRange('A', 'Z'),
			'_'
		);
	}

	Rule LiteralExpr() {
		return Sequence(
			Literal(), Spaces(),
			push(new ConstExpr(pop()))
		);
	}

	Rule Literal() {
		return FirstOf(
			NilLiteral(),
			BooleanLiteral(),
			NumberLiteral(),
			StringLiteral()
		);
	}

	Rule NilLiteral() {
		return Sequence( "nil", push(null) );
	}

	Rule BooleanLiteral() {
		return FirstOf(
			Sequence( "true", push(true) ),
			Sequence( "false", push(false) )
		);
	}

	Rule NumberLiteral() {
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
				Optional( '.', Optional(Int()) ),
				NumberExp()
			),
			Sequence( '.', Int(), NumberExp() )
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

	Rule StringLiteral() {
		return FirstOf(
			QuotedString('"'),
			QuotedString('\'')
		);
	}

	Rule QuotedString(char quote) {
		return Sequence(
			quote,
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
			quote,
			push(((StringBuffer)pop()).toString())
		);
	}

	Rule EscSeq() {
		return Sequence(
			'\\',
			FirstOf(
				Sequence( 'a', append('\u0007') ),
				Sequence( 'b', append('\b') ),
				Sequence( 'f', append('\f') ),
				Sequence( 'n', append('\n') ),
				Sequence( 'r', append('\r') ),
				Sequence( 't', append('\t') ),
				Sequence( 'v', append('\u000b') ),
				Sequence( '\\', append('\\') ),
				Sequence( '"', append('"') ),
				Sequence( '\'', append('\'') ),
				Sequence(
					'x',
					Sequence( HexDigit(), HexDigit() ),
					append( (char)Integer.parseInt(match(),16) )
				),
				Sequence(
					Sequence(
						Digit(),
						Optional(
							Digit(),
							Optional(
								Digit()
							)
						)
					),
					append( (char)Integer.parseInt(match()) )
				)
			)
		);
	}

	boolean append(char ch) {
		StringBuffer sb = (StringBuffer)peek();
		sb.append(ch);
		return true;
	}

	public Rule Spaces() {
		return ZeroOrMore(AnyOf(" \t"));
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
